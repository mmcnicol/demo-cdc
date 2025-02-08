package com.example.cdc;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Queue;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jboss.logging.Logger;

@Singleton
public class CDCPoller {

    private static final Logger logger = Logger.getLogger(CDCPoller.class);

    @Resource(lookup = "java:/jdbc/TestDS")
    private DataSource dataSource;

    @Resource(lookup = "java:/jms/queue/testQueue")
    private Queue hl7Queue;

    @Inject
    private JMSContext jmsContext;

    @Schedule(hour = "*", minute = "*", second = "*/10", persistent = false)
    public void pollChanges() {
        logger.info("Polling for changes...");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM cdc.fn_cdc_get_all_changes_dbo_HL7Events(?, ?, ?)")) { // Updated query

            // just testing...
            //sendJMSMessage("insert", 2, "event data...");

            byte[] fromLSN = getLastProcessedLSN(conn);
            byte[] toLSN = getCurrentMaxLSN(conn);

            // just testing...
            //byte[] toLSN = HexFormat.of().parseHex("0x00000000000000000000"); // java 17
            //byte[] toLSN = hexStringToByteArray("00000000000000000000");

            String captureInstance = "all"; // Assuming 'all' is the capture instance

            logger.infof("fromLSN: %s", bytesToHex(fromLSN));
            logger.infof("toLSN: %s", bytesToHex(toLSN));

            stmt.setBytes(1, fromLSN);
            stmt.setBytes(2, toLSN);
            stmt.setString(3, captureInstance); // Added third argument

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String operation = rs.getString("__$operation");
                int id = rs.getInt("Id");
                String eventData = rs.getString("EventData");

                String operationDescription;
                switch (operation) {
                    case "1":
                        operationDescription = "delete";
                        break;
                    case "2":
                        operationDescription = "insert";
                        break;
                    case "3":
                        operationDescription = "update (before)";
                        break;
                    case "4":
                        operationDescription = "update (after)";
                        break;
                    default:
                        operationDescription = "unknown";
                }

                logger.infof("Processing change: operation=%s, id=%d, description=%s", operation, id, operationDescription);
                sendJMSMessage(operationDescription, id, eventData);
                updateLastProcessedLSN(conn, toLSN);
            }

        } catch (SQLException e) {
            logger.error("SQL Exception occurred while polling changes: ", e);
        } catch (JMSException e) {
            logger.error("JMS Exception occurred while sending message: ", e);
        } catch (Exception e) {
            logger.error("Exception occurred: ", e);
        }
    }

    private byte[] getLastProcessedLSN(Connection conn) throws Exception {
        logger.debug("Fetching last processed LSN...");
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT TOP 1 LastLSN FROM ProcessedLSN ORDER BY Id DESC")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBytes("LastLSN");
            } else {
                // Handle the case where there is no last processed LSN
                throw new Exception("No last processed LSN found in ProcessedLSN table.");
            }
        }
    }

    private byte[] getCurrentMaxLSN(Connection conn) throws SQLException {
        logger.debug("Fetching current max LSN...");
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT sys.fn_cdc_get_max_lsn() AS MaxLSN")) {
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getBytes("MaxLSN") : null;
        }
    }

    private void sendJMSMessage(String operation, int id, String eventData) throws JMSException {
        logger.debugf("Sending JMS message: operation=%s, id=%d", operation, id);
        MapMessage message = jmsContext.createMapMessage();
        message.setString("operation", operation);
        message.setInt("id", id);
        message.setString("eventData", eventData);
        jmsContext.createProducer().send(hl7Queue, message);
    }

    private void updateLastProcessedLSN(Connection conn, byte[] lsn) throws SQLException {
        logger.debug("Updating last processed LSN...");
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO ProcessedLSN (LastLSN) VALUES (?)")) {
            stmt.setBytes(1, lsn);
            stmt.executeUpdate();
        }
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

}
