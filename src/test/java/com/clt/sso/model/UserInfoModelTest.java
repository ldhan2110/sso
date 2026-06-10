package com.clt.sso.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserInfoModelTest {

    @Test
    void lombokGettersAndSetters() {
        UserInfoModel model = new UserInfoModel();
        model.setTentId("T1");
        model.setUsrId("U1");
        model.setUsrNm("johndoe");
        model.setUsrPwd("hashed");
        model.setUsrEml("john@example.com");
        model.setActFlg("Y");
        model.setCreDt(LocalDateTime.of(2023, 11, 14, 22, 13, 20));
        model.setCreUsrId("admin");
        model.setUpdDt(LocalDateTime.of(2023, 11, 14, 22, 13, 21));
        model.setUpdUsrId("admin");
        model.setFirstName("John");
        model.setLastName("Doe");
        model.setEmailVerified(true);

        assertEquals("T1", model.getTentId());
        assertEquals("U1", model.getUsrId());
        assertEquals("johndoe", model.getUsrNm());
        assertEquals("hashed", model.getUsrPwd());
        assertEquals("john@example.com", model.getUsrEml());
        assertEquals("Y", model.getActFlg());
        assertEquals(LocalDateTime.of(2023, 11, 14, 22, 13, 20), model.getCreDt());
        assertEquals("admin", model.getCreUsrId());
        assertEquals(LocalDateTime.of(2023, 11, 14, 22, 13, 21), model.getUpdDt());
        assertEquals("admin", model.getUpdUsrId());
        assertEquals("John", model.getFirstName());
        assertEquals("Doe", model.getLastName());
        assertTrue(model.isEmailVerified());
    }

    @Test
    void customAttributes_defaultsToEmptyMap() {
        UserInfoModel model = new UserInfoModel();
        assertNotNull(model.getCustomAttributes());
        assertTrue(model.getCustomAttributes().isEmpty());
    }

    @Test
    void customAttributes_setAndGet() {
        UserInfoModel model = new UserInfoModel();
        Map<String, List<String>> attrs = new HashMap<>();
        attrs.put("phone", List.of("+1234567890"));
        attrs.put("department", List.of("Engineering", "Platform"));
        model.setCustomAttributes(attrs);

        assertEquals(List.of("+1234567890"), model.getCustomAttributes().get("phone"));
        assertEquals(List.of("Engineering", "Platform"), model.getCustomAttributes().get("department"));
    }

    @Test
    void equalsAndHashCode() {
        UserInfoModel a = new UserInfoModel();
        a.setTentId("T1");
        a.setUsrId("U1");

        UserInfoModel b = new UserInfoModel();
        b.setTentId("T1");
        b.setUsrId("U1");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notEqual_differentIds() {
        UserInfoModel a = new UserInfoModel();
        a.setTentId("T1");
        a.setUsrId("U1");

        UserInfoModel b = new UserInfoModel();
        b.setTentId("T1");
        b.setUsrId("U2");

        assertNotEquals(a, b);
    }
}
