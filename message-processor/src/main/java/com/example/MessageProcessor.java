package com.example;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.ejb.MessageDriven;
import javax.ejb.ActivationConfigProperty;
import org.jboss.logging.Logger;

@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/jms/queue/testQueue"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue")
})
public class MessageProcessor implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(MessageProcessor.class);

    @Resource(lookup = "java:/jms/queue/DLQ")
    private Queue deadLetterQueue;

    @Inject
    private JMSContext jmsContext;

    @Override
    public void onMessage(Message message) {
        try {
            LOGGER.info("Received a new message...");
            MapMessage mapMessage = (MapMessage) message;
            String operation = mapMessage.getString("operation");
            int id = mapMessage.getInt("id");
            String eventData = mapMessage.getString("eventData");

            LOGGER.infov("Operation: {0}, Id: {1}", operation, id);
            LOGGER.infov("Event Data: {0}", eventData);

            // Process the message (e.g., log or forward to another system)
            LOGGER.info("Processed the message successfully");

            // Acknowledge the message
            message.acknowledge();
        } catch (Exception e) {
            LOGGER.error("Error processing message, sending to Dead Letter Queue", e);
            // Send to Dead Letter Queue
            jmsContext.createProducer().send(deadLetterQueue, message);
        }
    }
}
