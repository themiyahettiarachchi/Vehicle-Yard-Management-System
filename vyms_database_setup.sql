-- ============================================================
-- VYMS - Vehicle Yard Management System
-- Full Database Setup Script for SQL Server (SSMS)
-- Database: vehicle_yard_db
-- ============================================================

-- Create and use the database
CREATE DATABASE vehicle_yard_db;
GO

USE vehicle_yard_db;
GO

-- ============================================================
-- 1. users table
-- ============================================================
CREATE TABLE users (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    username      NVARCHAR(255),
    email         NVARCHAR(255),
    password      NVARCHAR(255),
    role          NVARCHAR(20),        -- ADMIN | MANAGER | SALES | INVENTORY | MECHANIC
    contract_type NVARCHAR(50),        -- PERMANENT | CONTRACT
    salary_rate   DECIMAL(19, 2)       -- Monthly amount (PERMANENT) or daily rate (CONTRACT)
);
GO

-- ============================================================
-- 2. vehicle table
-- ============================================================
CREATE TABLE vehicle (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    chassis_number NVARCHAR(255) UNIQUE,
    license_plate  NVARCHAR(255),
    vehicle_model  NVARCHAR(255),
    purchase_price DECIMAL(19, 2),
    repair_cost    DECIMAL(19, 2),
    sale_price     DECIMAL(19, 2),
    status         NVARCHAR(50),       -- UNSOLD | SOLD
    image_path     NVARCHAR(255),
    make           NVARCHAR(255),
    model          NVARCHAR(255),
    year           INT,
    vin            NVARCHAR(255) UNIQUE
);
GO

-- ============================================================
-- 3. repair table
-- ============================================================
CREATE TABLE repair (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    vehicle_id  BIGINT NOT NULL,
    description NVARCHAR(255),
    cost        DECIMAL(19, 2),
    repair_date DATE,
    repair_type NVARCHAR(50),          -- INTERNAL | EXTERNAL
    status      NVARCHAR(50),          -- PENDING | INSPECTED
    CONSTRAINT FK_repair_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicle(id) ON DELETE CASCADE
);
GO

-- ============================================================
-- 4. sale table
-- ============================================================
CREATE TABLE sale (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    vehicle_id     BIGINT UNIQUE,
    seller_id      BIGINT,
    sale_price     DECIMAL(19, 2),
    sale_date      DATE,
    buyer_type     NVARCHAR(20),       -- REGULAR_CUSTOMER | AUCTION | EXPORT
    customer_name  NVARCHAR(255),
    contact_number NVARCHAR(255),
    email          NVARCHAR(255),
    sale_status    NVARCHAR(50),       -- DRAFT | FINALIZED
    total_cost     DECIMAL(19, 2),     -- Snapshot of purchasePrice + repairCost at sale time
    CONSTRAINT FK_sale_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    CONSTRAINT FK_sale_seller  FOREIGN KEY (seller_id)  REFERENCES users(id)
);
GO

-- ============================================================
-- 5. attendance table
-- ============================================================
CREATE TABLE attendance (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    date           DATE,
    status         NVARCHAR(50),       -- Present | Late | Absent | On Leave
    check_in_time  NVARCHAR(50),
    check_out_time NVARCHAR(50),
    CONSTRAINT FK_attendance_user FOREIGN KEY (user_id) REFERENCES users(id)
);
GO

-- ============================================================
-- 6. payroll table
-- ============================================================
CREATE TABLE payroll (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    base_salary  DECIMAL(19, 2),
    bonus        DECIMAL(19, 2),
    deductions   DECIMAL(19, 2),
    net_pay      DECIMAL(19, 2),
    period_start DATE,
    period_end   DATE,
    CONSTRAINT FK_payroll_user FOREIGN KEY (user_id) REFERENCES users(id)
);
GO

-- ============================================================
-- 7. system_log table
-- ============================================================
CREATE TABLE system_log (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    type        NVARCHAR(255),         -- USER_CREATED | LOGIN | DELETE | etc.
    description NVARCHAR(500),
    actor       NVARCHAR(255),         -- Username or email of the actor
    timestamp   DATETIME2,
    status      NVARCHAR(50)           -- SUCCESS | FAILURE
);
GO

-- ============================================================
-- SEED DATA: Default Admin User
-- Replace the password hash below with a real BCrypt hash
-- You can generate one at: https://bcrypt-generator.com/
-- Example below is BCrypt hash for "admin123"
-- ============================================================
INSERT INTO users (username, email, password, role, contract_type, salary_rate)
VALUES (
    'admin',
    'admin@vyms.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- "admin123"
    'ADMIN',
    'PERMANENT',
    150000.00
);
GO
