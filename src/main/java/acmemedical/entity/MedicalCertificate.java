/********************************************************************************************************
 * File:  MedicalCertificate.java Course Materials CST 8277
 *
 * @author Teddy Yap
 * 
 */
package acmemedical.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.io.Serializable;

@SuppressWarnings("unused")

/**
 * The persistent class for the medical_certificate database table.
 */
//TODO MC01 - Add the missing annotations.
//TODO MC02 - Do we need a mapped super class?  If so, which one?
@Entity(name = "MedicalCertificate")
@Table(name = "medical_certificate")
@Access(AccessType.FIELD)
@AttributeOverride(name = "id", column = @Column(name = "certificate_id"))
@NamedQuery(name = MedicalCertificate.ID_CARD_QUERY_NAME, query = "SELECT mc FROM MedicalCertificate mc WHERE mc.id = :param1")
public class MedicalCertificate extends PojoBase implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String ID_CARD_QUERY_NAME = "MedicalCertificate.findById";
	
	// TODO MC03 - Add annotations for 1:1 mapping.  What should be the cascade and fetch types?
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "training_id", nullable = false)
	private MedicalTraining medicalTraining;

	// TODO MC04 - Add annotations for M:1 mapping.  What should be the cascade and fetch types?
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "physician_id", nullable = false)
	private Physician owner;

	// TODO MC05 - Add annotations.
	@Column(name = "signed", nullable = false)
	private byte signed;

	public MedicalCertificate() {
		super();
	}
	
	public MedicalCertificate(MedicalTraining medicalTraining, Physician owner, byte signed) {
		this();
		this.medicalTraining = medicalTraining;
		this.owner = owner;
		this.signed = signed;
	}

	@JsonIgnore
	public MedicalTraining getMedicalTraining() {
		return medicalTraining;
	}

	public void setMedicalTraining(MedicalTraining medicalTraining) {
		this.medicalTraining = medicalTraining;
	}

	@JsonBackReference(value = "certificate-physician")
	public Physician getOwner() {
		return owner;
	}

	public void setOwner(Physician owner) {
		this.owner = owner;
	}

	public byte getSigned() {
		return signed;
	}

	@JsonProperty("signed")
	public void setSigned(byte signed) {
		this.signed = signed;
	}

//	@JsonProperty("signedAsBoolean")
//	public void setSigned(boolean signed) {
//		this.signed = (byte) (signed ? 0b0001 : 0b0000);
//	}
	
	//Inherited hashCode/equals is sufficient for this entity class

}