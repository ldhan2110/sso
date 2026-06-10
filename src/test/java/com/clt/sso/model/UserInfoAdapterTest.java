package com.clt.sso.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserInfoAdapterTest {

    private UserInfoModel entity;
    private UserInfoAdapter adapter;
    private KeycloakSession session;
    private RealmModel realm;
    private ComponentModel componentModel;
    private UserFederatedStorageProvider federatedStorage;

    @BeforeEach
    void setUp() {
        entity = new UserInfoModel();
        entity.setTentId("TENANT1");
        entity.setUsrId("USER001");
        entity.setUsrNm("johndoe");
        entity.setUsrEml("john@example.com");
        entity.setActFlg("Y");
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setEmailVerified(true);
        entity.setCreDt(1700000000L);

        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        componentModel = mock(ComponentModel.class);
        when(componentModel.getId()).thenReturn("provider-123");

        // Mock federated storage to return empty attributes by default
        federatedStorage = mock(UserFederatedStorageProvider.class);
        when(federatedStorage.getAttributes(eq(realm), anyString()))
                .thenReturn(new MultivaluedHashMap<>());

        // Wire session to return federated storage
        // AbstractUserAdapterFederatedStorage calls UserStorageUtil.userFederatedStorage(session)
        // which internally calls session.getProvider(UserFederatedStorageProvider.class)
        when(session.getProvider(UserFederatedStorageProvider.class))
                .thenReturn(federatedStorage);

        adapter = new UserInfoAdapter(session, realm, componentModel, entity);
    }

    // --- Identity ---

    @Test
    void getId_returnsStorageIdFormat() {
        String id = adapter.getId();
        // StorageId format: "f:<providerId>:<externalId>"
        assertTrue(id.contains("provider-123"));
        assertTrue(id.contains("TENANT1::USER001"));
    }

    @Test
    void getUsername_returnsTenantPrefixedUsername() {
        assertEquals("TENANT1::johndoe", adapter.getUsername());
    }

    @Test
    void getEmail_returnsEntityEmail() {
        assertEquals("john@example.com", adapter.getEmail());
    }

    @Test
    void getFirstName_returnsEntityFirstName() {
        assertEquals("John", adapter.getFirstName());
    }

    @Test
    void getLastName_returnsEntityLastName() {
        assertEquals("Doe", adapter.getLastName());
    }

    @Test
    void isEmailVerified_returnsEntityValue() {
        assertTrue(adapter.isEmailVerified());
    }

    @Test
    void isEnabled_trueWhenActFlgY() {
        assertTrue(adapter.isEnabled());
    }

    @Test
    void isEnabled_falseWhenActFlgN() {
        entity.setActFlg("N");
        assertFalse(adapter.isEnabled());
    }

    @Test
    void isEnabled_falseWhenActFlgNull() {
        entity.setActFlg(null);
        assertFalse(adapter.isEnabled());
    }

    @Test
    void getCreatedTimestamp_returnsEntityValue() {
        assertEquals(1700000000L, adapter.getCreatedTimestamp());
    }

    // --- Read-only setters ---

    @Test
    void setters_areNoOps() {
        adapter.setUsername("newuser");
        assertEquals("TENANT1::johndoe", adapter.getUsername());

        adapter.setEmail("new@example.com");
        assertEquals("john@example.com", adapter.getEmail());

        adapter.setFirstName("Jane");
        assertEquals("John", adapter.getFirstName());

        adapter.setLastName("Smith");
        assertEquals("Doe", adapter.getLastName());

        adapter.setEmailVerified(false);
        assertTrue(adapter.isEmailVerified());

        adapter.setEnabled(false);
        assertTrue(adapter.isEnabled());

        adapter.setCreatedTimestamp(9999L);
        assertEquals(1700000000L, adapter.getCreatedTimestamp());
    }

    // --- Attributes ---

    @Test
    void getFirstAttribute_returnsKnownFields() {
        assertEquals("John", adapter.getFirstAttribute(UserModel.FIRST_NAME));
        assertEquals("Doe", adapter.getFirstAttribute(UserModel.LAST_NAME));
        assertEquals("john@example.com", adapter.getFirstAttribute(UserModel.EMAIL));
        assertEquals("TENANT1::johndoe", adapter.getFirstAttribute(UserModel.USERNAME));
    }

    @Test
    void getFirstAttribute_returnsCustomAttribute() {
        Map<String, List<String>> custom = new HashMap<>();
        custom.put("phone", List.of("+1234567890"));
        entity.setCustomAttributes(custom);

        assertEquals("+1234567890", adapter.getFirstAttribute("phone"));
    }

    @Test
    void getFirstAttribute_fallsBackToFederatedStorage() {
        MultivaluedHashMap<String, String> fedAttrs = new MultivaluedHashMap<>();
        fedAttrs.put("department", List.of("Engineering"));
        when(federatedStorage.getAttributes(eq(realm), anyString()))
                .thenReturn(fedAttrs);

        assertEquals("Engineering", adapter.getFirstAttribute("department"));
    }

    @Test
    void getFirstAttribute_entityCustomAttributeWinsOverFederated() {
        // Entity has "phone"
        Map<String, List<String>> custom = new HashMap<>();
        custom.put("phone", List.of("+111"));
        entity.setCustomAttributes(custom);

        // Federated also has "phone"
        MultivaluedHashMap<String, String> fedAttrs = new MultivaluedHashMap<>();
        fedAttrs.put("phone", List.of("+999"));
        when(federatedStorage.getAttributes(eq(realm), anyString()))
                .thenReturn(fedAttrs);

        // Entity wins
        assertEquals("+111", adapter.getFirstAttribute("phone"));
    }

    @Test
    void getAttributes_mergesEntityAndFederated() {
        Map<String, List<String>> custom = new HashMap<>();
        custom.put("phone", List.of("+1234567890"));
        entity.setCustomAttributes(custom);

        MultivaluedHashMap<String, String> fedAttrs = new MultivaluedHashMap<>();
        fedAttrs.put("department", List.of("Engineering"));
        when(federatedStorage.getAttributes(eq(realm), anyString()))
                .thenReturn(fedAttrs);

        Map<String, List<String>> attrs = adapter.getAttributes();
        assertEquals(List.of("+1234567890"), attrs.get("phone"));
        assertEquals(List.of("Engineering"), attrs.get("department"));
        assertEquals(List.of("John"), attrs.get(UserModel.FIRST_NAME));
        assertEquals(List.of("TENANT1::johndoe"), attrs.get(UserModel.USERNAME));
    }

    @Test
    void getAttributeStream_returnsCustomAttribute() {
        Map<String, List<String>> custom = new HashMap<>();
        custom.put("roles_ext", List.of("admin", "user"));
        entity.setCustomAttributes(custom);

        List<String> result = adapter.getAttributeStream("roles_ext").toList();
        assertEquals(List.of("admin", "user"), result);
    }

    // --- Entity access ---

    @Test
    void getEntity_returnsWrappedEntity() {
        assertSame(entity, adapter.getEntity());
    }
}
