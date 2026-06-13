package com.clt.sso.model;

import java.util.List;

import lombok.Data;

@Data
public class RegistrationRequest {
    private String tentId;
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private List<String> groups;
}
