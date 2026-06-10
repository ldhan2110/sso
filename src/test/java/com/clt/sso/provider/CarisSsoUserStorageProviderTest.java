package com.clt.sso.provider;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.clt.sso.mapper.UserInfoMapper;
import com.clt.sso.model.UserInfoAdapter;
import com.clt.sso.model.UserInfoModel;

import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CarisSsoUserStorageProviderTest {

    private CarisSsoUserStorageProvider provider;
    private KeycloakSession session;
    private RealmModel realm;
    private ComponentModel componentModel;
    private SqlSession sqlSession;
    private UserInfoMapper mapper;
    private UserFederatedStorageProvider federatedStorage;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        componentModel = mock(ComponentModel.class);
        when(componentModel.getId()).thenReturn("provider-123");

        sqlSession = mock(SqlSession.class);
        mapper = mock(UserInfoMapper.class);
        when(sqlSession.getMapper(UserInfoMapper.class)).thenReturn(mapper);

        // Mock federated storage for UserInfoAdapter
        federatedStorage = mock(UserFederatedStorageProvider.class);
        when(federatedStorage.getAttributes(eq(realm), anyString()))
                .thenReturn(new MultivaluedHashMap<>());
        when(session.getProvider(UserFederatedStorageProvider.class))
                .thenReturn(federatedStorage);

        provider = new CarisSsoUserStorageProvider(session, componentModel, sqlSession);
    }

    private UserInfoModel createEntity() {
        UserInfoModel entity = new UserInfoModel();
        entity.setTentId("T1");
        entity.setUsrId("U1");
        entity.setUsrNm("johndoe");
        entity.setUsrEml("john@example.com");
        entity.setActFlg("Y");
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setUsrPwd(BCrypt.withDefaults().hashToString(10, "secret123".toCharArray()));
        return entity;
    }

    // --- getUserById ---

    @Test
    void getUserById_returnsAdapter() {
        UserInfoModel entity = createEntity();
        when(mapper.findByTentIdAndUsrId("T1", "U1")).thenReturn(entity);

        UserModel user = provider.getUserById(realm, "f:provider-123:T1::U1");
        assertNotNull(user);
        assertInstanceOf(UserInfoAdapter.class, user);
        assertEquals("T1::johndoe", user.getUsername());
    }

    @Test
    void getUserById_returnsNull_whenNotFound() {
        when(mapper.findByTentIdAndUsrId("T1", "MISSING")).thenReturn(null);

        UserModel user = provider.getUserById(realm, "f:provider-123:T1::MISSING");
        assertNull(user);
    }

    @Test
    void getUserById_returnsNull_whenInvalidFormat() {
        UserModel user = provider.getUserById(realm, "f:provider-123:badformat");
        assertNull(user);
    }

    // --- getUserByUsername ---

    @Test
    void getUserByUsername_returnsAdapter() {
        UserInfoModel entity = createEntity();
        when(mapper.findByTentIdAndUsrNm("T1", "johndoe")).thenReturn(entity);

        UserModel user = provider.getUserByUsername(realm, "T1::johndoe");
        assertNotNull(user);
        assertInstanceOf(UserInfoAdapter.class, user);
    }

    @Test
    void getUserByUsername_returnsNull_whenNotFound() {
        when(mapper.findByTentIdAndUsrNm("T1", "nobody")).thenReturn(null);

        UserModel user = provider.getUserByUsername(realm, "T1::nobody");
        assertNull(user);
    }

    @Test
    void getUserByUsername_returnsNull_whenInvalidFormat() {
        UserModel user = provider.getUserByUsername(realm, "no-separator");
        assertNull(user);
    }

    // --- getUserByEmail ---

    @Test
    void getUserByEmail_returnsNull() {
        assertNull(provider.getUserByEmail(realm, "john@example.com"));
    }

    // --- CredentialInputValidator ---

    @Test
    void supportsCredentialType_password() {
        assertTrue(provider.supportsCredentialType(PasswordCredentialModel.TYPE));
    }

    @Test
    void supportsCredentialType_rejectsOther() {
        assertFalse(provider.supportsCredentialType("otp"));
    }

    @Test
    void isValid_correctPassword() {
        UserInfoModel entity = createEntity();
        when(mapper.findByTentIdAndUsrNm("T1", "johndoe")).thenReturn(entity);

        UserModel user = provider.getUserByUsername(realm, "T1::johndoe");

        CredentialInput input = mock(CredentialInput.class);
        when(input.getType()).thenReturn(PasswordCredentialModel.TYPE);
        when(input.getChallengeResponse()).thenReturn("secret123");

        assertTrue(provider.isValid(realm, user, input));
    }

    @Test
    void isValid_wrongPassword() {
        UserInfoModel entity = createEntity();
        when(mapper.findByTentIdAndUsrNm("T1", "johndoe")).thenReturn(entity);

        UserModel user = provider.getUserByUsername(realm, "T1::johndoe");

        CredentialInput input = mock(CredentialInput.class);
        when(input.getType()).thenReturn(PasswordCredentialModel.TYPE);
        when(input.getChallengeResponse()).thenReturn("wrongpassword");

        assertFalse(provider.isValid(realm, user, input));
    }

    @Test
    void isValid_nullPassword() {
        UserInfoModel entity = createEntity();
        entity.setUsrPwd(null);
        when(mapper.findByTentIdAndUsrNm("T1", "johndoe")).thenReturn(entity);

        UserModel user = provider.getUserByUsername(realm, "T1::johndoe");

        CredentialInput input = mock(CredentialInput.class);
        when(input.getType()).thenReturn(PasswordCredentialModel.TYPE);
        when(input.getChallengeResponse()).thenReturn("secret123");

        assertFalse(provider.isValid(realm, user, input));
    }

    @Test
    void isValid_wrongCredentialType() {
        UserInfoModel entity = createEntity();
        when(mapper.findByTentIdAndUsrNm("T1", "johndoe")).thenReturn(entity);

        UserModel user = provider.getUserByUsername(realm, "T1::johndoe");

        CredentialInput input = mock(CredentialInput.class);
        when(input.getType()).thenReturn("otp");

        assertFalse(provider.isValid(realm, user, input));
    }

    // --- close ---

    @Test
    void close_closesSqlSession() {
        provider.close();
        verify(sqlSession).close();
    }
}
