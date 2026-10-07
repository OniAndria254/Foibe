package org.fam.dime.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "members")
@Getter
@Setter
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "church_id", nullable = false)
	private Long churchId;

	@Column(name = "envelope_number", nullable = false)
	private Integer envelopeNumber;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "sex", length = 10)
	@Enumerated(EnumType.STRING)
	private Sex sex;

	@Column(name = "birth_date")
	private LocalDate birthDate;

	@Column(name = "phone", length = 30)
	private String phone;

	@Column(name = "address", length = 300)
	private String address;

	@Column(name = "status", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private MemberStatus status;

	@Column(name = "joined_on", nullable = false)
	private LocalDate joinedOn;

	@Column(name = "notes", length = 2000)
	private String notes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "updated_by")
	private Long updatedBy;
}
