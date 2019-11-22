package org.axonframework.extensions.tracing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.mockito.Mockito.*;

import brave.ScopedSpan;
import brave.Tracing;
import brave.propagation.StrictScopeDecorator;
import brave.propagation.ThreadLocalCurrentTraceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.axonframework.queryhandling.GenericQueryResponseMessage;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.QueryResponseMessage;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import zipkin2.Span;

/**
 * Test class for the {@link TracingQueryGateway}.
 *
 * @author Christophe Bouhier
 * @author Steven van Beelen
 */
public class TracingQueryGatewayTest {

    private QueryBus mockQueryBus;

    private TracingQueryGateway testSubject;

    private QueryResponseMessage<String> answer;

    private Tracing tracing;
    private List<Span> spans = new ArrayList<>();

    @Before
    public void before() {
        mockQueryBus = mock(QueryBus.class);
        tracing = Tracing
            .newBuilder()
            .localServiceName("axon-tracing")
            .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder().
                addScopeDecorator(StrictScopeDecorator.create()).build())
            .spanReporter(spans::add)
            .build();

        testSubject = TracingQueryGateway.builder()
            .tracer(tracing)
            .delegateQueryBus(mockQueryBus)
            .build();

        answer = new GenericQueryResponseMessage<>("answer");
        when(mockQueryBus.query(ArgumentMatchers.any(QueryMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(answer));
    }

    @After
    public void close() {
        tracing.close();
        reset(mockQueryBus);
        spans.clear();
    }

    @Test
    public void test_query_with_span_ongoing() throws Exception {
        //noinspection unchecked
        when(mockQueryBus.query(any(QueryMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(answer));

        final ScopedSpan ongoingSpan = Tracing.currentTracer().startScopedSpan("test");

        CompletableFuture<String> query = testSubject.query("query", "Query", String.class);
        assertThat(query.get(), CoreMatchers.is("answer"));

        ongoingSpan.finish();

        assertThat(spans.size(), is(2));
        assertThat(spans.get(0).name(), is("query"));
        assertThat(spans.get(0).parentId(), is(spans.get(1).traceId()));
        System.out.println("[" + String.join(",", spans.stream().map(span -> span.toString()).collect(
            Collectors.toList())) + "]");
        
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_query_without_span_ongoing() throws Exception {

        CompletableFuture<String> query = testSubject.query("query", "Query", String.class);
        assertThat(query.get(), CoreMatchers.is("answer"));
        assertThat(spans.size(), is(1));
        assertThat(spans.get(0).name(), is("query"));
        assertThat(spans.get(0).parentId(), is(nullValue()));
    }
}
