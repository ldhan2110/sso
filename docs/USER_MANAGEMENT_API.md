# User Management API

Base URL: `/realms/{realm}/fwd`

All endpoints write directly to the SSO database and sync to downstream systems (CARIS, WMS, etc.) via `UserSyncService`.

---

## POST /register

Create a new user.

**Request Body:**
```json
{
  "tentId": "TENANT01",
  "username": "john.doe",
  "password": "secret123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com"
}
```

**Responses:**

| Status | Description |
|--------|-------------|
| 201 | User created |
| 400 | Missing required field (`tentId`, `username`, or `password`) |
| 409 | User already exists |

**Downstream sync action:** `create`

---

## PUT /user

Update user profile.

**Request Body:**
```json
{
  "tentId": "TENANT01",
  "username": "john.doe",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.new@example.com",
  "actFlg": "Y"
}
```

| Field | Required | Notes |
|-------|----------|-------|
| tentId | Yes | |
| username | Yes | |
| firstName | No | |
| lastName | No | |
| email | No | |
| actFlg | No | `"Y"` or `"N"`. Only updated if provided. |

**Responses:**

| Status | Description |
|--------|-------------|
| 200 | User updated |
| 400 | Missing required field (`tentId` or `username`) |
| 404 | User not found |

**Downstream sync action:** `update`

---

## DELETE /user

Delete a user.

**Query Parameters:**

| Param | Required | Example |
|-------|----------|---------|
| tentId | Yes | `TENANT01` |
| username | Yes | `john.doe` |

**Example:** `DELETE /realms/{realm}/fwd/user?tentId=TENANT01&username=john.doe`

**Responses:**

| Status | Description |
|--------|-------------|
| 200 | User deleted |
| 400 | Missing required param (`tentId` or `username`) |
| 404 | User not found |

**Downstream sync action:** `delete`

---

## POST /password

Change user password.

**Request Body:**
```json
{
  "tentId": "TENANT01",
  "username": "john.doe",
  "newPassword": "newSecret456"
}
```

**Responses:**

| Status | Description |
|--------|-------------|
| 200 | Password changed |
| 400 | Missing required field (`tentId`, `username`, or `newPassword`) |
| 404 | User not found |

**Downstream sync action:** `password`

> **Note:** Password is stored as MD5 hash. This endpoint bypasses Keycloak — no session/token invalidation occurs.

---

## Downstream Sync

All endpoints trigger `UserSyncService.syncUserToDownstream()` after the DB write. Sync targets are configured in the `sso_system_endpoint` table (type `USER_SYNC`). Each request includes an `X-Sync-Secret` header from the endpoint's `sync_secret` column.

**Sync payload:**
```json
{
  "action": "create|update|delete|password",
  "username": "TENANT01::john.doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "enabled": true
}
```

For `delete` and `password` actions, `email`/`firstName`/`lastName` are `null`.
