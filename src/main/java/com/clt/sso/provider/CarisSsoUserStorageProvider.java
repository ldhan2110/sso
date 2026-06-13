package com.clt.sso.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.apache.ibatis.session.SqlSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.clt.sso.mapper.UserInfoMapper;
import com.clt.sso.model.UserInfoAdapter;
import com.clt.sso.model.UserInfoModel;

public class CarisSsoUserStorageProvider implements UserStorageProvider, UserLookupProvider, UserQueryProvider, UserRegistrationProvider, CredentialInputValidator, CredentialInputUpdater {

    private final KeycloakSession session;
    private final ComponentModel model;
    private final SqlSession sqlSession;
    private final UserInfoMapper mapper;

    public CarisSsoUserStorageProvider(KeycloakSession session, ComponentModel model, SqlSession sqlSession) {
        this.session = session;
        this.model = model;
        this.sqlSession = sqlSession;
        this.mapper = sqlSession.getMapper(UserInfoMapper.class);
    }

    public UserInfoMapper getMapper() {
        return mapper;
    }

    public SqlSession getSqlSession() {
        return sqlSession;
    }

    // --- UserLookupProvider ---

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        String externalId = StorageId.externalId(id);
        String[] parts = externalId.split("::", 2);
        if (parts.length != 2) {
            return null;
        }
        UserInfoModel entity = mapper.findByTentIdAndUsrId(parts[0], parts[1]);
        if (entity == null) {
            return null;
        }
        return new UserInfoAdapter(session, realm, model, entity);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        String[] parts = username.split("::", 2);
        if (parts.length != 2) {
            return null;
        }
        UserInfoModel entity = mapper.findByTentIdAndUsrNm(parts[0], parts[1]);
        if (entity == null) {
            return null;
        }
        return new UserInfoAdapter(session, realm, model, entity);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    // --- CredentialInputValidator ---

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        if (!supportsCredentialType(credentialInput.getType())) {
            return false;
        }
        if (!(user instanceof UserInfoAdapter)) {
            return false;
        }
        UserInfoModel entity = ((UserInfoAdapter) user).getEntity();
        String storedHash = entity.getUsrPwd();
        if (storedHash == null) {
            return false;
        }
        String inputHash = md5Hex(credentialInput.getChallengeResponse());
        return storedHash.equals(inputHash);
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Integer.toHexString((0xF0 & b) >> 4));
                sb.append(Integer.toHexString(0x0F & b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    // --- UserQueryProvider ---

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        int first = firstResult != null ? firstResult : 0;
        int max = maxResults != null ? maxResults : Integer.MAX_VALUE;
        List<UserInfoModel> results = mapper.searchUsers(search, first, max);
        return results.stream()
                .map(entity -> (UserModel) new UserInfoAdapter(session, realm, model, entity));
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        String search = params.get(UserModel.SEARCH);
        if (search == null) {
            search = params.get(UserModel.USERNAME);
        }
        return searchForUserStream(realm, search != null ? search : "", firstResult, maxResults);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, org.keycloak.models.GroupModel group, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return Stream.empty();
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return mapper.countUsers(null);
    }

    // --- UserStorageProvider ---

    @Override
    public void close() {
        sqlSession.close();
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        String[] parts = username.split("::", 2);
        if (parts.length != 2) {
            return null;
        }
        String tentId = parts[0];
        String usrNm = parts[1];

        UserInfoModel entity = new UserInfoModel();
        entity.setTentId(tentId);
        entity.setUsrId(usrNm);
        entity.setUsrNm(usrNm);
        entity.setActFlg("Y");
        entity.setCreDt(LocalDateTime.now());
        entity.setCreUsrId("system");
        entity.setUpdDt(LocalDateTime.now());
        entity.setUpdUsrId("system");
        entity.setEmailVerified(false);

        mapper.insertUser(entity);
        sqlSession.commit();

        return new UserInfoAdapter(session, realm, model, entity);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        String externalId = StorageId.externalId(user.getId());
        String[] parts = externalId.split("::", 2);
        if (parts.length != 2) {
            return false;
        }
        mapper.deleteUser(parts[0], parts[1]);
        sqlSession.commit();
        return true;
    }

    // --- CredentialInputUpdater ---

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType())) {
            return false;
        }
        if (!(user instanceof UserInfoAdapter)) {
            return false;
        }
        UserInfoModel entity = ((UserInfoAdapter) user).getEntity();
        String hashedPwd = md5Hex(input.getChallengeResponse());
        mapper.updatePassword(entity.getTentId(), entity.getUsrId(), hashedPwd);
        sqlSession.commit();
        return true;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        // Not supported
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }
}
