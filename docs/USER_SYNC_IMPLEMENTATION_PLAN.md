# User Sync Implementation Plan — Event-Driven via Keycloak

## Architecture

```
User Created/Updated/Deleted in Keycloak
              │
              ▼
    EventListenerProvider SPI
              │
    reads SSO_SYSTEM_ENDPOINT table
    (WHERE endpoint_type = 'USER_SYNC' AND act_flg = 'Y')
              │
         ┌────┴────┐
         ▼         ▼
   CARIS API    WMS API
  /api/users   /api/users
    /sync        /sync
      │            │
      ▼            ▼
   DB_CARIS     DB_WMS
   TB_USER      TB_USER
```

Keycloak fires admin events on user CRUD → EventListener reads active endpoints from `SSO_SYSTEM_ENDPOINT` table → calls each app's sync endpoint → each app writes to its own DB.

Add/remove apps by INSERT/UPDATE on the table. No Keycloak restart needed.

---

## Part 1: Keycloak EventListener SPI (this repo)

### 1.1 DB Table: `SSO_SYSTEM_ENDPOINT`

```sql
CREATE TABLE sso_system_endpoint (
    endpoint_id    SERIAL PRIMARY KEY,
    app_name       VARCHAR(50)  NOT NULL,     -- 'CARIS', 'WMS', etc.
    endpoint_url   VARCHAR(500) NOT NULL,     -- 'http://caris:8080/api/users/sync'
    endpoint_type  VARCHAR(50)  NOT NULL,     -- 'USER_SYNC', 'ROLE_SYNC', 'HEALTH_CHECK'
    remarks        VARCHAR(500),             -- free-text description
    act_flg        CHAR(1)      DEFAULT 'Y', -- Y = active, N = disabled
    cre_dt         TIMESTAMP    DEFAULT NOW(),
    cre_usr_id     VARCHAR(50),
    upd_dt         TIMESTAMP,
    upd_usr_id     VARCHAR(50)
);

-- Seed data
INSERT INTO sso_system_endpoint (app_name, endpoint_url, endpoint_type, remarks, cre_usr_id)
VALUES
    ('CARIS', 'http://caris-backend:8080/api/users/sync', 'USER_SYNC', 'CARIS user sync endpoint', 'admin'),
    ('WMS',   'http://wms-backend:8080/api/users/sync',   'USER_SYNC', 'WMS user sync endpoint',   'admin');
```

**`endpoint_type` values:**

| Type          | Description                              |
|---------------|------------------------------------------|
| `USER_SYNC`   | User create/update/delete sync           |
| `ROLE_SYNC`   | Role assignment sync (future)            |
| `HEALTH_CHECK`| App health check endpoint (future)       |

**Managing endpoints:**

```sql
-- Add new app
INSERT INTO sso_system_endpoint (app_name, endpoint_url, endpoint_type, remarks, cre_usr_id)
VALUES ('NEW_APP', 'http://new-app:8080/api/users/sync', 'USER_SYNC', 'New app sync', 'admin');

-- Disable app (no delete needed)
UPDATE sso_system_endpoint SET act_flg = 'N', upd_dt = NOW() WHERE app_name = 'WMS' AND endpoint_type = 'USER_SYNC';

-- Re-enable
UPDATE sso_system_endpoint SET act_flg = 'Y', upd_dt = NOW() WHERE app_name = 'WMS' AND endpoint_type = 'USER_SYNC';
```

### 1.2 Create `UserSyncEventListenerProvider.java`

