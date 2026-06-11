# Custom JWT Claims Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `tenant_id` and `user_id` custom claims to access and ID tokens via Keycloak OIDCProtocolMapper SPI.

**Architecture:** Single `CarisSsoProtocolMapper` class extends `AbstractOIDCProtocolMapper`, implements `OIDCAccessTokenMapper` + `OIDCIDTokenMapper`. Overrides `setClaim()` to extract `tentId`/`usrId` from `UserInfoAdapter` entity and inject into token. SPI service file registers the mapper for Keycloak discovery.

**Tech Stack:** Java 21, Keycloak 26.2.2 (`keycloak-services` dependency), Gradle Shadow JAR

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `src/main/java/com/clt/sso/provider/CarisSsoProtocolMapper.java` | CREATE | OIDC protocol mapper — injects tenant_id and user_id claims |
| `src/main/resources/META-INF/services/org.keycloak.protocol.ProtocolMapper` | CREATE | SPI service registration for protocol mapper |
| `src/test/java/com/clt/sso/provider/CarisSsoProtocolMapperTest.java` | CREATE | Unit tests for protocol mapper |

No existing files modified.

---

### Task 1: Write CarisSsoProtocolMapper Tests

**Files:**
- Create: `src/test/java/com/clt/sso/provider/CarisSsoProtocolMapperTest.java`

- [ ] **Step 1: Write test class with all test cases**

```java
package com.clt.sso.provider;

import com.clt.sso.model.UserInfoAdapter;
import com.clt.sso.model.UserInfoModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.IDToken;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CarisSsoProtocolMapperTest {

    private CarisSsoProtocolMapper mapper;
    private IDToken token;
    private UserSessionModel userSession;
    private KeycloakSession keycloakSession;
    private ClientSessionContext clientSessionCtx;
    private ProtocolMapperModel mappingModel;

    @BeforeEach
    void setUp() {
        mapper = new CarisSsoProtocolMapper();
        token = new IDToken();
        userSession = mock(UserSessionModel.class);
        keycloakSession = mock(KeycloakSession.class);
        clientSessionCtx = mock(ClientSessionContext.class);
        mappingModel = mock(ProtocolMapperModel.class);
    }

    @Test
    void getId_returnsCarisSsoClaims() {
        assertEquals("caris-sso-claims-mapper", mapper.getId());
    }

    @Test
    void getDisplayType_returnsFriendlyName() {
        assertEquals("Caris SSO Claims", mapper.getDisplayType());
    }

    @Test
    void getDisplayCategory_returnsTokenMapper() {
        assertEquals("Token mapper", mapper.getDisplayCategory());
    }

    @Test
    void getHelpText_returnsDescription() {
        assertNotNull(mapper.getHelpText());
        assertFalse(mapper.getHelpText().isEmpty());
    }

    @Test
    void getConfigProperties_returnsEmptyList() {
        assertNotNull(mapper.getConfigProperties());
        assertTrue(mapper.getConfigProperties().isEmpty());
    }

    @Test
    void setClaim_addsTenanIdAndUserId_whenUserInfoAdapter() {
        UserInfoModel entity = new UserInfoModel();
        entity.setTentId("CNC");
        entity.setUsrId("cltmaster");

        KeycloakSession mockSession = mock(KeycloakSession.class);
        RealmModel mockRealm = mock(RealmModel.class);
        ComponentModel mockComponent = mock(ComponentModel.class);
        when(mockComponent.getId()).thenReturn("provider-id");

        UserInfoAdapter adapter = new UserInfoAdapter(mockSession, mockRealm, mockComponent, entity);
        when(userSession.getUser()).thenReturn(adapter);

        mapper.setClaim(token, mappingModel, userSession, keycloakSession, clientSessionCtx);

        assertEquals("CNC", token.getOtherClaims().get("tenant_id"));
        assertEquals("cltmaster", token.getOtherClaims().get("user_id"));
    }

    @Test
    void setClaim_doesNothing_whenNotUserInfoAdapter() {
        UserModel regularUser = mock(UserModel.class);
        when(userSession.getUser()).thenReturn(regularUser);

        mapper.setClaim(token, mappingModel, userSession, keycloakSession, clientSessionCtx);

        assertNull(token.getOtherClaims().get("tenant_id"));
        assertNull(token.getOtherClaims().get("user_id"));
    }

    @Test
    void setClaim_handlesNullTentId() {
        UserInfoModel entity = new UserInfoModel();
        entity.setTentId(null);
        entity.setUsrId("cltmaster");

        KeycloakSession mockSession = mock(KeycloakSession.class);
        RealmModel mockRealm = mock(RealmModel.class);
        ComponentModel mockComponent = mock(ComponentModel.class);
        when(mockComponent.getId()).thenReturn("provider-id");

        UserInfoAdapter adapter = new UserInfoAdapter(mockSession, mockRealm, mockComponent, entity);
        when(userSession.getUser()).thenReturn(adapter);

        mapper.setClaim(token, mappingModel, userSession, keycloakSession, clientSessionCtx);

        assertNull(token.getOtherClaims().get("tenant_id"));
        assertEquals("cltmaster", token.getOtherClaims().get("user_id"));
    }

    @Test
    void setClaim_handlesNullUsrId() {
        UserInfoModel entity = new UserInfoModel();
        entity.setTentId("CNC");
        entity.setUsrId(null);

        KeycloakSession mockSession = mock(KeycloakSession.class);
        RealmModel mockRealm = mock(RealmModel.class);
        ComponentModel mockComponent = mock(ComponentModel.class);
        when(mockComponent.getId()).thenReturn("provider-id");

        UserInfoAdapter adapter = new UserInfoAdapter(mockSession, mockRealm, mockComponent, entity);
        when(userSession.getUser()).thenReturn(adapter);

        mapper.setClaim(token, mappingModel, userSession, keycloakSession, clientSessionCtx);

        assertEquals("CNC", token.getOtherClaims().get("tenant_id"));
        assertNull(token.getOtherClaims().get("user_id"));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.clt.sso.provider.CarisSsoProtocolMapperTest" --info`
