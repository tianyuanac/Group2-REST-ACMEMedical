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
import acmemedical.entity.Prescription;
import acmemedical.entity.PrescriptionPK;
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

import java.util.List;

import static acmemedical.utility.MyConstants.*;

@Path(PRESCRIPTION_RESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PrescriptionResource {

    private static final Logger LOG = LogManager.getLogger();

    @EJB
    protected ACMEMedicalService service;

    @Inject
    protected SecurityContext sc;

    @POST
    @RolesAllowed({ADMIN_ROLE})
    public Response addPrescription(Prescription newPrescription) {
        LOG.debug("Attempting to add a new prescription: {}", newPrescription);

        // Persist the new prescription
        Prescription persistedPrescription = service.persistPrescription(newPrescription);

        LOG.info("Prescription successfully added with Physician ID: {} and Patient ID: {}",
                newPrescription.getId().getPhysicianId(),
                newPrescription.getId().getPatientId());

        return Response.status(Status.CREATED).entity(persistedPrescription).build();
    }

    @GET
    //Only a user with the SecurityRole ‘ADMIN_ROLE’ can get the list of all prescriptions.
    @RolesAllowed({ADMIN_ROLE})
    public Response getPrescriptions() {
        LOG.debug("retrieving all prescriptions ...");
        List<Prescription> prescriptions = service.getAllPrescriptions();
        Response response = Response.ok(prescriptions).build();
        return response;
    }

    @GET
    //A user with either the role ‘ADMIN_ROLE’ or ‘USER_ROLE’ can get a specific prescription.
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    @Path("/{physicianId}/{patientId}")
    public Response getPrescriptionById(
            @PathParam("physicianId") int physicianId,
            @PathParam("patientId") int patientId) {
        LOG.debug("Retrieving prescription with physicianId = {}, patientId = {}", physicianId, patientId);

        PrescriptionPK id = new PrescriptionPK(physicianId, patientId); // Create composite key
        Prescription prescription = service.getPrescriptionById(id);
        if (prescription == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(prescription).build();
    }

    @PUT
    @Path("/{physicianId}/{patientId}")
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    public Response updatePrescription(
            @PathParam("physicianId") int physicianId,
            @PathParam("patientId") int patientId,
            Prescription updatedPrescription) {
        LOG.debug("Attempting to update prescription with Physician ID: " + physicianId + ", Patient ID: " + patientId);

        // Create the composite key
        PrescriptionPK id = new PrescriptionPK(physicianId, patientId);
        Prescription existingPrescription = service.getPrescriptionById(id);

        if (existingPrescription == null) {
            LOG.warn("Prescription with Physician ID " + physicianId + " and Patient ID " + patientId + " not found.");
            return Response.status(Status.NOT_FOUND)
                    .entity("Prescription not found with Physician ID: " + physicianId + " and Patient ID: " + patientId)
                    .build();
        }

        // Update the existing prescription fields
        existingPrescription.setNumberOfRefills(updatedPrescription.getNumberOfRefills());
        existingPrescription.setPrescriptionInformation(updatedPrescription.getPrescriptionInformation());
        if (updatedPrescription.getMedicine() != null) {
            existingPrescription.setMedicine(updatedPrescription.getMedicine());
        }

        Prescription updatedPrescriptionEntity = service.updatePrescription(id, existingPrescription);
        LOG.info("Prescription with Physician ID " + physicianId + " and Patient ID " + patientId + " successfully updated.");
        return Response.ok(updatedPrescriptionEntity).build();
    }

    @DELETE
    @Path("/{physicianId}/{patientId}")
    @RolesAllowed({ADMIN_ROLE})
    public Response deletePrescription(
            @PathParam("physicianId") int physicianId,
            @PathParam("patientId") int patientId) {
        LOG.debug("Attempting to delete prescription with Physician ID: " + physicianId + ", Patient ID: " + patientId);

        // Create the composite key
        PrescriptionPK id = new PrescriptionPK(physicianId, patientId);
        Prescription prescription = service.getPrescriptionById(id);

        if (prescription == null) {
            LOG.warn("Prescription with Physician ID " + physicianId + " and Patient ID " + patientId + " not found.");
            return Response.status(Status.NOT_FOUND)
                    .entity("Prescription not found with Physician ID: " + physicianId + " and Patient ID: " + patientId)
                    .build();
        }

        service.deletePrescriptionById(id);
        LOG.info("Prescription with Physician ID " + physicianId + " and Patient ID " + patientId + " successfully deleted.");
        return Response.noContent().build();
    }
}