// JwtResponse.java (or TokenResponse.java)
package org.leeminkan.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // <-- ADD THIS ANNOTATION
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer"; // Standard type

}