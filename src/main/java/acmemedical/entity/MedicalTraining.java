/********************************************************************************************************
 * File:  MedicalTraining.java Course Materials CST 8277
 *
 * @author Teddy Yap
 * 
 */
package acmemedical.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

@SuppressWarnings("unused")

/**
 * The persistent class for the medical_training database table.
 */
//TODO MT01 - Add the missing annotations.
//TODO MT02 - Do we need a mapped super class?  If so, which one?
@Entity(name = "MedicalTraining")
@Table(name = "medical_training")
@AttributeOverride(name = "id", column = @Column(name = "training_id"))
@NamedQuery(name = MedicalTraining.FIND_BY_ID, query = "SELECT mt FROM MedicalTraining mt WHERE mt.id = :param1")
public class MedicalTraining extends PojoBase implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String FIND_BY_ID = "MedicalTraining.findById";
	
	// TODO MT03 - Add annotations for M:1.  What should be the cascade and fetch types?
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "school_id", nullable = false)
	private MedicalSchool school;

	// TODO MT04 - Add annotations for 1:1.  What should be the cascade and fetch types?
	@OneToOne(mappedBy = "medicalTraining", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
	private MedicalCertificate certificate;

	@Embedded
	private DurationAndStatus durationAndStatus;

	public MedicalTraining() {
		durationAndStatus = new DurationAndStatus();
	}

	@JsonBackReference(value = "medicalTrainings-school")
	public MedicalSchool getMedicalSchool() {
		return school;
	}

	public void setMedicalSchool(MedicalSchool school) {
		this.school = school;
	}

	public MedicalCertificate getCertificate() {
		return certificate;
	}
	
	public void setCertificate(MedicalCertificate certificate) {
		this.certificate = certificate;
	}
	
	public DurationAndStatus getDurationAndStatus() {
		return durationAndStatus;
	}

	public void setDurationAndStatus(DurationAndStatus durationAndStatus) {
		this.durationAndStatus = durationAndStatus;
	}
	
	//Inherited hashCode/equals NOT sufficient for this Entity class
	/**
	 * Very important:  Use getter's for member variables because JPA sometimes needs to intercept those calls<br/>
	 * and go to the database to retrieve the value
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		// Only include member variables that really contribute to an object's identity
		// i.e. if variables like version/updated/name/etc. change throughout an object's lifecycle,
		// they shouldn't be part of the hashCode calculation
		
		// include DurationAndStatus in identity
		return prime * result + Objects.hash(getId(), getDurationAndStatus());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof MedicalTraining otherMedicalTraining) {
			// See comment (above) in hashCode():  Compare using only member variables that are
			// truly part of an object's identity
			return Objects.equals(this.getId(), otherMedicalTraining.getId()) &&
				Objects.equals(this.getDurationAndStatus(), otherMedicalTraining.getDurationAndStatus());
		}
		return false;
	}
}
