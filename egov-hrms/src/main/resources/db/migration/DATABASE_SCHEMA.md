# HRMS Database Schema Documentation

## Overview
This document provides comprehensive documentation for the HRMS (Human Resource Management System) database schema, including all tables, columns, relationships, and metadata.

---

## Entity Relationship Diagram (ASCII)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         eg_hrms_employee                            │
│                         (Master Table)                              │
├─────────────────────────────────────────────────────────────────────┤
│ PK  uuid                    VARCHAR(1024)                           │
│     id                      BIGINT                                  │
│ UK  code                    VARCHAR(250)  (with tenantid)           │
│     dateOfAppointment       BIGINT                                  │
│     employeestatus          VARCHAR(250)                            │
│     employeetype            VARCHAR(250)                            │
│     active                  BOOLEAN                                 │
│     reactivateemployee      BOOLEAN                                 │
│     tenantid                VARCHAR(250)  NOT NULL                  │
│     + audit fields (createdby, createddate, etc.)                   │
└──────────────────┬──────────────────────────────────────────────────┘
                   │
                   │ 1:N (ON DELETE CASCADE)
                   │
        ┌──────────┴──────────┬─────────────┬─────────────┬────────────┐
        │                     │             │             │            │
        ▼                     ▼             ▼             ▼            │
┌──────────────────┐  ┌──────────────┐ ┌─────────────┐ ┌─────────────┐ │
│ eg_hrms_         │  │ eg_hrms_     │ │ eg_hrms_    │ │ eg_hrms_    │ │
│ assignment       │  │ educational  │ │ departmental│ │ empdocuments│ │
├──────────────────┤  │ details      │ │ tests       │ ├─────────────┤ │
│ PK uuid          │  ├──────────────┤ ├─────────────┤ │ PK uuid     │ │
│ FK employeeid    │  │ PK uuid      │ │ PK uuid     │ │ FK employee │ │
│    position      │  │ FK employeeid│ │ FK employee │ │    id       │ │
│    department    │  │ qualification│ │    id       │ │ documentid  │ │
│    designation   │  │ stream       │ │ test        │ │ documentname│ │
│    fromdate      │  │ yearofpassing│ │ yearofpass  │ │ referencety │ │
│    todate        │  │ university   │ │ remarks     │ │ referenceid │ │
│    govtorderno   │  │ remarks      │ │ isActive    │ │ + audit     │ │
│    reportingto   │  │ isActive     │ │ + audit     │ └─────────────┘ │
│    isHOD         │  │ + audit      │ │ + tenant    │                 │
│    iscurrentasgn │  │ + tenant     │ │ fields      │                 │
│    isActive      │  │ fields       │ └─────────────┘                 │
│    + audit       │  └──────────────┘                                 │
│    + tenant      │                                                   │
│ CHK: fromdate<=  │                                                   │
│      todate      │                                                   │
└──────────────────┘                                                   │
                                                                       │
        ┌───────────────────────────┬─────────────────┬────────────────┘
        │                           │                 │
        ▼                           ▼                 ▼
┌──────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│ eg_hrms_         │  │ eg_hrms_             │  │ eg_hrms_             │
│ servicehistory   │  │ jurisdiction         │  │ deactivationdetails  │
├──────────────────┤  ├──────────────────────┤  ├──────────────────────┤
│ PK uuid          │  │ PK uuid              │  │ PK uuid              │
│ FK employeeid    │  │ FK employeeid        │  │ FK employeeid        │
│ servicestatus    │  │ hierarchy NOT NULL   │  │ reasonfordeactivation│
│ servicefrom      │  │ boundarytype NOT NULL│  │ effectivefrom        │
│ serviceto        │  │ boundary NOT NULL    │  │ ordernumber          │
│ ordernumber      │  │ isActive             │  │ remarks              │
│ isCurrentPosition│  │ + audit              │  │ isActive             │
│ location         │  │ + tenant fields      │  │ + audit              │
│ isActive         │  └──────────────────────┘  │ + tenant fields      │
│ + audit          │                            └──────────────────────┘
│ + tenant fields  │
└──────────────────┘                    ┌──────────────────────────────┐
                                        │ eg_hrms_                     │
                                        │ reactivationdetails          │
                                        ├──────────────────────────────┤
                                        │ PK uuid                      │
                                        │ FK employeeid                │
                                        │ reasonforreactivation        │
                                        │ effectivefrom                │
                                        │ ordernumber                  │
                                        │ remarks                      │
                                        │ + audit                      │
                                        │ + tenant fields              │
                                        └──────────────────────────────┘

