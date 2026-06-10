# User Storage Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete Keycloak external user storage provider stack — MyBatis mapper, storage provider, factory, and SPI registration — so Keycloak can authenticate users against the external PostgreSQL `sso_user` table.

**Architecture:** `CarisSsoUserStorageProviderFactory` creates providers using `SessionFactory` for DB access. `CarisSsoUserStorageProvider` implements user lookup (by ID and username) via `UserInfoMapper`, wraps results in `UserInfoAdapter`, and validates passwords with BCrypt. SPI registration via `META-INF/services`.

**Tech Stack:** Java 21, Keycloak 26.2.2 SPI, MyBatis (standalone), BCrypt (`at.favre.lib:bcrypt:0.10.2`), JUnit 5, Mockito

---

### Task 1: Create UserInfoMapper Interface and XML

**Files:**
- Create: `src/main/java/com/clt/sso/mapper/UserInfoMapper.java`
- Create: `src/main/resources/mappers/UserInfoMapper.xml`

- [ ] **Step 1: Create mapper interface**

Create `src/main/java/com/clt/sso/mapper/UserInfoMapper.java`:

```java
package com.clt.sso.mapper;

import org.apache.ibatis.annotations.Param;

import com.clt.sso.model.UserInfoModel;

public interface UserInfoMapper {
    UserInfoModel findByTentIdAndUsrId(@Param("tentId") String tentId,
                                        @Param("usrId") String usrId);
    UserInfoModel findByTentIdAndUsrNm(@Param("tentId") String tentId,
                                        @Param("usrNm") String usrNm);
}
```

- [ ] **Step 2: Create mapper XML**

Create `src/main/resources/mappers/UserInfoMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.clt.sso.mapper.UserInfoMapper">

    <select id="findByTentIdAndUsrId" resultType="com.clt.sso.model.UserInfoModel">
        SELECT tent_id, usr_id, usr_nm, usr_pwd, usr_eml, act_flg,
               cre_dt, cre_usr_id, upd_dt, upd_usr_id,
               first_name, last_name, email_verified
        FROM sso_user
        WHERE tent_id = #{tentId} AND usr_id = #{usrId}
    </select>

    <select id="findByTentIdAndUsrNm" resultType="com.clt.sso.model.UserInfoModel">
        SELECT tent_id, usr_id, usr_nm, usr_pwd, usr_eml, act_flg,
               cre_dt, cre_usr_id, upd_dt, upd_usr_id,
               first_name, last_name, email_verified
        FROM sso_user
        WHERE tent_id = #{tentId} AND usr_nm = #{usrNm}
    </select>

</mapper>
```

- [ ] **Step 3: Verify compilation**

Run: `cd /Users/admin/Desktop/Projects/sso && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/clt/sso/mapper/UserInfoMapper.java src/main/resources/mappers/UserInfoMapper.xml
git commit -m "feat: create UserInfoMapper interface and XML for sso_user table"
```

---

### Task 2: Create CarisSsoUserStorageProvider

**Files:**
- Create: `src/main/java/com/clt/sso/provider/CarisSsoUserStorageProvider.java`

- [ ] **Step 1: Create provider class**

Create `src/main/java/com/clt/sso/provider/CarisSsoUserStorageProvider.java`:

```java
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

import at.favre.lib.crypto.bcrypt.BCrypt;

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
        BCrypt.Result result = BCrypt.verifyer().verify(
                credentialInput.getChallengeResponse().toCharArray(),
                storedHash.toCharArray());
        return result.verified;
    }

    // --- UserStorageProvider ---

    @Override
    public void close() {
        sqlSession.close();
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd /Users/admin/Desktop/Projects/sso && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/clt/sso/provider/CarisSsoUserStorageProvider.java
git commit -m "feat: create CarisSsoUserStorageProvider with user lookup and BCrypt validation"
```

---

### Task 3: Create CarisSsoUserStorageProviderFactory

**Files:**
- Create: `src/main/java/com/clt/sso/provider/CarisSsoUserStorageProviderFactory.java`

- [ ] **Step 1: Create factory class**

Create `src/main/java/com/clt/sso/provider/CarisSsoUserStorageProviderFactory.java`:

```java
package com.clt.sso.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

import com.clt.sso.utils.SessionFactory;

public class CarisSsoUserStorageProviderFactory implements UserStorageProviderFactory<CarisSsoUserStorageProvider> {

    public static final String PROVIDER_ID = "caris-external-users";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public CarisSsoUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new CarisSsoUserStorageProvider(session, model, SessionFactory.getSqlSession());
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd /Users/admin/Desktop/Projects/sso && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/clt/sso/provider/CarisSsoUserStorageProviderFactory.java
git commit -m "feat: create CarisSsoUserStorageProviderFactory"
```

