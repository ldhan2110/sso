package com.clt.sso.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRealmRoleMappingsStream'");
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getClientRoleMappingsStream'");
    }

    @Override
    public boolean hasRole(RoleModel role) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasRole'");
    }

    @Override
    public void grantRole(RoleModel role) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'grantRole'");
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRoleMappingsStream'");
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteRoleMapping'");
    }

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

    @Override
    public void setSingleAttribute(String name, String value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSingleAttribute'");
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setAttribute'");
    }

    @Override
    public void removeAttribute(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeAttribute'");
    }

    @Override
    public String getFirstAttribute(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFirstAttribute'");
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttributeStream'");
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttributes'");
    }

    @Override
    public Stream<String> getRequiredActionsStream() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRequiredActionsStream'");
    }

    @Override
    public void addRequiredAction(String action) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addRequiredAction'");
    }

    @Override
    public void removeRequiredAction(String action) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeRequiredAction'");
    }

    @Override
    public String getFirstName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFirstName'");
    }

    @Override
    public void setFirstName(String firstName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setFirstName'");
    }

    @Override
    public String getLastName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLastName'");
    }

    @Override
    public void setLastName(String lastName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setLastName'");
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isEmailVerified'");
    }

    @Override
    public void setEmailVerified(boolean verified) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setEmailVerified'");
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getGroupsStream'");
    }

    @Override
    public void joinGroup(GroupModel group) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'joinGroup'");
    }

    @Override
    public void leaveGroup(GroupModel group) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'leaveGroup'");
    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isMemberOf'");
    }

    @Override
    public String getFederationLink() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFederationLink'");
    }

    @Override
    public void setFederationLink(String link) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setFederationLink'");
    }

    @Override
    public String getServiceAccountClientLink() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getServiceAccountClientLink'");
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setServiceAccountClientLink'");
    }

    @Override
    public SubjectCredentialManager credentialManager() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'credentialManager'");
    }
    
}
