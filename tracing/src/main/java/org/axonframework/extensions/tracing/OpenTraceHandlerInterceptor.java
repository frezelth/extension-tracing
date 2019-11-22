/*
 * Copyright (c) 2010-2019. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.axonframework.extensions.tracing;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.unitofwork.UnitOfWork;


/**
 *  * A {@link MessageHandlerInterceptor} which maps the {@link MetaData} to the {@link brave.propagation.TraceContext}.
 *
 * @author Christophe Bouhier
 * @since 4.0
 */
public class OpenTraceHandlerInterceptor implements MessageHandlerInterceptor<Message<?>> {

    private final Tracing tracing;
    private final TraceContext.Extractor<Message> commandMessageExtractor;

    /**
     * Initialize a {@link MessageHandlerInterceptor} implementation which uses the provided {@link Tracing} to map span
     * information from the {@link Message} its {@link MetaData} on a {@link brave.propagation.TraceContext}.
     *
     * @param tracing the {@link Tracing} used to set a {@link brave.propagation.TraceContext} on from a {@link Message}'s {@link MetaData}
     */
    public OpenTraceHandlerInterceptor(Tracing tracing) {
        this.tracing = tracing;
        commandMessageExtractor = tracing.propagation().extractor((carrier, key) -> {
            if (carrier.getMetaData().containsKey(key)) {
                return carrier.getMetaData().get(key).toString();
            }
            return null;
        });
    }

    @Override
    public Object handle(UnitOfWork unitOfWork, InterceptorChain interceptorChain) throws Exception {
        MetaData metaData = unitOfWork.getMessage().getMetaData();

        String operationName = "handle" + SpanUtils.resolveType(unitOfWork.getMessage());
        TraceContextOrSamplingFlags extracted = commandMessageExtractor.extract(unitOfWork.getMessage());
        Span span;
        if (extracted == null) {
            span = tracing.tracer().nextSpan();
        } else {
            span = tracing.tracer().nextSpan(extracted);
        }
        span.name(operationName).kind(Span.Kind.SERVER).start();
        SpanUtils.withMessageTags(span, unitOfWork.getMessage());
        try(Tracer.SpanInScope ignored = tracing.tracer().withSpanInScope(span)) {
            unitOfWork.onCleanup(u -> span.finish());
            return interceptorChain.proceed();
        }
    }

}
