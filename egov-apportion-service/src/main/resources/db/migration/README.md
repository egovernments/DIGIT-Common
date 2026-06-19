# Apportion Service Database Schema Documentation

## Overview
This document provides a comprehensive overview of the database schema for the eGov Apportion Service. The apportion service is responsible for distributing (apportioning) payments across different tax heads based on business rules, priorities, and payment purposes (ARREAR, CURRENT, ADVANCE).

## Database Schema Version
The database follows a versioned migration approach using Flyway. The schema includes request and response tables for both bill-based and demand-based apportioning.

---

## ASCII Entity Relationship Diagram

**Legend:**
- `····>` Dotted line = Logical Reference (not enforced by FK)
- These tables have no foreign key constraints and operate independently

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                    APPORTION SERVICE DATABASE SCHEMA                                 │
└─────────────────────────────────────────────────────────────────────────────────────┘


BILL-BASED APPORTIONING (Request → Response):
═══════════════════════════════════════════════════════════════════════════════════════

┌────────────────────────────────────────────┐
│      eg_appr_bills_request                 │
│    (Input for Apportioning)                │
├────────────────────────────────────────────┤
│ No Primary Key                             │
├────────────────────────────────────────────┤
│tenantId                                    │
│billId (external bill reference) ··········> (external bill system)
│                                            │
│PAYER INFORMATION:                          │
│  payerName                                 │
│  payerAddress                              │
│  payerEmail                                │
│  paidBy                                    │
│  mobileNumber                              │
│                                            │
│BILL STATUS:                                │
│  isActive (boolean)                        │
│  isCancelled (boolean)                     │
│                                            │
│APPORTIONING DATA (JSONB):                  │
│  billDetails (bill structure)              │
│  taxAndPayments (payment mapping)          │
│                                            │
│AUDIT:                                      │
│  createdBy                                 │
│  createdTime (epoch ms)                    │
└────────────────────────────────────────────┘
            │
            │ Logical Flow
            │ (Apportioning Process)
            │
            ▼
┌────────────────────────────────────────────┐
│      eg_appr_bills_response                │
│   (Output after Apportioning)              │
├────────────────────────────────────────────┤
│ No Primary Key                             │
├────────────────────────────────────────────┤
│tenantId                                    │
│billId (external bill reference) ··········> (external bill system)
│                                            │
│PAYER INFORMATION:                          │
│  payerName                                 │
│  payerAddress                              │
│  payerEmail                                │
│  paidBy                                    │
│  mobileNumber                              │
│                                            │
│BILL STATUS:                                │
│  isActive (boolean)                        │
│  isCancelled (boolean)                     │
│                                            │
│APPORTIONED DATA (JSONB):                   │
│  billDetails (updated bill structure)      │
│  taxAndPayments (apportioned amounts)      │
│                                            │
│AUDIT:                                      │
│  createdBy                                 │
│  createdTime (epoch ms)                    │
└────────────────────────────────────────────┘


DEMAND-BASED APPORTIONING (Request → Response):
═══════════════════════════════════════════════════════════════════════════════════════

┌────────────────────────────────────────────┐
│     eg_appr_demand_request                 │
│    (Input for Apportioning)                │
├────────────────────────────────────────────┤
│ No Primary Key                             │
├────────────────────────────────────────────┤
│tenantId                                    │
│demandId (external demand reference) ······> (external demand system)
│consumerCode                                │
│                                            │
│TAX PERIOD:                                 │
│  taxperiodfrom (epoch ms)                  │
│  taxperiodto (epoch ms)                    │
│                                            │
│DEMAND STATUS:                              │
│  status (ACTIVE, CANCELLED, etc)           │
│                                            │
│APPORTIONING DATA (JSONB):                  │
│  demandDetails (demand structure with      │
│   tax heads and amounts)                   │
│                                            │
│AUDIT:                                      │
│  createdBy                                 │
│  createdTime (epoch ms)                    │
└────────────────────────────────────────────┘
            │
            │ Logical Flow
            │ (Apportioning Process)
            │
            ▼
┌────────────────────────────────────────────┐
│     eg_appr_demand_response                │
│   (Output after Apportioning)              │
├────────────────────────────────────────────┤
│ No Primary Key                             │
├────────────────────────────────────────────┤
│tenantId                                    │
│demandId (external demand reference) ······> (external demand system)
│consumerCode                                │
│                                            │
│TAX PERIOD:                                 │
│  taxperiodfrom (epoch ms)                  │
│  taxperiodto (epoch ms)                    │
│                                            │
│DEMAND STATUS:                              │
│  status (ACTIVE, CANCELLED, etc)           │
│                                            │
│APPORTIONED DATA (JSONB):                   │
│  demandDetails (updated demand structure   │
│   with apportioned amounts)                │
│                                            │
│AUDIT:                                      │
│  createdBy                                 │
│  createdTime (epoch ms)                    │
└────────────────────────────────────────────┘