```java
package com.clt.sso.provider;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import com.clt.sso.mapper.SsoSystemEndpointMapper;
import com.clt.sso.model.SsoSystemEndpointModel;
import com.clt.sso.utils.SessionFactory;

import org.apache.ibatis.session.SqlSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class UserSyncEventListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;
    private final HttpClient httpClient;

    public UserSyncEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void onEvent(Event event) {
        // Login events — not needed for user sync
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        if (event.getResourceType() != ResourceType.USER) return;

        OperationType op = event.getOperationType();
        if (op != OperationType.CREATE && op != OperationType.UPDATE && op != OperationType.DELETE) return;

        String representation = event.getRepresentation();
        if (representation == null && op != OperationType.DELETE) return;

        String resourcePath = event.getResourcePath();
        String keycloakUserId = extractUserId(resourcePath);
        String action = op.name().toLowerCase();

        // Build sync payload
        String payload;
        try {
            Map<String, Object> syncData = new java.util.LinkedHashMap<>();
            syncData.put("action", action);
            syncData.put("keycloakUserId", keycloakUserId);
            syncData.put("realmId", event.getRealmId());

            if (representation != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userData = JsonSerialization.readValue(representation, Map.class);
                syncData.put("username", userData.get("username"));
                syncData.put("email", userData.get("email"));
                syncData.put("firstName", userData.get("firstName"));
                syncData.put("lastName", userData.get("lastName"));
                syncData.put("enabled", userData.get("enabled"));
            }

            payload = JsonSerialization.writeValueAsString(syncData);
        } catch (Exception e) {
            System.err.println("[UserSync] Failed to build payload: " + e.getMessage());
            return;
        }

        // Read active endpoints from DB and fire sync
        try (SqlSession sqlSession = SessionFactory.getSqlSession()) {
            SsoSystemEndpointMapper mapper = sqlSession.getMapper(SsoSystemEndpointMapper.class);
            List<SsoSystemEndpointModel> endpoints = mapper.findActiveByType("USER_SYNC");

            for (SsoSystemEndpointModel ep : endpoints) {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(ep.getEndpointUrl()))
                            .header("Content-Type", "application/json")
                            .header("X-Sync-Secret", getSyncSecret())
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() >= 400) {
                        System.err.println("[UserSync] " + ep.getAppName() + " (" + ep.getEndpointUrl() + ") returned " + response.statusCode());
                    }
                } catch (Exception e) {
                    System.err.println("[UserSync] Failed to call " + ep.getAppName() + ": " + e.getMessage());
                }
            }
        }
    }

    private String extractUserId(String resourcePath) {
        if (resourcePath != null && resourcePath.startsWith("users/")) {
            return resourcePath.substring(6);
        }
        return null;
    }

    private String getSyncSecret() {
        return System.getenv("USER_SYNC_SECRET");
    }

    @Override
    public void close() {
        // no-op
    }
}
```

### 1.3 Create `UserSyncEventListenerProviderFactory.java`

```java
package com.clt.sso.provider;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UserSyncEventListenerProviderFactory implements EventListenerProviderFactory {

    @Override
    public String getId() {
        return "user-sync";
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new UserSyncEventListenerProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // no-op — endpoints read from DB at runtime
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}
```

### 1.4 Register SPI

Add file: `src/main/resources/META-INF/services/org.keycloak.events.EventListenerProviderFactory`

```
com.clt.sso.provider.UserSyncEventListenerProviderFactory
```

### 1.5 Enable in Keycloak

After deploying the JAR, enable the event listener in Keycloak Admin Console:

1. Realm Settings → Events → Event listeners
2. Add `user-sync` to the list

### 1.6 Environment Variables

```bash
# Shared secret for authenticating sync calls (still env var — secrets don't belong in DB)
USER_SYNC_SECRET=<generate-a-strong-secret>
```

---

## Part 2: App Sync Endpoint (implement in CARIS, WMS, and future apps)

Each app needs 3 things: a **filter**, a **controller**, and a **DTO**.

The sync endpoint uses path `/api/intg/sso/users/sync` — under the `/api/intg/` prefix to **bypass JWT authentication**. This follows the same pattern as the SHINE integration in CARIS (`/api/intg/shine/*` uses `ApiTokenAuthenticationFilter`).

### 2.1 Why `/api/intg/sso/*` ?

CARIS (and likely WMS) uses `JwtAuthenticationFilter` on all `/api/*` paths. Keycloak EventListener sends `X-Sync-Secret`, not a JWT. To avoid modifying the JWT filter, we use a **separate filter on a dedicated integration path** — same pattern as SHINE.

```
/api/v1/*          → JwtAuthenticationFilter (user-facing, JWT required)
/api/intg/shine/*  → ApiTokenAuthenticationFilter (SHINE integration)
/api/intg/sso/*    → SsoSyncAuthenticationFilter (Keycloak sync, X-Sync-Secret)
```

### 2.2 API Contract

```
POST /api/intg/sso/users/sync
Content-Type: application/json
X-Sync-Secret: <shared-secret>
```

#### Request Body

```json
{
  "action": "create | update | delete",
  "keycloakUserId": "abc-123-def-456",
  "realmId": "FWD",
  "username": "TENT01::john.doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "enabled": true
}
```

| Field            | Type    | Present On         | Description                                    |
|------------------|---------|--------------------|------------------------------------------------|
| `action`         | string  | always             | `create`, `update`, or `delete`                |
| `keycloakUserId` | string  | always             | Keycloak internal user ID (UUID)               |
| `realmId`        | string  | always             | Keycloak realm                                 |
| `username`       | string  | create, update     | Format: `{tentId}::{usrId}`                    |
| `email`          | string  | create, update     | User email                                     |
| `firstName`      | string  | create, update     | User first name                                |
| `lastName`       | string  | create, update     | User last name                                 |
| `enabled`        | boolean | create, update     | Active status                                  |

#### Responses

| Status | Meaning                |
|--------|------------------------|
| `200`  | Synced successfully    |
| `204`  | No action needed       |
| `401`  | Missing/invalid secret |
| `400`  | Bad request            |
| `500`  | Internal error         |

