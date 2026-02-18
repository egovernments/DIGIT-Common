# WhatsApp Notification Services — Low-Level Design

## 1. Overview

Three microservices power the WhatsApp notification delivery pipeline:

| Service | Language | Purpose |
|---------|----------|---------|
| **digit-config-service** | Java 17 / Spring Boot 3.2 | Runtime configuration management (template mappings, event schemas) |
| **digit-user-preferences-service** | Go | User consent & language preferences for notification channels |
| **digit-novu-bridge** | Java 17 / Spring Boot 3.2 | Kafka consumer that orchestrates notification delivery via Novu |

---

## 2. digit-config-service

### 2.1 Architecture

```
Controller (ConfigEntryController)
    │
    ▼
Service (ConfigEntryService)
    ├── Validator (ConfigEntryValidator)
    ├── Enricher (ConfigEntryEnricher)
    └── Repository (ConfigEntryRepository)
            ├── QueryBuilder (ConfigEntryQueryBuilder)
            └── RowMapper (ConfigEntryRowMapper)
```

**External dependency:** MDMS v2 (optional schema validation on `_create`)

### 2.2 Database Schema

Single table: **`cfg_entry`**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | VARCHAR(64) | PRIMARY KEY |
| `config_code` | VARCHAR(128) | NOT NULL (e.g. `NOTIF_TEMPLATE_MAP`, `NOTIF_EVENT_SCHEMA`) |
| `module` | VARCHAR(128) | Nullable (e.g. `billing`, `ws`, `tl`) |
| `event_type` | VARCHAR(128) | Nullable (e.g. `BILL_GENERATED`, `PAYMENT_DONE`) |
| `channel` | VARCHAR(64) | Nullable (e.g. `WHATSAPP`, `SMS`) |
| `tenant_id` | VARCHAR(256) | NOT NULL |
| `locale` | VARCHAR(16) | Nullable (e.g. `en_IN`, `hi_IN`, `*` for wildcard) |
| `enabled` | BOOLEAN | DEFAULT TRUE |
| `value` | TEXT (JSON) | NOT NULL — stores template mapping, schema payload, etc. |
| `revision` | INT | DEFAULT 1 — optimistic locking counter |
| `created_by` | VARCHAR(64) | |
| `created_time` | BIGINT | Epoch millis |
| `last_modified_by` | VARCHAR(64) | |
| `last_modified_time` | BIGINT | Epoch millis |

**Unique constraint:** `(tenant_id, config_code, module, event_type, channel)`

**Indexes:** `config_code`, `tenant_id`, `module`, `event_type`, `channel`

### 2.3 API Endpoints

Base path: `/config-service/config/v1/entry`

#### POST `/_create` → 201

Creates a new config entry.

**Request:**
```json
{
  "RequestInfo": { "apiId": "...", "userInfo": { "uuid": "..." } },
  "entry": {
    "configCode": "NOTIF_TEMPLATE_MAP",
    "module": "billing",
    "eventType": "BILL_GENERATED",
    "channel": "WHATSAPP",
    "tenantId": "pb.amritsar",
    "locale": "en_IN",
    "value": {
      "templateId": "bill_tpl_001",
      "workflowId": "whatsapp-bill"
    }
  }
}
```

**Response:**
```json
{
  "ResponseInfo": { "status": "successful" },
  "entry": {
    "id": "<uuid>",
    "configCode": "NOTIF_TEMPLATE_MAP",
    "module": "billing",
    "eventType": "BILL_GENERATED",
    "channel": "WHATSAPP",
    "tenantId": "pb.amritsar",
    "locale": "en_IN",
    "enabled": true,
    "value": { "templateId": "bill_tpl_001", "workflowId": "whatsapp-bill" },
    "revision": 1,
    "auditDetails": { "createdBy": "...", "createdTime": 1700000000000 }
  }
}
```

#### POST `/_update` → 200

Updates an existing config entry. Supports optimistic locking via `revision`.

**Request:**
```json
{
  "RequestInfo": { ... },
  "entry": {
    "id": "<uuid>",
    "revision": 1,
    "enabled": false,
    "value": { "templateId": "bill_tpl_002" }
  }
}
```

