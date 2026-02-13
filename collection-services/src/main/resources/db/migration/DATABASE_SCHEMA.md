# Collection Services Database Schema Documentation

## Overview
This document provides a comprehensive overview of the database schema for the eGov Collection Services. The collection service manages receipts, payments, instruments (cheques, online transactions, etc.), bills, and remittances for various government services.

## Database Schema Version
The database follows a versioned migration approach using Flyway. The current schema includes both v1 (current) and legacy tables.

---

## ASCII Entity Relationship Diagram

**Legend:**
- `────>` Solid line = Foreign Key Relationship (enforced in database)
- `····>` Dotted line = Logical Reference (not enforced by FK)
- `[1:N]` = One to Many relationship
- `[M:N]` = Many to Many relationship

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                    COLLECTION SERVICES DATABASE SCHEMA                               │
└─────────────────────────────────────────────────────────────────────────────────────┘


RECEIPT FLOW (v1 - Current Schema):
═══════════════════════════════════════════════════════════════════════════════════════

┌──────────────────────────────────────────────┐
│      egcl_receiptheader_v1                   │
├──────────────────────────────────────────────┤
│PK: id                                        │
├──────────────────────────────────────────────┤
│payername, payeraddress, payeremail           │
│paidby, payermobile, payerid                  │
│receiptnumber (system generated)              │
│manualreceiptnumber (manual receipt no)       │
│manualreceiptdate                             │
│receipttype (CHALLAN, BILL, MANUAL)           │
│receiptdate (epoch ms)                        │
│referencenumber, referencedesc                │
│referencedate                                 │
│businessdetails (service code)                │
│collectiontype (COUNTER, ONLINE)              │
│consumercode, consumertype                    │
│status (APPROVED, CANCELLED, REMITTED)        │
│totalamount, collectedamount                  │
│minimumamount                                 │
│isreconciled                                  │
│reasonforcancellation                         │
│cancellationremarks                           │
│transactionid                                 │
│channel (SYSTEM, MOBILE, WEB)                 │
│displaymsg                                    │
│collmodesnotallwd                             │
│demandid (no FK) ························> (external demand)
│demandfromdate, demandtodate                  │
│fund, fundsource, function                    │
│boundary, department                          │
│voucherheader, depositedbranch                │
│additionaldetails (JSONB)                     │
│+ audit fields                                │
└──────────────────────────────────────────────┘
            │                                │
            │ [1:N]                          │ [M:N via junction]
            │ FK enforced                    │
            │                                │
            ▼                                ▼
┌──────────────────────────────────┐    ┌────────────────────────────────┐
│  egcl_receiptdetails_v1          │    │ egcl_receiptinstrument_v1      │
├──────────────────────────────────┤    │    (Junction Table)            │
│PK: id                            │    ├────────────────────────────────┤
│FK: receiptheader ────────────────┘    │FK: receiptheader ──────────────┘
│   → egcl_receiptheader_v1             │   → egcl_receiptheader_v1
├──────────────────────────────────┤    │FK: instrumentheader
│receiptheader                     │    │   → egcl_instrumentheader_v1   │
│chartofaccount (GL code)          │    └────────────────────────────────┘
│ordernumber (display order)       │                 │
│dramount (debit amount)           │                 │ FK enforced
│cramount (credit amount)          │                 │
│actualcramounttobepaid            │                 ▼
│description                       │    ┌────────────────────────────────┐
│financialyear                     │    │  egcl_instrumentheader_v1      │
│isactualdemand                    │    ├────────────────────────────────┤
│purpose (ARREAR,CURRENT,ADVANCE)  │    │PK: id                          │
│amount                            │    ├────────────────────────────────┤
│adjustedamount                    │    │transactionnumber (unique)      │
│taxheadcode                       │    │transactiondate (epoch ms)      │
│demanddetailid (no FK) ···········│····│amount                          │
│additionaldetails (JSONB)         │    │instrumenttype (CASH, CHEQUE,   │
│+ tenantid                        │    │  DD, CARD, ONLINE, etc)        │
└──────────────────────────────────┘    │instrumentstatus (NEW,          │
                                        │  DEPOSITED, DISHONOURED, etc)  │
                                        │instrumentdate                  │
                                        │instrumentnumber (cheque/DD no) │
                                        │bankid, bankaccountid           │
                                        │branchname, ifsccode            │
                                        │financialstatus                 │
                                        │transactiontype (DEBIT/CREDIT)  │
                                        │payee, drawer                   │
                                        │serialno, surrenderreason       │
                                        │additionaldetails (JSONB)       │
                                        │+ audit fields                  │
                                        └────────────────────────────────┘


PAYMENT FLOW (Payment → Bill → Bill Details → Account Details):
═══════════════════════════════════════════════════════════════════════════════════════