### 2.3 Step 1: Authentication Filter (bypasses JWT)

Create a filter that only applies to `/api/intg/sso/*` and validates `X-Sync-Secret`.

```java
package com.clt.caris.core.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authentication filter for SSO sync endpoints.
 * Validates X-Sync-Secret header instead of JWT.
 * Follows the same pattern as ApiTokenAuthenticationFilter (SHINE).
 */
@Slf4j
@Component("ssoSyncAuthenticationFilter")
public class SsoSyncAuthenticationFilter extends OncePerRequestFilter {

    @Value("${app.sso.sync-secret:}")
    private String syncSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        if (requestPath.startsWith("/api/intg/sso")) {
            String secret = request.getHeader("X-Sync-Secret");

            if (secret == null || secret.isBlank()) {
                log.warn("Missing X-Sync-Secret header for SSO sync: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"success\":false,\"error\":\"Missing X-Sync-Secret header\"}");
                return;
            }

            if (!syncSecret.equals(secret)) {
                log.warn("Invalid X-Sync-Secret for SSO sync: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"success\":false,\"error\":\"Invalid sync secret\"}");
                return;
            }

            log.info("SSO sync secret validated for: {}", requestPath);
        }

        filterChain.doFilter(request, response);
    }
}
```

### 2.4 Step 2: Register Filter & Exclude from JWT Filter

**SecurityConfig.java** — register the SSO sync filter and exclude its path from JWT filter:

```java
// Add to SecurityConfig.java

@Autowired
@Qualifier("ssoSyncAuthenticationFilter")
private SsoSyncAuthenticationFilter ssoSyncAuthenticationFilter;

// Register SSO sync filter
@Bean
public FilterRegistrationBean<SsoSyncAuthenticationFilter> ssoSyncFilterRegistration() {
    FilterRegistrationBean<SsoSyncAuthenticationFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(ssoSyncAuthenticationFilter);
    registration.addUrlPatterns("/api/intg/sso/*");
    registration.setOrder(0); // Before JWT filter
    return registration;
}
```

**JwtAuthenticationFilter.java** — skip SSO sync paths:

```java
// Add this method to JwtAuthenticationFilter.java

@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/api/intg/sso");
}
```

**WebConfig.java** — exclude from TenantInterceptor:

```java
// Add to the excludePathPatterns list in WebConfig.java

.excludePathPatterns(
    "/api/debug/**",
    "/api/common/email/callback",
    "/api/intg/sso/**",          // ← add this line
    "/actuator/**"
)
```

### 2.5 Step 3: Controller

```java
package com.clt.caris.sso.adapter.input.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/intg/sso")
@RequiredArgsConstructor
public class UserSyncController {

    private final UserRepository userRepository; // your app's user repo

    @PostMapping("/users/sync")
    public ResponseEntity<?> syncUser(@RequestBody UserSyncRequest request) {
        // No secret check here — filter already validated X-Sync-Secret

        log.info("[SSO Sync] action={} username={}", request.getAction(), request.getUsername());

        // Parse username: "TENT01::john.doe" → tentId + usrId
        String username = request.getUsername();
        String tentId = null;
        String usrId = null;
        if (username != null && username.contains("::")) {
            String[] parts = username.split("::", 2);
            tentId = parts[0];
            usrId = parts[1];
        }

        switch (request.getAction()) {
            case "create":
                // Upsert — idempotent, safe to retry
                UserEntity user = userRepository.findByTentIdAndUsrId(tentId, usrId);
                if (user == null) {
                    user = new UserEntity();
                    user.setTentId(tentId);
                    user.setUsrId(usrId);
                    user.setCreDt(LocalDateTime.now());
                    user.setCreUsrId("keycloak-sync");
                }
                user.setUsrNm(usrId);
                user.setUsrEml(request.getEmail());
                user.setFirstName(request.getFirstName());
                user.setLastName(request.getLastName());
                user.setActFlg(Boolean.TRUE.equals(request.getEnabled()) ? "Y" : "N");
                user.setKeycloakUserId(request.getKeycloakUserId());
                user.setUpdDt(LocalDateTime.now());
                user.setUpdUsrId("keycloak-sync");
                userRepository.save(user);
                break;

            case "update":
                UserEntity existing = userRepository.findByTentIdAndUsrId(tentId, usrId);
                if (existing == null) {
                    return ResponseEntity.noContent().build();
                }
                existing.setUsrEml(request.getEmail());
                existing.setFirstName(request.getFirstName());
                existing.setLastName(request.getLastName());
                existing.setActFlg(Boolean.TRUE.equals(request.getEnabled()) ? "Y" : "N");
                existing.setUpdDt(LocalDateTime.now());
                existing.setUpdUsrId("keycloak-sync");
                userRepository.save(existing);
                break;

            case "delete":
                userRepository.deleteByKeycloakUserId(request.getKeycloakUserId());
                break;

            default:
                return ResponseEntity.badRequest().body(Map.of("error", "unknown action"));
        }

        return ResponseEntity.ok(Map.of("status", "synced"));
    }
}
```