If the provided `revision` does not match the current DB revision, returns `400 REVISION_MISMATCH`.

#### POST `/_search` → 200

Searches config entries by criteria.

**Request:**
```json
{
  "RequestInfo": { ... },
  "criteria": {
    "configCode": "NOTIF_TEMPLATE_MAP",
    "module": "billing",
    "eventType": "BILL_GENERATED",
    "channel": "WHATSAPP",
    "tenantId": "pb.amritsar",
    "locale": "en_IN",
    "enabled": true,
    "limit": 10,
    "offset": 0
  }
}
```

**Response:**
```json
{
  "ResponseInfo": { ... },
  "entries": [ ... ],
  "pagination": { "totalCount": 42, "limit": 10, "offSet": 0 }
}
```

#### POST `/_resolve` → 200

Resolves the best-matching config entry using tenant and locale fallback chains.

**Request:**
```json
{
  "RequestInfo": { ... },
  "resolveRequest": {
    "configCode": "NOTIF_TEMPLATE_MAP",
    "module": "billing",
    "tenantId": "pb.amritsar",
    "locale": "hi_IN"
  }
}
```

**Resolve precedence (first match wins):**

| Priority | Tenant | Locale |
|----------|--------|--------|
| 1 | `pb.amritsar` | `hi_IN` |
| 2 | `pb.amritsar` | `*` |
| 3 | `pb.amritsar` | NULL |
| 4 | `pb` | `hi_IN` |
| 5 | `pb` | `*` |
| 6 | `pb` | NULL |
| 7 | `*` | `hi_IN` |
| 8 | `*` | `*` |
| 9 | `*` | NULL |

**Response:**
```json
{
  "ResponseInfo": { ... },
  "resolved": {
    "entry": { ... },
    "resolutionMeta": {
      "matchedTenant": "pb",
      "matchedLocale": "hi_IN"
    }
  }
}
```

### 2.4 MDMS v2 Integration

On `_create`, when `mdms.v2.validation.enabled=true`, the service calls MDMS v2 to verify a schema definition exists:

```
POST {mdms.v2.host}/schema/v1/_search
{ "SchemaDefCriteria": { "tenantId": "...", "codes": ["NOTIF_TEMPLATE_MAP"] } }
```

If no schema is found → `400 CFG_SCHEMA_NOT_FOUND`.

### 2.5 Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `server.servlet.context-path` | `/config-service` | Servlet context path |
| `config.default.limit` | `10` | Default search page size |
| `config.default.offset` | `0` | Default search offset |
| `mdms.v2.host` | (empty) | MDMS v2 base URL |
| `mdms.v2.validation.enabled` | `false` | Enable schema validation on create |

---

## 3. digit-user-preferences-service

### 3.1 Architecture

Go service using Chi router, pgxpool for PostgreSQL, standard handler/service/repository layering.

### 3.2 Database Schema

Table: **`user_preferences`**

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | PRIMARY KEY |
| `user_id` | VARCHAR | NOT NULL, UNIQUE |
| `tenant_id` | VARCHAR | NOT NULL |
| `channel` | VARCHAR | e.g. `WHATSAPP`, `SMS` |
| `consent_status` | VARCHAR | `GRANTED` / `REVOKED` |
| `preferred_language` | VARCHAR | e.g. `en_IN`, `hi_IN` |
| `created_by` | VARCHAR | |
| `created_time` | BIGINT | |
| `last_modified_by` | VARCHAR | |
| `last_modified_time` | BIGINT | |

### 3.3 API Endpoints

Base path: `/user-preferences/v1`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/_create` | Create user preference |
| POST | `/_update` | Update consent/language |
| POST | `/_search` | Search preferences by userId, tenantId, channel |

### 3.4 Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `SERVER_CONTEXT_PATH` | `/user-preferences` | HTTP context path |
| `SERVER_PORT` | `8080` | Server port |
| `DB_HOST` | `localhost` | PostgreSQL host |

---

## 4. digit-novu-bridge (Planned)

### 4.1 Architecture

```
Kafka Topic (domain-events)
    │
    ▼
DomainEventConsumer
    │
    ▼
