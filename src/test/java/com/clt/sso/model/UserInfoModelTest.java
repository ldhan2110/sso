package com.clt.sso.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.UserModel;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class UserInfoModelTest {

    private UserInfoModel model;

    @BeforeEach
    void setUp() throws Exception {
        model = new UserInfoModel();
        setField(model, "tentId", "TENANT1");
        setField(model, "usrId", "USR001");
        setField(model, "usrNm", "john");
        setField(model, "usrEml", "john@example.com");
        setField(model, "actFlg", "Y");
        setField(model, "firstName", "John");
        setField(model, "lastName", "Doe");
        setField(model, "emailVerified", true);
        setField(model, "creDt", 1700000000000L);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // 1. getId returns tentId::usrId
    @Test
    void getId_returnsTenantColonColonUsrId() {
        assertEquals("TENANT1::USR001", model.getId());
    }

    // 2. getUsername returns tentId::usrNm
    @Test
    void getUsername_returnsTenantColonColonUsrNm() {
        assertEquals("TENANT1::john", model.getUsername());
    }

    // 3. isEnabled true when actFlg="Y", false when "N"
    @Test
    void isEnabled_trueWhenActFlgY() {
        assertTrue(model.isEnabled());
    }

    @Test
    void isEnabled_falseWhenActFlgN() throws Exception {
        setField(model, "actFlg", "N");
        assertFalse(model.isEnabled());
    }

    // 4. getFirstAttribute for each known field
    @Test
    void getFirstAttribute_firstName() {
        assertEquals("John", model.getFirstAttribute(UserModel.FIRST_NAME));
    }

    @Test
    void getFirstAttribute_lastName() {
        assertEquals("Doe", model.getFirstAttribute(UserModel.LAST_NAME));
    }

    @Test
    void getFirstAttribute_email() {
        assertEquals("john@example.com", model.getFirstAttribute(UserModel.EMAIL));
    }

    @Test
    void getFirstAttribute_username() {
        assertEquals("john", model.getFirstAttribute(UserModel.USERNAME));
    }

    @Test
    void getFirstAttribute_emailVerified() {
        assertEquals("true", model.getFirstAttribute("emailVerified"));
    }

    // 5. getFirstAttribute for custom attribute from map
    @Test
    @SuppressWarnings("unchecked")
    void getFirstAttribute_customAttribute() throws Exception {
        Field f = model.getClass().getDeclaredField("customAttributes");
        f.setAccessible(true);
        Map<String, List<String>> attrs = (Map<String, List<String>>) f.get(model);
        attrs.put("department", List.of("Engineering", "R&D"));
        assertEquals("Engineering", model.getFirstAttribute("department"));
    }

    // 6. getFirstAttribute returns null for unknown attribute
    @Test
    void getFirstAttribute_unknownReturnsNull() {
        assertNull(model.getFirstAttribute("nonExistentAttribute"));
    }

    // 7. getAttributeStream returns stream for known field
    @Test
    void getAttributeStream_knownField() {
        List<String> result = model.getAttributeStream(UserModel.FIRST_NAME).collect(Collectors.toList());
        assertEquals(List.of("John"), result);
    }

    // 8. getAttributeStream returns multi-valued stream for custom attribute
    @Test
    @SuppressWarnings("unchecked")
    void getAttributeStream_customAttributeMultiValued() throws Exception {
        Field f = model.getClass().getDeclaredField("customAttributes");
        f.setAccessible(true);
        Map<String, List<String>> attrs = (Map<String, List<String>>) f.get(model);
        attrs.put("roles", List.of("admin", "user", "viewer"));

        List<String> result = model.getAttributeStream("roles").collect(Collectors.toList());
        assertEquals(List.of("admin", "user", "viewer"), result);
    }

    // 9. getAttributeStream returns empty for unknown
    @Test
    void getAttributeStream_unknownReturnsEmpty() {
        List<String> result = model.getAttributeStream("doesNotExist").collect(Collectors.toList());
        assertTrue(result.isEmpty());
    }

    // 10. getAttributes merges all fields + custom into unmodifiable map
    @Test
    @SuppressWarnings("unchecked")
    void getAttributes_mergesAllFields() throws Exception {
        Field f = model.getClass().getDeclaredField("customAttributes");
        f.setAccessible(true);
        Map<String, List<String>> attrs = (Map<String, List<String>>) f.get(model);
        attrs.put("locale", List.of("en"));

        Map<String, List<String>> result = model.getAttributes();

        assertEquals(List.of("John"), result.get(UserModel.FIRST_NAME));
        assertEquals(List.of("Doe"), result.get(UserModel.LAST_NAME));
        assertEquals(List.of("john@example.com"), result.get(UserModel.EMAIL));
        assertEquals(List.of("john"), result.get(UserModel.USERNAME));
        assertEquals(List.of("true"), result.get("emailVerified"));
        assertEquals(List.of("en"), result.get("locale"));
    }

    // 11. getAttributes returns unmodifiable map (put throws)
    @Test
    void getAttributes_isUnmodifiable() {
        Map<String, List<String>> result = model.getAttributes();
        assertThrows(UnsupportedOperationException.class, () -> result.put("newKey", List.of("value")));
    }

    // 12. getFirstName/getLastName/getEmail/isEmailVerified return correct values
    @Test
    void getters_returnCorrectValues() {
        assertEquals("John", model.getFirstName());
        assertEquals("Doe", model.getLastName());
        assertEquals("john@example.com", model.getEmail());
        assertTrue(model.isEmailVerified());
    }

    // 13. All setters are no-ops
    @Test
    void setters_areNoOps() throws Exception {
        model.setUsername("changed");
        model.setEnabled(false);
        model.setFirstName("Changed");
        model.setLastName("Changed");
        model.setEmail("changed@example.com");
        model.setEmailVerified(false);
        model.setCreatedTimestamp(999L);
        model.setSingleAttribute("key", "val");
        model.setAttribute("key", List.of("val"));
        model.removeAttribute("key");
        model.setFederationLink("link");
        model.setServiceAccountClientLink("link");
        model.addRequiredAction("action");
        model.removeRequiredAction("action");

        assertEquals("TENANT1::john", model.getUsername());
        assertTrue(model.isEnabled());
        assertEquals("John", model.getFirstName());
        assertEquals("Doe", model.getLastName());
        assertEquals("john@example.com", model.getEmail());
        assertTrue(model.isEmailVerified());
        assertEquals(1700000000000L, model.getCreatedTimestamp());
        assertNull(model.getFederationLink());
        assertNull(model.getServiceAccountClientLink());
    }

    // 14. Role methods return empty/false
    @Test
    void roleMethods_returnEmptyOrFalse() {
        assertTrue(model.getRealmRoleMappingsStream().findAny().isEmpty());
        assertTrue(model.getClientRoleMappingsStream(null).findAny().isEmpty());
        assertTrue(model.getRoleMappingsStream().findAny().isEmpty());
        assertFalse(model.hasRole(null));
        // grantRole and deleteRoleMapping are no-ops — just verify no exception
        assertDoesNotThrow(() -> model.grantRole(null));
        assertDoesNotThrow(() -> model.deleteRoleMapping(null));
    }

    // 15. Group methods return empty/false
    @Test
    void groupMethods_returnEmptyOrFalse() {
        assertTrue(model.getGroupsStream().findAny().isEmpty());
        assertFalse(model.isMemberOf(null));
        assertDoesNotThrow(() -> model.joinGroup(null));
        assertDoesNotThrow(() -> model.leaveGroup(null));
    }

    // 16. Required actions return empty
    @Test
    void requiredActions_returnEmpty() {
        assertTrue(model.getRequiredActionsStream().findAny().isEmpty());
    }

    // 17. Federation/service account links return null
    @Test
    void federationAndServiceAccountLinks_returnNull() {
        assertNull(model.getFederationLink());
        assertNull(model.getServiceAccountClientLink());
    }

    // 18. credentialManager() returns non-null
    @Test
    void credentialManager_returnsNonNull() {
        assertNotNull(model.credentialManager());
    }
}
