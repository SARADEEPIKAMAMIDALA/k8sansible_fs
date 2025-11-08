package com.toursandtravel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponse {
    private boolean success;
    private String responseMessage;
    private String jwtToken;
    private UserDto user;   // <-- changed from User to UserDto
}
