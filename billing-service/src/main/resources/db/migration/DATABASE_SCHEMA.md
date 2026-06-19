# Billing Service Database Schema Documentation

## Overview
This document provides a comprehensive overview of the database schema for the eGov Billing Service (EGBS). The billing service manages demands, bills, tax calculations, receipts, and amendments for various government services.

## Database Schema Version
The database follows a versioned migration approach using Flyway. The current schema includes both v1 (current) and legacy tables.

---

## ASCII Entity Relationship Diagram

**Legend:**
- `────>` Solid line = Foreign Key Relationship (enforced in database)
- `····>` Dotted line = Logical Reference (not enforced by FK)
- `[1:N]` = One to Many relationship

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                    BILLING SERVICE DATABASE SCHEMA (v1 Tables)                       │
└─────────────────────────────────────────────────────────────────────────────────────┘


MASTER DATA / CONFIGURATION TABLES:
═══════════════════════════════════════════════════════════════════════════════════════

┌────────────────────────────┐        ┌────────────────────────────┐
│egbs_business_service_details        │    egbs_taxheadmaster      │
├────────────────────────────┤        ├────────────────────────────┤
│PK: id, tenantid            │        │PK: id, tenantid            │
│UK: businessservice,tenantid│        ├────────────────────────────┤
├────────────────────────────┤        │category (TAX,PENALTY,etc)  │
│businessservice             │        │service                     │
│collectionmodesnotallowed   │        │name                        │
│callbackforapportioning     │        │code (PT_TAX,PT_PENALTY)    │
│partpaymentallowed          │        │isdebit                     │
│callbackapportionurl        │        │isactualdemand              │
│+ audit fields              │        │orderno                     │
└────────────────────────────┘        │validfrom, validtill        │
                                      │+ audit fields              │
                                      └────────────────────────────┘
┌────────────────────────────┐        ┌────────────────────────────┐
│     egbs_taxperiod         │        │    egbs_glcodemaster       │
├────────────────────────────┤        ├────────────────────────────┤
│PK: id, tenantid            │        │PK: id, tenantid            │
│UK: service,code,tenantid   │        ├────────────────────────────┤
├────────────────────────────┤        │taxhead (tax head code)     │
│service (PT, WS, TL, etc)   │        │service                     │
│code (FY2023-24, Q1-2024)   │        │fromdate, todate            │
│fromdate, todate            │        │glcode (for accounting)     │
│financialyear               │        │+ audit fields              │
│periodcycle (ANNUAL, etc)   │        └────────────────────────────┘
│+ audit fields              │
└────────────────────────────┘


DEMAND FLOW (Tax Demand → Demand Details):
═══════════════════════════════════════════════════════════════════════════════════════

                businessservice (logical ref)
  egbs_business_service_details ···················> egbs_demand_v1
                                                           │
┌────────────────────────────────────────────┐             │
│         egbs_demand_v1                     │             │
├────────────────────────────────────────────┤             │
│PK: id, tenantid                            │             │ [1:N]
│UK: consumercode,tenantid,taxperiodfrom,    │             │ FK enforced
│    taxperiodto,businessservice,status      │             │
│    WHERE status='ACTIVE'                   │             │
├────────────────────────────────────────────┤             │
│consumercode (property ID, connection no)   │             │
│consumertype (RESIDENTIAL, COMMERCIAL)      │             │
│businessservice (PT, WS, TL)                │             │
│payer (user UUID)                           │             │
│taxperiodfrom, taxperiodto (epoch ms)       │             │
│minimumamountpayable                        │             │
│status (ACTIVE, CANCELLED, PAID)            │             │
│billexpirytime (epoch ms)                   │             ▼
│ispaymentcompleted (boolean)                │    ┌─────────────────────────────┐
│fixedBillExpiryDate (epoch ms)              │    │  egbs_demanddetail_v1       │
│additionaldetails (JSON)                    │    ├─────────────────────────────┤
│+ audit fields                              │    │PK: id, tenantid             │
└────────────────────────────────────────────┘    │FK: demandid,tenantid ───────┘
                                                  │   → egbs_demand_v1          │
                                                  ├─────────────────────────────┤
       taxheadcode (logical ref)                  │demandid                     │
  egbs_taxheadmaster ·····················>       │taxheadcode ◄················ egbs_taxheadmaster
                                                  │  (PT_TAX, PT_PENALTY, etc)  │
                                                  │taxamount                    │
                                                  │collectionamount             │
                                                  │additionaldetails (JSON)     │
                                                  │+ audit fields               │
                                                  └─────────────────────────────┘