┌──────────────────────────────────────────────┐
│          egcl_payment                        │
├──────────────────────────────────────────────┤
│PK: id                                        │
├──────────────────────────────────────────────┤
│tenantid                                      │
│transactionNumber (unique)                    │
│transactionDate (epoch ms)                    │
│totalDue                                      │
│totalAmountPaid                               │
│paymentMode (CASH, ONLINE, CHEQUE, etc)       │
│paymentStatus (NEW, DEPOSITED, etc)           │
│instrumentDate, instrumentNumber              │
│instrumentStatus                              │
│ifscCode                                      │
│paidBy, payerName                             │
│payerAddress, payerEmail                      │
│payerId, mobileNumber                         │
│filestoreid (receipt PDF reference)           │
│additionalDetails (JSONB)                     │
│+ audit fields                                │
└──────────────────────────────────────────────┘
            │
            │ [1:N]
            │ FK enforced
            │
            ▼
┌──────────────────────────────────────────────┐
│       egcl_paymentDetail                     │
├──────────────────────────────────────────────┤
│PK: id                                        │
│FK: paymentid ────────────────────────────────┘
│   → egcl_payment
│UK: billId (unique - one payment per bill)    │
├──────────────────────────────────────────────┤
│tenantid                                      │
│paymentid                                     │
│due                                           │
│amountPaid                                    │
│receiptNumber (generated)                     │
│receiptDate (epoch ms)                        │
│receiptType                                   │
│businessService (PT, WS, TL, etc)             │
│billId                                        │
│manualreceiptnumber                           │
│additionalDetails (JSONB)                     │
│+ audit fields                                │
└──────────────────────────────────────────────┘
            │
            │ [1:1]
            │ FK enforced (unique billId)
            │
            ▼
┌──────────────────────────────────────────────┐
│            egcl_bill                         │
├──────────────────────────────────────────────┤
│PK: id                                        │
│FK: id ───────────────────────────────────────┘
│   → egcl_paymentdetail(billid)
├──────────────────────────────────────────────┤
│tenantid                                      │
│businessService (PT, WS, TL, etc)             │
│consumerCode                                  │
│billNumber (human readable)                   │
│billDate (epoch ms)                           │
│totalAmount                                   │
│status (ACTIVE, CANCELLED, PAID, EXPIRED)     │
│isCancelled                                   │
│reasonForCancellation                         │
│collectionModesNotAllowed                     │
│partPaymentAllowed                            │
│isAdvanceAllowed                              │
│minimumAmountToBePaid                         │
│additionalDetails (JSONB)                     │
│+ audit fields                                │
└──────────────────────────────────────────────┘
            │
            │ [1:N]
            │ FK enforced
            │
            ▼
┌──────────────────────────────────────────────┐
│         egcl_billdetial                      │
│         (note: typo in table name)           │
├──────────────────────────────────────────────┤
│PK: id                                        │
│FK: billId ────────────────────────────────────┘
│   → egcl_bill
├──────────────────────────────────────────────┤
│tenantid                                      │
│billId                                        │
│demandId (no FK) ··················> (external demand)
│amount                                        │
│amountPaid                                    │
│fromPeriod (epoch ms)                         │
│toPeriod (epoch ms)                           │
│expiryDate                                    │
│billDescription                               │
│displayMessage                                │
│collectionType                                │
│channel (SYSTEM, CSC, MOBILE, etc)            │
│voucherHeader                                 │
│boundary                                      │
│manualReceiptDate                             │
│callBackForApportioning                       │
│cancellationRemarks                           │
│additionalDetails (JSONB)                     │
└──────────────────────────────────────────────┘
            │
            │ [1:N]
            │ FK enforced
            │
            ▼
┌──────────────────────────────────────────────┐
│      egcl_billAccountDetail                  │
├──────────────────────────────────────────────┤
│PK: id                                        │
│FK: billDetailid ──────────────────────────────┘
│   → egcl_billdetial
├──────────────────────────────────────────────┤
│tenantid                                      │
│billDetailid                                  │
│demandDetailId (no FK) ············> (external demand detail)
│taxHeadCode (PT_TAX, PENALTY, etc)            │
│"order" (display order)                       │
│amount                                        │
│adjustedamount                                │
│isActualDemand                                │
│additionalDetails (JSONB)                     │
└──────────────────────────────────────────────┘


REMITTANCE FLOW (Bank Remittance Management):
═══════════════════════════════════════════════════════════════════════════════════════

┌──────────────────────────────────────────────┐
│         egcl_remittance                      │
├──────────────────────────────────────────────┤
│PK: id                                        │
├──────────────────────────────────────────────┤
│tenantid                                      │
│referencenumber (challan/remittance ref)      │
│referencedate (epoch ms)                      │
│voucherheader (voucher reference)             │
│bankaccount                                   │
│fund, function                                │
│status (APPROVED, SUBMITTED, etc)             │
│remarks                                       │
│reasonfordelay                                │
│+ audit fields                                │
└──────────────────────────────────────────────┘
            │                     │
            │ [1:N]               │ [1:N]
            │ No FK               │ No FK
            │                     │
            ▼                     ▼