### 2.6 Step 4: DTO

```java
package com.clt.caris.sso.adapter.input.rest.dto;

import lombok.Data;

@Data
public class UserSyncRequest {
    private String action;          // "create", "update", "delete"
    private String keycloakUserId;  // Keycloak UUID
    private String realmId;
    private String username;        // "tentId::usrId"
    private String email;
    private String firstName;
    private String lastName;
    private Boolean enabled;
}
```

### 2.7 Step 5: Application Config

```yaml
# application.yml
app:
  sso:
    sync-secret: ${SSO_SYNC_SECRET:change-me-in-production}
```

### 2.8 Step 6: DB Migration — Add `keycloak_user_id` Column

Each app's user table needs a column to store Keycloak's user ID:

```sql
ALTER TABLE TB_USER ADD COLUMN keycloak_user_id VARCHAR(36);
CREATE INDEX idx_tb_user_keycloak_id ON TB_USER (keycloak_user_id);
```

---

## Part 3: Checklist Per App

| Step | Task | Files to Create/Modify |
|------|------|------------------------|
| 1 | Create `SsoSyncAuthenticationFilter` | New: `*.filter.SsoSyncAuthenticationFilter.java` |
| 2 | Register filter + exclude from JWT | Modify: `SecurityConfig.java` |
| 3 | Add `shouldNotFilter` to JWT filter | Modify: `JwtAuthenticationFilter.java` |
| 4 | Exclude `/api/intg/sso/**` from TenantInterceptor | Modify: `WebConfig.java` |
| 5 | Create `UserSyncRequest` DTO | New: `*.dto.UserSyncRequest.java` |
| 6 | Create `UserSyncController` | New: `*.controller.UserSyncController.java` |
| 7 | Add `keycloak_user_id` column to user table | DB migration script |
| 8 | Add `app.sso.sync-secret` to config | Modify: `application.yml` |
| 9 | Test with curl | See §4 below |
| 10 | INSERT endpoint URL into `SSO_SYSTEM_ENDPOINT` | SQL on caris-sso DB |

### SSO_SYSTEM_ENDPOINT seed data (updated URLs)

```sql
INSERT INTO sso_system_endpoint (app_name, endpoint_url, endpoint_type, remarks, cre_usr_id)
VALUES
    ('CARIS', 'http://caris-backend:8080/api/intg/sso/users/sync', 'USER_SYNC', 'CARIS user sync via integration path', 'admin'),
    ('WMS',   'http://wms-backend:8080/api/intg/sso/users/sync',   'USER_SYNC', 'WMS user sync via integration path',   'admin');
```

---

## Part 4: Testing

### Test sync endpoint directly

```bash
# Create
curl -X POST http://localhost:8080/api/intg/sso/users/sync \
  -H "Content-Type: application/json" \
  -H "X-Sync-Secret: your-secret" \
  -d '{
    "action": "create",
    "keycloakUserId": "test-uuid-123",
    "realmId": "FWD",
    "username": "TENT01::testuser",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "enabled": true
  }'

# Update
curl -X POST http://localhost:8080/api/intg/sso/users/sync \
  -H "Content-Type: application/json" \
  -H "X-Sync-Secret: your-secret" \
  -d '{
    "action": "update",
    "keycloakUserId": "test-uuid-123",
    "realmId": "FWD",
    "username": "TENT01::testuser",
    "email": "updated@example.com",
    "firstName": "Updated",
    "lastName": "User",
    "enabled": true
  }'

# Delete
curl -X POST http://localhost:8080/api/intg/sso/users/sync \
  -H "Content-Type: application/json" \
  -H "X-Sync-Secret: your-secret" \
  -d '{
    "action": "delete",
    "keycloakUserId": "test-uuid-123",
    "realmId": "FWD"
  }'
```

### Test full flow

1. Create user via Keycloak Admin Console or `/fwd/register` API
2. Check CARIS and WMS DB — user table should have new row
3. Update user in Keycloak → verify both DBs updated
4. Delete user in Keycloak → verify both DBs cleaned

---

## Security Notes

- `X-Sync-Secret` validates sync calls — never expose publicly
- `/api/intg/sso/*` bypasses JWT filter — only `X-Sync-Secret` required
- Sync endpoints should only be accessible on internal network
- Consider adding IP allowlist (only Keycloak server IP)
- Passwords are NEVER included in sync payload — Keycloak owns auth
- `app.sso.sync-secret` must match `USER_SYNC_SECRET` env var on Keycloak side