BILL FLOW (Bill → Bill Details → Account Details):
═══════════════════════════════════════════════════════════════════════════════════════

┌────────────────────────────────────────────┐
│         egbs_bill_v1                       │
├────────────────────────────────────────────┤
│PK: id, tenantid                            │
│UK: consumercode,tenantid,status            │             demandid (logical ref)
│    WHERE status='ACTIVE'                   │             no FK constraint
├────────────────────────────────────────────┤     egbs_demand_v1 ············>  │
│payername, payeraddress, payeremail         │                                   │
│mobilenumber, payerid                       │                                   │
│consumercode                                │                                   │
│isactive, iscancelled                       │                                   │
│status (ACTIVE, CANCELLED, EXPIRED)         │                                   │
│filestoreid (PDF reference)                 │                                   │
│additionaldetails (JSONB)                   │                                   ▼
│+ audit fields                              │   ┌─────────────────────────────────────┐
└────────────────────────────────────────────┘   │    egbs_billdetail_v1               │
            │                                    ├─────────────────────────────────────┤
            │ [1:N]                              │PK: id, tenantid                     │
            │ FK enforced                        │FK: billid,tenantid ─────────────────┘
            │                                    │   → egbs_bill_v1                    │
            ▼                                    ├─────────────────────────────────────┤
┌────────────────────────────────────────┐       │billid                               │
│    egbs_billdetail_v1                  │       │businessservice                      │
├────────────────────────────────────────┤       │billno (human readable)              │
│PK: id, tenantid                        │       │billdate (epoch ms)                  │
│FK: billid,tenantid ─────────────────────┘      │consumercode                         │
│   → egbs_bill_v1                               │consumertype                         │
├────────────────────────────────────────┤       │billdescription, displaymessage      │
│billid                                  │       │minimumamount, totalamount           │
│businessservice                         │       │callbackforapportioning              │
│billno, billdate                        │       │partpaymentallowed                   │
│consumercode, consumertype              │       │collectionmodesnotallowed            │
│billdescription, displaymessage         │       │receiptdate, receiptnumber           │
│minimumamount, totalamount              │       │fromperiod, toperiod                 │
│receiptdate, receiptnumber              │       │demandid (no FK) ◄··················· egbs_demand_v1
│fromperiod, toperiod                    │       │isadvanceallowed                     │
│demandid (NO FK CONSTRAINT)             │       │expirydate (epoch ms)                │
│isadvanceallowed, expirydate            │       │additionaldetails (JSONB)            │
│additionaldetails (JSONB)               │       │+ audit fields                       │
│+ audit fields                          │       └─────────────────────────────────────┘
└────────────────────────────────────────┘                    │
            │                                                 │ [1:N]
            │ [1:N]                                           │ FK enforced
            │ FK enforced                                     │
            ▼                                                 ▼
┌────────────────────────────────────────────┐    ┌─────────────────────────────────────┐
│  egbs_billaccountdetail_v1                 │    │  egbs_billaccountdetail_v1          │
├────────────────────────────────────────────┤    ├─────────────────────────────────────┤
│PK: id, tenantid                            │    │PK: id, tenantid                     │
│FK: billdetail,tenantid ────────────────────┘    │FK: billdetail,tenantid ─────────────┘
│   → egbs_billdetail_v1                          │   → egbs_billdetail_v1
├────────────────────────────────────────────┤    ├─────────────────────────────────────┤
│billdetail                                  │    │billdetail                           │
│glcode (for accounting) ◄···················│····│glcode ◄························ egbs_glcodemaster
│orderno                                     │    │orderno                              │
│accountdescription                          │    │accountdescription                   │
│creditamount, debitamount                   │    │creditamount, debitamount            │
│isactualdemand                              │    │isactualdemand                       │
│purpose (ARREAR,CURRENT,ADVANCE)            │    │purpose                              │
│cramounttobepaid                            │    │cramounttobepaid                     │
│taxheadcode ◄···························egbs_taxheadmaster                          │
│amount, adjustedamount                      │    │taxheadcode ◄·················· egbs_taxheadmaster
│demanddetailid (NO FK CONSTRAINT)           │    │amount, adjustedamount               │
│additionaldetails (JSONB)                   │    │demanddetailid (no FK) ◄············· egbs_demanddetail_v1
│+ audit fields                              │    │additionaldetails (JSONB)            │
└────────────────────────────────────────────┘    │+ audit fields                       │
                                                  └─────────────────────────────────────┘


