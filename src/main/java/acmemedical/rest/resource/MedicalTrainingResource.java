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

@Path(MEDICAL_TRAINING_RESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MedicalTrainingResource {

    private static final Logger LOG = LogManager.getLogger();

    @EJB
    protected ACMEMedicalService service;

    @Inject
    protected SecurityContext sc;

    @GET
    //A user with either the role ‘ADMIN_ROLE’ or ‘USER_ROLE’ can get the list of all medicalTrainings.
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    public Response getMedicalTrainings() {
        LOG.debug("retrieving all medicalTrainings ...");
        List<MedicalTraining> medicalTrainings = service.getAllMedicalTrainings();
        Response response = Response.ok(medicalTrainings).build();
        return response;
    }

    @GET
    //A user with either the role ‘ADMIN_ROLE’ or ‘USER_ROLE’ can get a specific patient.
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    @Path("/{medicalTrainingId}")
    public Response getMMedicalTrainingById(@PathParam("medicalTrainingId") int medicalTrainingId) {
        LOG.debug("Retrieving medicalTraining with id = {}", medicalTrainingId);
        MedicalTraining medicalTraining = service.getMedicalTrainingById(medicalTrainingId);
        Response response = Response.ok(medicalTraining).build();
        return response;
    }

    @POST
    //Only a user with the SecurityRole ‘ADMIN_ROLE’ can add a new patient.
    @RolesAllowed({ADMIN_ROLE})
    public Response addMedicalTraining(MedicalTraining newMedicalTraining) {
        Response response = null;
        MedicalTraining newMedicalTrainingWithIdTimestamps = service.persistMedicalTraining(newMedicalTraining);
        response = Response.ok(newMedicalTrainingWithIdTimestamps).build();
        return response;
    }

    @DELETE
    @Path(RESOURCE_PATH_ID_PATH)
    @RolesAllowed({ADMIN_ROLE})
    public Response deleteMedicalTraining(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
        LOG.debug("Attempting to delete patient with ID: " + id);
        MedicalTraining patient = service.getMedicalTrainingById(id);

        if (patient == null) {
            LOG.warn("MedicalTraining with ID " + id + " not found.");
            return Response.status(Status.NOT_FOUND)
                    .entity("MedicalTraining not found with ID: " + id)
                    .build();
        }

        service.deleteMedicalTrainingById(id);
        LOG.info("MedicalTraining with ID " + id + " successfully deleted.");
        return Response.noContent().build();
    }
}