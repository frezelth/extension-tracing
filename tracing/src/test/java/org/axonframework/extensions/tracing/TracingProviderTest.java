package org.axonframework.extensions.tracing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static java.lang.Long.toHexString;

import brave.ScopedSpan;
import brave.Tracing;
import brave.propagation.StrictScopeDecorator;
import brave.propagation.ThreadLocalCurrentTraceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MetaData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import zipkin2.Span;

public class TracingProviderTest {

    private Tracing tracing;
    private List<Span> spans = new ArrayList<>();
    private TracingProvider tracingProvider;

    @Before
    public void before() {
        tracing = Tracing
            .newBuilder()
            .localServiceName("axon-tracing")
            .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder().
                addScopeDecorator(StrictScopeDecorator.create()).build())
            .spanReporter(spans::add)
            .build();
        tracingProvider = new TracingProvider(tracing);
    }

    @After
    public void after() {
        Tracing.current().close();
        spans.clear();
    }

    @Test
    public void test_tracing_provider_with_active_span() {
        final ScopedSpan newSpan = Tracing.currentTracer().startScopedSpan("test");

        Message message = new GenericMessage<>("payload", MetaData.emptyInstance());

        Map<String, ?> correlated = tracingProvider.correlationDataFor(message);

        assertThat(correlated.get("X-B3-TraceId"), is(toHexString(newSpan.context().spanId())));
        assertThat(correlated.get("X-B3-SpanId"), is(toHexString(newSpan.context().traceId())));
        assertThat(correlated.get("X-B3-Sampled"), is("1"));
    }

    @Test
    public void testTracingProviderEmptyTraceContext() {
        Message message = new GenericMessage<>("payload", MetaData.emptyInstance());
        Map<String, ?> correlated = tracingProvider.correlationDataFor(message);
        assertThat(correlated.isEmpty(), is(true));
    }
}