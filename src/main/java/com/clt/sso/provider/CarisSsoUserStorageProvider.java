package com.clt.sso.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import org.apache.ibatis.session.SqlSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.clt.sso.mapper.UserInfoMapper;
import com.clt.sso.model.UserInfoAdapter;
import com.clt.sso.model.UserInfoModel;

public class CarisSsoUserStorageProvider implements UserStorageProvider, UserLookupProvider, CredentialInputValidator {

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

    // --- UserStorageProvider ---

    @Override
    public void close() {
        sqlSession.close();
    }
}