┌────────────────────────┐  ┌───────────────────────────┐
│egcl_remittancedetails  │  │  egcl_remittanceinstrument│
├────────────────────────┤  ├───────────────────────────┤
│PK: id                  │  │PK: id                     │
├────────────────────────┤  ├───────────────────────────┤
│tenantid                │  │tenantid                   │
│remittance (no FK)      │  │remittance (no FK)         │
│chartofaccount          │  │instrument (no FK)         │
│creditamount            │  │  → instrument reference   │
│debitamount             │  │reconciled (boolean)       │
└────────────────────────┘  └───────────────────────────┘
                                      │
                                      │ [1:N]
                                      │ No FK
                                      ▼
                           ┌───────────────────────────┐
                           │ egcl_remittancereceipt    │
                           ├───────────────────────────┤
                           │PK: id                     │
                           ├───────────────────────────┤
                           │tenantid                   │
                           │remittance (no FK)         │
                           │receipt (no FK)            │
                           │  → receipt reference      │
                           └───────────────────────────┘


CONFIGURATION TABLE:
═══════════════════════════════════════════════════════════════════════════════════════

┌──────────────────────────────────────────────┐
│   egcl_bankaccountservicemapping             │
├──────────────────────────────────────────────┤
│PK: id                                        │
├──────────────────────────────────────────────┤
│tenantid                                      │
│businessdetails (service code)                │
│bankaccount                                   │
│bank, bankbranch                              │
│active (boolean)                              │
│version                                       │
│+ audit fields                                │
└──────────────────────────────────────────────┘


HISTORY/AUDIT TABLES:
═══════════════════════════════════════════════════════════════════════════════════════

┌────────────────────────────────┐    ┌────────────────────────────────┐
│egcl_receiptheader_v1_history   │    │egcl_receiptdetails_v1_history  │
├────────────────────────────────┤    ├────────────────────────────────┤
│+ uuid (audit record ID)        │    │+ uuid (audit record ID)        │
│+ all columns from main table   │    │+ all columns from main table   │
└────────────────────────────────┘    └────────────────────────────────┘

┌────────────────────────────────┐
│egcl_instrumentheader_v1_history│
├────────────────────────────────┤
│+ uuid (audit record ID)        │
│+ all columns from main table   │
└────────────────────────────────┘

┌────────────────────────────────┐    ┌────────────────────────────────┐
│   egcl_payment_audit           │    │  egcl_paymentDetail_audit      │
├────────────────────────────────┤    ├────────────────────────────────┤
│all columns from egcl_payment   │    │all columns from                │
│(no PK)                         │    │egcl_paymentDetail (no PK)      │
└────────────────────────────────┘    └────────────────────────────────┘

┌────────────────────────────────┐    ┌────────────────────────────────┐
│      egcl_bill_audit           │    │   egcl_billdetial_audit        │
├────────────────────────────────┤    ├────────────────────────────────┤
│all columns from egcl_bill      │    │all columns from                │
│(no PK)                         │    │egcl_billdetial (no PK)         │
└────────────────────────────────┘    └────────────────────────────────┘

