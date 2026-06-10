# Keycloak User Storage Provider Design

## Context

Keycloak 26.2.2 external user storage SPI (`caris-sso`). Multi-tenant, read-only external PostgreSQL database. Provider runs inside Keycloak container. Existing components: `UserInfoModel` (Lombok POJO), `UserInfoAdapter` (extends `AbstractUserAdapterFederatedStorage`), `SessionFactory` (standalone MyBatis with hardcoded JDBC connection).

## Decision

Build the remaining provider stack: MyBatis mapper, storage provider, provider factory, and SPI service registration. Uses existing `SessionFactory` for DB access (hardcoded connection — configurable later).

## Architecture

```
Keycloak SPI
    → CarisSsoUserStorageProviderFactory.create()
        → SessionFactory.getSqlSession()
        → new CarisSsoUserStorageProvider(session, model, sqlSession)

CarisSsoUserStorageProvider
    → getUserById() / getUserByUsername()
        → UserInfoMapper (MyBatis)
            → SELECT from sso_user
        → new UserInfoAdapter(session, realm, model, entity)
    → isValid()
        → BCrypt.verifyer().verify(input, entity.getUsrPwd())
    → close()
        → sqlSession.close()
```

## 1. UserInfoMapper

MyBatis mapper interface + XML for `sso_user` table.

### Interface

```java
package com.clt.sso.mapper;

public interface UserInfoMapper {
    UserInfoModel findByTentIdAndUsrId(@Param("tentId") String tentId,
                                        @Param("usrId") String usrId);
    UserInfoModel findByTentIdAndUsrNm(@Param("tentId") String tentId,
                                        @Param("usrNm") String usrNm);
}
```

### XML (`src/main/resources/mappers/UserInfoMapper.xml`)

- Table: `sso_user`
- Column mapping: `mapUnderscoreToCamelCase=true` in mybatis-config.xml handles `tent_id` → `tentId`, `usr_id` → `usrId`, etc.
- `customAttributes` is NOT a DB column — left as default empty HashMap from POJO
- Select all columns needed by UserInfoModel: `tent_id`, `usr_id`, `usr_nm`, `usr_pwd`, `usr_eml`, `act_flg`, `cre_dt`, `cre_usr_id`, `upd_dt`, `upd_usr_id`, `first_name`, `last_name`, `email_verified`

### Queries

- `findByTentIdAndUsrId`: `SELECT * FROM sso_user WHERE tent_id = #{tentId} AND usr_id = #{usrId}`
- `findByTentIdAndUsrNm`: `SELECT * FROM sso_user WHERE tent_id = #{tentId} AND usr_nm = #{usrNm}`

## 2. CarisSsoUserStorageProvider

Implements `UserStorageProvider`, `UserLookupProvider`, `CredentialInputValidator`.

### Constructor

```java
public CarisSsoUserStorageProvider(KeycloakSession session,
                                    ComponentModel model,
                                    SqlSession sqlSession)
```

Stores all three. Gets `UserInfoMapper` from `sqlSession.getMapper(UserInfoMapper.class)`.

### getUserById(RealmModel realm, String id)

1. Extract external ID from StorageId: `StorageId.externalId(id)` → `"tentId::usrId"`
2. Split on `"::"` → `[tentId, usrId]`
3. `mapper.findByTentIdAndUsrId(tentId, usrId)`
4. If null, return null
5. Return `new UserInfoAdapter(session, realm, model, entity)`

### getUserByUsername(RealmModel realm, String username)

1. Split username on `"::"` → `[tentId, usrNm]`
2. `mapper.findByTentIdAndUsrNm(tentId, usrNm)`
3. If null, return null
4. Return `new UserInfoAdapter(session, realm, model, entity)`

### getUserByEmail(RealmModel realm, String email)

Return null. Not supported.

### isValid(RealmModel realm, UserModel user, CredentialInput credentialInput)

1. If `credentialInput.getType()` is not `PasswordCredentialModel.TYPE`, return false
2. Cast user to `UserInfoAdapter`, get entity via `getEntity()`
3. If `entity.getUsrPwd()` is null, return false
4. `BCrypt.verifyer().verify(credentialInput.getChallengeResponse().toCharArray(), entity.getUsrPwd().toCharArray())`
5. Return verification result

### supportsCredentialType(String credentialType)

Return `PasswordCredentialModel.TYPE.equals(credentialType)`

### isConfiguredFor(RealmModel realm, UserModel user, String credentialType)

Return `supportsCredentialType(credentialType)`

### close()

`sqlSession.close()`

## 3. CarisSsoUserStorageProviderFactory

Implements `UserStorageProviderFactory<CarisSsoUserStorageProvider>`.

### getId()

Return `"caris-external-users"`

### create(KeycloakSession session, ComponentModel model)

1. `SqlSession sqlSession = SessionFactory.getSqlSession()`
2. Return `new CarisSsoUserStorageProvider(session, model, sqlSession)`

## 4. SPI Service Registration

File: `src/main/resources/META-INF/services/org.keycloak.storage.UserStorageProviderFactory`
Content: `com.clt.sso.provider.CarisSsoUserStorageProviderFactory`

## File Changes

| File | Action | Package |
|---|---|---|
| `src/main/java/com/clt/sso/mapper/UserInfoMapper.java` | Create | `com.clt.sso.mapper` |
| `src/main/resources/mappers/UserInfoMapper.xml` | Create | — |
| `src/main/java/com/clt/sso/provider/CarisSsoUserStorageProvider.java` | Create | `com.clt.sso.provider` |
| `src/main/java/com/clt/sso/provider/CarisSsoUserStorageProviderFactory.java` | Create | `com.clt.sso.provider` |
| `src/main/resources/META-INF/services/org.keycloak.storage.UserStorageProviderFactory` | Create | — |

## Constraints

- Read-only: provider never writes to external DB
- Multi-tenant: all lookups require `tentId` parsed from composite keys
- `SessionFactory` uses standalone MyBatis (not Spring-managed) — appropriate since provider runs in Keycloak container
- BCrypt validation uses `at.favre.lib:bcrypt:0.10.2` (already in dependencies)
- `customAttributes` not populated from DB in this iteration (empty HashMap default)
- `getUserByEmail` returns null — not supported per user requirement
- No search (`searchForUserStream`) — not supported per user requirement

## Scope Boundary

NOT in scope:
- Configurable JDBC via Keycloak admin console (future enhancement)
- Search/listing users in Keycloak admin console
- `customAttributes` population from DB columns
- Integration tests against real PostgreSQL
