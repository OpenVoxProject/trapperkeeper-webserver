(ns puppetlabs.trapperkeeper.services.webserver.normalized-uri-helpers
  (:require [clojure.string :as str]
            [puppetlabs.i18n.core :as i18n]
            [ring.util.jakarta.servlet :as servlet]
            [schema.core :as schema])
  (:import (com.puppetlabs.trapperkeeper.services.webserver.jetty.utils
             HttpServletRequestWithAlternateRequestUri)
           (java.nio.charset StandardCharsets)
           (java.util EnumSet)
           (jakarta.servlet DispatcherType Filter)
           (jakarta.servlet.http HttpServletRequest HttpServletResponse)
           (org.eclipse.jetty.http HttpStatus)
           (org.eclipse.jetty.io Content$Sink)
           (org.eclipse.jetty.server Handler Handler$Wrapper Request Response)
           (org.eclipse.jetty.ee10.servlet FilterHolder ServletContextHandler)
           (org.eclipse.jetty.util Callback URIUtil)))

(defn- contains-relative-segment?
  "Check if a decoded URI path contains relative path segments (. or ..).
   A segment is considered relative if it is exactly '.' or '..' when
   isolated between slashes (or at start/end of path)."
  [^String path]
  (let [segments (str/split path #"/")]
    (some #(or (= % ".") (= % "..")) segments)))

(defn- normalize-uri-path-str
  "Normalize a URI path string. Returns the normalized path or throws
   IllegalArgumentException if the path contains relative segments."
  [^String uri-path]
  ;; Use Jetty's URIUtil.decodePath which handles percent-decoding and strips
  ;; path parameters (text after semicolons)
  (let [percent-decoded-uri-path (URIUtil/decodePath uri-path)]
    ;; Check for relative path segments (. or ..)
    (if (contains-relative-segment? percent-decoded-uri-path)
      (throw (IllegalArgumentException.
               ^String (i18n/trs "Invalid relative path (.. or .) in: {0}"
                                 percent-decoded-uri-path)))
      (URIUtil/compactPath percent-decoded-uri-path))))

(schema/defn ^:always-validate normalize-uri-path :- schema/Str
  "Return a 'normalized' version of the uri path represented on the incoming
  request.  The 'normalization' consists of three steps:

  1) URL (percent) decode the path, assuming any percent-encodings represent
     UTF-8 characters.

   An exception may be thrown if the request has malformed content, e.g.,
   partially-formed percent-encoded characters like '%A%B'.

   If a semicolon character, U+003B, is found during the decoding process, it
   and any following characters will be removed from the decoded path.

  2) Check the percent-decoded path for any relative path segments ('..' or
     '.').

   An IllegalArgumentException is thrown if one or more segments are found.

  3) Compact any repeated forward slash characters in a path."
  [request :- HttpServletRequest]
  (normalize-uri-path-str (.getRequestURI request)))

(defn- send-bad-request-response
  "Send an HTTP 400 Bad Request response with the given message."
  [^Response response ^Callback callback ^String message]
  (.setStatus response HttpStatus/BAD_REQUEST_400)
  (let [bytes (.getBytes message StandardCharsets/UTF_8)]
    ;; In Jetty 12, use getHeaders().put() to set Content-Length
    (-> response (.getHeaders) (.put "Content-Length" (str (alength bytes))))
    (Content$Sink/write response true bytes callback)))

(schema/defn ^:always-validate
  normalize-uri-handler :- Handler$Wrapper
  "Create a `Handler.Wrapper` which validates request URIs and rejects
  requests with invalid URIs (containing relative path segments like '..'
  or '.') with an HTTP 400 (Bad Request) response.

  Note: In Jetty 12, this handler operates at the core handler level and
  validates URIs. For servlet contexts where the request needs to be wrapped
  with a normalized URI, use `add-normalized-uri-filter-to-servlet-handler!`
  to add a servlet filter that provides the wrapped HttpServletRequest."
  []
  (proxy [Handler$Wrapper] []
    (handle [^Request request ^Response response ^Callback callback]
      (if-let [handler (.getHandler ^Handler$Wrapper this)]
        (try
          (let [uri-path (-> request (.getHttpURI) (.getPath))]
            ;; Validate the URI path - throws if invalid
            (normalize-uri-path-str uri-path)
            ;; URI is valid, pass to downstream handler
            (.handle handler request response callback))
          (catch IllegalArgumentException ex
            ;; Invalid URI - return 400 Bad Request
            (send-bad-request-response response callback (.getMessage ex))
            true))
        false))))

(schema/defn ^:always-validate normalized-uri-filter :- Filter
  "Create a servlet filter which provides a normalized request URI on to its
  downstream consumers for an incoming request.  The normalized URI would be
  returned for a 'getRequestURI' call on the HttpServletRequest parameter.
  Normalization is done per the rules described in the `normalize-uri-path`
  function.  If an error is encountered during request URI normalization, an
  HTTP 400 (Bad Request) response is returned rather than the request being
  passed on its downstream consumers."
  []
  (reify Filter
    (init [_ _])
    (doFilter [_ request response chain]
     ;; The method signature for a servlet filter has a 'request' of the
     ;; more generic 'ServletRequest' and 'response' of the more generic
     ;; 'ServletResponse'.  While we practically shouldn't see anything
     ;; but the more specific Http types for each, this code explicitly
     ;; checks to see that the requests are Http types as the URI
     ;; normalization would be irrelevant for other types.
      (if (and (instance? HttpServletRequest request)
               (instance? HttpServletResponse response))
        (if-let [normalized-uri
                 (try
                   (normalize-uri-path request)
                   (catch IllegalArgumentException ex
                     (servlet/update-servlet-response
                      response
                      {:status 400
                       :body (.getMessage ex)})
                     nil))]
          (.doFilter chain
                     (HttpServletRequestWithAlternateRequestUri.
                      request
                      normalized-uri)
                     response))
        (.doFilter chain request response)))
    (destroy [_])))

(schema/defn ^:always-validate
  add-normalized-uri-filter-to-servlet-handler!
  "Adds a servlet filter to the servlet handler which provides a normalized
  request URI on to its downstream consumers for an incoming request."
  [handler :- ServletContextHandler]
  (let [filter-holder (FilterHolder. (normalized-uri-filter))]
    (.addFilter handler
                filter-holder
                "/*"
                (EnumSet/of DispatcherType/REQUEST))))

(schema/defn ^:always-validate
  handler-maybe-wrapped-with-normalized-uri :- Handler
  "If the supplied `normalize-request-uri?` parameter is 'true', return a
  handler that validates request URIs and rejects invalid ones.

  Note: In Jetty 12, this wrapper validates URIs at the core handler level.
  For servlet contexts that need the actual normalized URI passed downstream,
  also use `add-normalized-uri-filter-to-servlet-handler!` to add a filter."
  [handler :- Handler
   normalize-request-uri? :- schema/Bool]
  (if normalize-request-uri?
    (doto (normalize-uri-handler)
      (.setHandler handler))
    handler))