---

### Task 4: Add SPI Service Registration

**Files:**
- Create: `src/main/resources/META-INF/services/org.keycloak.storage.UserStorageProviderFactory`

- [ ] **Step 1: Create services directory and file**

Run: `mkdir -p src/main/resources/META-INF/services`

Create `src/main/resources/META-INF/services/org.keycloak.storage.UserStorageProviderFactory`:

```
com.clt.sso.provider.CarisSsoUserStorageProviderFactory
```

(Single line, no trailing newline needed but acceptable)

- [ ] **Step 2: Verify the file is picked up by build**

Run: `cd /Users/admin/Desktop/Projects/sso && ./gradlew processResources && cat build/resources/main/META-INF/services/org.keycloak.storage.UserStorageProviderFactory`
Expected: `com.clt.sso.provider.CarisSsoUserStorageProviderFactory`

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/META-INF/services/org.keycloak.storage.UserStorageProviderFactory
git commit -m "feat: add SPI service registration for CarisSsoUserStorageProviderFactory"
```

---

### Task 5: Create CarisSsoUserStorageProviderTest

**Files:**
- Create: `src/test/java/com/clt/sso/provider/CarisSsoUserStorageProviderTest.java`

- [ ] **Step 1: Create test file**

Create `src/test/java/com/clt/sso/provider/CarisSsoUserStorageProviderTest.java`:

```java
package com.clt.sso.provider;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.clt.sso.mapper.UserInfoMapper;
import com.clt.sso.model.UserInfoAdapter;
import com.clt.sso.model.UserInfoModel;

