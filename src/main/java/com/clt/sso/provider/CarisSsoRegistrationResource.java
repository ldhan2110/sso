package com.clt.sso.provider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.keycloak.models.KeycloakSession;

import com.clt.sso.model.ChangePasswordRequest;
import com.clt.sso.model.RegistrationRequest;
import com.clt.sso.model.UpdateUserRequest;
import com.clt.sso.model.UserInfoModel;
import com.clt.sso.mapper.UserInfoMapper;
import com.clt.sso.service.UserSyncService;
import com.clt.sso.utils.SessionFactory;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.ibatis.session.SqlSession;

public class CarisSsoRegistrationResource {

    private final KeycloakSession session;
    private final UserSyncService userSyncService = new UserSyncService();

    public CarisSsoRegistrationResource(KeycloakSession session) {
        this.session = session;
    }

    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(RegistrationRequest req) {
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

        try (SqlSession sqlSession = SessionFactory.getSqlSession()) {
            UserInfoMapper mapper = sqlSession.getMapper(UserInfoMapper.class);

            // Check duplicate in external DB
            UserInfoModel existing = mapper.findByTentIdAndUsrNm(req.getTentId(), req.getUsername());
            if (existing != null) {
                return errorResponse(409, "User already exists: " + fullUsername);
            }

            // Insert directly into external SSO DB — no Keycloak local store
            UserInfoModel entity = new UserInfoModel();
            entity.setTentId(req.getTentId());
            entity.setUsrId(req.getUsername());
            entity.setUsrNm(req.getUsername());
            entity.setUsrPwd(md5Hex(req.getPassword()));
            entity.setFirstName(req.getFirstName());
            entity.setLastName(req.getLastName());
            entity.setUsrEml(req.getEmail());
            entity.setActFlg("Y");
            entity.setEmailVerified(false);
            entity.setCreDt(LocalDateTime.now());
            entity.setCreUsrId("api");
            entity.setUpdDt(LocalDateTime.now());
            entity.setUpdUsrId("api");

            mapper.insertUser(entity);
            sqlSession.commit();
        }

        // Sync to downstream apps (CARIS, WMS, etc.)
        userSyncService.syncUserToDownstream("create",
                req.getTentId(), req.getUsername(),
                req.getEmail(), req.getFirstName(), req.getLastName());

        // Build response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", fullUsername);
        result.put("firstName", req.getFirstName());
        result.put("lastName", req.getLastName());
        result.put("email", req.getEmail());

        return Response.status(201).entity(result).build();
    }

    @PUT
    @Path("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUser(UpdateUserRequest req) {
        if (req.getTentId() == null || req.getTentId().isBlank()) {
            return errorResponse(400, "tentId is required");
        }
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            return errorResponse(400, "username is required");
        }

        try (SqlSession sqlSession = SessionFactory.getSqlSession()) {
            UserInfoMapper mapper = sqlSession.getMapper(UserInfoMapper.class);

            UserInfoModel existing = mapper.findByTentIdAndUsrId(req.getTentId(), req.getUsername());
            if (existing == null) {
                return errorResponse(404, "User not found: " + req.getTentId() + "::" + req.getUsername());
            }

            existing.setFirstName(req.getFirstName());
            existing.setLastName(req.getLastName());
            existing.setUsrEml(req.getEmail());
            if (req.getActFlg() != null) {
                existing.setActFlg(req.getActFlg());
            }
            existing.setUpdDt(LocalDateTime.now());
            existing.setUpdUsrId("api");

            mapper.updateUser(existing);
            sqlSession.commit();
        }

        userSyncService.syncUserToDownstream("update",
                req.getTentId(), req.getUsername(),
                req.getEmail(), req.getFirstName(), req.getLastName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", req.getTentId() + "::" + req.getUsername());
        result.put("status", "updated");
        return Response.ok(result).build();
    }

    @DELETE
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@QueryParam("tentId") String tentId,
                               @QueryParam("username") String username) {
        if (tentId == null || tentId.isBlank()) {
            return errorResponse(400, "tentId is required");
        }
        if (username == null || username.isBlank()) {
            return errorResponse(400, "username is required");
        }

        try (SqlSession sqlSession = SessionFactory.getSqlSession()) {
            UserInfoMapper mapper = sqlSession.getMapper(UserInfoMapper.class);

            UserInfoModel existing = mapper.findByTentIdAndUsrId(tentId, username);
            if (existing == null) {
                return errorResponse(404, "User not found: " + tentId + "::" + username);
            }

            mapper.deleteUser(tentId, username);
            sqlSession.commit();
        }

        userSyncService.syncUserToDownstream("delete",
                tentId, username, null, null, null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", tentId + "::" + username);
        result.put("status", "deleted");
        return Response.ok(result).build();
    }

    @POST
    @Path("password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(ChangePasswordRequest req) {
        if (req.getTentId() == null || req.getTentId().isBlank()) {
            return errorResponse(400, "tentId is required");
        }
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            return errorResponse(400, "username is required");
        }
        if (req.getNewPassword() == null || req.getNewPassword().isBlank()) {
            return errorResponse(400, "newPassword is required");
        }

        try (SqlSession sqlSession = SessionFactory.getSqlSession()) {
            UserInfoMapper mapper = sqlSession.getMapper(UserInfoMapper.class);

            UserInfoModel existing = mapper.findByTentIdAndUsrId(req.getTentId(), req.getUsername());
            if (existing == null) {
                return errorResponse(404, "User not found: " + req.getTentId() + "::" + req.getUsername());
            }

            mapper.updatePassword(req.getTentId(), req.getUsername(), md5Hex(req.getNewPassword()));
            sqlSession.commit();
        }

        userSyncService.syncUserToDownstream("password",
                req.getTentId(), req.getUsername(), null, null, null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", req.getTentId() + "::" + req.getUsername());
        result.put("status", "password_changed");
        return Response.ok(result).build();
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

    private Response errorResponse(int status, String message) {
        return Response.status(status)
                .entity(Map.of("error", message))
                .build();
    }
}
