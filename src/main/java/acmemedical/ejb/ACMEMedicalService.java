/********************************************************************************************************
 * File:  ACMEMedicalService.java Course Materials CST 8277
 *
 * @author Teddy Yap
 * @author Shariar (Shawn) Emami
 * 
 */
package acmemedical.ejb;

import static acmemedical.utility.MyConstants.DEFAULT_KEY_SIZE;
import static acmemedical.utility.MyConstants.DEFAULT_PROPERTY_ALGORITHM;
import static acmemedical.utility.MyConstants.DEFAULT_PROPERTY_ITERATIONS;
import static acmemedical.utility.MyConstants.DEFAULT_SALT_SIZE;
import static acmemedical.utility.MyConstants.DEFAULT_USER_PASSWORD;
import static acmemedical.utility.MyConstants.DEFAULT_USER_PREFIX;
import static acmemedical.utility.MyConstants.PARAM1;
import static acmemedical.utility.MyConstants.PROPERTY_ALGORITHM;
import static acmemedical.utility.MyConstants.PROPERTY_ITERATIONS;
import static acmemedical.utility.MyConstants.PROPERTY_KEY_SIZE;
import static acmemedical.utility.MyConstants.PROPERTY_SALT_SIZE;
import static acmemedical.utility.MyConstants.PU_NAME;
import static acmemedical.utility.MyConstants.USER_ROLE;
import static acmemedical.entity.Physician.ALL_PHYSICIANS_QUERY_NAME;
import static acmemedical.entity.MedicalSchool.ALL_MEDICAL_SCHOOLS_QUERY_NAME;
import static acmemedical.entity.MedicalSchool.IS_DUPLICATE_QUERY_NAME;
import static acmemedical.entity.MedicalSchool.SPECIFIC_MEDICAL_SCHOOL_QUERY_NAME;
import static acmemedical.entity.MedicalCertificate.ID_CARD_QUERY_NAME;
import static acmemedical.entity.MedicalTraining.FIND_BY_ID;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;
import jakarta.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import acmemedical.entity.MedicalTraining;
import acmemedical.entity.Patient;
import acmemedical.entity.MedicalCertificate;
import acmemedical.entity.Medicine;
import acmemedical.entity.Prescription;
import acmemedical.entity.PrescriptionPK;
import acmemedical.entity.SecurityRole;
import acmemedical.entity.SecurityUser;
import acmemedical.entity.Physician;
import acmemedical.entity.MedicalSchool;

@SuppressWarnings("unused")

/**
 * Stateless Singleton EJB Bean - ACMEMedicalService
 */
