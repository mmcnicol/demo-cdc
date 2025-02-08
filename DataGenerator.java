@Singleton
public class DataGenerator {

    @Resource(lookup = "java:/YourDataSource")
    private DataSource dataSource;

    @Schedule(hour = "*", minute = "*", second = "*/30", persistent = false)
    public void generateData() {
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

            stmt.setString(1, eventType);
            stmt.setString(2, xmlDocument);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}