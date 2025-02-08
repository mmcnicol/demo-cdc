package com.example;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;
import org.jboss.logging.Logger;

@Singleton
public class DataGenerator {

    private static final Logger LOGGER = Logger.getLogger(DataGenerator.class);

    @Resource(lookup = "java:/jdbc/TestDS")
    private DataSource dataSource;

    @Schedule(hour = "*", minute = "*", second = "*/30", persistent = false)
    public void generateData() {
        LOGGER.info("Executing generateData()...");
        String[] eventTypes = {"MedicationOrder", "MedicationAdministration", "PatientAdmission"};
        String[] xmlDocuments = {
            "<MedicationOrder><Patient>John Doe</Patient><Medication>Paracetamol</Medication></MedicationOrder>",
            "<MedicationAdministration><Patient>Jane Doe</Patient><Medication>Ibuprofen</Medication></MedicationAdministration>",
            "<PatientAdmission><Patient>John Smith</Patient><Ward>Cardiology</Ward></PatientAdmission>"
        };

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO HL7Events (EventType, EventData) VALUES (?, ?)")) {
            String eventType = eventTypes[new Random().nextInt(eventTypes.length)];
            String xmlDocument = xmlDocuments[new Random().nextInt(xmlDocuments.length)];

            LOGGER.infov("Inserting event: {0} with data: {1}", eventType, xmlDocument);

            stmt.setString(1, eventType);
            stmt.setString(2, xmlDocument);
            stmt.executeUpdate();

            LOGGER.info("Event inserted successfully");
        } catch (SQLException e) {
            LOGGER.error("Error inserting event", e);
        }
    }
}
