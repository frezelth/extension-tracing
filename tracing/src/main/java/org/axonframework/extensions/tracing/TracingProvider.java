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
import brave.propagation.TraceContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.correlation.CorrelationDataProvider;

/**
 * A {@link CorrelationDataProvider} that attaches the current tracing headers as correlation data on the message.
 * active span.
 *
 * @author Christophe Bouhier
 * @since 4.0
 */
public class TracingProvider implements CorrelationDataProvider {

  private Tracing tracing;

    /**
     * Initialize a {@link CorrelationDataProvider} implementation which uses the provided {@link Tracing} to set the
     * active span on a {@link Message}'s {@link MetaData}.
     *
     * @param tracing the {@link Tracing} used to retrieve the active span to be placed on a {@link Message}'s
     *               {@link MetaData}
     */
    public TracingProvider(Tracing tracing) {
        this.tracing = tracing;
    }

    @Override
    public Map<String, ?> correlationDataFor(Message<?> message) {
      TraceContext.Injector<Map> mapInjector = tracing.propagation().injector(Map::put);
      Map<String, String> headers = new LinkedHashMap();
      if (tracing.currentTraceContext().get() != null) {
        mapInjector.inject(tracing.currentTraceContext().get(), headers);
      }
      return headers;
    }
}