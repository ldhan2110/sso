package com.clt.sso.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;

public class UserInfoModel implements UserModel {
    private String tentId;
    private String usrId;
    private String usrNm;
    private String usrPwd;
    private String usrEml;
    private String actFlg;
    private long creDt;
    private String creUsrId;
	private long updDt;
    private String updUsrId;
    private Map<String, List<String>> customAttributes = new HashMap<>();
    private String firstName;
    private String lastName;
    private boolean emailVerified;

    // ── Role mappings ────────────────────────────────────────────────────────

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        return Stream.empty();
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        return Stream.empty();
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return false;
    }

    @Override
    public void grantRole(RoleModel role) {
        // Keycloak-managed — no-op
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        return Stream.empty();
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        // Read-only storage — no-op
    }

    // ── Identity ─────────────────────────────────────────────────────────────

    @Override
    public String getId() {
        return this.tentId + "::" + this.usrId;
    }

    @Override
    public String getUsername() {
        return this.tentId + "::" + this.usrNm;
    }

    @Override
    public void setUsername(String username) {
        this.usrNm = username;
    }

    @Override
    public Long getCreatedTimestamp() {
        return this.creDt;
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        this.creDt = timestamp;
    }

    @Override
    public boolean isEnabled() {
        return "Y".equals(actFlg);
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.actFlg = enabled ? "Y" : "N";
    }

    // ── Attributes ───────────────────────────────────────────────────────────

    @Override
    public void setSingleAttribute(String name, String value) {
        // Read-only storage — no-op
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        // Read-only storage — no-op
    }

    @Override
    public void removeAttribute(String name) {
        // Read-only storage — no-op
    }

    @Override
    public String getFirstAttribute(String name) {
        switch (name) {
            case UserModel.FIRST_NAME:     return this.firstName;
            case UserModel.LAST_NAME:      return this.lastName;
            case UserModel.EMAIL:          return this.usrEml;
            case UserModel.USERNAME:       return this.usrNm;
            case "emailVerified":          return String.valueOf(this.emailVerified);
            default:
                List<String> values = customAttributes.get(name);
                return (values != null && !values.isEmpty()) ? values.get(0) : null;
        }
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        String first = getFirstAttribute(name);
        if (first == null) {
            return Stream.empty();
        }
        List<String> values = customAttributes.get(name);
        if (values != null) {
            return values.stream();
        }
        return Stream.of(first);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attrs = new HashMap<>(customAttributes);
        if (this.firstName != null)  attrs.put(UserModel.FIRST_NAME, List.of(this.firstName));
        if (this.lastName != null)   attrs.put(UserModel.LAST_NAME,  List.of(this.lastName));
        if (this.usrEml != null)     attrs.put(UserModel.EMAIL,      List.of(this.usrEml));
        if (this.usrNm != null)      attrs.put(UserModel.USERNAME,   List.of(this.usrNm));
        attrs.put("emailVerified", List.of(String.valueOf(this.emailVerified)));
        return Collections.unmodifiableMap(attrs);
    }

    // ── Required actions ─────────────────────────────────────────────────────

    @Override
    public Stream<String> getRequiredActionsStream() {
        return Stream.empty();
    }

    @Override
    public void addRequiredAction(String action) {
        // Read-only storage — no-op
    }

    @Override
    public void removeRequiredAction(String action) {
        // Read-only storage — no-op
    }

    // ── Name / email ─────────────────────────────────────────────────────────

    @Override
    public String getFirstName() {
        return this.firstName;
    }

    @Override
    public void setFirstName(String firstName) {
        // Read-only storage — no-op
    }

    @Override
    public String getLastName() {
        return this.lastName;
    }

    @Override
    public void setLastName(String lastName) {
        // Read-only storage — no-op
    }

    @Override
    public String getEmail() {
        return this.usrEml;
    }

    @Override
    public void setEmail(String email) {
        this.usrEml = email;
    }

    @Override
    public boolean isEmailVerified() {
        return this.emailVerified;
    }

    @Override
    public void setEmailVerified(boolean verified) {
        // Read-only storage — no-op
    }

    // ── Groups ───────────────────────────────────────────────────────────────

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return Stream.empty();
    }

    @Override
    public void joinGroup(GroupModel group) {
        // Keycloak-managed — no-op
    }

    @Override
    public void leaveGroup(GroupModel group) {
        // Read-only storage — no-op
    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        return false;
    }

    // ── Federation / service account ─────────────────────────────────────────

    @Override
    public String getFederationLink() {
        return null;
    }

    @Override
    public void setFederationLink(String link) {
        // Read-only storage — no-op
    }

    @Override
    public String getServiceAccountClientLink() {
        return null;
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        // Read-only storage — no-op
    }

    // ── Credential manager ───────────────────────────────────────────────────

    @Override
    public SubjectCredentialManager credentialManager() {
        return new SubjectCredentialManager() {
            @Override
            public boolean isValid(List<CredentialInput> inputs) {
                return false;
            }

            @Override
            public boolean updateCredential(CredentialInput input) {
                return false;
            }

            @Override
            public void updateStoredCredential(CredentialModel cred) {
                // no-op
            }

            @Override
            public CredentialModel createStoredCredential(CredentialModel cred) {
                return null;
            }

            @Override
            public boolean removeStoredCredentialById(String id) {
                return false;
            }

            @Override
            public CredentialModel getStoredCredentialById(String id) {
                return null;
            }

            @Override
            public Stream<CredentialModel> getStoredCredentialsStream() {
                return Stream.empty();
            }

            @Override
            public Stream<CredentialModel> getStoredCredentialsByTypeStream(String type) {
                return Stream.empty();
            }

            @Override
            public CredentialModel getStoredCredentialByNameAndType(String name, String type) {
                return null;
            }

            @Override
            public boolean moveStoredCredentialTo(String id, String newPreviousCredentialId) {
                return false;
            }

            @Override
            public void updateCredentialLabel(String credentialId, String credentialLabel) {
                // no-op
            }

            @Override
            public void disableCredentialType(String credentialType) {
                // no-op
            }

            @Override
            public Stream<String> getDisableableCredentialTypesStream() {
                return Stream.empty();
            }

            @Override
            public boolean isConfiguredFor(String type) {
                return false;
            }

            @Override
            @Deprecated
            public boolean isConfiguredLocally(String type) {
                return false;
            }

            @Override
            @Deprecated
            public Stream<String> getConfiguredUserStorageCredentialTypesStream() {
                return Stream.empty();
            }

            @Override
            @Deprecated
            public CredentialModel createCredentialThroughProvider(CredentialModel model) {
                return null;
            }
        };
    }
}
