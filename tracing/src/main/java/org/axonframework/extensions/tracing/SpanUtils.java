package org.axonframework.extensions.tracing;

import brave.Span;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.Message;
import org.axonframework.queryhandling.QueryMessage;

/**
 * Utility class providing methods useful for attaching information to Spans
 */
public class SpanUtils {
    static final String TAG_AXON_PAYLOAD_TYPE = "axon.message.payloadtype";
    static final String TAG_AXON_ID = "axon.message.id";
    static final String TAG_AXON_MSG_TYPE = "axon.message.type";
    static final String TAG_AXON_COMMAND_NAME = "axon.message.commandname";

    /**
     * Registers message-specific tags to the given {@code Span} based on the given {@code message}.
     *
     * @param span The Span to register the tags with
     * @param message     The message to retrieve details from
     * @return a builder with tags attached
     */
    public static void withMessageTags(Span span, Message<?> message) {
        span.tag(TAG_AXON_ID, message.getIdentifier());
        span.tag(TAG_AXON_MSG_TYPE, resolveType(message));
        span.tag(TAG_AXON_PAYLOAD_TYPE, message.getPayloadType().getName());
        if (message instanceof CommandMessage) {
            span.tag(TAG_AXON_COMMAND_NAME, ((CommandMessage<?>) message).getCommandName());
        }
    }

    /**
     * Resolves the type of message as a String, for use in description of operations.
     * <p>
     * This method will check if the message is a
     * <ul>
     * <li>{@link QueryMessage}, returning "{@code QueryMessage}",</li>
     * <li>{@link CommandMessage}, returning "{@code CommandMessage}",</li>
     * <li>{@link EventMessage}, returning "{@code EventMessage}",</li>
     * <li>otherwise returns "{@code Message}"</li>
     * </ul>
     *
     * @param message The message to resolve the type of
     * @return a String describing the type of message
     */
    public static String resolveType(Message message) {
        Class<?> clazz = Message.class;
        if (message instanceof QueryMessage) {
            clazz = QueryMessage.class;
        } else if (message instanceof CommandMessage) {
            clazz = CommandMessage.class;
        } else if (message instanceof EventMessage) {
            clazz = EventMessage.class;
        }
        return clazz.getSimpleName();
    }
}
