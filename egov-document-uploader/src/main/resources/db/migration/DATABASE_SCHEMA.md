# Document Uploader Database Schema

## Overview
This database schema supports the DIGIT Document Uploader service, which manages document uploads, storage references, and metadata for various tenants and categories.

---

## Table: `eg_du_document`

### Purpose
Stores document metadata, file references, and tracking information for uploaded documents across different tenants and categories.

### Table Structure

| Column Name | Data Type | Max Length | Nullable | Description |
|-------------|-----------|------------|----------|-------------|
| `uuid` | character varying | 128 | Yes | **Unique identifier** for the document record. Used for referencing specific documents across the system. |
| `tenantid` | character varying | 128 | No | **Tenant/Organization identifier**. Identifies which tenant/organization owns this document. Part of composite primary key. |
| `name` | character varying | 128 | No | **Document name**. Human-readable name of the document. Part of composite primary key. |
| `category` | character varying | 128 | No | **Document category**. Classifies the document type (e.g., "CERTIFICATE", "INVOICE", "REPORT"). Part of composite primary key. |
| `description` | character varying | 140 | Yes | **Document description**. Brief description or notes about the document. Limited to 140 characters (similar to tweet length). |
| `filestoreId` | character varying | 1024 | Yes | **File store reference ID**. Reference to the file in the file storage system. Used to retrieve the actual file. |
| `documentLink` | character varying | 1024 | Yes | **Document URL/Link**. Direct link to access the document (alternative to filestoreId). |
| `postedby` | character varying | 128 | Yes | **Posted by user**. User identifier of the person who uploaded/posted the document. |
| `active` | boolean | - | No | **Active status flag**. Indicates if the document is active (true) or soft-deleted/inactive (false). Part of composite primary key. |
| `createdBy` | character varying | 64 | Yes | **Creator user ID**. Audit field tracking who created this record. |
| `lastModifiedBy` | character varying | 64 | Yes | **Last modifier user ID**. Audit field tracking who last modified this record. |
| `createdTime` | bigint | - | Yes | **Creation timestamp**. Unix epoch timestamp (milliseconds) when the record was created. |
| `lastModifiedTime` | bigint | - | Yes | **Last modification timestamp**. Unix epoch timestamp (milliseconds) when the record was last updated. |
| `filetype` | character varying | 64 | Yes | **File type/extension**. MIME type or file extension (e.g., "pdf", "jpg", "application/pdf"). Added in V20210927. |
| `filesize` | bigint | - | Yes | **File size in bytes**. Size of the uploaded file. Added in V20210927. |

### Constraints

#### Primary Key
```
CONSTRAINT pk_eg_du_document
PRIMARY KEY (tenantid, category, name, active)
```
**Purpose**: Ensures unique combination of tenant, category, name, and active status. This allows the same document name to exist in different categories or different tenants, and supports soft deletion (active/inactive versions).

#### Unique Constraint
```
CONSTRAINT uk_eg_du_document
UNIQUE (uuid)
```
**Purpose**: Ensures each document has a globally unique identifier for direct lookups.

### Indexes

#### UUID Index
```
CREATE INDEX IF NOT EXISTS index_eg_du_document_uuid
ON eg_du_document (uuid);
```
**Purpose**: Optimizes queries that search or filter by UUID. Improves performance for document retrieval by unique identifier.

---

## ASCII ER Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         eg_du_document                              │
├─────────────────────────────────────────────────────────────────────┤
│ PK  tenantid          VARCHAR(128)  [Part of Composite PK]          │
│ PK  category          VARCHAR(128)  [Part of Composite PK]          │
│ PK  name              VARCHAR(128)  [Part of Composite PK]          │
│ PK  active            BOOLEAN       [Part of Composite PK]          │
│                                                                     │
│ UK  uuid              VARCHAR(128)  [Unique, Indexed]               │
│                                                                     │
│     description       VARCHAR(140)                                  │
│     filestoreId       VARCHAR(1024) ─────► References File Store    │
│     documentLink      VARCHAR(1024)                                 │
│     postedby          VARCHAR(128)  ─────► User Reference           │
│                                                                     │
│     filetype          VARCHAR(64)   [Added: V20210927]              │
│     filesize          BIGINT        [Added: V20210927]              │
│                                                                     │
│     createdBy         VARCHAR(64)   ─────► Audit: User Reference    │
│     createdTime       BIGINT        ─────► Audit: Timestamp         │
│     lastModifiedBy    VARCHAR(64)   ─────► Audit: User Reference    │
│     lastModifiedTime  BIGINT        ─────► Audit: Timestamp         │
└─────────────────────────────────────────────────────────────────────┘

                              ▲
                              │
                              │ References (External)
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
   ┌─────────┐          ┌─────────┐          ┌─────────┐
   │  Tenant │          │ File    │          │  User   │
   │ System  │          │ Store   │          │ System  │
   └─────────┘          └─────────┘          └─────────┘
```

## Relationship Descriptions

### Internal Relationships
- **Composite Primary Key**: The combination of `tenantid`, `category`, `name`, and `active` ensures data integrity and supports multi-tenancy with soft deletion.

### External Relationships (Implicit)
While this table doesn't have explicit foreign keys, it references external systems:

1. **Tenant System** (via `tenantid`)
   - Links to tenant/organization management system
   - Enables multi-tenant document isolation

2. **File Storage System** (via `filestoreId`)
   - References files stored in external file storage (S3, local storage, etc.)
   - The actual file content is stored separately

3. **User System** (via `postedby`, `createdBy`, `lastModifiedBy`)
   - Links to user management/authentication system
   - Tracks document ownership and audit trail

---
