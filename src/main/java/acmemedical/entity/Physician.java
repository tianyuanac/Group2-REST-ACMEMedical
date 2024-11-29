/********************************************************************************************************
 * File:  Physician.java Course Materials CST 8277
 *
 * @author Teddy Yap
 * 
 */
package acmemedical.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The persistent class for the physician database table.
 */
@SuppressWarnings("unused")

//TODO PH01 - Add the missing annotations.
//TODO PH02 - Do we need a mapped super class? If so, which one?
@Entity(name = "Physician")
@Table(name = "physician")
@Access(AccessType.FIELD)
@NamedQuery(name = "Physician.ALL_PHYSICIANS_QUERY_NAME", query = "SELECT p FROM Physician p")
public class Physician extends PojoBase implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String ALL_PHYSICIANS_QUERY_NAME = "Physician.findAll";

    public Physician() {
    	super();
    }

	// TODO PH03 - Add annotations.
	@Column(name = "first_name", nullable = false, length = 50)
	private String firstName;

	// TODO PH04 - Add annotations.
	@Column(name = "last_name", nullable = false, length = 50)
	private String lastName;

	// TODO PH05 - Add annotations for 1:M relation.  What should be the cascade and fetch types?
	@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<MedicalCertificate> medicalCertificates = new HashSet<>();

	// TODO PH06 - Add annotations for 1:M relation.  What should be the cascade and fetch types?
	@OneToMany(mappedBy = "physician", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<Prescription> prescriptions = new HashSet<>();

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	// TODO PH07 - Is an annotation needed here?
	@JsonManagedReference(value = "certificate-physician")
    public Set<MedicalCertificate> getMedicalCertificates() {
		return medicalCertificates;
	}

	public void setMedicalCertificates(Set<MedicalCertificate> medicalCertificates) {
		this.medicalCertificates = medicalCertificates;
	}

	// TODO PH08 - Is an annotation needed here?
	@JsonManagedReference(value = "prescription-physician")
    public Set<Prescription> getPrescriptions() {
		return prescriptions;
	}

	public void setPrescriptions(Set<Prescription> prescriptions) {
		this.prescriptions = prescriptions;
	}

	public void setFullName(String firstName, String lastName) {
		setFirstName(firstName);
		setLastName(lastName);
	}
	
	//Inherited hashCode/equals is sufficient for this entity class

}
