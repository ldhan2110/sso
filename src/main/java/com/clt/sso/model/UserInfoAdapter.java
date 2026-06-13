package com.clt.sso.model;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

public class UserInfoAdapter extends AbstractUserAdapterFederatedStorage {

    private final UserInfoModel entity;

    public UserInfoAdapter(KeycloakSession session, RealmModel realm,
                           ComponentModel storageProviderModel, UserInfoModel entity) {
        super(session, realm, storageProviderModel);
        this.entity = entity;
        this.storageId = new StorageId(storageProviderModel.getId(),
                entity.getTentId() + "::" + entity.getUsrId());
    }

    @Override
    public String getUsername() {
        return entity.getTentId() + "::" + entity.getUsrNm();
    }

    @Override
    public void setUsername(String username) {
        String[] parts = username.split("::", 2);
        if (parts.length == 2) {
            entity.setUsrNm(parts[1]);
        }
    }

    @Override
    public String getEmail() {
        return entity.getUsrEml();
    }

    @Override
    public void setEmail(String email) {
        entity.setUsrEml(email);
    }

    @Override
    public String getFirstName() {
        return entity.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        entity.setFirstName(firstName);
    }

    @Override
    public String getLastName() {
        return entity.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        entity.setLastName(lastName);
    }

    @Override
    public boolean isEmailVerified() {
        return entity.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        entity.setEmailVerified(verified);
    }

    @Override
    public boolean isEnabled() {
        return "Y".equals(entity.getActFlg());
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setActFlg(enabled ? "Y" : "N");
    }

    @Override
    public Long getCreatedTimestamp() {
        return entity.getCreDt().atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        // Read-only external storage — no-op
    }

    @Override
    public String getFirstAttribute(String name) {
        // Check entity custom attributes first, then fall back to federated storage
        switch (name) {
            case UserModel.FIRST_NAME: return entity.getFirstName();
            case UserModel.LAST_NAME:  return entity.getLastName();
            case UserModel.EMAIL:      return entity.getUsrEml();
            case UserModel.USERNAME:   return getUsername();
            case "emailVerified":      return String.valueOf(entity.isEmailVerified());
            default:
                List<String> values = entity.getCustomAttributes().get(name);
                if (values != null && !values.isEmpty()) {
                    return values.get(0);
                }
                return super.getFirstAttribute(name);
        }
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        switch (name) {
            case UserModel.FIRST_NAME:
            case UserModel.LAST_NAME:
            case UserModel.EMAIL:
            case UserModel.USERNAME:
            case "emailVerified":
                String val = getFirstAttribute(name);
                return val != null ? Stream.of(val) : Stream.empty();
            default:
                List<String> values = entity.getCustomAttributes().get(name);
                if (values != null) {
                    return values.stream();
                }
                return super.getAttributeStream(name);
        }
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> attrs = new MultivaluedHashMap<>();

        // Start with federated storage attributes
        Map<String, List<String>> federated = super.getAttributes();
        if (federated != null) {
            attrs.putAll(federated);
        }

        // Entity custom attributes override federated
        attrs.putAll(entity.getCustomAttributes());

        // Entity identity fields always win
        if (entity.getFirstName() != null) attrs.put(UserModel.FIRST_NAME, List.of(entity.getFirstName()));
        if (entity.getLastName() != null)  attrs.put(UserModel.LAST_NAME,  List.of(entity.getLastName()));
        if (entity.getUsrEml() != null)    attrs.put(UserModel.EMAIL,      List.of(entity.getUsrEml()));
        attrs.put(UserModel.USERNAME, List.of(getUsername()));
        attrs.put("emailVerified", List.of(String.valueOf(entity.isEmailVerified())));

        return attrs;
    }

    public UserInfoModel getEntity() {
        return entity;
    }
}
