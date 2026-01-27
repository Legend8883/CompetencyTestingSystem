package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;

@Data
public class UserSimpleResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}
