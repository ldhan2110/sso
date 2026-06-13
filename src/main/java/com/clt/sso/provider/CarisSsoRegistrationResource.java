package com.clt.sso.provider;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserCredentialModel;

import com.clt.sso.model.RegistrationRequest;
import com.clt.sso.model.UserInfoModel;
import com.clt.sso.mapper.UserInfoMapper;
import com.clt.sso.utils.SessionFactory;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.ibatis.session.SqlSession;

public class CarisSsoRegistrationResource {

    private final KeycloakSession session;

    public CarisSsoRegistrationResource(KeycloakSession session) {
        this.session = session;
    }

    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(RegistrationRequest req) {
        RealmModel realm = session.getContext().getRealm();

        // Validate required fields
        if (req.getTentId() == null || req.getTentId().isBlank()) {
            return errorResponse(400, "tentId is required");
        }
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            return errorResponse(400, "username is required");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            return errorResponse(400, "password is required");
        }

        String fullUsername = req.getTentId() + "::" + req.getUsername();

        // Check if user already exists
        UserModel existing = session.users().getUserByUsername(realm, fullUsername);
        if (existing != null) {
            return errorResponse(409, "User already exists: " + fullUsername);
        }

        // Create user (triggers CarisSsoUserStorageProvider.addUser())
        UserModel user = session.users().addUser(realm, fullUsername);
        if (user == null) {
            return errorResponse(500, "Failed to create user");
        }

        // Set user attributes
        if (req.getFirstName() != null) {
            user.setFirstName(req.getFirstName());
        }
        if (req.getLastName() != null) {
            user.setLastName(req.getLastName());
        }
        if (req.getEmail() != null) {
            user.setEmail(req.getEmail());
        }
        user.setEnabled(true);

        // Set password
        user.credentialManager().updateCredential(
                UserCredentialModel.password(req.getPassword(), false));

        // Update entity in DB with the additional fields
        try (SqlSession sqlSession = SessionFactory.getSqlSession()) {
            UserInfoMapper mapper = sqlSession.getMapper(UserInfoMapper.class);
            UserInfoModel entity = mapper.findByTentIdAndUsrId(req.getTentId(), req.getUsername());
            if (entity != null) {
                entity.setFirstName(req.getFirstName());
                entity.setLastName(req.getLastName());
                entity.setUsrEml(req.getEmail());
                entity.setUpdDt(LocalDateTime.now());
                entity.setUpdUsrId("api");
                mapper.updateUser(entity);
                sqlSession.commit();
            }
        }

        // Assign groups
        List<String> assignedGroups = new ArrayList<>();
        List<String> failedGroups = new ArrayList<>();
        if (req.getGroups() != null) {
            for (String groupName : req.getGroups()) {
                GroupModel group = findGroupByName(realm, groupName);
                if (group != null) {
                    user.joinGroup(group);
                    assignedGroups.add(groupName);
                } else {
                    failedGroups.add(groupName);
                }
            }
        }

        // Build response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", fullUsername);
        result.put("firstName", req.getFirstName());
        result.put("lastName", req.getLastName());
        result.put("email", req.getEmail());
        result.put("assignedGroups", assignedGroups);
        if (!failedGroups.isEmpty()) {
            result.put("groupsNotFound", failedGroups);
        }

        return Response.status(201).entity(result).build();
    }

    private GroupModel findGroupByName(RealmModel realm, String name) {
        return session.groups().getGroupsStream(realm)
                .filter(g -> g.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private Response errorResponse(int status, String message) {
        return Response.status(status)
                .entity(Map.of("error", message))
                .build();
    }
}