APPORTIONING FLOW:
═══════════════════════════════════════════════════════════════════════════════════════

    BILL-BASED APPORTIONING                 DEMAND-BASED APPORTIONING
    ═══════════════════════                 ═════════════════════════

    1. Bill + Payment Amount                1. Demand + Payment Amount
              ↓                                       ↓
    2. Store in                             2. Store in
       eg_appr_bills_request                   eg_appr_demand_request
              ↓                                       ↓
    3. Apply Apportioning Rules             3. Apply Apportioning Rules
       - Priority Order                        - Priority Order
       - Purpose (ARREAR,CURRENT,              - Purpose (ARREAR,CURRENT,
         ADVANCE)                                ADVANCE)
       - Partial Payment Logic                 - Partial Payment Logic
              ↓                                       ↓
    4. Store Result in                      4. Store Result in
       eg_appr_bills_response                  eg_appr_demand_response
              ↓                                       ↓
    5. Return Apportioned Bill              5. Return Apportioned Demand

```

**Key Points:**
1. **No Foreign Keys**: These tables are standalone and do not have foreign key constraints
2. **Request/Response Pattern**: Each entity type (bills, demands) has separate request and response tables
3. **JSONB Storage**: Complex structures are stored as JSONB for flexibility
4. **Logical References**: billId, demandId, and consumerCode reference external systems
5. **No Primary Keys**: Tables are designed for temporary storage and processing

---

## Table Details

### 1. eg_appr_bills_request (Bill Apportioning - Input)

**Purpose**: Stores input bills and payment information before apportioning. This table captures the original bill structure and the payment amount that needs to be distributed across tax heads.

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| tenantid | VARCHAR(64) | YES | Tenant/ULB identifier for multi-tenancy |
| billId | VARCHAR(64) | YES | Unique identifier of the bill to be apportioned (references external billing system) |
| payerName | VARCHAR(256) | YES | Name of the person making the payment |
| payerAddress | VARCHAR(1024) | YES | Address of the payer |
| payerEmail | VARCHAR(254) | YES | Email address of the payer |
| paidBy | VARCHAR(1024) | YES | Person who made the payment (may differ from payer) |
| mobileNumber | VARCHAR(64) | YES | Mobile number of the payer |
| isActive | BOOLEAN | YES | Flag indicating if the bill is active |
| isCancelled | BOOLEAN | YES | Flag indicating if the bill has been cancelled |
| billDetails | JSONB | YES | Complete bill structure in JSON format including bill details, bill account details, amounts, and tax head codes |
| taxAndPayments | JSONB | YES | Payment mapping showing which tax heads receive how much payment (originally named collectionMap, renamed in V20190311135135) |
| createdBy | VARCHAR(64) | YES | User who created the record |
| createdTime | BIGINT | YES | Creation timestamp (epoch milliseconds) |

**Constraints**:
- No Primary Key
- No Foreign Keys

---

### 2. eg_appr_bills_response (Bill Apportioning - Output)

**Purpose**: Stores the result after payment has been apportioned across tax heads. This table contains the updated bill with adjusted amounts showing how the payment was distributed.

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| tenantid | VARCHAR(64) | YES | Tenant/ULB identifier for multi-tenancy |
| billId | VARCHAR(64) | YES | Unique identifier of the apportioned bill (references external billing system) |
| payerName | VARCHAR(256) | YES | Name of the person making the payment |
| payerAddress | VARCHAR(1024) | YES | Address of the payer |
| payerEmail | VARCHAR(254) | YES | Email address of the payer |
| paidBy | VARCHAR(1024) | YES | Person who made the payment (may differ from payer) |
| mobileNumber | VARCHAR(64) | YES | Mobile number of the payer |
| isActive | BOOLEAN | YES | Flag indicating if the bill is active |
| isCancelled | BOOLEAN | YES | Flag indicating if the bill has been cancelled |
| billDetails | JSONB | YES | Updated bill structure in JSON format with apportioned amounts showing how payment was distributed across tax heads |
| taxAndPayments | JSONB | YES | Final payment distribution showing actual amounts applied to each tax head after apportioning logic |
| createdBy | VARCHAR(64) | YES | User who created the record |
| createdTime | BIGINT | YES | Creation timestamp (epoch milliseconds) |

**Constraints**:
- No Primary Key
- No Foreign Keys

---

### 3. eg_appr_demand_request (Demand Apportioning - Input)

**Purpose**: Stores input demands and payment information before apportioning. This table captures the original demand structure for scenarios where apportioning is done at the demand level rather than bill level.

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| tenantid | VARCHAR(128) | YES | Tenant/ULB identifier for multi-tenancy |
| demandId | VARCHAR(128) | YES | Unique identifier of the demand to be apportioned (references external demand system) |
| consumerCode | VARCHAR(128) | YES | Consumer/Service identifier (e.g., property ID, connection number) |
| taxperiodfrom | BIGINT | YES | Tax period start date (epoch milliseconds) |
| taxperiodto | BIGINT | YES | Tax period end date (epoch milliseconds) |
| status | VARCHAR(128) | YES | Demand status (ACTIVE, CANCELLED, PAID, etc) |
| demandDetails | JSONB | YES | Complete demand structure in JSON format including demand details with tax head codes and amounts |
| createdBy | VARCHAR(64) | YES | User who created the record |
| createdTime | BIGINT | YES | Creation timestamp (epoch milliseconds) |

**Constraints**:
- No Primary Key
- No Foreign Keys

---

### 4. eg_appr_demand_response (Demand Apportioning - Output)

**Purpose**: Stores the result after payment has been apportioned across demand details. This table contains the updated demand with collection amounts showing how the payment was distributed.

**Columns**:

| Column Name | Data Type | Nullable | Description |
|-------------|-----------|----------|-------------|
| tenantid | VARCHAR(128) | YES | Tenant/ULB identifier for multi-tenancy |
| demandId | VARCHAR(128) | YES | Unique identifier of the apportioned demand (references external demand system) |
| consumerCode | VARCHAR(128) | YES | Consumer/Service identifier (e.g., property ID, connection number) |
| taxperiodfrom | BIGINT | YES | Tax period start date (epoch milliseconds) |
| taxperiodto | BIGINT | YES | Tax period end date (epoch milliseconds) |
| status | VARCHAR(128) | YES | Demand status (ACTIVE, CANCELLED, PAID, etc) |
| demandDetails | JSONB | YES | Updated demand structure in JSON format with collection amounts showing how payment was apportioned across tax heads |
| createdBy | VARCHAR(64) | YES | User who created the record |
| createdTime | BIGINT | YES | Creation timestamp (epoch milliseconds) |

**Constraints**:
- No Primary Key
- No Foreign Keys

---

## Apportioning Logic

### What is Apportioning?

Apportioning is the process of distributing a payment amount across multiple tax heads (like tax, penalty, interest, rebate, etc.) in a specific order based on business rules. When a citizen makes a partial payment, the system needs to decide which tax heads get paid first.

### Business Rules

1. **Priority Order**: Tax heads have a priority order (defined in egbs_taxheadmaster.orderno)
   - Example: Tax → Penalty → Interest → Rebate

2. **Purpose-based Apportioning**:
   - **ARREAR**: Past due amounts are paid first
   - **CURRENT**: Current period amounts are paid next
   - **ADVANCE**: Advance payments are applied last

3. **Tax Head Types**:
   - **Debit (isdebit=true)**: Amounts owed by citizen (positive)
   - **Credit (isdebit=false)**: Amounts owed to citizen like rebates (negative)

4. **Partial Payment Logic**:
   - If payment < total due, apportion based on priority
   - Higher priority tax heads get paid first
   - Remaining amount goes to next priority

### Apportioning Flow

```
Step 1: Receive Payment Request
  ├─ Bill/Demand with tax heads and amounts
  ├─ Payment amount to be apportioned
  └─ Tax head master data (priority, order)

