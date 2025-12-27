package appender;

import ch.qos.logback.access.common.PatternLayout;
import ch.qos.logback.access.common.PatternLayoutEncoder;
import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Preben Ingvaldsen on 9/17/14.
 */
public class TestListAppender<E> extends AppenderBase<E> {
    public static List<String> list = new ArrayList<String>();
    protected PatternLayoutEncoder encoder;

    @Override
    public void start() {
        if (encoder != null) {
            encoder.start();
        }
        super.start();
    }

    protected void append(E e) {
        PatternLayout layout = (PatternLayout)encoder.getLayout();
        String s = layout.doLayout((IAccessEvent)e);
        list.add(s);
    }

    public void setEncoder(Encoder<E> encoder) {
        this.encoder = (PatternLayoutEncoder)encoder;
    }
}
