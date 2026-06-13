# CARIS SSO - Keycloak User Storage Provider

A Keycloak User Storage SPI plugin that authenticates users against an external PostgreSQL database (CARIS system). Supports multi-tenancy via tenant-scoped usernames, user registration via custom REST API, and group-based application access control.

## Architecture

```
Keycloak Login Request
        │
        ▼
┌──────────────────────────────────┐
│  CarisSsoUserStorageProviderFactory  │  ← Keycloak discovers via SPI
│  getId() = "caris-external-users"    │
└──────────────┬───────────────────┘
               │ create()
               ▼
┌──────────────────────────────────┐
│  CarisSsoUserStorageProvider         │  ← Core provider
│  - getUserByUsername()               │
│  - getUserById()                     │
│  - isValid() (password check)        │
│  - addUser() (registration)          │
│  - removeUser() (deletion)           │
│  - updateCredential() (password)     │
└──────────────┬───────────────────┘
               │ MyBatis
               ▼
┌──────────────────────────────────┐
│  UserInfoMapper                      │  ← SQL queries via MyBatis XML
│  - findByTentIdAndUsrNm()           │
│  - findByTentIdAndUsrId()           │
│  - insertUser() / updateUser()      │
│  - updatePassword() / deleteUser()  │
└──────────────┬───────────────────┘
               │
               ▼
┌──────────────────────────────────┐
│  PostgreSQL (sso_user table)         │
└──────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────┐
│  UserInfoModel → UserInfoAdapter     │  ← Adapts DB record to Keycloak UserModel
└──────────────────────────────────┘
```

## Project Structure

```
src/main/java/com/clt/sso/
├── provider/
│   ├── CarisSsoUserStorageProviderFactory.java   # SPI factory, creates provider instances
│   ├── CarisSsoUserStorageProvider.java          # User lookup, registration, password, deletion
│   ├── CarisSsoRestProviderFactory.java          # REST API SPI factory
│   ├── CarisSsoRestProvider.java                 # REST API resource provider
│   └── CarisSsoRegistrationResource.java         # POST /realms/{realm}/fwd/register endpoint
├── model/
│   ├── UserInfoModel.java                        # DB entity (Lombok POJO)
│   ├── UserInfoAdapter.java                      # Adapts entity to Keycloak UserModel
│   └── RegistrationRequest.java                  # Registration API request body
├── mapper/
│   └── UserInfoMapper.java                       # MyBatis mapper interface
└── utils/
    └── SessionFactory.java                       # MyBatis SqlSession factory

src/main/resources/
├── META-INF/services/
│   ├── org.keycloak.storage.UserStorageProviderFactory          # User storage SPI
│   └── org.keycloak.services.resource.RealmResourceProviderFactory  # REST API SPI
├── mapper/
│   └── UserInfoMapper.xml                                # SQL query definitions
└── mybatis-config.xml                                    # DB connection + MyBatis settings
```

## How It Works

### Authentication Flow

1. User logs in with username format `TENANT_ID::username` (e.g. `TENANT1::johndoe`)
2. Provider splits on `::` to extract tenant ID and username
3. Queries `sso_user` table by `tent_id` + `usr_nm`
4. Validates password by comparing MD5 hash of input against stored `usr_pwd`
5. Returns user attributes via `UserInfoAdapter` (read-only)

### Registration Flow (Custom REST API)

1. Client sends `POST /realms/{realm}/fwd/register` with user details + group names
2. Provider creates user in `sso_user` table with `tentId::username` format
3. Password is MD5-hashed and stored in `usr_pwd`
4. User attributes (firstName, lastName, email) are persisted to DB
5. Specified groups are resolved by name and assigned via Keycloak's group system
6. Returns created user ID, username, and assigned groups

### Group-Based Application Access

Groups control which applications a user can access. Managed in Keycloak Admin Console:

```
Keycloak Groups:
├── CARIS-Users        → client role mappings → caris-app
├── WMS-Users          → client role mappings → wms-app
├── FreightLite-Users  → client role mappings → freightlite-app
└── Full-Access        → all of the above
```

Groups are assigned during registration or via Keycloak Admin Console/API.

### Key Design Decisions

- **Multi-tenancy**: Users identified as `tentId::usrId` — tenant isolation at the username level
- **Writable storage**: User attributes and passwords can be created/updated via the provider
- **MD5 passwords**: Legacy compatibility for migrated tenant data
- **Email lookup**: Not supported (`getUserByEmail` returns null)
- **Federated storage**: Keycloak's built-in federated storage handles additional attributes not in the external DB
- **Group management**: Delegated to Keycloak (not stored in external DB)

## Build

Requires Java 21.

```bash
# Build fat JAR (bundles MyBatis + PostgreSQL driver)
./gradlew shadowJar

# Output: build/libs/caris-sso-0.0.1-SNAPSHOT-all.jar
```

Rebuild after code changes:

```bash
./gradlew shadowJar

# Or clean build:
./gradlew clean shadowJar
```

## Deploy to Keycloak

```bash
# Copy JAR to Keycloak providers directory
cp build/libs/caris-sso-0.0.1-SNAPSHOT-all.jar /opt/keycloak/providers/

# Rebuild Keycloak to register the provider
/opt/keycloak/bin/kc.sh build

# Start Keycloak
/opt/keycloak/bin/kc.sh start-dev
```

Then in Keycloak Admin Console:
1. Go to your realm → **User Federation**
2. Select **caris-external-users** from the provider dropdown
3. Configure and save

## Run Tests

```bash
./gradlew test
```

## Database

The provider connects to PostgreSQL and queries the `sso_user` table:

| Column | Description |
|--------|-------------|
| `tent_id` | Tenant ID |
| `usr_id` | User ID |
| `usr_nm` | Username |
| `usr_pwd` | Password (MD5 hash) |
| `usr_eml` | Email |
| `first_name` | First name |
| `last_name` | Last name |
| `act_flg` | Active flag (`Y`/`N`) |
| `email_verified` | Email verified flag |
| `cre_dt` | Created date |
| `upd_dt` | Updated date |

DB connection is configured in `src/main/resources/mybatis-config.xml`.

## Registration API

### Endpoint

```
POST /realms/{realm}/fwd/register
Content-Type: application/json
```

### Request Body

```json
{
  "tentId": "CLT",
  "username": "john",
  "password": "secret123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@clt.com",
  "groups": ["CARIS-Users", "WMS-Users"]
}
```

### Responses

**201 Created**
```json
{
  "id": "f:provider-id:CLT::john",
  "username": "CLT::john",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@clt.com",
  "assignedGroups": ["CARIS-Users", "WMS-Users"]
}
```

**400 Bad Request** — missing `tentId`, `username`, or `password`

**409 Conflict** — user already exists

### Setup Groups

1. Keycloak Admin Console → Realm → **Groups** → Create group (e.g., `CARIS-Users`)
2. Open group → **Role Mappings** → assign client roles for the target application
3. Client Scopes → add a **Group Membership** mapper so groups appear in JWT tokens
