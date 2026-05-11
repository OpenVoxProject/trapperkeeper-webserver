(def i18n-version "1.0.4")
(def jetty-version "12.1.9")
(def logback-version "1.5.32")
(def slf4j-version "2.0.17")

(defproject org.openvoxproject/trapperkeeper-webserver "12.0.1-SNAPSHOT"
  :description "A jetty-based webserver implementation for use with the org.openvoxproject/trapperkeeper service framework."
  :url "https://github.com/openvoxproject/trapperkeeper-webserver"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :min-lein-version "2.9.1"

  ;; Abort when version ranges or version conflicts are detected in
  ;; dependencies. Also supports :warn to simply emit warnings.
  ;; requires lein 2.2.0+.
  :pedantic? :abort

  ;; Generally, try to keep version pins in :managed-dependencies and the libraries
  ;; this project actually uses in :dependencies, inheriting the version from
  ;; :managed-dependencies. This prevents endless version conflicts due to deps of deps.
  ;; Renovate should keep the versions largely in sync between projects.
  :managed-dependencies [[org.clojure/clojure "1.12.4"]
                         [org.clojure/java.jmx "1.1.1"]
                         [org.clojure/tools.logging "1.3.1"]
                         [org.clojure/tools.namespace "0.3.1"]
                         [org.clojure/tools.reader "1.6.0"]
                         [org.clojure/tools.macro "0.2.2"]

                         ;; Jetty Webserver (12.x with ee10 modules for Jakarta EE 10)
                         [jakarta.servlet/jakarta.servlet-api "6.0.0"]
                         [org.eclipse.jetty/jetty-server ~jetty-version]
                         [org.eclipse.jetty.ee10/jetty-ee10-servlet ~jetty-version]
                         [org.eclipse.jetty.ee10/jetty-ee10-servlets ~jetty-version]
                         [org.eclipse.jetty.ee10/jetty-ee10-webapp ~jetty-version]
                         [org.eclipse.jetty.ee10/jetty-ee10-proxy ~jetty-version]
                         [org.eclipse.jetty/jetty-jmx ~jetty-version]

                         [ch.qos.logback.access/logback-access-common "2.0.12"]
                         [ch.qos.logback.access/logback-access-jetty12 "2.0.12"]
                         [ch.qos.logback/logback-classic ~logback-version]
                         [ch.qos.logback/logback-core ~logback-version]
                         [clj-time "0.15.2"]
                         [commons-codec "1.22.0"]
                         [commons-io "2.22.0"]
                         [compojure "1.7.2"]
                         [hato "1.0.0"]
                         [javax.servlet/javax.servlet-api "4.0.1"]
                         [org.bouncycastle/bcpkix-jdk18on "1.84"]
                         [org.bouncycastle/bcpkix-fips "1.0.8"]
                         [org.bouncycastle/bc-fips "1.0.2.6"]
                         [org.bouncycastle/bctls-fips "1.0.19"]
                         [org.flatland/ordered "1.15.12"]
                         [org.openvoxproject/http-client "2.3.0"]
                         [org.openvoxproject/i18n ~i18n-version]
                         [org.openvoxproject/kitchensink "3.5.7"]
                         [org.openvoxproject/kitchensink "3.5.7" :classifier "test"]
                         [org.openvoxproject/ssl-utils "3.6.4"]
                         [org.openvoxproject/trapperkeeper "5.0.0"]
                         [org.openvoxproject/trapperkeeper "5.0.0" :classifier "test"]
                         [org.openvoxproject/trapperkeeper-filesystem-watcher "1.5.2"]
                         [org.slf4j/jul-to-slf4j ~slf4j-version]
                         [org.slf4j/log4j-over-slf4j ~slf4j-version]
                         [org.slf4j/slf4j-api ~slf4j-version]
                         [prismatic/schema "1.4.1"]
                         [ring/ring-codec "1.3.0"]
                         [ring/ring-core "1.15.4"]
                         [org.ring-clojure/ring-jakarta-servlet "1.15.4"]]
  
  :dependencies [[org.clojure/clojure]
                 [org.clojure/java.jmx]
                 [org.clojure/tools.logging]

                 ;; Jetty Webserver (12.x with ee10 modules for Jakarta EE 10)
                 [jakarta.servlet/jakarta.servlet-api]
                 [org.eclipse.jetty/jetty-server]
                 [org.eclipse.jetty.ee10/jetty-ee10-servlet]
                 [org.eclipse.jetty.ee10/jetty-ee10-servlets]
                 [org.eclipse.jetty.ee10/jetty-ee10-webapp]
                 [org.eclipse.jetty.ee10/jetty-ee10-proxy]
                 [org.eclipse.jetty/jetty-jmx]

                 [ch.qos.logback.access/logback-access-common]
                 [ch.qos.logback.access/logback-access-jetty12]
                 [ch.qos.logback/logback-classic]
                 [ch.qos.logback/logback-core]
                 [org.flatland/ordered]
                 [org.openvoxproject/i18n]
                 [org.openvoxproject/kitchensink]
                 [org.openvoxproject/ssl-utils]
                 [org.openvoxproject/trapperkeeper]
                 [org.openvoxproject/trapperkeeper-filesystem-watcher]
                 [org.slf4j/jul-to-slf4j]
                 [prismatic/schema]
                 [ring/ring-codec]
                 [org.ring-clojure/ring-jakarta-servlet]]

  :source-paths  ["src"]
  :java-source-paths  ["java"]

  :plugins [[org.openvoxproject/i18n ~i18n-version]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/CLOJARS_USERNAME
                                     :password :env/CLOJARS_PASSWORD
                                     :sign-releases false}]]

  ;; By declaring a classifier here and a corresponding profile below we'll get an additional jar
  ;; during `lein jar` that has all the code in the test/ directory. Downstream projects can then
  ;; depend on this test jar using a :classifier in their :dependencies to reuse the test utility
  ;; code that we have.
  :classifiers [["test" :testutils]]

  :test-paths ["test/clj"]

  :profiles {:shared {:source-paths ["examples/multiserver_app/src"
                                     "examples/ring_app/src"
                                     "examples/servlet_app/src/clj"
                                     "examples/war_app/src"
                                     "examples/webrouting_app/src"]
                      :java-source-paths ["examples/servlet_app/src/java"
                                          "test/java"]
                      :resource-paths ["dev-resources"]
                      :dependencies [[compojure]
                                     [hato]
                                     [org.clojure/tools.namespace]
                                     [org.openvoxproject/http-client]
                                     [org.openvoxproject/kitchensink :classifier "test"]
                                     [org.openvoxproject/trapperkeeper :classifier "test"]
                                     [ring/ring-core]
                                     ]}
             :dev-only {:dependencies [[org.bouncycastle/bcpkix-jdk18on]]
                        :jvm-opts ["-Djava.util.logging.config.file=dev-resources/logging.properties"]}
             :dev [:shared :dev-only]
             :fips-only {:dependencies [[org.bouncycastle/bcpkix-fips]
                                        [org.bouncycastle/bc-fips]
                                        [org.bouncycastle/bctls-fips]]
                         ;; this only ensures that we run with the proper profiles
                         ;; during testing. This JVM opt will be set in the puppet module
                         ;; that sets up the JVM classpaths during installation.
                         ;; Note: Jetty 12 requires Java 17+
                         :jvm-opts ~(let [version (System/getProperty "java.version")
                                          [major minor _] (clojure.string/split version #"\.")
                                          unsupported-ex (ex-info "Unsupported major Java version. Expects 17 or 21"
                                                                  {:major major
                                                                   :minor minor})]
                                      (condp = (java.lang.Integer/parseInt major)
                                        17 ["-Djava.security.properties==dev-resources/jdk17-fips-security"]
                                        21 ["-Djava.security.properties==dev-resources/jdk21-fips-security"]
                                        25 ["-Djava.security.properties==dev-resources/jdk25-fips-security"]
                                        (throw unsupported-ex)))}
             :fips [:shared :fips-only]

             ;; per https://github.com/technomancy/leiningen/issues/1907
             ;; the provided profile is necessary for lein jar / lein install
             :provided {:dependencies [[org.bouncycastle/bcpkix-jdk18on]]
                        :resource-paths ["dev-resources"]}

             :testutils {:source-paths ^:replace ["test/clj"]
                         :java-source-paths ^:replace ["test/java"]}}

  :main puppetlabs.trapperkeeper.main)
