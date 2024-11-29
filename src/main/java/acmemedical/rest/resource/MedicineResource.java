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
import acmemedical.entity.Medicine;
import acmemedical.entity.Medicine;
import acmemedical.entity.Physician;
import acmemedical.entity.SecurityUser;
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

@Path(MEDICINE_RESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MedicineResource {

    private static final Logger LOG = LogManager.getLogger();

    @EJB
    protected ACMEMedicalService service;

    @Inject
    protected SecurityContext sc;

    @GET
    //A user with either the role ‘ADMIN_ROLE’ or ‘USER_ROLE’ can get the list of all medicines.
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    public Response getMedicines() {
        LOG.debug("retrieving all medicines ...");
        List<Medicine> medicines = service.getAllMedicines();
        Response response = Response.ok(medicines).build();
        return response;
    }

    @GET
    //A user with either the role ‘ADMIN_ROLE’ or ‘USER_ROLE’ can get a specific medicine.
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response getMedicineById(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
        LOG.debug("try to retrieve specific medicine " + id);
        Response response = null;
        Medicine medicine = null;
        Physician physician = null;

        if (sc.isCallerInRole(ADMIN_ROLE)) {
            medicine = service.getMedicineById(id);
            response = Response.status(medicine == null ? Status.NOT_FOUND : Status.OK).entity(medicine).build();
        } else if (sc.isCallerInRole(USER_ROLE)) {
            WrappingCallerPrincipal wCallerPrincipal = (WrappingCallerPrincipal) sc.getCallerPrincipal();
            SecurityUser sUser = (SecurityUser) wCallerPrincipal.getWrapped();
            physician = sUser.getPhysician();
            if (physician != null && physician.getId() == id) {
                response = Response.status(Status.OK).entity(physician).build();
            } else {
            	//disallows a ‘USER_ROLE’ user from getting a physician that is not linked to the SecurityUser.
                throw new ForbiddenException("User trying to access resource it does not own (wrong userid)");
            }
        } else {
            response = Response.status(Status.BAD_REQUEST).build();
        }
        return response;
    }

    @POST
    //Only a user with the SecurityRole ‘ADMIN_ROLE’ can add a new Medicine.
    @RolesAllowed({ADMIN_ROLE})
    public Response addMedicine(Medicine newMedicine) {
        Response response = null;
        Medicine newMedicineWithIdTimestamps = service.persistMedicine(newMedicine);
        // Build a SecurityUser linked to the new physician
        response = Response.ok(newMedicineWithIdTimestamps).build();
        return response;
    }

    @DELETE
    @Path(RESOURCE_PATH_ID_PATH)
    @RolesAllowed({ADMIN_ROLE})
    public Response deleteMedicine(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
        LOG.debug("Attempting to delete medicine with ID: " + id);
        Medicine medicine = service.getMedicineById(id);

        if (medicine == null) {
            LOG.warn("Medicine with ID " + id + " not found.");
            return Response.status(Status.NOT_FOUND)
                    .entity("Medicine not found with ID: " + id)
                    .build();
        }

        service.deleteMedicineById(id);
        LOG.info("Medicine with ID " + id + " successfully deleted.");
        return Response.noContent().build();
    }
}