NotificationOrchestrator
    ├── UserPreferencesClient  → user-preferences-service
    ├── ConfigServiceClient    → config-service
    ├── TemplateRenderer       → {{placeholder}} substitution
    └── NovuApiClient          → Novu API trigger
```

### 4.2 Database Schema

Table: **`notification_event`**

| Column | Type | Description |
|--------|------|-------------|
| `id` | VARCHAR(64) | PRIMARY KEY |
| `tenant_id` | VARCHAR(256) | NOT NULL |
| `event_type` | VARCHAR(128) | Source domain event type |
| `source_id` | VARCHAR(128) | ID of source entity (bill, payment, etc.) |
| `user_id` | VARCHAR(128) | Target user |
| `channel` | VARCHAR(64) | `WHATSAPP`, `SMS` |
| `status` | VARCHAR(32) | `PENDING` / `SENT` / `FAILED` / `SKIPPED` |
| `novu_transaction_id` | VARCHAR(128) | Novu's transaction reference |
| `failure_reason` | TEXT | Error details if FAILED |
| `config_snapshot_id` | VARCHAR(64) | FK to config_snapshot |
| `rendered_message` | TEXT | Final rendered notification text |
| `created_by` | VARCHAR(64) | |
| `created_time` | BIGINT | |
| `last_modified_by` | VARCHAR(64) | |
| `last_modified_time` | BIGINT | |

Table: **`config_snapshot`**

| Column | Type | Description |
|--------|------|-------------|
| `id` | VARCHAR(64) | PRIMARY KEY |
| `notification_event_id` | VARCHAR(64) | FK to notification_event |
| `config_entry_id` | VARCHAR(64) | ID of config entry used |
| `config_code` | VARCHAR(128) | Config code at time of send |
| `config_revision` | INT | Revision at time of send |
| `config_value` | TEXT (JSON) | Snapshot of config value |
| `created_time` | BIGINT | |

### 4.3 Orchestration Flow

1. **Consume** Kafka `domain-events` topic → deserialize `DomainEvent`
2. **Create** PENDING `notification_event` audit record
3. **Check consent** via user-preferences `POST /user-preferences/v1/_search`
   - If consent is `REVOKED` → mark `SKIPPED`, return
4. **Get preferred language** from user preferences response
5. **Resolve template** via config-service `POST /config-service/config/v1/entry/_resolve`
   - Config code: `NOTIF_TEMPLATE_MAP`, module + tenantId + locale from event/preferences
6. **Save config snapshot** for audit trail
7. **Render template** — substitute `{{placeholder}}` variables from event data
8. **Trigger Novu** via `POST {novu.api.host}/v1/events/trigger`
   - Payload: workflow ID, subscriber phone, rendered variables
9. **Update status** to `SENT` or `FAILED`

### 4.4 Kafka Consumer Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka brokers |
| `spring.kafka.consumer.group-id` | `novu-bridge-group` | Consumer group |
| `kafka.topics.domain.events` | `domain-events` | Topic to consume |

### 4.5 REST Client Configuration

| Property | Description |
|----------|-------------|
| `config.service.host` | Config service base URL |
| `user.preferences.host` | User preferences service base URL |
| `novu.api.host` | Novu API base URL |
| `novu.api.key` | Novu API key |

---

## 5. Service Interaction Diagram

```
                    ┌─────────────────────┐
                    │   Kafka Topic        │
                    │  (domain-events)     │
                    └─────────┬───────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │  digit-novu-bridge   │
                    │                     │
                    │  1. Check consent   │──────► digit-user-preferences-service
                    │  2. Resolve config  │──────► digit-config-service
                    │  3. Render template │
                    │  4. Trigger Novu    │──────► Novu API
                    └─────────────────────┘
```

---

## 6. Technology Stack

| Component | Technology |
|-----------|-----------|
| Config Service | Java 17, Spring Boot 3.2.2, JdbcTemplate, Flyway, Lombok |
| User Preferences | Go 1.23, Chi router, pgxpool, golang-migrate |
| Novu Bridge | Java 17, Spring Boot 3.2.2, Spring Kafka, JdbcTemplate, Flyway |
| Database | PostgreSQL (prod), H2 (test) |
| Message Broker | Apache Kafka |
| Notification Provider | Novu |
