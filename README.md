# CARIS SSO - Keycloak User Storage Provider

A Keycloak User Storage SPI plugin that authenticates users against an external PostgreSQL database (CARIS system). Supports multi-tenancy via tenant-scoped usernames.

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
└──────────────┬───────────────────┘
               │ MyBatis
               ▼
┌──────────────────────────────────┐
│  UserInfoMapper                      │  ← SQL queries via MyBatis XML
│  - findByTentIdAndUsrNm()           │
│  - findByTentIdAndUsrId()           │
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
│   └── CarisSsoUserStorageProvider.java          # User lookup + password validation
├── model/
│   ├── UserInfoModel.java                        # DB entity (Lombok POJO)
│   └── UserInfoAdapter.java                      # Adapts entity to Keycloak UserModel
├── mapper/
│   └── UserInfoMapper.java                       # MyBatis mapper interface
└── utils/
    └── SessionFactory.java                       # MyBatis SqlSession factory

src/main/resources/
├── META-INF/services/
│   └── org.keycloak.storage.UserStorageProviderFactory   # SPI registration
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

### Key Design Decisions

- **Multi-tenancy**: Users identified as `tentId::usrId` — tenant isolation at the username level
- **Read-only**: All setters are no-ops. User data lives in the external CARIS DB
- **MD5 passwords**: Legacy compatibility for migrated tenant data
- **Email lookup**: Not supported (`getUserByEmail` returns null)
- **Federated storage**: Keycloak's built-in federated storage handles additional attributes not in the external DB

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
