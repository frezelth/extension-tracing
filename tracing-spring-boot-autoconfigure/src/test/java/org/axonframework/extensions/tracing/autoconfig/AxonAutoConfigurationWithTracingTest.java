package org.axonframework.extensions.tracing.autoconfig;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.extensions.tracing.OpenTraceDispatchInterceptor;
import org.axonframework.extensions.tracing.OpenTraceHandlerInterceptor;
import org.axonframework.extensions.tracing.TracingCommandGateway;
import org.axonframework.extensions.tracing.TracingQueryGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.springboot.autoconfig.AxonServerAutoConfiguration;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

@EnableAutoConfiguration(exclude = {
        JmxAutoConfiguration.class,
        WebClientAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        AxonServerAutoConfiguration.class
})
@RunWith(SpringRunner.class)
@Ignore
public class AxonAutoConfigurationWithTracingTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private QueryGateway queryGateway;

    @Autowired
    private OpenTraceHandlerInterceptor openTraceHandlerInterceptor;

    @Autowired
    private OpenTraceDispatchInterceptor openTraceDispatchInterceptor;

    @Autowired
    private CommandGateway commandGateway;

    @Test
    public void testContextInitialization() {
        assertNotNull(applicationContext);
        assertThat(openTraceDispatchInterceptor, notNullValue());
        assertThat(openTraceHandlerInterceptor, notNullValue());
        assertThat(queryGateway, instanceOf(TracingQueryGateway.class));
        assertThat(commandGateway, instanceOf(TracingCommandGateway.class));
    }
}