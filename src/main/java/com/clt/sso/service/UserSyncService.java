package com.clt.sso.service;

import com.clt.sso.mapper.SsoSystemEndpointMapper;
import com.clt.sso.model.SsoSystemEndpointModel;
import com.clt.sso.utils.SessionFactory;

import org.apache.ibatis.session.SqlSession;
import org.keycloak.util.JsonSerialization;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserSyncService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void syncUserToDownstream(String action, String tentId, String username,
                                     String email, String firstName, String lastName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("username", tentId + "::" + username);
        payload.put("email", email);
        payload.put("firstName", firstName);
        payload.put("lastName", lastName);
        payload.put("enabled", true);

        String json;
        try {
            json = JsonSerialization.writeValueAsString(payload);
        } catch (Exception e) {
            System.err.println("[UserSync] Failed to serialize payload: " + e.getMessage());
            return;
        }

        List<SsoSystemEndpointModel> endpoints;
        try (SqlSession sqlSession = SessionFactory.getSqlSession()) {
            SsoSystemEndpointMapper mapper = sqlSession.getMapper(SsoSystemEndpointMapper.class);
            endpoints = mapper.findActiveByType("USER_SYNC");
        }

        for (SsoSystemEndpointModel ep : endpoints) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(ep.getEndpointUrl()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json));

                // Secret per endpoint — stored in sso_system_endpoint table
                String secret = ep.getSyncSecret();
                if (secret != null && !secret.isBlank()) {
                    builder.header("X-Sync-Secret", secret);
                }

                HttpResponse<String> response = httpClient.send(builder.build(),
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    System.err.println("[UserSync] " + ep.getAppName() + " returned " + response.statusCode()
                            + ": " + response.body());
                } else {
                    System.out.println("[UserSync] " + ep.getAppName() + " synced OK (" + response.statusCode() + ")");
                }
            } catch (Exception e) {
                System.err.println("[UserSync] Failed to call " + ep.getAppName() + ": " + e.getMessage());
            }
        }
    }
}
