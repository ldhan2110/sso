package com.clt.sso.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class UserInfoModel {
    private String tentId;
    private String usrId;
    private String usrNm;
    private String usrPwd;
    private String usrEml;
    private String actFlg;
    private LocalDateTime creDt;
    private String creUsrId;
    private LocalDateTime updDt;
    private String updUsrId;
    private String firstName;
    private String lastName;
    private boolean emailVerified;
    private Map<String, List<String>> customAttributes = new HashMap<>();
}
