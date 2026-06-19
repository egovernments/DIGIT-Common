# User Event Service - Database Schema Documentation

## Overview
This document describes the database schema for the User Event Service, which manages user notifications, events, and tracking user access times.

## Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    eg_usrevents_events                      │
│─────────────────────────────────────────────────────────────│
│ PK: id (varchar 500)                                        │
│─────────────────────────────────────────────────────────────│
│ • tenantid          (varchar 256) [INDEXED]                 │
│ • source            (varchar 256)                           │
│ • eventtype         (varchar 256) [INDEXED]                 │
│ • name              (varchar 256) [INDEXED]                 │
│ • description       (text)                                  │
│ • status            (varchar 256) [INDEXED]                 │
│ • postedby          (varchar 256) [INDEXED]                 │
│ • referenceid       (varchar 256) [INDEXED]                 │
│ • recepient         (jsonb)                                 │
│ • eventdetails      (jsonb)                                 │
│ • actions           (jsonb)                                 │
│ • category          (varchar 256)                           │
│ • createdby         (varchar 256)                           │
│ • createdtime       (bigint)                                │
│ • lastmodifiedby    (varchar 256)                           │
│ • lastmodifiedtime  (bigint)                                │
└─────────────────────────────────────────────────────────────┘
                          │
                          │ 1
                          │
                          │ N
                          ▼
┌─────────────────────────────────────────────────────────────┐
│          eg_usrevents_recepnt_event_registry                │
│─────────────────────────────────────────────────────────────│
│ • recepient         (varchar 500) [INDEXED]                 │
│ • eventid           (varchar 500) ───────────┐              │
└─────────────────────────────────────────────────────────────┘
                                               │
                          Logical FK           │
                          (No physical FK)     │
                                               └──► eg_usrevents_events.id


┌─────────────────────────────────────────────────────────────┐
│                 eg_usrevents_user_lat                       │
│─────────────────────────────────────────────────────────────│
│ PK: userid (varchar 500)                                    │
│─────────────────────────────────────────────────────────────│
│ • lastaccesstime    (bigint)                                │
└─────────────────────────────────────────────────────────────┘
```

## Tables

### 1. eg_usrevents_events

**Purpose**: Stores user events and notifications within the system. This is the main table for managing all types of user events including notifications, alerts, and system messages.

| Column Name      | Data Type      | Nullable | Description |
|-----------------|----------------|----------|-------------|
| id              | varchar(500)   | NOT NULL | **Primary Key**. Unique identifier for each event |
| tenantid        | varchar(256)   | NOT NULL | Tenant identifier for multi-tenancy support. Identifies which tenant/organization this event belongs to |
| source          | varchar(256)   | NULL     | Source system or module that generated the event (e.g., "billing-service", "property-tax") |
| eventtype       | varchar(256)   | NULL     | Type/category of the event (e.g., "NOTIFICATION", "ALERT", "SYSTEM_UPDATE"). Used for filtering and processing |
| name            | varchar(256)   | NULL     | Display name or title of the event. Shown to users as the event heading |
| description     | text           | NULL     | Detailed description of the event. Contains the main message content for users |
| status          | varchar(256)   | NULL     | Current status of the event (e.g., "ACTIVE", "INACTIVE", "CANCELLED"). Used to track event lifecycle |
| postedby        | varchar(256)   | NULL     | User ID or system identifier that posted/created this event |
| referenceid     | varchar(256)   | NULL     | Reference to related entity (e.g., application ID, bill ID). Links event to source record |
| recepient       | jsonb          | NULL     | JSON object containing recipient information. Stores complex recipient data including user IDs, roles, or groups |
| eventdetails    | jsonb          | NULL     | JSON object containing additional event details. Flexible field for storing event-specific metadata |
| actions         | jsonb          | NULL     | JSON array of action objects. Defines clickable actions available for this event (e.g., "Pay Bill", "View Details") |
| category        | varchar(256)   | NULL     | Event category for grouping and filtering (e.g., "PAYMENT", "APPROVAL", "REMINDER") |
| createdby       | varchar(256)   | NOT NULL | User ID who created this event record |
| createdtime     | bigint         | NOT NULL | Event creation timestamp in epoch milliseconds |
| lastmodifiedby  | varchar(256)   | NULL     | User ID who last modified this event |
| lastmodifiedtime| bigint         | NULL     | Last modification timestamp in epoch milliseconds |

**Indexes**:
- `index_eg_usrevents_events_name` on `name` - Optimizes searches by event name
- `index_eg_usrevents_events_referenceid` on `referenceid` - Speeds up lookups by reference ID
- `on_eventtype` on `eventtype` - Enables fast filtering by event type
- `on_status` on `status` - Enables fast filtering by status
- `on_postedby` on `postedby` - Optimizes queries for events by specific posters
- `on_tenantid` on `tenantid` - Critical for multi-tenant data isolation and queries

---

### 2. eg_usrevents_recepnt_event_registry

**Purpose**: Registry/mapping table that links recipients to events. Supports many-to-many relationship between users and events, allowing efficient recipient-based queries.

| Column Name | Data Type    | Nullable | Description |
|------------|--------------|----------|-------------|
| recepient  | varchar(500) | NOT NULL | Recipient identifier (user ID, role name, or group identifier). Can be individual user or group |
| eventid    | varchar(500) | NOT NULL | Foreign key reference to `eg_usrevents_events.id`. Links to the event record |

**Indexes**:
- `on_recepient` on `recepient` - Enables fast lookup of all events for a specific recipient

**Relationships**:
- `eventid` logically references `eg_usrevents_events.id` (no physical foreign key constraint)
- Many-to-Many relationship: One event can have multiple recipients, one recipient can have multiple events

---

### 3. eg_usrevents_user_lat

**Purpose**: Tracks last access time for users. Used to determine which events are new/unread for each user and for user activity tracking.

| Column Name    | Data Type    | Nullable | Description |
|---------------|--------------|----------|-------------|
| userid        | varchar(500) | NOT NULL | **Primary Key**. Unique user identifier |
| lastaccesstime| bigint       | NOT NULL | Timestamp in epoch milliseconds when user last accessed the user events. Used to mark events as read/unread |

**Usage Pattern**:
- When a user views their events, `lastaccesstime` is updated to current timestamp
- Events with `createdtime` > `lastaccesstime` are considered "new" or "unread"
- One record per user in the system

---