Step 2: Store in Request Table
  ├─ eg_appr_bills_request (for bills)
  └─ eg_appr_demand_request (for demands)

Step 3: Apply Apportioning Rules
  ├─ Sort tax heads by: Purpose (ARREAR→CURRENT→ADVANCE) + Priority Order
  ├─ Distribute payment amount starting from highest priority
  ├─ Handle rebates and adjustments
  └─ Calculate adjustedAmount for each tax head

Step 4: Store in Response Table
  ├─ eg_appr_bills_response (apportioned bill)
  └─ eg_appr_demand_response (apportioned demand)

Step 5: Return Result
  └─ Updated bill/demand with adjusted amounts
```

### Example Scenario

**Input**:
- Total Due: ₹6,000
- Payment Made: ₹3,500

**Tax Heads** (in priority order):
1. PT_TAX (CURRENT): ₹5,000 (order: 1)
2. PT_PENALTY (CURRENT): ₹1,000 (order: 2)

**Apportioning Result**:
1. PT_TAX gets: ₹3,500 (full payment goes to highest priority)
   - Remaining due: ₹1,500
2. PT_PENALTY gets: ₹0 (no payment left)
   - Remaining due: ₹1,000

**Output**:
- PT_TAX: amount=5000, adjustedAmount=3500
- PT_PENALTY: amount=1000, adjustedAmount=0

---

## Data Flow

### Bill-Based Apportioning Flow

1. **Payment Request Received**:
   - User makes a payment against a bill
   - System receives bill details + payment amount

2. **Store Request**:
   - Bill data stored in `eg_appr_bills_request`
   - Includes original amounts and tax head structure

3. **Apportion Payment**:
   - Service reads bill structure
   - Applies apportioning logic based on:
     - Tax head priority
     - Purpose (ARREAR, CURRENT, ADVANCE)
     - Payment amount
   - Calculates adjustedAmount for each tax head

4. **Store Response**:
   - Updated bill stored in `eg_appr_bills_response`
   - Contains adjusted amounts showing payment distribution

5. **Return Result**:
   - Apportioned bill returned to calling service
   - Used to update billing and collection records

### Demand-Based Apportioning Flow

1. **Payment Request Received**:
   - User makes a payment against a demand
   - System receives demand details + payment amount

2. **Store Request**:
   - Demand data stored in `eg_appr_demand_request`
   - Includes original demand amounts

3. **Apportion Payment**:
   - Service reads demand structure
   - Applies same apportioning logic
   - Calculates collectionAmount for each demand detail

4. **Store Response**:
   - Updated demand stored in `eg_appr_demand_response`
   - Contains collection amounts showing payment distribution

5. **Return Result**:
   - Apportioned demand returned to calling service
   - Used to update demand records

---

## Business Rules

1. **Priority-Based Distribution**: Payments are distributed based on tax head priority order
2. **Purpose Hierarchy**: ARREAR → CURRENT → ADVANCE
3. **Partial Payments**: When payment < total due, higher priority items get paid first
4. **Rebate Handling**: Rebates (negative amounts) are applied after positive amounts
5. **No Persistence**: These tables are temporary storage for apportioning calculations
6. **Multi-tenancy**: All operations are tenant-specific
7. **Audit Trail**: All records include createdBy and createdTime for tracking

---

## Common Use Cases

### Use Case 1: Full Payment

**Scenario**: Citizen pays full amount due
- Total Due: ₹10,000
- Payment: ₹10,000

**Result**: All tax heads get fully paid
- PT_TAX: ₹5,000 → adjusted ₹5,000
- PT_PENALTY: ₹3,000 → adjusted ₹3,000
- PT_INTEREST: ₹2,000 → adjusted ₹2,000

### Use Case 2: Partial Payment

**Scenario**: Citizen pays partial amount
- Total Due: ₹10,000
- Payment: ₹6,000

**Result**: Priority-based distribution
- PT_TAX (priority 1): ₹5,000 → adjusted ₹5,000 ✓ FULLY PAID
- PT_PENALTY (priority 2): ₹3,000 → adjusted ₹1,000 ⚠ PARTIALLY PAID
- PT_INTEREST (priority 3): ₹2,000 → adjusted ₹0 ✗ NOT PAID

### Use Case 3: Arrear + Current

**Scenario**: Citizen has arrears and current period dues
- Arrear PT_TAX: ₹3,000 (priority: ARREAR + order 1)
- Current PT_TAX: ₹5,000 (priority: CURRENT + order 1)
- Payment: ₹6,000

**Result**: Arrears paid first
- Arrear PT_TAX: ₹3,000 → adjusted ₹3,000 ✓ FULLY PAID
- Current PT_TAX: ₹5,000 → adjusted ₹3,000 ⚠ PARTIALLY PAID

### Use Case 4: With Rebate

**Scenario**: Citizen has tax and rebate
- PT_TAX: ₹5,000
- PT_REBATE: -₹500 (early payment rebate)
- Net Due: ₹4,500
- Payment: ₹4,500

**Result**: Rebate applied
- PT_TAX: ₹5,000 → adjusted ₹4,500
- PT_REBATE: -₹500 → adjusted -₹500
- Net Payment: ₹4,000

---

## Integration Points

### 1. Billing Service Integration
```
Billing Service ──> Apportion Service
                    (eg_appr_bills_request)
                           │
                           │ Apportion Logic
                           ▼
                    (eg_appr_bills_response)
Billing Service <── Apportion Service
```

**Purpose**: When a bill is paid, billing service calls apportion service to calculate payment distribution

### 2. Collection Service Integration
```
Collection Service ──> Apportion Service
                       (eg_appr_bills_request/
                        eg_appr_demand_request)
                              │
                              │ Apportion Logic
                              ▼
                       (eg_appr_bills_response/
                        eg_appr_demand_response)
Collection Service <── Apportion Service
```

**Purpose**: During payment collection, determine how to distribute payment across tax heads

### 3. Demand Service Integration
```
Demand Service ──> Apportion Service
                   (eg_appr_demand_request)
                          │
                          │ Apportion Logic
                          ▼
                   (eg_appr_demand_response)
Demand Service <── Apportion Service
```

**Purpose**: When demand is updated with payment, calculate collection amounts per tax head

---