SEQUENCES:
  - EG_HRMS_POSITION (used for generating position numbers)

INDEXES:
  - idx_eg_hrms_employee_tenantid (btree on tenantid)
  - code_idx (on employee code)
  - dept_idx (on assignment department)
  - posn_idx (on assignment position)
  - desg_idx (on assignment designation)
  - reactivation_employeeid_idx (on reactivationdetails employeeid)
```

---

## Detailed Table Specifications

### 1. eg_hrms_employee
**Purpose**: Master table storing core employee information

| Column Name | Data Type | Constraints | Description |
|------------|-----------|-------------|-------------|
| **id** | BIGINT | NOT NULL | Internal numeric identifier |
| **uuid** | VARCHAR(1024) | PRIMARY KEY, NOT NULL | Unique identifier for employee record |
| **code** | VARCHAR(250) | UNIQUE (with tenantid) | Employee code/number |
| **dateOfAppointment** | BIGINT | - | Appointment date (epoch timestamp) |
| **employeestatus** | VARCHAR(250) | - | Current employment status (e.g., ACTIVE, RETIRED) |
| **employeetype** | VARCHAR(250) | - | Type of employment (e.g., PERMANENT, CONTRACT) |
| **active** | BOOLEAN | - | Whether employee record is active |
| **reactivateemployee** | BOOLEAN | - | Flag indicating if employee is being reactivated |
| **tenantid** | VARCHAR(250) | NOT NULL | Tenant/organization identifier |
| **createdby** | VARCHAR(250) | NOT NULL | User who created the record |
| **createddate** | BIGINT | NOT NULL | Creation timestamp (epoch) |
| **lastmodifiedby** | VARCHAR(250) | - | User who last modified the record |
| **lastModifiedDate** | BIGINT | - | Last modification timestamp (epoch) |

**Indexes**:
- `code_idx` on code
- `idx_eg_hrms_employee_tenantid` on tenantid

**Notes**:
- Originally had `phone` and `name` columns which were removed in migration V20190219163221
- Unique constraint on code includes tenantid for multi-tenant support

---

### 2. eg_hrms_assignment
**Purpose**: Stores employee job assignments, positions, and organizational placement

| Column Name | Data Type | Constraints | Description |
|------------|-----------|-------------|-------------|
| **uuid** | VARCHAR(1024) | PRIMARY KEY, NOT NULL | Unique identifier for assignment |
| **employeeid** | VARCHAR(1024) | FOREIGN KEY, NOT NULL | Reference to employee (CASCADE DELETE) |
| **position** | BIGINT | - | Position number (generated from EG_HRMS_POSITION seq) |
| **department** | VARCHAR(250) | - | Department code/identifier |
| **designation** | VARCHAR(250) | - | Designation code/identifier |
| **fromdate** | BIGINT | - | Assignment start date (epoch timestamp) |
| **todate** | BIGINT | - | Assignment end date (epoch timestamp) |
| **govtordernumber** | VARCHAR(250) | - | Government order/notification number |
| **reportingto** | VARCHAR(250) | - | UUID of reporting officer |
| **isHOD** | BOOLEAN | - | Whether employee is Head of Department |
| **iscurrentassignment** | BOOLEAN | - | Flag for current active assignment |
| **isActive** | BOOLEAN | - | Whether assignment record is active |
| **tenantid** | VARCHAR(250) | NOT NULL | Tenant identifier |
| **+ audit fields** | - | - | createdby, createddate, lastmodifiedby, lastModifiedDate |

**Constraints**:
- CHECK: `fromdate <= todate`
- ON DELETE CASCADE from employee table

**Indexes**:
- `dept_idx` on department
- `posn_idx` on position
- `desg_idx` on designation

---

### 3. eg_hrms_educationaldetails
**Purpose**: Stores educational qualifications of employees

| Column Name | Data Type | Constraints | Description |
|------------|-----------|-------------|-------------|
| **uuid** | VARCHAR(1024) | PRIMARY KEY, NOT NULL | Unique identifier |
| **employeeid** | VARCHAR(1024) | FOREIGN KEY, NOT NULL | Reference to employee (CASCADE DELETE) |
| **qualification** | VARCHAR(250) | - | Educational qualification (degree name) |
| **stream** | VARCHAR(250) | - | Stream/specialization |
| **yearofpassing** | BIGINT | - | Year of passing/completion |
| **university** | VARCHAR(250) | - | University/institution name |
| **remarks** | VARCHAR(250) | - | Additional notes |
| **isActive** | BOOLEAN | - | Whether record is active |
| **tenantid** | VARCHAR(250) | NOT NULL | Tenant identifier |
| **+ audit fields** | - | - | createdby, createddate, lastmodifiedby, lastModifiedDate |

---

### 4. eg_hrms_departmentaltests
**Purpose**: Records departmental/promotional examinations taken by employees

| Column Name | Data Type | Constraints | Description |
|------------|-----------|-------------|-------------|
| **uuid** | VARCHAR(1024) | PRIMARY KEY, NOT NULL | Unique identifier |
| **employeeid** | VARCHAR(1024) | FOREIGN KEY, NOT NULL | Reference to employee (CASCADE DELETE) |
| **test** | VARCHAR(250) | - | Name/code of the test |
| **yearofpassing** | BIGINT | - | Year of passing the test |
| **remarks** | VARCHAR(250) | - | Additional notes |
| **isActive** | BOOLEAN | - | Whether record is active |
| **tenantid** | VARCHAR(250) | NOT NULL | Tenant identifier |
| **+ audit fields** | - | - | createdby, createddate, lastmodifiedby, lastModifiedDate |

---

### 5. eg_hrms_empdocuments
**Purpose**: Stores references to employee-related documents

| Column Name | Data Type | Constraints | Description |
|------------|-----------|-------------|-------------|
| **uuid** | VARCHAR(1024) | PRIMARY KEY, NOT NULL | Unique identifier |
| **employeeid** | VARCHAR(1024) | FOREIGN KEY, NOT NULL | Reference to employee (CASCADE DELETE) |
| **documentid** | VARCHAR(250) | NOT NULL | Document identifier/file ID |
| **documentname** | VARCHAR(250) | - | Display name of document |
| **referencetype** | VARCHAR(250) | - | Type of entity this document references |
| **referenceid** | VARCHAR(250) | NOT NULL | ID of the referenced entity |
| **tenantid** | VARCHAR(250) | NOT NULL | Tenant identifier |
| **+ audit fields** | - | - | createdby, createddate, lastmodifiedby, lastModifiedDate |

**Notes**: Documents are typically stored in a separate file storage system; this table maintains metadata and references.

---

### 6. eg_hrms_servicehistory
**Purpose**: Maintains service history and position changes of employees

| Column Name | Data Type | Constraints | Description |
|------------|-----------|-------------|-------------|
| **uuid** | VARCHAR(1024) | PRIMARY KEY, NOT NULL | Unique identifier |
| **employeeid** | VARCHAR(1024) | FOREIGN KEY, NOT NULL | Reference to employee (CASCADE DELETE) |
| **servicestatus** | VARCHAR(250) | - | Service status during this period |
| **servicefrom** | BIGINT | - | Service period start date (epoch) |
| **serviceto** | BIGINT | - | Service period end date (epoch) |
| **ordernumber** | VARCHAR(250) | - | Order/notification number |
| **isCurrentPosition** | BOOLEAN | - | Flag for current position |
| **location** | VARCHAR(250) | - | Service location |
| **isActive** | BOOLEAN | - | Whether record is active |
| **tenantid** | VARCHAR(250) | NOT NULL | Tenant identifier |
| **+ audit fields** | - | - | createdby, createddate, lastmodifiedby, lastModifiedDate |

---

### 7. eg_hrms_jurisdiction
**Purpose**: Defines geographical/administrative jurisdiction boundaries for employees

| Column Name | Data Type | Constraints | Description |
|------------|-----------|-------------|-------------|
| **uuid** | VARCHAR(1024) | PRIMARY KEY, NOT NULL | Unique identifier |
| **employeeid** | VARCHAR(1024) | FOREIGN KEY, NOT NULL | Reference to employee (CASCADE DELETE) |
| **hierarchy** | VARCHAR(250) | NOT NULL | Hierarchy type (e.g., REVENUE, ADMIN) |
| **boundarytype** | VARCHAR(250) | NOT NULL | Type of boundary (e.g., ZONE, WARD, LOCALITY) |
| **boundary** | VARCHAR(250) | NOT NULL | Boundary identifier/code |
| **isActive** | BOOLEAN | - | Whether record is active |
| **tenantid** | VARCHAR(250) | NOT NULL | Tenant identifier |
| **+ audit fields** | - | - | createdby, createddate, lastmodifiedby, lastModifiedDate |

---

### 8. eg_hrms_deactivationdetails
**Purpose**: Records employee deactivation/separation details

| Column Name | Data Type | Constraints | Description |
|------------|-----------|-------------|-------------|
| **uuid** | VARCHAR(1024) | PRIMARY KEY, NOT NULL | Unique identifier |
| **employeeid** | VARCHAR(1024) | FOREIGN KEY, NOT NULL | Reference to employee (CASCADE DELETE) |
| **reasonfordeactivation** | VARCHAR(250) | - | Reason code for deactivation |
| **effectivefrom** | BIGINT | - | Effective date of deactivation (epoch) |
| **ordernumber** | VARCHAR(250) | - | Order/notification number |
| **remarks** | VARCHAR(250) | - | Additional notes/remarks |
| **isActive** | BOOLEAN | - | Whether record is active |
| **tenantid** | VARCHAR(250) | NOT NULL | Tenant identifier |
| **+ audit fields** | - | - | createdby, createddate, lastmodifiedby, lastModifiedDate |

**Notes**:
- Column `remarks` was originally named `typeOfDeactivation` and was renamed in migration V20190204163735

---

### 9. eg_hrms_reactivationdetails
**Purpose**: Records employee reactivation details after deactivation

| Column Name | Data Type | Constraints | Description |
|------------|-----------|-------------|-------------|
| **uuid** | VARCHAR(1024) | PRIMARY KEY, NOT NULL | Unique identifier |
| **employeeid** | VARCHAR(1024) | FOREIGN KEY, NOT NULL | Reference to employee (CASCADE DELETE) |
| **reasonforreactivation** | VARCHAR(250) | - | Reason code for reactivation |
| **effectivefrom** | BIGINT | - | Effective date of reactivation (epoch) |
| **ordernumber** | VARCHAR(250) | - | Order/notification number |
| **remarks** | VARCHAR(250) | - | Additional notes |
| **tenantid** | VARCHAR(250) | NOT NULL | Tenant identifier |
| **+ audit fields** | - | - | createdby, createddate, lastmodifiedby, lastModifiedDate |

**Indexes**:
- `reactivation_employeeid_idx` on employeeid

---

## Key Design Patterns

### 1. Multi-Tenancy
All tables include a `tenantid` column to support multi-tenant deployments where multiple organizations share the same database.

### 2. Audit Trail
All tables include standard audit fields:
- `createdby`: User who created the record
- `createddate`: Creation timestamp
- `lastmodifiedby`: User who last modified the record
- `lastModifiedDate`: Last modification timestamp

### 3. Soft Deletes
Many tables include an `isActive` boolean flag to support soft deletion of records rather than physical deletion.

### 4. Referential Integrity
All child tables use `ON DELETE CASCADE` to ensure data consistency when an employee record is deleted.

### 5. Timestamps
All date/time fields use BIGINT to store epoch timestamps (milliseconds since Jan 1, 1970).

---

## Relationships Summary

- **One-to-Many (1:N)**:
  - One employee can have multiple assignments
  - One employee can have multiple educational details
  - One employee can have multiple departmental test records
  - One employee can have multiple documents
  - One employee can have multiple service history records
  - One employee can have multiple jurisdiction assignments
  - One employee can have deactivation details
  - One employee can have reactivation details

- **Referential Actions**: All foreign key relationships use `ON DELETE CASCADE`

---