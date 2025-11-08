package com.toursandtravel.dto;

import lombok.Data;

@Data
public class UserLoginRequest {
    private String emailId;   // ✅ same as frontend
    private String password;
    private String role;
}
