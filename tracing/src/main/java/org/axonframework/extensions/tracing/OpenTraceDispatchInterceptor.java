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

import brave.Tracing;
import brave.propagation.TraceContext.Injector;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageDispatchInterceptor;

/**
 * A {@link MessageDispatchInterceptor} which maps the {@link brave.propagation.TraceContext} to
 * {@link org.axonframework.messaging.MetaData}.
 *
 * @author Christophe Bouhier
 * @since 4.0
 */
public class OpenTraceDispatchInterceptor implements MessageDispatchInterceptor<Message<?>> {

    private final Tracing tracing;

    /**
     * Initialize a {@link MessageDispatchInterceptor} implementation which uses the provided {@link Tracing} to map a
     * {@link brave.propagation.TraceContext} on an ingested {@link Message}.
     *
     * @param tracing the {@link Tracing} used to set a {@link brave.propagation.TraceContext} on {@link Message}s
     */
    public OpenTraceDispatchInterceptor(Tracing tracing) {
        this.tracing = tracing;
    }

    @Override
    public BiFunction<Integer, Message<?>, Message<?>> handle(List<? extends Message<?>> messages) {
        return (integer, message) -> {
            Injector<Map> mapInjector = tracing.propagation().injector(Map::put);
            Map<String, String> headers = new LinkedHashMap();
            mapInjector.inject(tracing.currentTraceContext().get(), headers);
            return message.andMetaData(headers);
        };
    }
}
