/********************************************************************************************************
 * File:  PhysicianResource.java Course Materials CST 8277
 *
 * @author Teddy Yap
 * @author Shariar (Shawn) Emami
 * @author (original) Mike Norman
 * 
 */
package acmemedical.rest.resource;

import acmemedical.ejb.ACMEMedicalService;
import acmemedical.entity.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.soteria.WrappingCallerPrincipal;

import java.util.List;

import static acmemedical.utility.MyConstants.*;

@Path(PATIENT_RESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PatientResource {

    private static final Logger LOG = LogManager.getLogger();

    @EJB
    protected ACMEMedicalService service;

    @Inject
    protected SecurityContext sc;

    @GET
    //Only a user with the SecurityRole ‘ADMIN_ROLE’ can get the list of all patients.
    @RolesAllowed({ADMIN_ROLE})
    public Response getPatients() {
        LOG.debug("retrieving all patients ...");
        List<Patient> patients = service.getAllPatients();
        Response response = Response.ok(patients).build();
        return response;
    }

    @GET
    //A user with either the role ‘ADMIN_ROLE’ or ‘USER_ROLE’ can get a specific patient.
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    @Path("/{patientId}")
    public Response getMPatientById(@PathParam("patientId") int patientId) {
        LOG.debug("Retrieving patient with id = {}", patientId);
        Patient patient = service.getPatientById(patientId);
        Response response = Response.ok(patient).build();
        return response;
    }

    @POST
    //Only a user with the SecurityRole ‘ADMIN_ROLE’ can add a new patient.
    @RolesAllowed({ADMIN_ROLE})
    public Response addPatient(Patient newPatient) {
        Response response = null;
        Patient newPatientWithIdTimestamps = service.persistPatient(newPatient);
        response = Response.ok(newPatientWithIdTimestamps).build();
        return response;
    }

    @DELETE
    @Path(RESOURCE_PATH_ID_PATH)
    @RolesAllowed({ADMIN_ROLE})
    public Response deletePatient(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
        LOG.debug("Attempting to delete patient with ID: " + id);
        Patient patient = service.getPatientById(id);

        if (patient == null) {
            LOG.warn("Patient with ID " + id + " not found.");
            return Response.status(Status.NOT_FOUND)
                    .entity("Patient not found with ID: " + id)
                    .build();
        }

        service.deletePatientById(id);
        LOG.info("Patient with ID " + id + " successfully deleted.");
        return Response.noContent().build();
    }
}