RECEIPT TRACKING:
═══════════════════════════════════════════════════════════════════════════════════════

┌────────────────────────────────────────────┐
│      egbs_collectedreceipts                │
├────────────────────────────────────────────┤
│PK: id, tenantid                            │
├────────────────────────────────────────────┤
│businessservice                             │
│consumercode                                │
│receiptnumber                               │
│receiptamount                               │
│receiptdate (epoch ms)                      │
│status                                      │
│+ audit fields                              │
└────────────────────────────────────────────┘


AMENDMENT FLOW (Amendment → Tax Details & Documents):
═══════════════════════════════════════════════════════════════════════════════════════

┌────────────────────────────────────────────┐
│         egbs_amendment                     │
├────────────────────────────────────────────┤
│PK: amendmentId, tenantid                   │
│UK: id                                      │
├────────────────────────────────────────────┤
│id, amendmentId                             │
│businessservice                             │
│consumercode                                │
│amendmentReason                             │
│reasonDocumentNumber                        │
│status (PENDING,APPROVED,REJECTED)          │
│effectiveFrom, effectiveTill (epoch ms)     │
│amendedDemandId (NO FK CONSTRAINT)          │···> egbs_demand_v1
│additionaldetails (JSONB)                   │
│+ audit fields                              │
└────────────────────────────────────────────┘
            │                       │
            │ [1:N]                 │ [1:N]
            │ No FK                 │ No FK
            ▼                       ▼
┌─────────────────────────┐  ┌────────────────────────┐
│egbs_amendment_taxdetail │  │   egbs_document        │
├─────────────────────────┤  ├────────────────────────┤
│PK: id, amendmentid      │  │PK: id                  │
├─────────────────────────┤  ├────────────────────────┤
│amendmentid (no FK)      │  │amendmentid (no FK)     │
│taxheadcode              │  │documentType            │
│taxamount                │  │fileStoreid             │
└─────────────────────────┘  │documentuid             │
                             │status                  │
                             └────────────────────────┘


AUDIT TABLES:
═══════════════════════════════════════════════════════════════════════════════════════

┌─────────────────────────────────────┐    ┌──────────────────────────────────────┐
│   egbs_demand_v1_audit              │    │   egbs_demanddetail_v1_audit         │
├─────────────────────────────────────┤    ├──────────────────────────────────────┤
│PK: id, tenantid                     │    │PK: id, tenantid                      │
│+ demandid (original demand ref)     │    │+ demanddetailid (original detail ref)│
│+ all columns from egbs_demand_v1    │    │+ all columns from egbs_demanddetail  │
└─────────────────────────────────────┘    └──────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│   egbs_payment_backupdate_audit                 │
├─────────────────────────────────────────────────┤
│UK: paymentid,isreceiptcancellation              │
│    WHERE isbackupdatesuccess='TRUE'             │
├─────────────────────────────────────────────────┤
│paymentid                                        │
│isbackupdatesuccess (boolean)                    │
│isreceiptcancellation (boolean)                  │
│errorMessage                                     │
└─────────────────────────────────────────────────┘

