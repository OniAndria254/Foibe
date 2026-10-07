package org.fam.dime.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.fam.dime.member.MemberStatus;
import org.fam.dime.member.Sex;

public class CreateMemberRequest {

	@NotBlank(message = "{member.error.lastName}")
	@Size(max = 100, message = "{member.error.maxLength}")
	private String lastName;

	@NotBlank(message = "{member.error.firstName}")
	@Size(max = 100, message = "{member.error.maxLength}")
	private String firstName;

	private Sex sex;

	private LocalDate birthDate;

	@Size(max = 30, message = "{member.error.maxLength}")
	private String phone;

	@Size(max = 300, message = "{member.error.maxLength}")
	private String address;

	private MemberStatus status;

	private LocalDate joinedOn;

	@Size(max = 2000, message = "{member.error.maxLength}")
	private String notes;

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public Sex getSex() {
		return sex;
	}

	public void setSex(Sex sex) {
		this.sex = sex;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public MemberStatus getStatus() {
		return status;
	}

	public void setStatus(MemberStatus status) {
		this.status = status;
	}

	public LocalDate getJoinedOn() {
		return joinedOn;
	}

	public void setJoinedOn(LocalDate joinedOn) {
		this.joinedOn = joinedOn;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}