package com.clt.sso.provider;

import com.clt.sso.model.UserInfoAdapter;
import com.clt.sso.model.UserInfoModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.representations.IDToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CarisSsoProtocolMapperTest {

    private CarisSsoProtocolMapper mapper;
    private IDToken token;
    private UserSessionModel userSession;
    private KeycloakSession keycloakSession;
    private ClientSessionContext clientSessionCtx;
    private ProtocolMapperModel mappingModel;

    @BeforeEach
    void setUp() {
        mapper = new CarisSsoProtocolMapper();
        token = new IDToken();
        userSession = mock(UserSessionModel.class);
        keycloakSession = mock(KeycloakSession.class);
        clientSessionCtx = mock(ClientSessionContext.class);
        mappingModel = mock(ProtocolMapperModel.class);
    }

    @Test
    void getId_returnsCarisSsoClaims() {
        assertEquals("caris-sso-claims-mapper", mapper.getId());
    }

    @Test
    void getDisplayType_returnsFriendlyName() {
        assertEquals("Caris SSO Claims", mapper.getDisplayType());
    }

    @Test
    void getDisplayCategory_returnsTokenMapper() {
        assertEquals("Token mapper", mapper.getDisplayCategory());
    }

    @Test
    void getHelpText_returnsDescription() {
        assertNotNull(mapper.getHelpText());
        assertFalse(mapper.getHelpText().isEmpty());
    }

    @Test
    void getConfigProperties_returnsEmptyList() {
        assertNotNull(mapper.getConfigProperties());
        assertTrue(mapper.getConfigProperties().isEmpty());
    }

    @Test
    void setClaim_addsTenanIdAndUserId_whenUserInfoAdapter() {
        UserInfoModel entity = new UserInfoModel();
        entity.setTentId("CNC");
        entity.setUsrId("cltmaster");

        KeycloakSession mockSession = mock(KeycloakSession.class);
        RealmModel mockRealm = mock(RealmModel.class);
        ComponentModel mockComponent = mock(ComponentModel.class);
        when(mockComponent.getId()).thenReturn("provider-id");

        UserInfoAdapter adapter = new UserInfoAdapter(mockSession, mockRealm, mockComponent, entity);
        when(userSession.getUser()).thenReturn(adapter);

        mapper.setClaim(token, mappingModel, userSession, keycloakSession, clientSessionCtx);

        assertEquals("CNC", token.getOtherClaims().get("tenant_id"));
        assertEquals("cltmaster", token.getOtherClaims().get("user_id"));
    }

    @Test
    void setClaim_doesNothing_whenNotUserInfoAdapter() {
        UserModel regularUser = mock(UserModel.class);
        when(userSession.getUser()).thenReturn(regularUser);

        mapper.setClaim(token, mappingModel, userSession, keycloakSession, clientSessionCtx);

        assertNull(token.getOtherClaims().get("tenant_id"));
        assertNull(token.getOtherClaims().get("user_id"));
    }

    @Test
    void setClaim_handlesNullTentId() {
        UserInfoModel entity = new UserInfoModel();
        entity.setTentId(null);
        entity.setUsrId("cltmaster");

        KeycloakSession mockSession = mock(KeycloakSession.class);
        RealmModel mockRealm = mock(RealmModel.class);
        ComponentModel mockComponent = mock(ComponentModel.class);
        when(mockComponent.getId()).thenReturn("provider-id");

        UserInfoAdapter adapter = new UserInfoAdapter(mockSession, mockRealm, mockComponent, entity);
        when(userSession.getUser()).thenReturn(adapter);

        mapper.setClaim(token, mappingModel, userSession, keycloakSession, clientSessionCtx);

        assertNull(token.getOtherClaims().get("tenant_id"));
        assertEquals("cltmaster", token.getOtherClaims().get("user_id"));
    }

    @Test
    void setClaim_handlesNullUsrId() {
        UserInfoModel entity = new UserInfoModel();
        entity.setTentId("CNC");
        entity.setUsrId(null);

        KeycloakSession mockSession = mock(KeycloakSession.class);
        RealmModel mockRealm = mock(RealmModel.class);
        ComponentModel mockComponent = mock(ComponentModel.class);
        when(mockComponent.getId()).thenReturn("provider-id");

        UserInfoAdapter adapter = new UserInfoAdapter(mockSession, mockRealm, mockComponent, entity);
        when(userSession.getUser()).thenReturn(adapter);

        mapper.setClaim(token, mappingModel, userSession, keycloakSession, clientSessionCtx);

        assertEquals("CNC", token.getOtherClaims().get("tenant_id"));
        assertNull(token.getOtherClaims().get("user_id"));
    }
}