```

**Key Points:**
1. **Enforced Foreign Keys** (solid lines `────>`):
   - egbs_demand_v1 → egbs_demanddetail_v1
   - egbs_bill_v1 → egbs_billdetail_v1
   - egbs_billdetail_v1 → egbs_billaccountdetail_v1

2. **Logical References** (dotted lines `····>`): These are reference fields without FK constraints:
   - egbs_demanddetail_v1.taxheadcode → egbs_taxheadmaster.code
   - egbs_demand_v1.businessservice → egbs_business_service_details.businessservice
   - egbs_billdetail_v1.demandid → egbs_demand_v1.id (field exists but no FK)
   - egbs_billaccountdetail_v1.demanddetailid → egbs_demanddetail_v1.id (no FK)
   - egbs_billaccountdetail_v1.glcode → egbs_glcodemaster.glcode
   - egbs_amendment.amendedDemandId → egbs_demand_v1.id (no FK)

3. **Multi-tenancy**: All tables use composite primary keys with tenantid for data isolation

---

## Table Details

### 1. egbs_demand_v1 (Current - Demand/Tax Demand Management)

**Purpose**: Stores tax demands generated for consumers across various business services (Property Tax, Water Charges, Trade License, etc.)

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(64) | NO | Unique identifier for the demand (Primary Key) |
| tenantid | VARCHAR(250) | NO | Tenant/ULB identifier (Primary Key) |
| consumercode | VARCHAR(250) | NO | Consumer/Service identifier (e.g., property ID, connection number) |
| consumertype | VARCHAR(250) | NO | Type of consumer (e.g., RESIDENTIAL, COMMERCIAL) |
| businessservice | VARCHAR(250) | NO | Business service code (e.g., PT, WS, TL) |
| payer | VARCHAR(250) | YES | UUID of the payer/owner |
| taxperiodfrom | BIGINT | NO | Tax period start date (epoch milliseconds) |
| taxperiodto | BIGINT | NO | Tax period end date (epoch milliseconds) |
| minimumamountpayable | NUMERIC(12,2) | YES | Minimum amount that must be paid |
| status | VARCHAR(64) | YES | Demand status (ACTIVE, CANCELLED, PAID) |
| billexpirytime | BIGINT | YES | Bill expiry timestamp (epoch milliseconds) |
| ispaymentcompleted | BOOLEAN | YES | Flag indicating if payment is completed (default: false) |
| fixedBillExpiryDate | BIGINT | YES | Fixed bill expiry date (epoch milliseconds) |
| additionaldetails | JSON | YES | Additional metadata in JSON format |
| createdby | VARCHAR(256) | NO | User who created the record |
| createdtime | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastmodifiedby | VARCHAR(256) | YES | User who last modified the record |
| lastmodifiedtime | BIGINT | YES | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id, tenantid)
- Unique: (consumercode, tenantid, taxperiodfrom, taxperiodto, businessservice, status) WHERE status='ACTIVE'

**Indexes**:
- idx_egbs_demand_v1_id
- idx_egbs_demand_v1_consumercode
- idx_egbs_demand_v1_consumertype
- idx_egbs_demand_v1_businessservice
- idx_egbs_demand_v1_payer
- idx_egbs_demand_v1_taxperiodfrom
- idx_egbs_demand_v1_taxperiodto
- idx_egbs_demand_v1_tenantid

---

### 2. egbs_demanddetail_v1 (Demand Line Items/Tax Head Details)

**Purpose**: Stores individual tax head details for each demand (e.g., property tax, penalty, interest, cess)

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(64) | NO | Unique identifier for the demand detail (Primary Key) |
| tenantid | VARCHAR(250) | NO | Tenant/ULB identifier (Primary Key) |
| demandid | VARCHAR(64) | NO | Reference to parent demand (Foreign Key) |
| taxheadcode | VARCHAR(250) | NO | Tax head code (e.g., PT_TAX, PT_PENALTY, WATER_CHARGE) |
| taxamount | NUMERIC(12,2) | NO | Tax amount for this tax head |
| collectionamount | NUMERIC(12,2) | NO | Amount collected against this tax head |
| additionaldetails | JSON | YES | Additional metadata in JSON format |
| createdby | VARCHAR(256) | NO | User who created the record |
| createdtime | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastmodifiedby | VARCHAR(256) | YES | User who last modified the record |
| lastmodifiedtime | BIGINT | YES | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id, tenantid)
- Foreign Key: (tenantid, demandid) REFERENCES egbs_demand_v1(tenantid, id)

**Indexes**:
- idx_egbs_demanddetail_v1_tenantid
- idx_egbs_demanddetail_v1_demandid

---

### 3. egbs_bill_v1 (Bill Master)

**Purpose**: Stores bill header information including payer details

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(64) | NO | Unique identifier for the bill (Primary Key) |
| tenantid | VARCHAR(250) | NO | Tenant/ULB identifier (Primary Key) |
| payername | VARCHAR(256) | YES | Name of the payer |
| payeraddress | VARCHAR(1024) | YES | Address of the payer |
| payeremail | VARCHAR(256) | YES | Email address of the payer |
| mobilenumber | VARCHAR(20) | YES | Mobile number of the payer |
| payerid | VARCHAR(128) | YES | User ID of the payer |
| consumercode | VARCHAR(256) | NO | Consumer/Service identifier |
| isactive | BOOLEAN | YES | Flag indicating if bill is active |
| iscancelled | BOOLEAN | YES | Flag indicating if bill is cancelled |
| status | VARCHAR(64) | YES | Bill status (ACTIVE, CANCELLED, EXPIRED) |
| filestoreid | VARCHAR(256) | YES | Reference to stored bill PDF file |
| additionaldetails | JSONB | YES | Additional metadata in JSONB format |
| createdby | VARCHAR(256) | NO | User who created the record |
| createddate | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastmodifiedby | VARCHAR(256) | YES | User who last modified the record |
| lastmodifieddate | BIGINT | YES | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id, tenantid)
- Unique Index: (consumercode, tenantid, status) WHERE status='ACTIVE'

**Indexes**:
- idx_egbs_bill_v1_id
- idx_egbs_bill_v1_isactive
- idx_egbs_bill_v1_tenantid

---

### 4. egbs_billdetail_v1 (Bill Details)

**Purpose**: Stores detailed bill information for each business service

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(64) | NO | Unique identifier for the bill detail (Primary Key) |
| tenantid | VARCHAR(250) | NO | Tenant/ULB identifier (Primary Key) |
| billid | VARCHAR(64) | NO | Reference to parent bill (Foreign Key) |
| businessservice | VARCHAR(250) | NO | Business service code (e.g., PT, WS, TL) |
| billno | VARCHAR(1024) | YES | Bill number (human-readable) |
| billdate | BIGINT | NO | Bill generation date (epoch milliseconds) |
| consumercode | VARCHAR(250) | NO | Consumer/Service identifier |
| consumertype | VARCHAR(250) | YES | Type of consumer |
| billdescription | VARCHAR(1024) | YES | Description of the bill |
| displaymessage | VARCHAR(1024) | YES | Message to display to the user |
| minimumamount | NUMERIC(12,2) | YES | Minimum amount that can be paid |
| totalamount | NUMERIC(12,2) | YES | Total bill amount |
| callbackforapportioning | BOOLEAN | YES | Flag for apportioning callback |
| partpaymentallowed | BOOLEAN | YES | Flag indicating if partial payment is allowed |
| collectionmodesnotallowed | VARCHAR(512) | YES | Collection modes not allowed for this bill |
| receiptdate | BIGINT | YES | Receipt generation date (epoch milliseconds) |
| receiptnumber | VARCHAR(256) | YES | Receipt number |
| fromperiod | BIGINT | YES | Billing period start (epoch milliseconds) |
| toperiod | BIGINT | YES | Billing period end (epoch milliseconds) |
| demandid | VARCHAR(64) | YES | Reference to associated demand |
| isadvanceallowed | BOOLEAN | YES | Flag indicating if advance payment is allowed |
| expirydate | BIGINT | YES | Bill expiry date (epoch milliseconds) |
| additionaldetails | JSONB | YES | Additional metadata in JSONB format |
| createdby | VARCHAR(256) | NO | User who created the record |
| createddate | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastmodifiedby | VARCHAR(256) | YES | User who last modified the record |
| lastmodifieddate | BIGINT | YES | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id, tenantid)
- Foreign Key: (tenantid, billid) REFERENCES egbs_bill_v1(tenantid, id)

**Indexes**:
- idx_egbs_billdetail_v1_businessservice
- idx_egbs_billdetail_v1_consumercode
- idx_egbs_billdetail_v1_tenantid
- idx_egbs_billdetail_v1_billid

---

### 5. egbs_billaccountdetail_v1 (Bill Account Details/Ledger Entries)

**Purpose**: Stores accounting/ledger details for bills including GL codes and amounts (debit/credit)

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(64) | NO | Unique identifier for the account detail (Primary Key) |
| tenantid | VARCHAR(250) | NO | Tenant/ULB identifier (Primary Key) |
| billdetail | VARCHAR(64) | NO | Reference to parent bill detail (Foreign Key) |
| glcode | VARCHAR(250) | YES | General Ledger code for accounting |
| orderno | INTEGER | YES | Display order number |
| accountdescription | VARCHAR(512) | YES | Description of the account entry |
| creditamount | NUMERIC(12,2) | YES | Credit amount |
| debitamount | NUMERIC(12,2) | YES | Debit amount |
| isactualdemand | BOOLEAN | YES | Flag indicating if this is actual demand |
| purpose | VARCHAR(250) | YES | Purpose of the entry (ARREAR, CURRENT, ADVANCE) |
| cramounttobepaid | NUMERIC(12,2) | YES | Credit amount to be paid |
| taxheadcode | VARCHAR(256) | YES | Associated tax head code |
| amount | NUMERIC(10,2) | YES | Amount |
| adjustedamount | NUMERIC(10,2) | YES | Adjusted amount after apportioning |
| demanddetailid | VARCHAR(64) | YES | Reference to demand detail |
| additionaldetails | JSONB | YES | Additional metadata in JSONB format |
| createdby | VARCHAR(256) | NO | User who created the record |
| createddate | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastmodifiedby | VARCHAR(256) | YES | User who last modified the record |
| lastmodifieddate | BIGINT | YES | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id, tenantid)
- Foreign Key: (billdetail, tenantid) REFERENCES egbs_billdetail_v1(id, tenantid)

**Indexes**:
- idx_egbs_billaccountdetail_v1_billdetail

---

### 6. egbs_taxperiod (Tax Period Configuration)

**Purpose**: Defines tax periods for various business services (e.g., FY 2023-24, Q1 2024)

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(64) | NO | Unique identifier for the tax period (Primary Key) |
| tenantid | VARCHAR(250) | NO | Tenant/ULB identifier (Primary Key) |
| service | VARCHAR(100) | NO | Business service code |
| code | VARCHAR(25) | NO | Tax period code (e.g., FY2023-24) |
| fromdate | BIGINT | NO | Period start date (epoch milliseconds) |
| todate | BIGINT | NO | Period end date (epoch milliseconds) |
| financialyear | VARCHAR(50) | YES | Financial year (e.g., 2023-24) |
| periodcycle | VARCHAR(64) | YES | Period cycle (ANNUAL, QUARTERLY, MONTHLY) |
| createddate | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastmodifieddate | BIGINT | NO | Last modification timestamp (epoch milliseconds) |
| createdby | VARCHAR(64) | NO | User who created the record |
| lastmodifiedby | VARCHAR(64) | NO | User who last modified the record |

**Constraints**:
- Primary Key: (id, tenantid)
- Unique: (service, code, tenantid)

---

### 7. egbs_business_service_details (Business Service Configuration)

**Purpose**: Stores configuration for different business services (payment behavior, apportioning, etc.)

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(64) | NO | Unique identifier (Primary Key) |
| tenantid | VARCHAR(250) | NO | Tenant/ULB identifier (Primary Key) |
| businessservice | VARCHAR(250) | NO | Business service code (e.g., PT, WS, TL) |
| collectionmodesnotallowed | VARCHAR(250) | YES | Collection modes not allowed (comma-separated) |
| callbackforapportioning | BOOLEAN | YES | Flag for apportioning callback (default: false) |
| partpaymentallowed | BOOLEAN | YES | Flag for allowing partial payments (default: false) |
| callbackapportionurl | VARCHAR(250) | YES | URL for apportioning callback |
| createddate | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastmodifieddate | BIGINT | NO | Last modification timestamp (epoch milliseconds) |
| createdby | VARCHAR(64) | NO | User who created the record |
| lastmodifiedby | VARCHAR(64) | NO | User who last modified the record |

**Constraints**:
- Primary Key: (id, tenantid)
- Unique: (businessservice, tenantid)

---

### 8. egbs_taxheadmaster (Tax Head Master Data)

**Purpose**: Master data defining various tax heads (e.g., PT_TAX, PT_PENALTY, WATER_CHARGE, etc.)

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(64) | NO | Unique identifier (Primary Key) |
| tenantid | VARCHAR(128) | NO | Tenant/ULB identifier (Primary Key) |
| category | VARCHAR(250) | NO | Category of tax head (TAX, PENALTY, REBATE, etc.) |
| service | VARCHAR(64) | NO | Associated business service |
| name | VARCHAR(64) | NO | Display name of the tax head |
| code | VARCHAR(64) | YES | Unique tax head code |
| isdebit | BOOLEAN | YES | Flag indicating if this is a debit entry |
| isactualdemand | BOOLEAN | YES | Flag indicating if this represents actual demand |
| orderno | INTEGER | YES | Display order number |
| validfrom | BIGINT | YES | Valid from date (epoch milliseconds) |
| validtill | BIGINT | YES | Valid till date (epoch milliseconds) |
| createdby | VARCHAR(64) | YES | User who created the record |
| createdtime | BIGINT | YES | Creation timestamp (epoch milliseconds) |
| lastmodifiedby | VARCHAR(64) | YES | User who last modified the record |
| lastmodifiedtime | BIGINT | YES | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id, tenantid)

---

### 9. egbs_glcodemaster (GL Code Master Data)

**Purpose**: Maps tax heads to General Ledger (GL) codes for accounting integration

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(64) | NO | Unique identifier (Primary Key) |
| tenantid | VARCHAR(128) | NO | Tenant/ULB identifier (Primary Key) |
| taxhead | VARCHAR(250) | NO | Tax head code |
| service | VARCHAR(64) | NO | Business service code |
| fromdate | BIGINT | NO | Valid from date (epoch milliseconds) |
| todate | BIGINT | NO | Valid to date (epoch milliseconds) |
| glcode | VARCHAR(64) | YES | General Ledger code |
| createdby | VARCHAR(64) | YES | User who created the record |
| createdtime | BIGINT | YES | Creation timestamp (epoch milliseconds) |
| lastmodifiedby | VARCHAR(64) | YES | User who last modified the record |
| lastmodifiedtime | BIGINT | YES | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id, tenantid)

---

### 10. egbs_collectedreceipts (Collected Receipts Tracking)

**Purpose**: Tracks receipts collected against demands to prevent duplicate payment processing

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(64) | NO | Unique identifier (Primary Key) |
| tenantid | VARCHAR(250) | NO | Tenant/ULB identifier (Primary Key) |
| businessservice | VARCHAR(256) | NO | Business service code |
| consumercode | VARCHAR(250) | NO | Consumer/Service identifier |
| receiptnumber | VARCHAR(1024) | YES | Receipt number |
| receiptamount | NUMERIC(12,2) | NO | Receipt amount |
| receiptdate | BIGINT | YES | Receipt date (epoch milliseconds) |
| status | VARCHAR(1024) | YES | Receipt status |
| createdby | VARCHAR(64) | NO | User who created the record |
| createddate | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastmodifiedby | VARCHAR(64) | YES | User who last modified the record |
| lastmodifieddate | BIGINT | YES | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id, tenantid)

---

### 11. egbs_amendment (Amendment Records)

**Purpose**: Stores amendment requests for modifying existing demands

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(256) | NO | Unique identifier |
| tenantid | VARCHAR(256) | NO | Tenant/ULB identifier (Primary Key) |
| amendmentId | VARCHAR(256) | NO | Amendment ID (Primary Key) |
| businessservice | VARCHAR(256) | NO | Business service code |
| consumercode | VARCHAR(256) | NO | Consumer/Service identifier |
| amendmentReason | VARCHAR(256) | NO | Reason for amendment |
| reasonDocumentNumber | VARCHAR(256) | YES | Supporting document number |
| status | VARCHAR(256) | NO | Amendment status (PENDING, APPROVED, REJECTED) |
| effectiveTill | BIGINT | YES | Amendment effective till date (epoch milliseconds) |
| effectiveFrom | BIGINT | YES | Amendment effective from date (epoch milliseconds) |
| amendedDemandId | VARCHAR(256) | YES | Reference to amended demand |
| additionaldetails | JSONB | YES | Additional metadata in JSONB format |
| createdby | VARCHAR(256) | NO | User who created the record |
| createdtime | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastmodifiedby | VARCHAR(256) | NO | User who last modified the record |
| lastmodifiedtime | BIGINT | NO | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (amendmentId, tenantid)
- Unique: (id)

---

### 12. egbs_amendment_taxdetail (Amendment Tax Details)

**Purpose**: Stores tax head-wise details for amendments

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(128) | NO | Unique identifier (Primary Key) |
| amendmentid | VARCHAR(128) | NO | Reference to amendment (Primary Key) |
| taxheadcode | VARCHAR(250) | NO | Tax head code being amended |
| taxamount | NUMERIC(12,2) | NO | New tax amount |

**Constraints**:
- Primary Key: (id, amendmentid)

---

### 13. egbs_document (Amendment Documents)

**Purpose**: Stores documents attached to amendments

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(128) | NO | Unique identifier (Primary Key) |
| amendmentid | VARCHAR(256) | NO | Reference to amendment |
| documentType | VARCHAR(256) | NO | Type of document |
| fileStoreid | VARCHAR(256) | NO | File store reference ID |
| documentuid | VARCHAR(256) | YES | Document UID |
| status | VARCHAR(256) | NO | Document status |

**Constraints**:
- Primary Key: (id)

---

### 14. egbs_payment_backupdate_audit (Payment Back Update Audit)

**Purpose**: Audit trail for payment back update operations (updating demands after payment)

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| paymentid | VARCHAR(256) | NO | Payment identifier |
| isbackupdatesuccess | BOOLEAN | NO | Flag indicating if back update was successful |
| isreceiptcancellation | BOOLEAN | NO | Flag indicating if this is a receipt cancellation |
| errorMessage | VARCHAR | YES | Error message if update failed |

**Constraints**:
- Unique Index: (paymentid, isreceiptcancellation) WHERE isbackupdatesuccess='TRUE'

---

## Audit Tables

### egbs_demand_v1_audit

**Purpose**: Audit trail for all changes to demands

**Columns**: Similar to egbs_demand_v1 with additional:
- demandid (VARCHAR(64)) - Reference to original demand

### egbs_demanddetail_v1_audit

**Purpose**: Audit trail for all changes to demand details

**Columns**: Similar to egbs_demanddetail_v1 with additional:
- demanddetailid (VARCHAR(64)) - Reference to original demand detail

---

## Legacy Tables (Deprecated - Use v1 tables instead)

The following tables are legacy versions and should not be used for new development:

1. **egbs_demand** - Replaced by egbs_demand_v1
2. **egbs_demanddetail** - Replaced by egbs_demanddetail_v1
3. **egbs_bill** - Replaced by egbs_bill_v1
4. **egbs_billdetail** - Replaced by egbs_billdetail_v1
5. **egbs_billaccountdetail** - Replaced by egbs_billaccountdetail_v1

---

## Sequences

The following sequences are used for generating IDs:

- seq_egbs_demand
- seq_egbs_demanddetail
- seq_egbs_bill
- seq_egbs_billdetail
- seq_egbs_billnumber
- seq_egbs_billaccountdetail
- seq_egbs_taxperiod
- seq_egbs_business_srvc_details
- seq_egbs_taxHeadMaster
- seq_egbs_taxHeadMastercode
- seq_egbs_glcodemaster
- seq_egbs_collectedreceipts

---

## Key Relationships

1. **egbs_demand_v1** ➔ **egbs_demanddetail_v1** (1:N)
   - One demand can have multiple demand details (tax heads)

2. **egbs_bill_v1** ➔ **egbs_billdetail_v1** (1:N)
   - One bill can have multiple bill details (for different services)

3. **egbs_billdetail_v1** ➔ **egbs_billaccountdetail_v1** (1:N)
   - One bill detail can have multiple account details (GL entries)

4. **egbs_amendment** ➔ **egbs_amendment_taxdetail** (1:N)
   - One amendment can have multiple tax details

5. **egbs_amendment** ➔ **egbs_document** (1:N)
   - One amendment can have multiple supporting documents

6. **egbs_taxheadmaster** ➔ **egbs_demanddetail_v1** (1:N)
   - Tax head master defines the tax heads used in demand details

7. **egbs_glcodemaster** ➔ **egbs_billaccountdetail_v1** (1:N)
   - GL code master provides GL codes for bill account details