```

**Key Points:**
1. **Enforced Foreign Keys** (solid lines `────>`):
   - egcl_receiptheader_v1 → egcl_receiptdetails_v1
   - egcl_receiptheader_v1 → egcl_receiptinstrument_v1 ← egcl_instrumentheader_v1 (M:N via junction)
   - egcl_payment → egcl_paymentDetail
   - egcl_paymentDetail → egcl_bill (via billId unique constraint)
   - egcl_bill → egcl_billdetial
   - egcl_billdetial → egcl_billAccountDetail

2. **Logical References** (dotted lines `····>`): These are reference fields without FK constraints:
   - egcl_receiptheader_v1.demandid → external demand system
   - egcl_receiptdetails_v1.demanddetailid → external demand detail
   - egcl_billdetial.demandId → external demand system
   - egcl_billAccountDetail.demandDetailId → external demand detail
   - egcl_remittance* tables have no FK constraints (legacy design)

3. **Multi-tenancy**: All tables include tenantid for data isolation

---

## Table Details

### RECEIPT TABLES (v1 - Current)

### 1. egcl_receiptheader_v1 (Receipt Master)

**Purpose**: Stores receipt header information for all types of collections (counter, online, challan, bill-based)

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(36) | NO | Unique identifier (UUID) for the receipt (Primary Key) |
| tenantid | VARCHAR | NO | Tenant/ULB identifier |
| payername | VARCHAR(256) | YES | Name of the payer |
| payeraddress | VARCHAR(1024) | YES | Address of the payer |
| payeremail | VARCHAR(254) | YES | Email address of the payer |
| payermobile | VARCHAR(50) | YES | Mobile number of the payer |
| paidby | VARCHAR(1024) | YES | Person who made the payment |
| payerid | VARCHAR(256) | YES | User ID of the payer |
| receiptnumber | VARCHAR(50) | YES | System-generated receipt number |
| manualreceiptnumber | VARCHAR(50) | YES | Manual receipt number (for counter receipts) |
| manualreceiptdate | BIGINT | YES | Manual receipt date (epoch milliseconds) |
| receipttype | VARCHAR(32) | NO | Type of receipt (CHALLAN, BILL, MANUAL) |
| receiptdate | BIGINT | NO | Receipt generation date (epoch milliseconds) |
| referencenumber | VARCHAR(50) | YES | Reference number (bill/challan number) |
| referencedesc | VARCHAR(250) | YES | Description of reference |
| referencedate | BIGINT | NO | Reference date (epoch milliseconds) |
| businessdetails | VARCHAR(32) | NO | Business service code (PT, WS, TL, etc) |
| collectiontype | VARCHAR(50) | NO | Collection type (COUNTER, ONLINE) |
| consumercode | VARCHAR(256) | YES | Consumer/Service identifier |
| consumertype | VARCHAR(100) | YES | Type of consumer |
| status | VARCHAR(50) | NO | Receipt status (APPROVED, CANCELLED, REMITTED) |
| totalamount | NUMERIC(12,2) | YES | Total receipt amount |
| collectedamount | NUMERIC(12,2) | YES | Amount actually collected |
| minimumamount | NUMERIC(12,2) | YES | Minimum amount to be collected |
| isreconciled | BOOLEAN | YES | Flag indicating if receipt is reconciled |
| reasonforcancellation | VARCHAR(250) | YES | Reason for cancellation |
| cancellationremarks | VARCHAR(256) | YES | Remarks for cancellation |
| transactionid | VARCHAR(50) | YES | Payment gateway transaction ID |
| channel | VARCHAR(20) | YES | Channel used (SYSTEM, CSC, MOBILE, WEB) |
| displaymsg | VARCHAR(256) | YES | Message to display to user |
| collmodesnotallwd | VARCHAR(256) | YES | Collection modes not allowed |
| demandid | VARCHAR(256) | YES | Reference to demand (no FK constraint) |
| demandfromdate | BIGINT | YES | Demand period from date (epoch milliseconds) |
| demandtodate | BIGINT | YES | Demand period to date (epoch milliseconds) |
| fund | VARCHAR | YES | Fund code |
| fundsource | VARCHAR | YES | Fund source |
| function | VARCHAR | YES | Function code |
| boundary | VARCHAR | YES | Boundary/ward code |
| department | VARCHAR | YES | Department code |
| voucherheader | VARCHAR | YES | Voucher reference |
| depositedbranch | VARCHAR | YES | Branch where amount was deposited |
| reference_ch_id | BIGINT | YES | Legacy reference field |
| stateid | BIGINT | YES | Workflow state ID |
| location | BIGINT | YES | Location code |
| version | BIGINT | NO | Optimistic locking version (default: 1) |
| additionaldetails | JSONB | YES | Additional metadata in JSONB format |
| createdby | VARCHAR(256) | NO | User who created the record |
| createddate | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastmodifiedby | VARCHAR(256) | NO | User who last modified the record |
| lastmodifieddate | BIGINT | NO | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id)

**Indexes**:
- idx_rcpthd_v1_consumercode
- idx_rcpthd_v1_transactionid
- idx_rcpthd_v1_mreceiptnumber
- idx_rcpthd_v1_refno
- idx_rcpthd_v1_business
- idx_rcpthd_v1_status

---

### 2. egcl_receiptdetails_v1 (Receipt Line Items)

**Purpose**: Stores line-item details for receipts including account codes and amounts

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(36) | NO | Unique identifier (UUID) for the receipt detail (Primary Key) |
| tenantid | VARCHAR | NO | Tenant/ULB identifier |
| receiptheader | VARCHAR(36) | NO | Reference to parent receipt (Foreign Key) |
| chartofaccount | VARCHAR | YES | Chart of account/GL code |
| ordernumber | BIGINT | YES | Display order number |
| dramount | NUMERIC(12,2) | YES | Debit amount |
| cramount | NUMERIC(12,2) | YES | Credit amount |
| actualcramounttobepaid | NUMERIC(12,2) | YES | Actual credit amount to be paid |
| amount | NUMERIC(12,2) | YES | Line item amount |
| adjustedamount | NUMERIC(12,2) | YES | Adjusted amount after apportioning |
| description | VARCHAR(500) | YES | Description of the line item |
| financialyear | VARCHAR | YES | Financial year |
| isactualdemand | BOOLEAN | YES | Flag indicating if this is actual demand |
| purpose | VARCHAR(50) | YES | Purpose of collection (ARREAR, CURRENT, ADVANCE) |
| taxheadcode | VARCHAR(256) | YES | Tax head code (PT_TAX, PT_PENALTY, etc) |
| demanddetailid | VARCHAR(256) | YES | Reference to demand detail (no FK constraint) |
| additionaldetails | JSONB | YES | Additional metadata in JSONB format |

**Constraints**:
- Primary Key: (id)
- Foreign Key: (receiptheader) REFERENCES egcl_receiptheader_v1(id) ON UPDATE CASCADE ON DELETE CASCADE

**Indexes**:
- idx_receiptdetails_v1_receiptheader

---

### 3. egcl_instrumentheader_v1 (Payment Instrument)

**Purpose**: Stores payment instrument information (cash, cheque, DD, card, online transactions)

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(36) | NO | Unique identifier (UUID) for the instrument (Primary Key) |
| tenantid | VARCHAR(250) | YES | Tenant/ULB identifier |
| transactionnumber | VARCHAR(50) | NO | Unique transaction number |
| transactiondate | BIGINT | NO | Transaction date (epoch milliseconds) |
| transactiontype | VARCHAR(6) | YES | Transaction type (DEBIT/CREDIT) |
| amount | NUMERIC(12,2) | NO | Instrument amount |
| instrumenttype | VARCHAR(50) | NO | Type (CASH, CHEQUE, DD, CARD, ONLINE, etc) |
| instrumentstatus | VARCHAR(50) | NO | Status (NEW, DEPOSITED, DISHONOURED, RECONCILED) |
| instrumentdate | BIGINT | YES | Instrument date (epoch milliseconds) |
| instrumentnumber | VARCHAR(50) | YES | Instrument number (cheque/DD number) |
| bankid | VARCHAR(50) | YES | Bank identifier |
| bankaccountid | VARCHAR(50) | YES | Bank account identifier |
| branchname | VARCHAR(50) | YES | Bank branch name |
| ifsccode | VARCHAR(20) | YES | IFSC code |
| financialstatus | VARCHAR(50) | YES | Financial status |
| payee | VARCHAR(50) | YES | Payee name |
| drawer | VARCHAR(100) | YES | Drawer name (for cheques) |
| serialno | VARCHAR(50) | YES | Serial number |
| surrenderreason | VARCHAR(50) | YES | Reason for surrender/dishonor |
| additionaldetails | JSONB | YES | Additional metadata in JSONB format |
| createdby | VARCHAR(50) | YES | User who created the record |
| createddate | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastmodifiedby | VARCHAR(50) | YES | User who last modified the record |
| lastmodifieddate | BIGINT | NO | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id)

**Indexes**:
- idx_ins_transactionnumber_v1

---

### 4. egcl_receiptinstrument_v1 (Receipt-Instrument Junction)

**Purpose**: Many-to-many relationship table linking receipts with payment instruments (a receipt can have multiple instruments and an instrument can be used for multiple receipts)

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| receiptheader | VARCHAR(36) | NO | Reference to receipt (Foreign Key) |
| instrumentheader | VARCHAR(36) | NO | Reference to instrument (Foreign Key) |

**Constraints**:
- Foreign Key: (receiptheader) REFERENCES egcl_receiptheader_v1(id) ON UPDATE CASCADE ON DELETE CASCADE
- Foreign Key: (instrumentheader) REFERENCES egcl_instrumentheader_v1(id) ON UPDATE CASCADE ON DELETE CASCADE

**Indexes**:
- idx_receiptinstrument_v1_receiptheader
- idx_receiptinstrument_v1_instrumentheader

---

### PAYMENT TABLES

### 5. egcl_payment (Payment Master)

**Purpose**: Stores payment transaction information

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(256) | NO | Unique identifier for the payment (Primary Key) |
| tenantid | VARCHAR(256) | NO | Tenant/ULB identifier |
| transactionNumber | VARCHAR(256) | NO | Unique transaction number |
| transactionDate | BIGINT | NO | Transaction date (epoch milliseconds) |
| totalDue | NUMERIC(12,2) | NO | Total amount due |
| totalAmountPaid | NUMERIC(12,2) | NO | Total amount paid |
| paymentMode | VARCHAR(64) | NO | Payment mode (CASH, ONLINE, CHEQUE, DD, CARD) |
| paymentStatus | VARCHAR(256) | NO | Payment status (NEW, DEPOSITED, DISHONOURED) |
| instrumentDate | BIGINT | YES | Instrument date (epoch milliseconds) |
| instrumentNumber | VARCHAR(256) | YES | Instrument number |
| instrumentStatus | VARCHAR(256) | NO | Instrument status |
| ifscCode | VARCHAR(64) | YES | IFSC code |
| paidBy | VARCHAR(256) | YES | Person who made the payment |
| payerName | VARCHAR(256) | YES | Name of the payer |
| payerAddress | VARCHAR(1024) | YES | Address of the payer |
| payerEmail | VARCHAR(256) | YES | Email of the payer |
| payerId | VARCHAR(256) | YES | User ID of the payer |
| mobileNumber | VARCHAR(64) | NO | Mobile number of the payer |
| filestoreid | VARCHAR(1024) | YES | File store ID for receipt PDF |
| additionalDetails | JSONB | YES | Additional metadata in JSONB format |
| createdBy | VARCHAR(256) | NO | User who created the record |
| createdtime | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastModifiedBy | VARCHAR(256) | NO | User who last modified the record |
| lastModifiedTime | BIGINT | NO | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id)

**Indexes**:
- idx_egcl_payment_transactionNumber
- idx_egcl_payment_payerId
- idx_egcl_payment_mobileNumber

---

### 6. egcl_paymentDetail (Payment Details)

**Purpose**: Stores detailed information for each bill paid in a payment transaction

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(256) | NO | Unique identifier for the payment detail (Primary Key) |
| tenantid | VARCHAR(256) | NO | Tenant/ULB identifier |
| paymentid | VARCHAR(256) | NO | Reference to parent payment (Foreign Key) |
| due | NUMERIC(12,2) | NO | Amount due for this bill |
| amountPaid | NUMERIC(12,2) | NO | Amount paid for this bill |
| receiptNumber | VARCHAR(256) | NO | Receipt number generated |
| receiptDate | BIGINT | NO | Receipt generation date (epoch milliseconds) |
| receiptType | VARCHAR(256) | NO | Type of receipt |
| businessService | VARCHAR(256) | NO | Business service code (PT, WS, TL, etc) |
| billId | VARCHAR(256) | NO | Bill identifier (Unique constraint) |
| manualreceiptnumber | VARCHAR(256) | YES | Manual receipt number |
| additionalDetails | JSONB | YES | Additional metadata in JSONB format |
| createdBy | VARCHAR(256) | NO | User who created the record |
| createdTime | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastModifiedBy | VARCHAR(256) | NO | User who last modified the record |
| lastModifiedTime | BIGINT | NO | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id)
- Unique: (billId) - ensures one payment per bill
- Foreign Key: (paymentid) REFERENCES egcl_payment(id) ON UPDATE CASCADE ON DELETE CASCADE

**Indexes**:
- idx_egcl_paymentDetail_receiptNumber
- idx_egcl_paymentDetail_billId

---

### 7. egcl_bill (Bill Information)

**Purpose**: Stores bill information for various services

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(256) | NO | Unique identifier for the bill (Primary Key) |
| tenantid | VARCHAR(256) | NO | Tenant/ULB identifier |
| businessService | VARCHAR(256) | NO | Business service code (PT, WS, TL, etc) |
| consumerCode | VARCHAR(256) | NO | Consumer/Service identifier |
| billNumber | VARCHAR(256) | NO | Human-readable bill number |
| billDate | BIGINT | NO | Bill generation date (epoch milliseconds) |
| totalAmount | NUMERIC(12,2) | NO | Total bill amount |
| status | VARCHAR(256) | NO | Bill status (ACTIVE, CANCELLED, PAID, EXPIRED) |
| isCancelled | BOOLEAN | YES | Flag indicating if bill is cancelled |
| reasonForCancellation | VARCHAR(2048) | YES | Reason for cancellation |
| collectionModesNotAllowed | VARCHAR(256) | YES | Collection modes not allowed |
| partPaymentAllowed | BOOLEAN | YES | Flag indicating if partial payment is allowed |
| isAdvanceAllowed | BOOLEAN | YES | Flag indicating if advance payment is allowed |
| minimumAmountToBePaid | NUMERIC(12,2) | YES | Minimum amount that can be paid |
| additionalDetails | JSONB | YES | Additional metadata in JSONB format |
| createdBy | VARCHAR(256) | NO | User who created the record |
| createdTime | BIGINT | NO | Creation timestamp (epoch milliseconds) |
| lastModifiedBy | VARCHAR(256) | NO | User who last modified the record |
| lastModifiedTime | BIGINT | NO | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id)
- Foreign Key: (id) REFERENCES egcl_paymentdetail(billid)

**Indexes**:
- idx_egcl_bill_consumerCode

---

### 8. egcl_billdetial (Bill Details)

**Purpose**: Stores detailed billing information including period and amounts (Note: typo in table name - "detial" instead of "detail")

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(256) | NO | Unique identifier for the bill detail (Primary Key) |
| tenantid | VARCHAR(256) | NO | Tenant/ULB identifier |
| billId | VARCHAR(256) | NO | Reference to parent bill (Foreign Key) |
| demandId | VARCHAR(256) | NO | Reference to demand (no FK constraint) |
| amount | NUMERIC(12,2) | NO | Bill detail amount |
| amountPaid | NUMERIC(12,2) | NO | Amount paid |
| fromPeriod | BIGINT | NO | Billing period from date (epoch milliseconds) |
| toPeriod | BIGINT | NO | Billing period to date (epoch milliseconds) |
| expiryDate | VARCHAR(256) | NO | Bill expiry date |
| billDescription | VARCHAR(256) | YES | Description of the bill |
| displayMessage | VARCHAR(2048) | YES | Message to display to user |
| collectionType | VARCHAR(256) | YES | Type of collection |
| channel | VARCHAR(256) | YES | Channel (SYSTEM, CSC, MOBILE, etc) |
| voucherHeader | VARCHAR(256) | YES | Voucher reference |
| boundary | VARCHAR(256) | YES | Boundary/ward code |
| manualReceiptDate | BIGINT | YES | Manual receipt date (epoch milliseconds) |
| callBackForApportioning | VARCHAR(256) | YES | Callback URL for apportioning |
| cancellationRemarks | VARCHAR(2048) | YES | Remarks for cancellation |
| additionalDetails | JSONB | YES | Additional metadata in JSONB format |

**Constraints**:
- Primary Key: (id)
- Foreign Key: (billId) REFERENCES egcl_bill(id)

---

### 9. egcl_billAccountDetail (Bill Account Details)

**Purpose**: Stores accounting/ledger details for bills including tax head-wise breakup

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(256) | NO | Unique identifier for the account detail (Primary Key) |
| tenantid | VARCHAR(256) | NO | Tenant/ULB identifier |
| billDetailid | VARCHAR(256) | NO | Reference to parent bill detail (Foreign Key) |
| demandDetailId | VARCHAR(256) | NO | Reference to demand detail (no FK constraint) |
| taxHeadCode | VARCHAR(256) | NO | Tax head code (PT_TAX, PT_PENALTY, etc) |
| "order" | INTEGER | NO | Display order (quoted because ORDER is SQL keyword) |
| amount | NUMERIC(12,2) | NO | Account detail amount |
| adjustedamount | NUMERIC(12,2) | YES | Adjusted amount after apportioning |
| isActualDemand | BOOLEAN | YES | Flag indicating if this is actual demand |
| additionalDetails | JSONB | YES | Additional metadata in JSONB format |

**Constraints**:
- Primary Key: (id)
- Foreign Key: (billDetailid) REFERENCES egcl_billdetial(id) ON UPDATE CASCADE ON DELETE CASCADE

---

### REMITTANCE TABLES

### 10. egcl_remittance (Remittance Header)

**Purpose**: Stores bank remittance information for depositing collections

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(250) | NO | Unique identifier for the remittance (Primary Key) |
| tenantid | VARCHAR(252) | NO | Tenant/ULB identifier |
| referencenumber | VARCHAR(50) | NO | Challan/remittance reference number |
| referencedate | BIGINT | NO | Reference date (epoch milliseconds) |
| voucherheader | VARCHAR(250) | YES | Voucher reference |
| bankaccount | VARCHAR(250) | YES | Bank account identifier |
| fund | VARCHAR(250) | YES | Fund code |
| function | VARCHAR(250) | YES | Function code |
| status | VARCHAR(250) | NO | Remittance status (APPROVED, SUBMITTED, etc) |
| remarks | VARCHAR(250) | YES | Remarks |
| reasonfordelay | VARCHAR(250) | YES | Reason for delay |
| createdby | BIGINT | NO | User who created the record |
| createddate | BIGINT | YES | Creation timestamp (epoch milliseconds) |
| lastmodifiedby | BIGINT | NO | User who last modified the record |
| lastmodifieddate | BIGINT | YES | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id)

---

### 11. egcl_remittancedetails (Remittance Line Items)

**Purpose**: Stores line-item details for remittances including account codes and amounts

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(250) | NO | Unique identifier for the remittance detail (Primary Key) |
| tenantid | VARCHAR(252) | NO | Tenant/ULB identifier |
| remittance | VARCHAR(250) | NO | Reference to parent remittance (no FK constraint) |
| chartofaccount | VARCHAR(250) | NO | Chart of account/GL code |
| creditamount | DOUBLE PRECISION | YES | Credit amount |
| debitamount | DOUBLE PRECISION | YES | Debit amount |

**Constraints**:
- Primary Key: (id)

**Note**: No foreign key constraint enforced

---

### 12. egcl_remittanceinstrument (Remittance Instrument Mapping)

**Purpose**: Maps remittances to payment instruments

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(250) | NO | Unique identifier (Primary Key) |
| tenantid | VARCHAR(252) | NO | Tenant/ULB identifier |
| remittance | VARCHAR(250) | NO | Reference to remittance (no FK constraint) |
| instrument | VARCHAR(250) | NO | Reference to instrument (no FK constraint) |
| reconciled | BOOLEAN | YES | Flag indicating if reconciled (default: false) |

**Constraints**:
- Primary Key: (id)

**Note**: No foreign key constraints enforced

---

### 13. egcl_remittancereceipt (Remittance Receipt Mapping)

**Purpose**: Maps remittances to receipts

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | VARCHAR(250) | NO | Unique identifier (Primary Key) |
| tenantid | VARCHAR(252) | NO | Tenant/ULB identifier |
| remittance | VARCHAR(250) | NO | Reference to remittance (no FK constraint) |
| receipt | VARCHAR(250) | NO | Reference to receipt (no FK constraint) |

**Constraints**:
- Primary Key: (id)

**Note**: No foreign key constraints enforced

---

### CONFIGURATION TABLE

### 14. egcl_bankaccountservicemapping (Bank Account Service Mapping)

**Purpose**: Maps business services to bank accounts for deposit management

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| id | BIGINT | NO | Unique identifier (Primary Key) |
| tenantid | VARCHAR(252) | NO | Tenant/ULB identifier |
| businessdetails | VARCHAR(12) | NO | Business service code |
| bankaccount | VARCHAR(12) | NO | Bank account identifier |
| bank | VARCHAR(12) | YES | Bank code |
| bankbranch | VARCHAR(12) | YES | Bank branch code |
| active | BOOLEAN | YES | Active flag |
| version | BIGINT | NO | Optimistic locking version (default: 1) |
| createdby | BIGINT | NO | User who created the record |
| createddate | BIGINT | YES | Creation timestamp (epoch milliseconds) |
| lastmodifiedby | BIGINT | NO | User who last modified the record |
| lastmodifieddate | BIGINT | YES | Last modification timestamp (epoch milliseconds) |

**Constraints**:
- Primary Key: (id)

---

## History/Audit Tables

### egcl_receiptheader_v1_history

**Purpose**: Audit trail for all changes to receipt headers

**Columns**: Same as egcl_receiptheader_v1 plus:
- uuid (VARCHAR(256)) - Unique audit record identifier

**Note**: No primary key, used for audit trail only

---

### egcl_receiptdetails_v1_history

**Purpose**: Audit trail for all changes to receipt details

**Columns**: Same as egcl_receiptdetails_v1 plus:
- uuid (VARCHAR(256)) - Unique audit record identifier

**Note**: No primary key, used for audit trail only

---

### egcl_instrumentheader_v1_history

**Purpose**: Audit trail for all changes to instruments

**Columns**: Same as egcl_instrumentheader_v1 plus:
- uuid (VARCHAR(256)) - Unique audit record identifier

**Note**: No primary key, used for audit trail only

---

### egcl_payment_audit

**Purpose**: Audit trail for all changes to payments

**Columns**: Same as egcl_payment

**Note**: No primary key, used for audit trail only

---

### egcl_paymentDetail_audit

**Purpose**: Audit trail for all changes to payment details

**Columns**: Same as egcl_paymentDetail plus:
- manualreceiptnumber (VARCHAR(256))

**Note**: No primary key, used for audit trail only

---

### egcl_bill_audit

**Purpose**: Audit trail for all changes to bills

**Columns**: Same as egcl_bill

**Note**: No primary key, used for audit trail only

---

### egcl_billdetial_audit

**Purpose**: Audit trail for all changes to bill details

**Columns**: Same as egcl_billdetial plus:
- receiptDate (BIGINT)
- receiptType (VARCHAR(256))

**Note**: No primary key, used for audit trail only

---

## Legacy Tables (Deprecated)

The following tables are legacy versions and should not be used for new development:

1. **egcl_receiptheader** - Replaced by egcl_receiptheader_v1
2. **egcl_receiptdetails** - Replaced by egcl_receiptdetails_v1
3. **egcl_instrumentheader** - Replaced by egcl_instrumentheader_v1
4. **egcl_receiptinstrument** - Replaced by egcl_receiptinstrument_v1

---

## Sequences

The following sequence is used for generating IDs:

- seq_egcl_bankaccountservicemapping

---

## Key Relationships

1. **egcl_receiptheader_v1** ➔ **egcl_receiptdetails_v1** (1:N)
   - One receipt can have multiple line items

2. **egcl_receiptheader_v1** ↔ **egcl_instrumentheader_v1** (M:N via egcl_receiptinstrument_v1)
   - A receipt can have multiple instruments (split payment)
   - An instrument can be used for multiple receipts

3. **egcl_payment** ➔ **egcl_paymentDetail** (1:N)
   - One payment can have multiple payment details (multiple bills)

4. **egcl_paymentDetail** ➔ **egcl_bill** (1:1)
   - One payment detail maps to one bill (unique constraint on billId)

5. **egcl_bill** ➔ **egcl_billdetial** (1:N)
   - One bill can have multiple bill details (different periods/charges)

6. **egcl_billdetial** ➔ **egcl_billAccountDetail** (1:N)
   - One bill detail can have multiple account details (tax head breakdown)

7. **egcl_remittance** ➔ **egcl_remittancedetails** (1:N) - Logical only
   - One remittance can have multiple line items (no FK enforced)

8. **egcl_remittance** ↔ **egcl_instrumentheader** (M:N via egcl_remittanceinstrument) - Logical only
   - A remittance can have multiple instruments (no FK enforced)

9. **egcl_remittance** ↔ **egcl_receiptheader** (M:N via egcl_remittancereceipt) - Logical only
   - A remittance can have multiple receipts (no FK enforced)
