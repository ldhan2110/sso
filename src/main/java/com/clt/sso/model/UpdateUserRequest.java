package com.clt.sso.model;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String tentId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String actFlg;
}
