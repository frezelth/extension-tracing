package org.axonframework.extensions.tracing;

import static java.lang.Long.toHexString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import brave.ScopedSpan;
import brave.Tracing;
import brave.propagation.StrictScopeDecorator;
import brave.propagation.ThreadLocalCurrentTraceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MetaData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import zipkin2.Span;

public class OpenTraceDispatchInterceptorTest {

    private OpenTraceDispatchInterceptor openTraceDispatchInterceptor;
    private List<Span> spans = new ArrayList<>();
    private Tracing tracing;

    @Before
    public void before() {
        tracing = Tracing
            .newBuilder()
            .localServiceName("axon-tracing")
            .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder().
                addScopeDecorator(StrictScopeDecorator.create()).build())
            .spanReporter(spans::add)
            .build();
        openTraceDispatchInterceptor = new OpenTraceDispatchInterceptor(tracing);
    }

    @After
    public void close() {
        Tracing.current().close();
        spans.clear();
    }

    @Test
    public void testDispatch() {
        final ScopedSpan testSpan = Tracing.currentTracer().startScopedSpan("test");

        GenericMessage<String> msg = new GenericMessage<>("Payload");
        BiFunction<Integer, Message<?>, Message<?>> handle =
                openTraceDispatchInterceptor.handle(Collections.singletonList(msg));

        Message<?> apply = handle.apply(0, msg);
        MetaData metaData = apply.getMetaData();
        metaData.entrySet().forEach(entry -> System.out.println(entry.getKey() + "->" + entry.getValue()));
        // verify that the message has the span headers attached to it
        assertThat(metaData.size(), is(3));
        assertThat(metaData.get("X-B3-TraceId"), is(toHexString(testSpan.context().spanId())));
        assertThat(metaData.get("X-B3-SpanId"), is(toHexString(testSpan.context().traceId())));
        assertThat(metaData.get("X-B3-Sampled"), is("1"));

        testSpan.finish();
        assertThat(spans.size(), is(1));
    }
}