import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CarisSsoUserStorageProviderTest {

    private CarisSsoUserStorageProvider provider;
    private KeycloakSession session;
    private RealmModel realm;
    private ComponentModel componentModel;
    private SqlSession sqlSession;
    private UserInfoMapper mapper;
    private UserFederatedStorageProvider federatedStorage;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        componentModel = mock(ComponentModel.class);
        when(componentModel.getId()).thenReturn("provider-123");

        sqlSession = mock(SqlSession.class);
        mapper = mock(UserInfoMapper.class);
        when(sqlSession.getMapper(UserInfoMapper.class)).thenReturn(mapper);

        // Mock federated storage for UserInfoAdapter
        federatedStorage = mock(UserFederatedStorageProvider.class);
        when(federatedStorage.getAttributes(eq(realm), anyString()))
                .thenReturn(new MultivaluedHashMap<>());
        when(session.getProvider(UserFederatedStorageProvider.class))
                .thenReturn(federatedStorage);

        provider = new CarisSsoUserStorageProvider(session, componentModel, sqlSession);
    }

    private UserInfoModel createEntity() {
        UserInfoModel entity = new UserInfoModel();
        entity.setTentId("T1");
        entity.setUsrId("U1");
        entity.setUsrNm("johndoe");
        entity.setUsrEml("john@example.com");
        entity.setActFlg("Y");
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setUsrPwd(BCrypt.withDefaults().hashToString(10, "secret123".toCharArray()));
        return entity;
    }

    // --- getUserById ---

    @Test
    void getUserById_returnsAdapter() {
        UserInfoModel entity = createEntity();
        when(mapper.findByTentIdAndUsrId("T1", "U1")).thenReturn(entity);

        UserModel user = provider.getUserById(realm, "f:provider-123:T1::U1");
        assertNotNull(user);
        assertInstanceOf(UserInfoAdapter.class, user);
        assertEquals("T1::johndoe", user.getUsername());
    }

    @Test
    void getUserById_returnsNull_whenNotFound() {
        when(mapper.findByTentIdAndUsrId("T1", "MISSING")).thenReturn(null);

        UserModel user = provider.getUserById(realm, "f:provider-123:T1::MISSING");
        assertNull(user);
    }

    @Test
    void getUserById_returnsNull_whenInvalidFormat() {
        UserModel user = provider.getUserById(realm, "f:provider-123:badformat");
        assertNull(user);
    }

    // --- getUserByUsername ---

    @Test
    void getUserByUsername_returnsAdapter() {
        UserInfoModel entity = createEntity();
        when(mapper.findByTentIdAndUsrNm("T1", "johndoe")).thenReturn(entity);

        UserModel user = provider.getUserByUsername(realm, "T1::johndoe");
        assertNotNull(user);
        assertInstanceOf(UserInfoAdapter.class, user);
    }

    @Test
    void getUserByUsername_returnsNull_whenNotFound() {
        when(mapper.findByTentIdAndUsrNm("T1", "nobody")).thenReturn(null);

        UserModel user = provider.getUserByUsername(realm, "T1::nobody");
        assertNull(user);
    }

    @Test
    void getUserByUsername_returnsNull_whenInvalidFormat() {
        UserModel user = provider.getUserByUsername(realm, "no-separator");
        assertNull(user);
    }

    // --- getUserByEmail ---

    @Test
    void getUserByEmail_returnsNull() {
        assertNull(provider.getUserByEmail(realm, "john@example.com"));
    }

    // --- CredentialInputValidator ---

    @Test
    void supportsCredentialType_password() {
        assertTrue(provider.supportsCredentialType(PasswordCredentialModel.TYPE));
    }

    @Test
    void supportsCredentialType_rejectsOther() {
        assertFalse(provider.supportsCredentialType("otp"));
    }

    @Test
    void isValid_correctPassword() {
        UserInfoModel entity = createEntity();
        when(mapper.findByTentIdAndUsrNm("T1", "johndoe")).thenReturn(entity);

        UserModel user = provider.getUserByUsername(realm, "T1::johndoe");

        CredentialInput input = mock(CredentialInput.class);
        when(input.getType()).thenReturn(PasswordCredentialModel.TYPE);
        when(input.getChallengeResponse()).thenReturn("secret123");

        assertTrue(provider.isValid(realm, user, input));
    }

    @Test
    void isValid_wrongPassword() {
        UserInfoModel entity = createEntity();
        when(mapper.findByTentIdAndUsrNm("T1", "johndoe")).thenReturn(entity);

        UserModel user = provider.getUserByUsername(realm, "T1::johndoe");

        CredentialInput input = mock(CredentialInput.class);
        when(input.getType()).thenReturn(PasswordCredentialModel.TYPE);
        when(input.getChallengeResponse()).thenReturn("wrongpassword");

        assertFalse(provider.isValid(realm, user, input));
    }

    @Test
    void isValid_nullPassword() {
        UserInfoModel entity = createEntity();
        entity.setUsrPwd(null);
        when(mapper.findByTentIdAndUsrNm("T1", "johndoe")).thenReturn(entity);

        UserModel user = provider.getUserByUsername(realm, "T1::johndoe");

        CredentialInput input = mock(CredentialInput.class);
        when(input.getType()).thenReturn(PasswordCredentialModel.TYPE);
        when(input.getChallengeResponse()).thenReturn("secret123");

        assertFalse(provider.isValid(realm, user, input));
    }

    @Test
    void isValid_wrongCredentialType() {
        UserInfoModel entity = createEntity();
        when(mapper.findByTentIdAndUsrNm("T1", "johndoe")).thenReturn(entity);

        UserModel user = provider.getUserByUsername(realm, "T1::johndoe");

        CredentialInput input = mock(CredentialInput.class);
        when(input.getType()).thenReturn("otp");

        assertFalse(provider.isValid(realm, user, input));
    }

    // --- close ---

    @Test
    void close_closesSqlSession() {
        provider.close();
        verify(sqlSession).close();
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd /Users/admin/Desktop/Projects/sso && ./gradlew test --tests "com.clt.sso.provider.CarisSsoUserStorageProviderTest"`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/clt/sso/provider/CarisSsoUserStorageProviderTest.java
git commit -m "test: add CarisSsoUserStorageProviderTest covering lookup, auth, and lifecycle"
```

---

### Task 6: Create CarisSsoUserStorageProviderFactoryTest

**Files:**
- Create: `src/test/java/com/clt/sso/provider/CarisSsoUserStorageProviderFactoryTest.java`

- [ ] **Step 1: Create test file**

Create `src/test/java/com/clt/sso/provider/CarisSsoUserStorageProviderFactoryTest.java`:

```java
package com.clt.sso.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CarisSsoUserStorageProviderFactoryTest {

    @Test
    void getId_returnsProviderName() {
        CarisSsoUserStorageProviderFactory factory = new CarisSsoUserStorageProviderFactory();
        assertEquals("caris-external-users", factory.getId());
    }
}
```

Note: Testing `create()` would require a real `SessionFactory` with DB connection. The factory is a thin wrapper — `getId()` is the only unit-testable logic. Integration testing `create()` is deferred to when a test DB is available.

- [ ] **Step 2: Run tests**

Run: `cd /Users/admin/Desktop/Projects/sso && ./gradlew test --tests "com.clt.sso.provider.CarisSsoUserStorageProviderFactoryTest"`
Expected: PASS

- [ ] **Step 3: Run full model + provider tests**

Run: `cd /Users/admin/Desktop/Projects/sso && ./gradlew test --tests "com.clt.sso.model.*" --tests "com.clt.sso.provider.*"`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/clt/sso/provider/CarisSsoUserStorageProviderFactoryTest.java
git commit -m "test: add CarisSsoUserStorageProviderFactoryTest"
```