@Singleton
public class ACMEMedicalService implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final Logger LOG = LogManager.getLogger();
    
    @PersistenceContext(name = PU_NAME)
    protected EntityManager em;
    
    @Inject
    protected Pbkdf2PasswordHash pbAndjPasswordHash;

    // CRUD for Physician
    public List<Physician> getAllPhysicians() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Physician> cq = cb.createQuery(Physician.class);
        cq.select(cq.from(Physician.class));
        return em.createQuery(cq).getResultList();
    }

    public Physician getPhysicianById(int id) {
        return em.find(Physician.class, id);
    }

    @Transactional
    public Physician persistPhysician(Physician newPhysician) {
        em.persist(newPhysician);
        return newPhysician;
    }

    @Transactional
    public void buildUserForNewPhysician(Physician newPhysician) {
        SecurityUser userForNewPhysician = new SecurityUser();
        userForNewPhysician.setUsername(
            DEFAULT_USER_PREFIX + "_" + newPhysician.getFirstName() + "." + newPhysician.getLastName());
        Map<String, String> pbAndjProperties = new HashMap<>();
        pbAndjProperties.put(PROPERTY_ALGORITHM, DEFAULT_PROPERTY_ALGORITHM);
        pbAndjProperties.put(PROPERTY_ITERATIONS, DEFAULT_PROPERTY_ITERATIONS);
        pbAndjProperties.put(PROPERTY_SALT_SIZE, DEFAULT_SALT_SIZE);
        pbAndjProperties.put(PROPERTY_KEY_SIZE, DEFAULT_KEY_SIZE);
        pbAndjPasswordHash.initialize(pbAndjProperties);
        String pwHash = pbAndjPasswordHash.generate(DEFAULT_USER_PASSWORD.toCharArray());
        userForNewPhysician.setPwHash(pwHash);
        userForNewPhysician.setPhysician(newPhysician);
        /* TODO ACMECS01 - Use NamedQuery on SecurityRole to find USER_ROLE */
        SecurityRole userRole = em.createNamedQuery("SecurityRole.roleByName", SecurityRole.class)
                .setParameter(PARAM1, USER_ROLE)
                .getSingleResult();
        userForNewPhysician.getRoles().add(userRole);
        userRole.getUsers().add(userForNewPhysician);
        em.merge(userForNewPhysician);
    }

    @Transactional
    public Medicine setMedicineForPhysicianPatient(int physicianId, int patientId, Medicine newMedicine) {
        Physician physicianToBeUpdated = em.find(Physician.class, physicianId);
        if (physicianToBeUpdated != null) { // Physician exists
            Set<Prescription> prescriptions = physicianToBeUpdated.getPrescriptions();
            prescriptions.forEach(p -> {
                if (p.getPatient().getId() == patientId) {
                    if (p.getMedicine() != null) { // Medicine exists
                        Medicine medicine = em.find(Medicine.class, p.getMedicine().getId());
                        medicine.setMedicine(newMedicine.getDrugName(),
                        				  newMedicine.getManufacturerName(),
                        				  newMedicine.getDosageInformation());
                        em.merge(medicine);
                    }
                    else { // Medicine does not exist
                        p.setMedicine(newMedicine);
                        em.merge(physicianToBeUpdated);
                    }
                }
            });
            return newMedicine;
        }
        else return null;  // Physician doesn't exists
    }

    /**
     * To update a physician
     * 
     * @param id - id of entity to update
     * @param physicianWithUpdates - entity with updated information
     * @return Entity with updated information
     */
    @Transactional
    public Physician updatePhysicianById(int id, Physician physicianWithUpdates) {
    	Physician physicianToBeUpdated = getPhysicianById(id);
        if (physicianToBeUpdated != null) {
            em.refresh(physicianToBeUpdated);
            em.merge(physicianWithUpdates);
            em.flush();
        }
        return physicianToBeUpdated;
    }

    /**
     * To delete a physician by id
     * 
     * @param id - physician id to delete
     */
    @Transactional
    public void deletePhysicianById(int id) {
        Physician physician = getPhysicianById(id);
        if (physician != null) {
            em.refresh(physician);
            /* TODO ACMECS02 - Use NamedQuery on SecurityRole to find this related Student
               so that when we remove it, the relationship from SECURITY_USER table
               is not dangling
            */
            TypedQuery<SecurityUser> findUser = em.createNamedQuery("SecurityUser.userByPhysicianId", SecurityUser.class)
                    .setParameter(PARAM1, id);

            SecurityUser sUser = findUser.getSingleResult();
            em.remove(sUser);
            em.remove(physician);
        }
    }

    // CRUD for School
    public List<MedicalSchool> getAllMedicalSchools() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MedicalSchool> cq = cb.createQuery(MedicalSchool.class);
        cq.select(cq.from(MedicalSchool.class));
        return em.createQuery(cq).getResultList();
    }

    // Why not use the build-in em.find?  The named query SPECIFIC_MEDICAL_SCHOOL_QUERY_NAME
    // includes JOIN FETCH that we cannot add to the above API
    public MedicalSchool getMedicalSchoolById(int id) {
        TypedQuery<MedicalSchool> specificMedicalSchoolQuery = em.createNamedQuery(SPECIFIC_MEDICAL_SCHOOL_QUERY_NAME, MedicalSchool.class);
        specificMedicalSchoolQuery.setParameter(PARAM1, id);
        return specificMedicalSchoolQuery.getSingleResult();
    }
    
    // These methods are more generic.
    public <T> List<T> getAll(Class<T> entity, String namedQuery) {
        TypedQuery<T> allQuery = em.createNamedQuery(namedQuery, entity);
        return allQuery.getResultList();
    }
    public <T> T getById(Class<T> entity, String namedQuery, int id) {
        TypedQuery<T> allQuery = em.createNamedQuery(namedQuery, entity);
        allQuery.setParameter(PARAM1, id);
        return allQuery.getSingleResult();
    }

    @Transactional
    public MedicalSchool deleteMedicalSchool(int id) {
        //MedicalSchool ms = getMedicalSchoolById(id);
    	MedicalSchool ms = getById(MedicalSchool.class, MedicalSchool.SPECIFIC_MEDICAL_SCHOOL_QUERY_NAME, id);
        if (ms != null) {
            Set<MedicalTraining> medicalTrainings = ms.getMedicalTrainings();
            List<MedicalTraining> list = new LinkedList<>();
            medicalTrainings.forEach(list::add);
            list.forEach(mt -> {
                if (mt.getCertificate() != null) {
                    MedicalCertificate mc = getById(MedicalCertificate.class, MedicalCertificate.ID_CARD_QUERY_NAME, mt.getCertificate().getId());
                    mc.setMedicalTraining(null);
                }
                mt.setCertificate(null);
                em.merge(mt);
            });
            em.remove(ms);
            return ms;
        }
        return null;
    }
    
    // Please study & use the methods below in your test suites
    public boolean isDuplicated(MedicalSchool newMedicalSchool) {
        TypedQuery<Long> allMedicalSchoolsQuery = em.createNamedQuery(IS_DUPLICATE_QUERY_NAME, Long.class);
        allMedicalSchoolsQuery.setParameter(PARAM1, newMedicalSchool.getName());
        return (allMedicalSchoolsQuery.getSingleResult() >= 1);
    }

    @Transactional
    public MedicalSchool persistMedicalSchool(MedicalSchool newMedicalSchool) {
        em.persist(newMedicalSchool);
        return newMedicalSchool;
    }

    @Transactional
    public MedicalSchool updateMedicalSchool(int id, MedicalSchool updatingMedicalSchool) {
    	MedicalSchool medicalSchoolToBeUpdated = getMedicalSchoolById(id);
        if (medicalSchoolToBeUpdated != null) {
            em.refresh(medicalSchoolToBeUpdated);
            medicalSchoolToBeUpdated.setName(updatingMedicalSchool.getName());
            em.merge(medicalSchoolToBeUpdated);
            em.flush();
        }
        return medicalSchoolToBeUpdated;
    }

    // CRUD for Training
    public List<MedicalTraining> getAllMedicalTrainings() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MedicalTraining> cq = cb.createQuery(MedicalTraining.class);
        cq.select(cq.from(MedicalTraining.class));
        return em.createQuery(cq).getResultList();
    }

    @Transactional
    public MedicalTraining persistMedicalTraining(MedicalTraining newMedicalTraining) {
        em.persist(newMedicalTraining);
        return newMedicalTraining;
    }
    
    public MedicalTraining getMedicalTrainingById(int mtId) {
        TypedQuery<MedicalTraining> allMedicalTrainingQuery = em.createNamedQuery(MedicalTraining.FIND_BY_ID, MedicalTraining.class);
        allMedicalTrainingQuery.setParameter(PARAM1, mtId);
        return allMedicalTrainingQuery.getSingleResult();
    }

    @Transactional
    public void deleteMedicalTrainingById(int id) {
        MedicalTraining medicalTraining = getMedicalTrainingById(id);
        if (medicalTraining != null) {
            em.refresh(medicalTraining);
            em.remove(medicalTraining);
        }
    }

    @Transactional
    public MedicalTraining updateMedicalTraining(int id, MedicalTraining medicalTrainingWithUpdates) {
    	MedicalTraining medicalTrainingToBeUpdated = getMedicalTrainingById(id);
        if (medicalTrainingToBeUpdated != null) {
            em.refresh(medicalTrainingToBeUpdated);
            em.merge(medicalTrainingWithUpdates);
            em.flush();
        }
        return medicalTrainingToBeUpdated;
    }

    // CRUD for Patient
    public List<Patient> getAllPatients() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Patient> cq = cb.createQuery(Patient.class);
        cq.select(cq.from(Patient.class));
        return em.createQuery(cq).getResultList();
    }

    public Patient getPatientById(int id) {
        return em.find(Patient.class, id);
    }

    @Transactional
    public Patient persistPatient(Patient newPatient) {
        em.persist(newPatient);
        return newPatient;
    }

    @Transactional
    public Patient updatePatient(int id, Patient updatedPatient) {
        Patient patientToBeUpdated = getPatientById(id);
        if (patientToBeUpdated != null) {
            em.refresh(patientToBeUpdated);
            patientToBeUpdated.setFirstName(updatedPatient.getFirstName());
            patientToBeUpdated.setLastName(updatedPatient.getLastName());
            patientToBeUpdated.setYear(updatedPatient.getYear());
            patientToBeUpdated.setAddress(updatedPatient.getAddress());
            patientToBeUpdated.setHeight(updatedPatient.getHeight());
            patientToBeUpdated.setWeight(updatedPatient.getWeight());
            patientToBeUpdated.setSmoker(updatedPatient.getSmoker());
            em.merge(patientToBeUpdated);
            em.flush();
        }
        return patientToBeUpdated;
    }

    @Transactional
    public void deletePatientById(int id) {
        Patient patient = getPatientById(id);
        if (patient != null) {
            em.refresh(patient);
            em.remove(patient);
        }
    }

    // CRUD for Medicine
    public List<Medicine> getAllMedicines() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Medicine> cq = cb.createQuery(Medicine.class);
        cq.select(cq.from(Medicine.class));
        return em.createQuery(cq).getResultList();
    }

    public Medicine getMedicineById(int id) {
        return em.find(Medicine.class, id);
    }

    @Transactional
    public Medicine persistMedicine(Medicine newMedicine) {
        em.persist(newMedicine);
        return newMedicine;
    }

    @Transactional
    public Medicine updateMedicine(int id, Medicine updatedMedicine) {
        Medicine medicineToBeUpdated = getMedicineById(id);
        if (medicineToBeUpdated != null) {
            em.refresh(medicineToBeUpdated);
            medicineToBeUpdated.setDrugName(updatedMedicine.getDrugName());
            medicineToBeUpdated.setManufacturerName(updatedMedicine.getManufacturerName());
            medicineToBeUpdated.setDosageInformation(updatedMedicine.getDosageInformation());
            em.merge(medicineToBeUpdated);
            em.flush();
        }
        return medicineToBeUpdated;
    }

    @Transactional
    public void deleteMedicineById(int id) {
        Medicine medicine = getMedicineById(id);
        if (medicine != null) {
            em.refresh(medicine);
            em.remove(medicine);
        }
    }

    // CRUD for Certificate
    public List<MedicalCertificate> getAllMedicalCertificates() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MedicalCertificate> cq = cb.createQuery(MedicalCertificate.class);
        cq.select(cq.from(MedicalCertificate.class));
        return em.createQuery(cq).getResultList();
    }

    public MedicalCertificate getMedicalCertificateById(int id) {
        return em.find(MedicalCertificate.class, id);
    }

    @Transactional
    public MedicalCertificate persistMedicalCertificate(MedicalCertificate newMedicalCertificate) {
        em.persist(newMedicalCertificate);
        return newMedicalCertificate;
    }

    @Transactional
    public MedicalCertificate updateMedicalCertificate(int id, MedicalCertificate updatedMedicalCertificate) {
        MedicalCertificate medicalCertificateToBeUpdated = getMedicalCertificateById(id);
        if (medicalCertificateToBeUpdated != null) {
            em.refresh(medicalCertificateToBeUpdated);
            medicalCertificateToBeUpdated.setMedicalTraining(updatedMedicalCertificate.getMedicalTraining());
            medicalCertificateToBeUpdated.setOwner(updatedMedicalCertificate.getOwner());
            medicalCertificateToBeUpdated.setSigned(updatedMedicalCertificate.getSigned());
            em.merge(medicalCertificateToBeUpdated);
            em.flush();
        }
        return medicalCertificateToBeUpdated;
    }

    @Transactional
    public void deleteMedicalCertificateById(int id) {
        MedicalCertificate medicine = getMedicalCertificateById(id);
        if (medicine != null) {
            em.refresh(medicine);
            em.remove(medicine);
        }
    }

    // CRUD for Prescription
    public List<Prescription> getAllPrescriptions() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Prescription> cq = cb.createQuery(Prescription.class);
        cq.select(cq.from(Prescription.class));
        return em.createQuery(cq).getResultList();
    }

    public Prescription getPrescriptionById(PrescriptionPK id) {
        return em.find(Prescription.class, id); // Use composite key to find the entity
    }

    @Transactional
    public Prescription persistPrescription(Prescription newPrescription) {
        // Ensure that related entities (Physician, Patient, and Medicine) are properly managed
        Physician physician = em.find(Physician.class, newPrescription.getId().getPhysicianId());
        Patient patient = em.find(Patient.class, newPrescription.getId().getPatientId());
        Medicine medicine = newPrescription.getMedicine() != null
                ? em.find(Medicine.class, newPrescription.getMedicine().getId())
                : null;
        newPrescription.setPhysician(physician);
        newPrescription.setPatient(patient);

        if (physician == null || patient == null) {
            throw new IllegalArgumentException("Physician or Patient not found. Cannot create Prescription.");
        }
        if (newPrescription.getId() == null) {
            throw new IllegalArgumentException("Prescription ID (composite key) cannot be null");
        }
        if (!em.contains(newPrescription.getPhysician())) {
            throw new IllegalStateException("Physician is not managed: " + newPrescription.getPhysician());
        }
        if (!em.contains(newPrescription.getPatient())) {
            throw new IllegalStateException("Patient is not managed: " + newPrescription.getPatient());
        }

        em.persist(newPrescription);
        return newPrescription;
    }

    @Transactional
    public Prescription updatePrescription(PrescriptionPK id, Prescription updatedPrescription) {
        Prescription existingPrescription = em.find(Prescription.class, id);
        if (existingPrescription != null) {
            // Merge updates into the existing prescription
            existingPrescription.setNumberOfRefills(updatedPrescription.getNumberOfRefills());
            existingPrescription.setPrescriptionInformation(updatedPrescription.getPrescriptionInformation());
            if (updatedPrescription.getMedicine() != null) {
                existingPrescription.setMedicine(updatedPrescription.getMedicine());
            }
            em.merge(existingPrescription);
            return existingPrescription;
        }
        return null; // Return null if the entity was not found
    }

    @Transactional
    public void deletePrescriptionById(PrescriptionPK id) {
        Prescription prescription = em.find(Prescription.class, id);
        if (prescription != null) {
            em.remove(prescription);
        }
    }
}