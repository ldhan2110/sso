# Custom JWT Claims — Protocol Mapper SPI

**Date:** 2026-06-11
**Status:** Approved

## Summary

Add `tenant_id` and `user_id` custom claims to both access and ID tokens via a Keycloak OIDCProtocolMapper SPI implementation.

## Requirements

- `tenant_id` claim = `UserInfoModel.tentId` (the tenant identifier, e.g. "CNC")
- `user_id` claim = `UserInfoModel.usrId` (the DB user ID, e.g. "cltmaster")
- Claims present in both access token and ID token
- Extensible for future claims sourced from joined tables

## Architecture

```
Token Request
  → Keycloak invokes registered ProtocolMappers
    → CarisSsoProtocolMapper.transformAccessToken()
    → CarisSsoProtocolMapper.transformIDToken()
      → Cast UserModel to UserInfoAdapter
      → Extract tentId/usrId from entity
      → Set token.getOtherClaims().put("tenant_id", tentId)
      → Set token.getOtherClaims().put("user_id", usrId)
```

For non-UserInfoAdapter users (e.g. Keycloak-local users), mapper is a no-op — claims not added.

## Approach: OIDCProtocolMapper SPI

### Why this over User Attribute mappers (Approach B)

User Attribute mappers work for fields already on the user model. But future claims may require DB joins (roles, permissions, org hierarchy). Protocol Mapper SPI allows arbitrary logic including new MyBatis queries. Build extensible foundation once.

### Implementation

**New file: `CarisSsoProtocolMapper.java`**
- Package: `com.clt.sso.provider`
- Extends: `AbstractOIDCProtocolMapper`
- Implements: `OIDCAccessTokenMapper`, `OIDCIDTokenMapper`
- Overrides: `setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx)`
- Logic: get UserModel from userSession, check instanceof UserInfoAdapter, extract entity fields, put claims

**New file: `META-INF/services/org.keycloak.protocol.ProtocolMapper`**
- Single line: `com.clt.sso.provider.CarisSsoProtocolMapper`

**No changes to existing files.**

### Claim names

| Claim | Source | Example |
|-------|--------|---------|
| `tenant_id` | `UserInfoModel.getTentId()` | `"CNC"` |
| `user_id` | `UserInfoModel.getUsrId()` | `"cltmaster"` |

### Token output

```json
{
  "sub": "f:provider-id:CNC::cltmaster",
  "tenant_id": "CNC",
  "user_id": "cltmaster",
  "preferred_username": "CNC::cltmaster",
  "email": "user@example.com",
  "..."
}
```

### Keycloak admin setup (post-deploy)

1. Go to realm → Client Scopes → select scope (or specific client)
2. Add mapper → select "Caris SSO Claims"
3. Claims automatically added to tokens for users from this provider

### Extensibility path

When future claims need joined table data:
1. Add new MyBatis mapper method with JOIN query
2. Open SqlSession in `setClaim()` via `SessionFactory.getSqlSession()`
3. Query and add new claims
4. No structural changes needed

## Out of scope

- No admin console configuration UI
- No per-client claim name customization
- No userinfo endpoint mapper
- No new DB queries (v1 uses entity fields only)
