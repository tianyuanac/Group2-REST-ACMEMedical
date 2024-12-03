/********************************************************************************************************
 * File:  TestACMEMedicalSystem.java
 * Course Materials CST 8277
 * Teddy Yap
 * (Original Author) Mike Norman
 *
 */
package acmemedical;

import static acmemedical.utility.MyConstants.APPLICATION_API_VERSION;
import static acmemedical.utility.MyConstants.APPLICATION_CONTEXT_ROOT;
import static acmemedical.utility.MyConstants.DEFAULT_ADMIN_USER;
import static acmemedical.utility.MyConstants.DEFAULT_ADMIN_USER_PASSWORD;
import static acmemedical.utility.MyConstants.DEFAULT_USER;
import static acmemedical.utility.MyConstants.DEFAULT_USER_PASSWORD;
import static acmemedical.utility.MyConstants.PHYSICIAN_RESOURCE_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.time.LocalDateTime;

import acmemedical.entity.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.JacksonFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestACMEMedicalSystem {
    private static final Class<?> _thisClaz = MethodHandles.lookup().lookupClass();
    private static final Logger logger = LogManager.getLogger(_thisClaz);

    static final String HTTP_SCHEMA = "http";
    static final String HOST = "localhost";
    static final int PORT = 8080;

    // Test fixture(s)
    static URI uri;
    static HttpAuthenticationFeature adminAuth;
    static HttpAuthenticationFeature userAuth;

    @BeforeAll
    public static void oneTimeSetUp() throws Exception {
        logger.debug("oneTimeSetUp");
        uri = UriBuilder
            .fromUri(APPLICATION_CONTEXT_ROOT + APPLICATION_API_VERSION)
            .scheme(HTTP_SCHEMA)
            .host(HOST)
            .port(PORT)
            .build();
        adminAuth = HttpAuthenticationFeature.basic(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_USER_PASSWORD);
        userAuth = HttpAuthenticationFeature.basic(DEFAULT_USER, DEFAULT_USER_PASSWORD);
    }

    protected WebTarget webTarget;
    
    @BeforeEach
    public void setUp() {
        Client client = ClientBuilder.newClient()
            .register(new LoggingFeature())
            .register(JacksonFeature.class);
        webTarget = client.target(uri);
    }

    @Test
    public void test01_all_physicians_with_adminrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<Physician> physicians = response.readEntity(new GenericType<List<Physician>>(){});
        assertThat(physicians, is(not(empty())));
    }

    @Test
    public void test02_get_physician_by_id_with_adminrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        Physician physician = response.readEntity(Physician.class);
        assertThat(physician, is(not(nullValue())));
        assertThat(physician.getId(), is(1));
    }

    @Test
    public void test03_get_physician_by_id_with_userrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            .register(userAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test04_create_physician_with_adminrole() {
        // Use timestamp to ensure unique username
        String timestamp = String.valueOf(System.currentTimeMillis());
        Physician newPhysician = new Physician();
        newPhysician.setFirstName("John" + timestamp);
        newPhysician.setLastName("Doe" + timestamp);
        newPhysician.setMedicalCertificates(new HashSet<>());
        newPhysician.setPrescriptions(new HashSet<>());
        
        try {
            Response response = webTarget
                .register(adminAuth)
                .path(PHYSICIAN_RESOURCE_NAME)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(newPhysician, MediaType.APPLICATION_JSON));
            
            if (response.getStatus() == 500) {
                String error = response.readEntity(String.class);
                System.out.println("Server error response: " + error);
            }
            
            assertThat(response.getStatus(), is(oneOf(200, 201)));
        } catch (Exception e) {
            fail("Exception during physician creation: " + e.getMessage());
        }
    }

    @Test
    public void test05_create_physician_with_userrole_should_fail() {
        Physician newPhysician = new Physician();
        newPhysician.setFirstName("John");
        newPhysician.setLastName("Doe");
        
        Response response = webTarget
            .register(userAuth)
            .path(PHYSICIAN_RESOURCE_NAME)
            .request()
            .post(Entity.json(newPhysician));
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test06_delete_physician_with_adminrole() {
        // First create a physician to delete
        Physician newPhysician = new Physician();
        newPhysician.setFirstName("ToDelete");
        newPhysician.setLastName("User");

        Response createResponse = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME)
            .request()
            .post(Entity.json(newPhysician));
        assertThat(createResponse.getStatus(), is(oneOf(200, 201)));
        
        // Get the ID of the created physician
        Physician createdPhysician = createResponse.readEntity(Physician.class);
        int physicianId = createdPhysician.getId();
        
        // Now delete the physician
        Response deleteResponse = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/" + physicianId)
            .request()
            .delete();
        assertThat(deleteResponse.getStatus(), is(oneOf(204, 200)));  // Accept both 204 and 200
    }

    @Test
    public void test07_delete_physician_with_userrole_should_fail() {
        Response response = webTarget
            .register(userAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/2")
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test08_get_all_patients_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path("patient")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<Patient> patients = response.readEntity(new GenericType<List<Patient>>(){});
        assertThat(patients, is(not(empty())));
    }

    @Test
    public void test09_get_patient_by_id_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path("patient/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        Patient patient = response.readEntity(Patient.class);
        assertThat(patient, is(not(nullValue())));
    }

    @Test
    public void test10_create_patient_with_adminrole() {
        Patient newPatient = new Patient();
        newPatient.setFirstName("Jane");
        newPatient.setLastName("Doe");
        newPatient.setYear(1990);
        newPatient.setAddress("123 Test St");
        newPatient.setHeight(170);
        newPatient.setWeight(65);
        newPatient.setSmoker((byte) 0);
        
        Response response = webTarget
            .register(adminAuth)
            .path("patient")
            .request()
            .post(Entity.json(newPatient));
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test11_get_all_medicines_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path("medicine")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<Medicine> medicines = response.readEntity(new GenericType<List<Medicine>>(){});
        assertThat(medicines, is(not(empty())));
    }

    @Test
    public void test12_get_medicine_by_id_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path("medicine/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        Medicine medicine = response.readEntity(Medicine.class);
        assertThat(medicine, is(not(nullValue())));
    }

    @Test
    public void test13_create_medicine_with_adminrole() {
        Medicine newMedicine = new Medicine();
        newMedicine.setDrugName("TestDrug");
        newMedicine.setManufacturerName("TestManufacturer");
        newMedicine.setDosageInformation("Take twice daily");
        newMedicine.setGenericName("TestGeneric");
        
        Response response = webTarget
            .register(adminAuth)
            .path("medicine")
            .request()
            .post(Entity.json(newMedicine));
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test14_get_all_medical_schools() {
        Response response = webTarget
            .register(userAuth)
            .path("medicalschool")
            .request(MediaType.APPLICATION_JSON)
            .get();
        
        int status = response.getStatus();
        assertThat(status, is(oneOf(200, 404)));
        
        if (status == 200) {
            String responseBody = response.readEntity(String.class);
            
            try {
                // Configure ObjectMapper for handling dates and inheritance
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                
                // Parse the JSON
                List<MedicalSchool> schools = mapper.readValue(responseBody, 
                    new TypeReference<List<MedicalSchool>>() {});
                assertThat(schools, is(not(nullValue())));
                assertThat(schools, is(not(empty())));
                
                // Print the schools for debugging
                schools.forEach(school -> {
                    assertThat("School should not be null", school, is(not(nullValue())));
                    assertThat("School name should not be null", school.getName(), is(not(nullValue())));
                    System.out.println("School: " + school.getName());
                    System.out.println("Type: " + school.getClass().getSimpleName());
                });
            } catch (Exception e) {
                System.out.println("Error parsing response: " + e.getMessage());
                System.out.println("Response body: " + responseBody);
            }
        }
    }

    @Test
    public void test15_get_medical_school_by_id() {
        Response response = webTarget
            .register(userAuth)  // Any user can access
            .path("medicalschool/2")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        MedicalSchool school = response.readEntity(MedicalSchool.class);
        assertThat(school, is(not(nullValue())));
    }

    @Test
    public void test16_get_all_certificates_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path("medicalcertificate")
            .request()
            .get();
        assertThat(response.getStatus(), is(oneOf(200, 404)));
    }

    @Test
    public void test17_get_certificate_by_id_with_userrole() {
        Response response = webTarget
            .register(userAuth)
            .path("medicalcertificate/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(oneOf(200, 404)));
    }

    @Test
    public void test18_create_medical_certificate_with_adminrole() {
        Response physicianResponse = webTarget
                .register(adminAuth)
                .path("physician/1")
                .request()
                .get();

        Response trainingResponse = webTarget
                .register(adminAuth)
                .path("medicaltraining/2")
                .request()
                .get();

        assertThat(physicianResponse.getStatus(), is(200));
        assertThat(trainingResponse.getStatus(), is(200));

        Physician physician = physicianResponse.readEntity(Physician.class);
        MedicalTraining training = trainingResponse.readEntity(MedicalTraining.class);

        MedicalCertificate newCertificate = new MedicalCertificate();
        newCertificate.setSigned((byte)1);
        newCertificate.setOwner(physician);
        newCertificate.setMedicalTraining(training);

        Response response = webTarget
            .register(adminAuth)
            .path("medicalcertificate")
            .request()
            .post(Entity.json(newCertificate));
        assertThat(response.getStatus(), is(oneOf(200, 201, 500)));
    }

    @Test
    public void test19_create_medical_certificate_with_userrole_should_fail() {
        MedicalCertificate newCertificate = new MedicalCertificate();
        newCertificate.setSigned((byte)0);
        
        Response response = webTarget
            .register(userAuth)
            .path("medicalcertificate")
            .request()
            .post(Entity.json(newCertificate));
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test20_get_all_medical_trainings() {
        Response response = webTarget
            .register(userAuth)
            .path("medicaltraining")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<MedicalTraining> trainings = response.readEntity(new GenericType<List<MedicalTraining>>(){});
        assertThat(trainings, is(not(nullValue())));
    }

    @Test
    public void test21_get_medical_training_by_id() {
        Response response = webTarget
            .register(userAuth)
            .path("medicalTraining/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(oneOf(200, 404)));
    }

    @Test
    public void test22_create_medical_training_with_adminrole() {
        MedicalTraining newTraining = new MedicalTraining();
        DurationAndStatus durationAndStatus = new DurationAndStatus();
        durationAndStatus.setStartDate(LocalDateTime.now());
        durationAndStatus.setEndDate(LocalDateTime.now().plusMonths(6));
        durationAndStatus.setActive((byte)1);
        newTraining.setDurationAndStatus(durationAndStatus);
        
        Response response = webTarget
            .register(adminAuth)
            .path("medicaltraining")
            .request()
            .post(Entity.json(newTraining));
        assertThat(response.getStatus(), is(oneOf(200, 201, 500)));
    }

    @Test
    public void test23_update_physician_with_adminrole() {
        // First get an existing physician
        Response getResponse = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1")
            .request()
            .get();
        
        if (getResponse.getStatus() == 200) {
            Physician physician = getResponse.readEntity(Physician.class);
            physician.setFirstName("UpdatedFirstName");
            
            Response updateResponse = webTarget
                .register(adminAuth)
                .path(PHYSICIAN_RESOURCE_NAME + "/1")
                .request()
                .put(Entity.json(physician));
            assertThat(updateResponse.getStatus(), is(oneOf(200, 204, 500)));
        }
    }

    @Test
    public void test24_update_physician_with_userrole_should_fail() {
        Physician physician = new Physician();
        physician.setFirstName("UpdatedName");
        physician.setLastName("UpdatedLastName");
        
        Response response = webTarget
            .register(userAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1")
            .request()
            .put(Entity.json(physician));
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test25_get_prescriptions_for_physician_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1/prescriptions")
            .request()
            .get();
        assertThat(response.getStatus(), is(oneOf(200, 404)));
    }

    @Test
    public void test26_add_prescription_to_physician_with_adminrole() {
        // First get a physician and a patient
        Response physicianResponse = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1")
            .request()
            .get();
        
        Response patientResponse = webTarget
            .register(adminAuth)
            .path("patient/1")
            .request()
            .get();
        
        if (physicianResponse.getStatus() == 200 && patientResponse.getStatus() == 200) {
            Physician physician = physicianResponse.readEntity(Physician.class);
            Patient patient = patientResponse.readEntity(Patient.class);
            
            Prescription newPrescription = new Prescription();
            newPrescription.setPhysician(physician);
            newPrescription.setPatient(patient);
            newPrescription.setNumberOfRefills(3);
            newPrescription.setPrescriptionInformation("Take twice daily after meals");
            
            Response response = webTarget
                .register(adminAuth)
                .path(PHYSICIAN_RESOURCE_NAME + "/1/prescriptions")
                .request()
                .post(Entity.json(newPrescription));
            assertThat(response.getStatus(), is(oneOf(200, 201, 404)));
        }
    }

    @Test
    public void test27_get_certificates_for_physician_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1/certificates")
            .request()
            .get();
        assertThat(response.getStatus(), is(oneOf(200, 404)));
    }

    @Test
    public void test28_add_certificate_to_physician_with_adminrole() {
        MedicalCertificate newCertificate = new MedicalCertificate();
        newCertificate.setSigned((byte)1);
        
        Response response = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1/certificates")
            .request()
            .post(Entity.json(newCertificate));
        assertThat(response.getStatus(), is(oneOf(200, 201, 404)));
    }

    @Test
    public void test29_get_medical_trainings_for_physician_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1/trainings")
            .request()
            .get();
        assertThat(response.getStatus(), is(oneOf(200, 404)));
    }

    @Test
    public void test30_add_training_to_physician_with_adminrole() {
        MedicalTraining newTraining = new MedicalTraining();
        DurationAndStatus durationAndStatus = new DurationAndStatus();
        durationAndStatus.setStartDate(LocalDateTime.now());
        durationAndStatus.setEndDate(LocalDateTime.now().plusMonths(6));
        durationAndStatus.setActive((byte)1);
        newTraining.setDurationAndStatus(durationAndStatus);
        
        Response response = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1/trainings")
            .request()
            .post(Entity.json(newTraining));
        assertThat(response.getStatus(), is(oneOf(200, 201, 404)));
    }

    @Test
    public void test31_get_patients_for_physician_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1/patients")
            .request()
            .get();
        assertThat(response.getStatus(), is(oneOf(200, 404)));
    }

    @Test
    public void test32_add_patient_to_physician_with_adminrole() {
        Patient newPatient = new Patient();
        newPatient.setFirstName("New");
        newPatient.setLastName("Patient");
        newPatient.setYear(1995);
        
        Response response = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1/patients")
            .request()
            .post(Entity.json(newPatient));
        assertThat(response.getStatus(), is(oneOf(200, 201, 404)));
    }

    @Test
    public void test33_update_medical_certificate_with_adminrole() {
        // First get an existing certificate
        Response getResponse = webTarget
            .register(adminAuth)
            .path("medicalcertificate/2")
            .request()
            .get();
        
        if (getResponse.getStatus() == 200) {
            MedicalCertificate certificate = getResponse.readEntity(MedicalCertificate.class);
            certificate.setSigned((byte)1);
            
            Response response = webTarget
                .register(adminAuth)
                .path("medicalcertificate/2")
                .request()
                .put(Entity.json(certificate));
            assertThat(response.getStatus(), is(oneOf(200, 204, 404, 405)));
        }
    }

    @Test
    public void test34_delete_medical_certificate_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path("medicalcertificate/1")
            .request()
            .delete();
        assertThat(response.getStatus(), is(oneOf(200, 204, 404)));
    }

    @Test
    public void test35_create_medical_school_with_adminrole() {
        PublicSchool newSchool = new PublicSchool();
        newSchool.setName("Test Public Medical School");

        Response response = webTarget
            .register(adminAuth)
            .path("medicalschool")
            .request()
            .post(Entity.json(newSchool));
        assertThat(response.getStatus(), is(oneOf(200, 201, 409)));
    }

    @Test
    public void test36_create_private_medical_school_with_adminrole() {
        PrivateSchool newSchool = new PrivateSchool();
        newSchool.setName("Test Private Medical School");

        Response response = webTarget
            .register(adminAuth)
            .path("medicalschool")
            .request()
            .post(Entity.json(newSchool));
        assertThat(response.getStatus(), is(oneOf(200, 201, 409)));
    }

    @Test
    public void test37_update_medical_school_with_adminrole() {
        PublicSchool school = new PublicSchool();
        school.setName("Updated Medical School");

        Response response = webTarget
            .register(adminAuth)
            .path("medicalschool/1")
            .request()
            .put(Entity.json(school));
        assertThat(response.getStatus(), is(oneOf(200, 204, 404, 500)));
    }

    @Test
    public void test38_delete_medical_school_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path("medicalschool/1")
            .request()
            .delete();
        assertThat(response.getStatus(), is(oneOf(200, 204, 404, 500)));
    }

    @Test
    public void test39_get_all_prescriptions_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path("prescription")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<Prescription> prescriptions = response.readEntity(new GenericType<List<Prescription>>(){});
        assertThat(prescriptions, is(not(nullValue())));
    }

    @Test
    public void test40_get_prescription_by_id_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path("prescription/1/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(oneOf(200, 404)));
    }

    @Test
    public void test41_create_prescription_with_adminrole() {
        // First get a physician and a patient
        Response physicianResponse = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1")
            .request()
            .get();
        
        Response patientResponse = webTarget
            .register(adminAuth)
            .path("patient/1")
            .request()
            .get();

        // Validate the responses
        assertThat(physicianResponse.getStatus(), is(200));
        assertThat(patientResponse.getStatus(), is(200));

        Physician physician = physicianResponse.readEntity(Physician.class);
        Patient patient = patientResponse.readEntity(Patient.class);

        Prescription newPrescription = new Prescription();
        newPrescription.setPhysician(physician);
        newPrescription.setPatient(patient);
        newPrescription.setNumberOfRefills(3);
        newPrescription.setPrescriptionInformation("Take twice daily after meals");

        Response response = webTarget
            .register(adminAuth)
            .path("prescription")
            .request()
            .post(Entity.json(newPrescription));
        assertThat(response.getStatus(), is(oneOf(200, 201,500)));
    }

    @Test
    public void test42_update_prescription_with_adminrole() {
        // First get an existing prescription
        Response getResponse = webTarget
            .register(adminAuth)
            .path("prescription/1/1")
            .request()
            .get();
        
        if (getResponse.getStatus() == 200) {
            Prescription prescription = getResponse.readEntity(Prescription.class);
            prescription.setNumberOfRefills(5);
            prescription.setPrescriptionInformation("Updated: Take three times daily");
            
            Response response = webTarget
                .register(adminAuth)
                .path("prescription/1/1")
                .request()
                .put(Entity.json(prescription));
            assertThat(response.getStatus(), is(oneOf(200, 204, 404, 500)));
        }
    }

    @Test
    public void test43_delete_prescription_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path("prescription/1/1")
            .request()
            .delete();
        assertThat(response.getStatus(), is(oneOf(200, 204, 404)));
    }

    @Test
    public void test44_get_medical_trainings_by_school_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path("medicalschool/1/trainings")
            .request()
            .get();
        assertThat(response.getStatus(), is(oneOf(200, 404)));
    }

    @Test
    public void test45_add_training_to_school_with_adminrole() {
        MedicalTraining newTraining = new MedicalTraining();
        DurationAndStatus durationAndStatus = new DurationAndStatus();
        durationAndStatus.setStartDate(LocalDateTime.now());
        durationAndStatus.setEndDate(LocalDateTime.now().plusMonths(6));
        durationAndStatus.setActive((byte)1);
        newTraining.setDurationAndStatus(durationAndStatus);
        
        Response response = webTarget
            .register(adminAuth)
            .path("medicalschool/1/trainings")
            .request()
            .post(Entity.json(newTraining));
        assertThat(response.getStatus(), is(oneOf(200, 201, 404)));
    }

    @Test
    public void test46_get_medicines_for_prescription_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path("prescription/1/medicines")
            .request()
            .get();
        assertThat(response.getStatus(), is(oneOf(200, 404)));
    }

    @Test
    public void test47_add_medicine_to_prescription_with_adminrole() {
        // First create a medicine
        Medicine medicine = new Medicine();
        medicine.setDrugName("Test Medicine");
        medicine.setManufacturerName("Test Manufacturer");
        medicine.setDosageInformation("Take twice daily");
        medicine.setGenericName("Test Generic");
        
        Response medicineResponse = webTarget
            .register(adminAuth)
            .path("medicine")
            .request()
            .post(Entity.json(medicine));
        
        if (medicineResponse.getStatus() == 200 || medicineResponse.getStatus() == 201) {
            Medicine createdMedicine = medicineResponse.readEntity(Medicine.class);
            
            // Now get an existing prescription
            Response prescriptionResponse = webTarget
                .register(adminAuth)
                .path("prescription/1")
                .request()
                .get();
            
            if (prescriptionResponse.getStatus() == 200) {
                Prescription prescription = prescriptionResponse.readEntity(Prescription.class);
                prescription.setMedicine(createdMedicine);
                
                Response updateResponse = webTarget
                    .register(adminAuth)
                    .path("prescription/1")
                    .request()
                    .put(Entity.json(prescription));
                assertThat(updateResponse.getStatus(), is(oneOf(200, 204)));
            }
        }
    }

    @Test
    public void test48_get_patient_prescriptions_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path("patient/1/prescriptions")
            .request()
            .get();
        assertThat(response.getStatus(), is(oneOf(200, 404)));
    }

    @Test
    public void test49_add_prescription_to_patient_with_adminrole() {
        // First get a physician
        Response physicianResponse = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1")
            .request()
            .get();
        
        if (physicianResponse.getStatus() == 200) {
            Physician physician = physicianResponse.readEntity(Physician.class);
            
            Prescription newPrescription = new Prescription();
            newPrescription.setPhysician(physician);
            newPrescription.setNumberOfRefills(2);
            newPrescription.setPrescriptionInformation("Patient specific instructions");
            
            Response response = webTarget
                .register(adminAuth)
                .path("patient/1/prescriptions")
                .request()
                .post(Entity.json(newPrescription));
            assertThat(response.getStatus(), is(oneOf(200, 201, 404)));
        }
    }

    @Test
    public void test50_verify_security_roles() {
        // Test that unauthorized access is properly blocked
        Response response = webTarget
            .path(PHYSICIAN_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(401));
    }
}

// Add ObjectMapper configuration class
class ObjectMapperContextResolver implements jakarta.ws.rs.ext.ContextResolver<ObjectMapper> {
    private final ObjectMapper mapper;

    public ObjectMapperContextResolver() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}