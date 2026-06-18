package com.clt.sso.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SsoSystemEndpointModel {
    private Long endpointId;
    private String appName;
    private String endpointUrl;
    private String endpointType;   // USER_SYNC, ROLE_SYNC, HEALTH_CHECK, etc.
    private String syncSecret;
    private String remarks;
    private String actFlg;
    private LocalDateTime creDt;
    private String creUsrId;
    private LocalDateTime updDt;
    private String updUsrId;
}
