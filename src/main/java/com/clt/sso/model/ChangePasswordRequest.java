package com.clt.sso.model;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String tentId;
    private String username;
    private String newPassword;
}
