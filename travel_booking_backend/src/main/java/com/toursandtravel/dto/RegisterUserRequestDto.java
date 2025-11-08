package com.toursandtravel.dto;

import com.toursandtravel.entity.User;

public class RegisterUserRequestDto {

    private String emailId;
    private String password;
    private String role;

    // ✅ Add address-related fields
    private String city;
    private String street;
    private String pincode;

    public RegisterUserRequestDto() {}

    // --- Getters and Setters ---
    public String getEmailId() {
        return emailId;
    }
    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }
    public void setStreet(String street) {
        this.street = street;
    }

    public String getPincode() {
        return pincode;
    }
    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    // ✅ Convert DTO to Entity
    public static User toUserEntity(RegisterUserRequestDto dto) {
        User user = new User();
        user.setEmailId(dto.getEmailId());
        user.setPassword(dto.getPassword());
        user.setRole(dto.getRole());
        return user;
    }
}
