package org.axonframework.extensions.tracing;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import brave.ScopedSpan;
import brave.Tracing;
import brave.propagation.StrictScopeDecorator;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.propagation.TraceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import zipkin2.Span;

public class OpenTraceHandlerInterceptorTest {

    private List<Span> spans = new ArrayList<>();
    private OpenTraceHandlerInterceptor openTraceDispatchInterceptor;
    private DefaultUnitOfWork<Message<?>> unitOfWork;
    private InterceptorChain mockInterceptorChain;
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
        openTraceDispatchInterceptor = new OpenTraceHandlerInterceptor(tracing);
        mockInterceptorChain = mock(InterceptorChain.class);
        unitOfWork = new DefaultUnitOfWork<>(null);
    }

    @After
    public void close() {
        Tracing.current().close();
        spans.clear();
    }

    @Test
    public void test_handle_with_span_ongoing() throws Exception {
        final ScopedSpan testSpan = Tracing.currentTracer().startScopedSpan("test");
        final TraceContext.Injector<Map> injector = Tracing.current().propagation().injector(Map::put);
        Map<String, String> metadata = new HashMap<>();
        injector.inject(tracing.currentTraceContext().get(), metadata);
        final Message message = executeHandlerInterceptor(new GenericMessage<Object>("Payload").withMetaData(metadata));

        testSpan.finish();

        assertThat(spans.size(), is(2));
        // child span completes first
        checkInterceptorSpan(spans.get(0), message);
        // check that the root span is there
        assertThat(spans.get(1).name(), is("test"));
        // and the child span pointing to it
        assertThat(spans.get(1).traceId(), is(spans.get(0).parentId()));
    }

    @Test
    public void test_handle_without_span_ongoing() throws Exception {

        Message message = executeHandlerInterceptor();

        assertThat(spans.size(), is(1));
        // only a root span is present
        assertThat(spans.get(0).name(), is("handlemessage"));
        assertThat(spans.get(0).tags().get("axon.message.id"), is(message.getIdentifier()));
        assertThat(spans.get(0).tags().get("axon.message.type"), is("Message"));
        assertThat(spans.get(0).tags().get("axon.message.payloadtype"), is("java.lang.String"));
        assertThat(spans.get(0).kind(), is(Span.Kind.SERVER));
        assertThat(spans.get(0).parentId(), is(nullValue()));
    }

    private void checkInterceptorSpan(Span span, Message message) {
        assertThat(span.name(), is("handlemessage"));
        assertThat(span.tags().get("axon.message.id"), is(message.getIdentifier()));
        assertThat(span.tags().get("axon.message.type"), is("Message"));
        assertThat(span.tags().get("axon.message.payloadtype"), is("java.lang.String"));
        assertThat(span.kind(), is(Span.Kind.SERVER));
    }

    private Message executeHandlerInterceptor() throws Exception {
        return executeHandlerInterceptor(new GenericMessage<Object>("Payload"));
    }

    private Message executeHandlerInterceptor(Message message) throws Exception {
        unitOfWork.transformMessage(m -> message);
        openTraceDispatchInterceptor.handle(unitOfWork, mockInterceptorChain);
        // Push the state, so the child span is finished.
        unitOfWork.start();
        unitOfWork.commit();
        return message;
    }
}