Expected: FAIL — `CarisSsoProtocolMapper` class does not exist yet.

- [ ] **Step 3: Commit test file**

```bash
git add src/test/java/com/clt/sso/provider/CarisSsoProtocolMapperTest.java
git commit -m "test: add CarisSsoProtocolMapper tests for custom JWT claims"
```

---

### Task 2: Implement CarisSsoProtocolMapper

**Files:**
- Create: `src/main/java/com/clt/sso/provider/CarisSsoProtocolMapper.java`

- [ ] **Step 1: Write the protocol mapper implementation**

```java
package com.clt.sso.provider;

import com.clt.sso.model.UserInfoAdapter;
import com.clt.sso.model.UserInfoModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.Collections;
import java.util.List;

public class CarisSsoProtocolMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper {

    public static final String PROVIDER_ID = "caris-sso-claims-mapper";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Caris SSO Claims";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Adds tenant_id and user_id claims from Caris SSO user storage";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel,
                           UserSessionModel userSession, KeycloakSession keycloakSession,
                           ClientSessionContext clientSessionCtx) {
        if (!(userSession.getUser() instanceof UserInfoAdapter)) {
            return;
        }
        UserInfoModel entity = ((UserInfoAdapter) userSession.getUser()).getEntity();

        if (entity.getTentId() != null) {
            token.getOtherClaims().put("tenant_id", entity.getTentId());
        }
        if (entity.getUsrId() != null) {
            token.getOtherClaims().put("user_id", entity.getUsrId());
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew test --tests "com.clt.sso.provider.CarisSsoProtocolMapperTest" --info`
Expected: ALL PASS (8 tests)

- [ ] **Step 3: Commit implementation**

```bash
git add src/main/java/com/clt/sso/provider/CarisSsoProtocolMapper.java
git commit -m "feat: add CarisSsoProtocolMapper for tenant_id and user_id JWT claims"
```

---

### Task 3: Register SPI Service and Build

**Files:**
- Create: `src/main/resources/META-INF/services/org.keycloak.protocol.ProtocolMapper`

- [ ] **Step 1: Create SPI service registration file**

```
com.clt.sso.provider.CarisSsoProtocolMapper
```

- [ ] **Step 2: Run full test suite**

Run: `./gradlew test --info`
Expected: All tests pass (except pre-existing `SsoApplicationTests.initializationError`)

- [ ] **Step 3: Build shadow JAR**

Run: `./gradlew shadowJar`
Expected: JAR built at `build/libs/sso-0.0.1-SNAPSHOT-all.jar`

- [ ] **Step 4: Verify SPI service file is in JAR**

Run: `jar tf build/libs/sso-0.0.1-SNAPSHOT-all.jar | grep ProtocolMapper`
Expected: `META-INF/services/org.keycloak.protocol.ProtocolMapper`

- [ ] **Step 5: Commit SPI registration**

```bash
git add src/main/resources/META-INF/services/org.keycloak.protocol.ProtocolMapper
git commit -m "feat: register CarisSsoProtocolMapper as Keycloak SPI service"
```

---

## Post-Deploy: Keycloak Admin Setup

After deploying the shadow JAR to Keycloak's `providers/` directory:

1. Restart Keycloak
2. Go to **Realm Settings → Client Scopes → (select a scope or create new)→ Mappers → Add mapper → By configuration**
3. Select **"Caris SSO Claims"** from the list
4. Save — `tenant_id` and `user_id` will appear in both access and ID tokens for users from the Caris SSO provider
