@Singleton
public class CDCPoller {

    @Resource(lookup = "java:/YourDataSource")
    private DataSource dataSource;

    @Resource(lookup = "java:/jms/queue/HL7Queue")
    private Queue hl7Queue;

    @Inject
    private JMSContext jmsContext;

    @Schedule(hour = "*", minute = "*", second = "*/10", persistent = false)
    public void pollChanges() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM cdc.fn_cdc_get_all_changes_dbo_HL7Events(?, ?, 'all')")) {
            byte[] fromLSN = getLastProcessedLSN(conn);
            byte[] toLSN = getCurrentMaxLSN(conn);

            stmt.setBytes(1, fromLSN);
            stmt.setBytes(2, toLSN);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String operation = rs.getString("__$operation");
                int id = rs.getInt("Id");
                String eventData = rs.getString("EventData");

                sendJMSMessage(operation, id, eventData);
                updateLastProcessedLSN(conn, toLSN);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private byte[] getLastProcessedLSN(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT TOP 1 LastLSN FROM ProcessedLSN ORDER BY Id DESC")) {
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getBytes("LastLSN") : sys.fn_cdc_get_min_lsn('dbo_HL7Events');
        }
    }

    private byte[] getCurrentMaxLSN(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT sys.fn_cdc_get_max_lsn() AS MaxLSN")) {
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getBytes("MaxLSN") : null;
        }
    }

    private void sendJMSMessage(String operation, int id, String eventData) {
        MapMessage message = jmsContext.createMapMessage();
        message.setString("operation", operation);
        message.setInt("id", id);
        message.setString("eventData", eventData);
        jmsContext.createProducer().send(hl7Queue, message);
    }

    private void updateLastProcessedLSN(Connection conn, byte[] lsn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO ProcessedLSN (LastLSN) VALUES (?)")) {
            stmt.setBytes(1, lsn);
            stmt.executeUpdate();
        }
    }
}