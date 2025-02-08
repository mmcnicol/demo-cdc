@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/jms/queue/HL7Queue"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue")
})
public class MessageProcessor implements MessageListener {

    @Resource(lookup = "java:/jms/queue/DeadLetterQueue")
    private Queue deadLetterQueue;

    @Inject
    private JMSContext jmsContext;

    @Override
    public void onMessage(Message message) {
        try {
            MapMessage mapMessage = (MapMessage) message;
            String operation = mapMessage.getString("operation");
            int id = mapMessage.getInt("id");
            String eventData = mapMessage.getString("eventData");

            // Process the message (e.g., log or forward to another system)
            System.out.println("Processed: " + eventData);

            // Acknowledge the message
            message.acknowledge();
        } catch (Exception e) {
            // Send to Dead Letter Queue
            jmsContext.createProducer().send(deadLetterQueue, message);
        }
    }
}