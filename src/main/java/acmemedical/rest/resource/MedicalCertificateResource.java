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

@Path(MEDICAL_CERTIFICATE_RESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MedicalCertificateResource {

    private static final Logger LOG = LogManager.getLogger();

    @EJB
    protected ACMEMedicalService service;

    @Inject
    protected SecurityContext sc;

    @GET
    //A user with either the role ‘ADMIN_ROLE’ or ‘USER_ROLE’ can get the list of all medicines.
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    public Response getMedicalCertificates() {
        LOG.debug("retrieving all medicines ...");
        List<MedicalCertificate> medicalCertificates = service.getAllMedicalCertificates();
        Response response = Response.ok(medicalCertificates).build();
        return response;
    }

    @GET
    //A user with either the role ‘ADMIN_ROLE’ or ‘USER_ROLE’ can get a specific medicalCertificate.
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    @Path("/{medicalCertificateId}")
    public Response getMedicalCertificateById(@PathParam("medicalCertificateId") int medicalCertificateId) {
        LOG.debug("Retrieving medical school with id = {}", medicalCertificateId);
        MedicalCertificate medicalCertificate = service.getMedicalCertificateById(medicalCertificateId);
        Response response = Response.ok(medicalCertificate).build();
        return response;
    }

    @POST
    //Only a user with the SecurityRole ‘ADMIN_ROLE’ can add a new medicalCertificate.
    @RolesAllowed({ADMIN_ROLE})
    public Response addMedicalCertificate(MedicalCertificate newMedicalCertificate) {
        Response response = null;
        MedicalCertificate newMedicalCertificateWithIdTimestamps = service.persistMedicalCertificate(newMedicalCertificate);
        response = Response.ok(newMedicalCertificateWithIdTimestamps).build();
        return response;
    }

    @DELETE
    @Path(RESOURCE_PATH_ID_PATH)
    @RolesAllowed({ADMIN_ROLE})
    public Response deleteMedicalCertificate(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
        LOG.debug("Attempting to delete medicalCertificate with ID: " + id);
        MedicalCertificate medicalCertificate = service.getMedicalCertificateById(id);

        if (medicalCertificate == null) {
            LOG.warn("MedicalCertificate with ID " + id + " not found.");
            return Response.status(Status.NOT_FOUND)
                    .entity("MedicalCertificate not found with ID: " + id)
                    .build();
        }

        service.deleteMedicalCertificateById(id);
        LOG.info("MedicalCertificate with ID " + id + " successfully deleted.");
        return Response.noContent().build();
    }
}