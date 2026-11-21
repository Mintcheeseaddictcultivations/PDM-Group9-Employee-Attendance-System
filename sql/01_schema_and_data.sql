-- ============================================================================
-- IT079IU - PRINCIPLES OF DATABASE MANAGEMENT (Semester 1, 2026-2027)
-- GROUP 9 - TOPIC 15: EMPLOYEE ATTENDANCE AND SHIFT SCHEDULING
-- FILE 1: COMPLETE SCHEMA, CONSTRAINTS, INDEXES & SAMPLE DATA (Triggers in 01b_triggers.sql)
-- DBMS: MySQL 8.0+ (InnoDB)
-- Idempotent: Runs cleanly multiple times in a row without manual repair.
-- ============================================================================

DROP DATABASE IF EXISTS employee_attendance_db;
CREATE DATABASE employee_attendance_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE employee_attendance_db;

-- ============================================================================
-- PART A: TABLE DEFINITIONS & DECLARATIVE CONSTRAINTS (15 TABLES)
-- ============================================================================

-- 1. Department
CREATE TABLE Department (
    DepartmentID INT AUTO_INCREMENT,
    DepartmentName VARCHAR(100) NOT NULL,
    Location VARCHAR(150) NOT NULL,
    CONSTRAINT pk_department PRIMARY KEY (DepartmentID),
    CONSTRAINT uq_department_name UNIQUE (DepartmentName)
) ENGINE=InnoDB;

-- 2. Position
CREATE TABLE `Position` (
    PositionID INT AUTO_INCREMENT,
    PositionName VARCHAR(100) NOT NULL,
    Description TEXT NULL,
    CONSTRAINT pk_position PRIMARY KEY (PositionID),
    CONSTRAINT uq_position_name UNIQUE (PositionName)
) ENGINE=InnoDB;

-- 3. DepartmentPosition (M:N Relationship carrying its own attribute MaxHeadcount)
CREATE TABLE DepartmentPosition (
    DepartmentID INT NOT NULL,
    PositionID INT NOT NULL,
    MaxHeadcount INT NOT NULL DEFAULT 10,
    CONSTRAINT pk_department_position PRIMARY KEY (DepartmentID, PositionID),
    CONSTRAINT chk_deptpos_headcount CHECK (MaxHeadcount > 0),
    CONSTRAINT fk_deptpos_department FOREIGN KEY (DepartmentID)
        REFERENCES Department(DepartmentID)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_deptpos_position FOREIGN KEY (PositionID)
        REFERENCES `Position`(PositionID)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 4. Employee (Composite FK enforces Position must be offered by Department; Recursive FK for Supervises)
CREATE TABLE Employee (
    EmployeeID INT AUTO_INCREMENT,
    FullName VARCHAR(100) NOT NULL,
    Ssn CHAR(9) NOT NULL,
    Sex ENUM('M', 'F', 'Other') NOT NULL,
    Phone VARCHAR(20) NOT NULL,
    Email VARCHAR(100) NOT NULL,
    HireDate DATE NOT NULL,
    EmploymentStatus ENUM('Active', 'Inactive', 'OnLeave', 'Terminated') NOT NULL DEFAULT 'Active',
    DepartmentID INT NOT NULL,
    PositionID INT NOT NULL,
    ManagerID INT NULL,
    CONSTRAINT pk_employee PRIMARY KEY (EmployeeID),
    CONSTRAINT uq_employee_ssn UNIQUE (Ssn),
    CONSTRAINT uq_employee_email UNIQUE (Email),
    CONSTRAINT fk_employee_dept_pos FOREIGN KEY (DepartmentID, PositionID)
        REFERENCES DepartmentPosition(DepartmentID, PositionID)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_employee_manager FOREIGN KEY (ManagerID)
        REFERENCES Employee(EmployeeID)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 5. FullTime (ISA Subclass - Disjoint & Total with PartTime)
CREATE TABLE FullTime (
    EmployeeID INT NOT NULL,
    MonthlyBaseSalary DECIMAL(12, 2) NOT NULL,
    StandardMonthlyHours INT NOT NULL DEFAULT 160,
    CONSTRAINT pk_fulltime PRIMARY KEY (EmployeeID),
    CONSTRAINT chk_fulltime_salary CHECK (MonthlyBaseSalary > 0),
    CONSTRAINT chk_fulltime_hours CHECK (StandardMonthlyHours > 0),
    CONSTRAINT fk_fulltime_employee FOREIGN KEY (EmployeeID)
        REFERENCES Employee(EmployeeID)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 6. PartTime (ISA Subclass - Disjoint & Total with FullTime)
CREATE TABLE PartTime (
    EmployeeID INT NOT NULL,
    HourlyRate DECIMAL(10, 2) NOT NULL,
    ExpectedWeeklyHours INT NOT NULL DEFAULT 20,
    CONSTRAINT pk_parttime PRIMARY KEY (EmployeeID),
    CONSTRAINT chk_parttime_rate CHECK (HourlyRate > 0),
    CONSTRAINT chk_parttime_hours CHECK (ExpectedWeeklyHours > 0 AND ExpectedWeeklyHours <= 40),
    CONSTRAINT fk_parttime_employee FOREIGN KEY (EmployeeID)
        REFERENCES Employee(EmployeeID)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 7. UserAccount (1:1 optional from Employee, total on UserAccount)
CREATE TABLE UserAccount (
    UserID INT AUTO_INCREMENT,
    EmployeeID INT NOT NULL,
    Username VARCHAR(50) NOT NULL,
    PasswordHash VARCHAR(255) NOT NULL,
    Role ENUM('ADMIN', 'MANAGER', 'USER') NOT NULL DEFAULT 'USER',
    AccountStatus ENUM('Active', 'Disabled', 'Locked') NOT NULL DEFAULT 'Active',
    CreatedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    LastLoginAt DATETIME NULL,
    CONSTRAINT pk_useraccount PRIMARY KEY (UserID),
    CONSTRAINT uq_useraccount_emp UNIQUE (EmployeeID),
    CONSTRAINT uq_useraccount_username UNIQUE (Username),
    CONSTRAINT fk_useraccount_employee FOREIGN KEY (EmployeeID)
        REFERENCES Employee(EmployeeID)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 8. Shift (Supports overnight shifts via IsOvernight flag)
CREATE TABLE Shift (
    ShiftID INT AUTO_INCREMENT,
    ShiftName VARCHAR(50) NOT NULL,
    StartTime TIME NOT NULL,
    EndTime TIME NOT NULL,
    IsOvernight BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_shift PRIMARY KEY (ShiftID),
    CONSTRAINT uq_shift_name UNIQUE (ShiftName),
    CONSTRAINT chk_shift_times CHECK (
        (IsOvernight = FALSE AND EndTime > StartTime)
        OR (IsOvernight = TRUE AND EndTime < StartTime)
    )
) ENGINE=InnoDB;

-- 9. ShiftRequirement (Weak Entity identified by ShiftID + WorkDate)
CREATE TABLE ShiftRequirement (
    ShiftID INT NOT NULL,
    WorkDate DATE NOT NULL,
    RequiredEmployees INT NOT NULL DEFAULT 1,
    CONSTRAINT pk_shift_requirement PRIMARY KEY (ShiftID, WorkDate),
    CONSTRAINT chk_shift_req_count CHECK (RequiredEmployees > 0),
    CONSTRAINT fk_shiftreq_shift FOREIGN KEY (ShiftID)
        REFERENCES Shift(ShiftID)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 10. ShiftAssignment (Roster: identified by EmployeeID + WorkDate + ShiftID)
CREATE TABLE ShiftAssignment (
    ShiftAssignmentID INT AUTO_INCREMENT,
    EmployeeID INT NOT NULL,
    ShiftID INT NOT NULL,
    WorkDate DATE NOT NULL,
    AssignmentStatus ENUM('Scheduled', 'Confirmed', 'Cancelled') NOT NULL DEFAULT 'Scheduled',
    CONSTRAINT pk_shift_assignment PRIMARY KEY (ShiftAssignmentID),
    CONSTRAINT uq_emp_shift_date UNIQUE (EmployeeID, WorkDate, ShiftID),
    CONSTRAINT fk_shiftassign_employee FOREIGN KEY (EmployeeID)
        REFERENCES Employee(EmployeeID)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_shiftassign_shift FOREIGN KEY (ShiftID)
        REFERENCES Shift(ShiftID)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 11. Attendance (1:1 with ShiftAssignment)
CREATE TABLE Attendance (
    AttendanceID INT AUTO_INCREMENT,
    ShiftAssignmentID INT NOT NULL,
    ActualCheckIn DATETIME NULL,
    ActualCheckOut DATETIME NULL,
    AttendanceStatus ENUM('Present', 'Absent', 'Late', 'Excused') NOT NULL DEFAULT 'Absent',
    OvertimeHours DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    Notes VARCHAR(255) NULL,
    CONSTRAINT pk_attendance PRIMARY KEY (AttendanceID),
    CONSTRAINT uq_attendance_assignment UNIQUE (ShiftAssignmentID),
    CONSTRAINT chk_attendance_overtime CHECK (OvertimeHours >= 0),
    CONSTRAINT chk_attendance_times CHECK (ActualCheckOut IS NULL OR ActualCheckIn IS NULL OR ActualCheckOut >= ActualCheckIn),
    CONSTRAINT fk_attendance_assignment FOREIGN KEY (ShiftAssignmentID)
        REFERENCES ShiftAssignment(ShiftAssignmentID)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 12. AttendanceEvent (Weak Entity identified by AttendanceID + Partial Key EventNo)
CREATE TABLE AttendanceEvent (
    AttendanceID INT NOT NULL,
    EventNo INT NOT NULL,
    EventType ENUM('ClockIn', 'ClockOut', 'BreakStart', 'BreakEnd') NOT NULL,
    EventTimestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    DeviceLocation VARCHAR(100) NOT NULL,
    CONSTRAINT pk_attendance_event PRIMARY KEY (AttendanceID, EventNo),
    CONSTRAINT chk_event_no CHECK (EventNo > 0),
    CONSTRAINT fk_attevent_attendance FOREIGN KEY (AttendanceID)
        REFERENCES Attendance(AttendanceID)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 13. LeaveBalance (Weak Entity: leave balance per employee per year per type)
CREATE TABLE LeaveBalance (
    EmployeeID INT NOT NULL,
    LeaveYear INT NOT NULL,
    LeaveType ENUM('Annual', 'Sick', 'Unpaid', 'Maternity', 'Personal') NOT NULL,
    AllocatedDays DECIMAL(5, 2) NOT NULL DEFAULT 12.00,
    UsedDays DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT pk_leave_balance PRIMARY KEY (EmployeeID, LeaveYear, LeaveType),
    CONSTRAINT chk_leave_balance_year CHECK (LeaveYear BETWEEN 2000 AND 2100),
    CONSTRAINT chk_leave_balance_days CHECK (AllocatedDays >= 0 AND UsedDays >= 0 AND UsedDays <= AllocatedDays),
    CONSTRAINT fk_leavebalance_employee FOREIGN KEY (EmployeeID)
        REFERENCES Employee(EmployeeID)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 14. LeaveRequest (Includes SubmittedAt to track approval waiting time)
CREATE TABLE LeaveRequest (
    LeaveRequestID INT AUTO_INCREMENT,
    EmployeeID INT NOT NULL,
    LeaveType ENUM('Annual', 'Sick', 'Unpaid', 'Maternity', 'Personal') NOT NULL,
    StartDate DATE NOT NULL,
    EndDate DATE NOT NULL,
    Reason TEXT NOT NULL,
    SubmittedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    RequestStatus ENUM('Pending', 'Approved', 'Rejected') NOT NULL DEFAULT 'Pending',
    ReviewedBy INT NULL,
    ReviewedAt DATETIME NULL,
    CONSTRAINT pk_leave_request PRIMARY KEY (LeaveRequestID),
    CONSTRAINT chk_leave_dates CHECK (EndDate >= StartDate),
    CONSTRAINT fk_leavereq_employee FOREIGN KEY (EmployeeID)
        REFERENCES Employee(EmployeeID)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_leavereq_reviewer FOREIGN KEY (ReviewedBy)
        REFERENCES Employee(EmployeeID)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 15. PublicHoliday (Required for working-day calculations in Queries 4 & 5)
CREATE TABLE PublicHoliday (
    HolidayDate DATE NOT NULL,
    HolidayName VARCHAR(100) NOT NULL,
    IsPaidHoliday BOOLEAN NOT NULL DEFAULT TRUE,
    Description VARCHAR(255) NULL,
    CONSTRAINT pk_public_holiday PRIMARY KEY (HolidayDate)
) ENGINE=InnoDB;

-- ============================================================================
-- PART B: INDEXES (Query Performance Optimization - Section 2.3)
-- ============================================================================
-- Optimizes Q1 (Department roster by date range) & Q5 (Monthly working-day checks)
CREATE INDEX idx_shiftassignment_workdate_status ON ShiftAssignment(WorkDate, AssignmentStatus);
-- Optimizes Q4 (Pending leave requests filtered by status and SubmittedAt)
CREATE INDEX idx_leaverequest_status_submitted ON LeaveRequest(RequestStatus, SubmittedAt);
-- Optimizes Q2 & Q12 (Filtering attendance logs by status such as 'Late' or 'Present')
CREATE INDEX idx_attendance_status ON Attendance(AttendanceStatus);

-- ============================================================================
-- PART C: SAMPLE DATA INSERTION (>= 15 ROWS IN EVERY SINGLE TABLE)
-- (Note: Triggers are placed in 01b_triggers.sql for DBeaver compatibility)
-- ============================================================================

-- 1. Department (15 rows)
INSERT INTO Department (DepartmentID, DepartmentName, Location) VALUES
(1,  'Human Resources',          'Floor 2, Building A - District 1'),
(2,  'Information Technology',   'Floor 5, Building A - District 1'),
(3,  'Store Operations',         'Ground Floor, Flagship Store - District 1'),
(4,  'Customer Service',         'Floor 3, Building B - District 3'),
(5,  'Finance & Accounting',     'Floor 4, Building A - District 1'),
(6,  'Marketing & Branding',     'Floor 6, Building A - District 1'),
(7,  'Supply Chain & Logistics', 'Warehouse Zone 1 - Thu Duc City'),
(8,  'Quality Assurance',        'Floor 5, Building B - District 3'),
(9,  'Sales & Business Dev',     'Floor 2, Building B - District 3'),
(10, 'Security & Facility',      'Ground Floor, Building A - District 1'),
(11, 'Food & Beverage Ops',      'Branch 02 - Phu Nhuan District'),
(12, 'Retail Branch District 7', 'Crescent Hub - District 7'),
(13, 'Retail Branch Binh Thanh', 'Pearl Plaza - Binh Thanh District'),
(14, 'Training & Development',   'Floor 3, Building A - District 1'),
(15, 'Data & Analytics',         'Floor 6, Building B - District 3');

-- 2. Position (15 rows)
INSERT INTO `Position` (PositionID, PositionName, Description) VALUES
(1,  'System Administrator',     'Manages IT infrastructure, user permissions, and system security.'),
(2,  'Operations Manager',       'Oversees daily shift schedules, staffing levels, and branch performance.'),
(3,  'HR Specialist',            'Handles recruitment, employee records, and leave policy compliance.'),
(4,  'Shift Supervisor',         'Supervises on-site staff, approves clock-in exceptions, and monitors shifts.'),
(5,  'Senior Software Engineer', 'Develops and maintains internal attendance and scheduling software.'),
(6,  'Cashier',                  'Processes customer payments, refunds, and daily register reconciliation.'),
(7,  'Barista',                  'Prepares beverages, maintains counter hygiene, and serves customers.'),
(8,  'Customer Support Agent',   'Resolves customer inquiries via phone, chat, and email.'),
(9,  'Accountant',               'Manages payroll inputs, expense audits, and financial reporting.'),
(10, 'Warehouse Coordinator',    'Tracks inventory intake, dispatch schedules, and stock audits.'),
(11, 'Sales Associate',          'Assists customers on the sales floor and achieves branch sales targets.'),
(12, 'Security Officer',         'Monitors building access, CCTV, and workplace safety.'),
(13, 'QA Analyst',               'Tests software releases and audits operational standard procedures.'),
(14, 'Marketing Executive',      'Executes promotional campaigns and social media engagement.'),
(15, 'Data Analyst',             'Analyzes workforce attendance trends and shift demand forecasting.');

-- 3. DepartmentPosition (21 rows)
INSERT INTO DepartmentPosition (DepartmentID, PositionID, MaxHeadcount) VALUES
(1,  3,  8),
(1,  2,  2),
(2,  1,  3),
(2,  5,  15),
(3,  2,  2),
(3,  4,  5),
(3,  6,  20),
(3,  11, 25),
(4,  8,  15),
(5,  9,  6),
(6,  14, 8),
(7,  10, 12),
(8,  13, 6),
(9,  11, 15),
(10, 12, 10),
(11, 4,  4),
(11, 7,  18),
(12, 6,  10),
(13, 7,  12),
(14, 3,  4),
(15, 15, 6);

-- 4. Employee (30 rows: 15 FullTime [IDs 1..15] + 15 PartTime [IDs 16..30])
INSERT INTO Employee (EmployeeID, FullName, Ssn, Sex, Phone, Email, HireDate, EmploymentStatus, DepartmentID, PositionID, ManagerID) VALUES
(1,  'Do Phan Minh Tri',      '100000001', 'M', '0901000001', 'tri.do@company.vn',        '2022-01-10', 'Active',   2,  1,  NULL),
(2,  'Dinh Tuong Ngan',       '100000002', 'F', '0901000002', 'ngan.dinh@company.vn',     '2022-03-15', 'Active',   3,  2,  1),
(3,  'Le Tan Thinh',          '100000003', 'M', '0901000003', 'thinh.le@company.vn',      '2022-06-01', 'Active',   1,  2,  1),
(4,  'Tran Huy Truong',       '100000004', 'M', '0901000004', 'truong.tran@company.vn',   '2023-01-20', 'Active',   3,  4,  2),
(5,  'Nguyen Pham Nhat Minh', '100000005', 'M', '0901000005', 'minh.nguyen@company.vn',   '2023-02-14', 'Active',   11, 4,  2),
(6,  'Dinh Quoc Binh',        '100000006', 'M', '0901000006', 'binh.dinh@company.vn',     '2023-04-01', 'Active',   2,  5,  1),
(7,  'Le Van Nhan',           '100000007', 'M', '0901000007', 'nhan.le@company.vn',       '2023-05-10', 'Active',   2,  5,  1),
(8,  'Tran Hoang Doanh',      '100000008', 'M', '0901000008', 'doanh.tran@company.vn',    '2023-06-15', 'Active',   8,  13, 1),
(9,  'Huynh Do Dinh Nguyen',  '100000009', 'M', '0901000009', 'nguyen.huynh@company.vn',  '2023-07-01', 'Active',   15, 15, 1),
(10, 'Ly Khai Minh',          '100000010', 'M', '0901000010', 'khaiminh.ly@company.vn',   '2023-08-20', 'Active',   1,  3,  3),
(11, 'Vo Thi Minh Anh',       '100000011', 'F', '0901000011', 'anh.vo@company.vn',        '2023-09-12', 'Active',   5,  9,  3),
(12, 'Pham Gia Bao',          '100000012', 'M', '0901000012', 'bao.pham@company.vn',      '2024-01-08', 'Active',   7,  10, 2),
(13, 'Bui Thanh Truc',        '100000013', 'F', '0901000013', 'truc.bui@company.vn',      '2024-02-19', 'OnLeave',  6,  14, 3),
(14, 'Dang Quoc Khanh',       '100000014', 'M', '0901000014', 'khanh.dang@company.vn',    '2024-03-25', 'Active',   10, 12, 2),
(15, 'Hoang My Linh',         '100000015', 'F', '0901000015', 'linh.hoang@company.vn',    '2024-05-02', 'Active',   4,  8,  3),
(16, 'Nguyen Thi Thu Ha',     '100000016', 'F', '0901000016', 'ha.nguyen@company.vn',     '2024-06-01', 'Active',   3,  6,  4),
(17, 'Tran Minh Quan',        '100000017', 'M', '0901000017', 'quan.tran@company.vn',     '2024-06-15', 'Active',   3,  6,  4),
(18, 'Le Hoang Nam',          '100000018', 'M', '0901000018', 'nam.le@company.vn',        '2024-07-01', 'Active',   3,  11, 4),
(19, 'Pham Ngoc Han',         '100000019', 'F', '0901000019', 'han.pham@company.vn',      '2024-07-10', 'Active',   3,  11, 4),
(20, 'Vu Duc Thang',          '100000020', 'M', '0901000020', 'thang.vu@company.vn',      '2024-08-01', 'Active',   11, 7,  5),
(21, 'Doan Thuy Tien',        '100000021', 'F', '0901000021', 'tien.doan@company.vn',     '2024-08-12', 'Active',   11, 7,  5),
(22, 'Ngo Gia Huy',           '100000022', 'M', '0901000022', 'huy.ngo@company.vn',       '2024-09-01', 'Active',   11, 7,  5),
(23, 'Truong Bao Ngoc',       '100000023', 'F', '0901000023', 'ngoc.truong@company.vn',   '2024-09-18', 'Active',   12, 6,  4),
(24, 'Phan Anh Tuan',         '100000024', 'M', '0901000024', 'tuan.phan@company.vn',     '2024-10-05', 'Active',   12, 6,  4),
(25, 'Duong Khanh Vy',        '100000025', 'F', '0901000025', 'vy.duong@company.vn',      '2024-10-20', 'Active',   13, 7,  5),
(26, 'Mai Xuan Truong',       '100000026', 'M', '0901000026', 'xuan.mai@company.vn',      '2024-11-01', 'Active',   13, 7,  5),
(27, 'Hoang Gia Han',         '100000027', 'F', '0901000027', 'giahan.hoang@company.vn',  '2024-11-15', 'Active',   9,  11, 2),
(28, 'Luong Minh Triet',      '100000028', 'M', '0901000028', 'triet.luong@company.vn',   '2024-12-01', 'Active',   4,  8,  3),
(29, 'Ta Thanh Thao',         '100000029', 'F', '0901000029', 'thao.ta@company.vn',       '2025-01-10', 'Inactive', 14, 3,  3),
(30, 'Nguyen Huu Phuoc',      '100000030', 'M', '0901000030', 'phuoc.nguyen@company.vn',  '2025-02-01', 'Active',   3,  6,  4);

-- 5. FullTime (15 rows: EmployeeID 1..15)
INSERT INTO FullTime (EmployeeID, MonthlyBaseSalary, StandardMonthlyHours) VALUES
(1,  35000000.00, 160),
(2,  30000000.00, 160),
(3,  28000000.00, 160),
(4,  22000000.00, 168),
(5,  21000000.00, 168),
(6,  25000000.00, 160),
(7,  24500000.00, 160),
(8,  20000000.00, 160),
(9,  23000000.00, 160),
(10, 18000000.00, 160),
(11, 19500000.00, 160),
(12, 17500000.00, 168),
(13, 18500000.00, 160),
(14, 15000000.00, 176),
(15, 16500000.00, 160);

-- 6. PartTime (15 rows: EmployeeID 16..30)
INSERT INTO PartTime (EmployeeID, HourlyRate, ExpectedWeeklyHours) VALUES
(16, 35000.00, 24),
(17, 35000.00, 20),
(18, 38000.00, 24),
(19, 38000.00, 16),
(20, 32000.00, 28),
(21, 32000.00, 24),
(22, 30000.00, 20),
(23, 36000.00, 24),
(24, 36000.00, 20),
(25, 33000.00, 24),
(26, 31000.00, 16),
(27, 40000.00, 20),
(28, 37000.00, 24),
(29, 34000.00, 12),
(30, 32000.00, 20);

-- 7. UserAccount (25 rows - Employees 25..28 & 30 intentionally have no account for LEFT JOIN Q11)
INSERT INTO UserAccount (UserID, EmployeeID, Username, PasswordHash, Role, AccountStatus, CreatedAt, LastLoginAt) VALUES
(1,  1,  'admin.tri',    '$2a$12$hashAdminTri0000000000000001', 'ADMIN',   'Active',   '2022-01-10 08:00:00', '2026-11-19 08:15:00'),
(2,  2,  'mgr.ngan',     '$2a$12$hashMgrNgan0000000000000002',  'MANAGER', 'Active',   '2022-03-15 08:30:00', '2026-11-19 08:45:00'),
(3,  3,  'mgr.thinh',    '$2a$12$hashMgrThinh000000000000003',  'MANAGER', 'Active',   '2022-06-01 09:00:00', '2026-11-18 17:20:00'),
(4,  4,  'sup.truong',   '$2a$12$hashSupTruong00000000000004',  'MANAGER', 'Active',   '2023-01-20 08:00:00', '2026-11-19 07:55:00'),
(5,  5,  'sup.minh',     '$2a$12$hashSupMinh0000000000000005',  'MANAGER', 'Active',   '2023-02-14 08:00:00', '2026-11-19 09:05:00'),
(6,  6,  'binh.dinh',    '$2a$12$hashUserBinh000000000000006',  'USER',    'Active',   '2023-04-01 09:00:00', '2026-11-19 08:50:00'),
(7,  7,  'nhan.le',      '$2a$12$hashUserNhan000000000000007',  'USER',    'Active',   '2023-05-10 09:00:00', '2026-11-18 18:00:00'),
(8,  8,  'doanh.tran',   '$2a$12$hashUserDoanh00000000000008',  'USER',    'Active',   '2023-06-15 09:00:00', '2026-11-19 08:30:00'),
(9,  9,  'nguyen.huynh', '$2a$12$hashUserNguyen000000000009',   'USER',    'Active',   '2023-07-01 09:00:00', '2026-11-19 08:40:00'),
(10, 10, 'khaiminh.ly',  '$2a$12$hashUserKhaiMinh0000000010',   'USER',    'Active',   '2023-08-20 09:00:00', '2026-11-17 16:45:00'),
(11, 11, 'anh.vo',       '$2a$12$hashUserAnhVo0000000000011',   'USER',    'Active',   '2023-09-12 09:00:00', '2026-11-19 08:10:00'),
(12, 12, 'bao.pham',     '$2a$12$hashUserBaoPham00000000012',   'USER',    'Active',   '2024-01-08 09:00:00', '2026-11-18 14:15:00'),
(13, 13, 'truc.bui',     '$2a$12$hashUserTrucBui00000000013',   'USER',    'Active',   '2024-02-19 09:00:00', '2026-11-10 17:00:00'),
(14, 14, 'khanh.dang',   '$2a$12$hashUserKhanhDang000000014',   'USER',    'Active',   '2024-03-25 09:00:00', '2026-11-19 06:50:00'),
(15, 15, 'linh.hoang',   '$2a$12$hashUserLinhHoang000000015',   'USER',    'Active',   '2024-05-02 09:00:00', '2026-11-19 08:55:00'),
(16, 16, 'ha.nguyen',    '$2a$12$hashUserHaNguyen0000000016',   'USER',    'Active',   '2024-06-01 09:00:00', '2026-11-19 07:50:00'),
(17, 17, 'quan.tran',    '$2a$12$hashUserQuanTran0000000017',   'USER',    'Active',   '2024-06-15 09:00:00', '2026-11-18 13:05:00'),
(18, 18, 'nam.le',       '$2a$12$hashUserNamLe0000000000018',   'USER',    'Active',   '2024-07-01 09:00:00', '2026-11-19 08:02:00'),
(19, 19, 'han.pham',     '$2a$12$hashUserHanPham00000000019',   'USER',    'Active',   '2024-07-10 09:00:00', '2026-11-16 12:00:00'),
(20, 20, 'thang.vu',     '$2a$12$hashUserThangVu00000000020',   'USER',    'Active',   '2024-08-01 09:00:00', '2026-11-19 07:45:00'),
(21, 21, 'tien.doan',    '$2a$12$hashUserTienDoan0000000021',   'USER',    'Active',   '2024-08-12 09:00:00', '2026-11-18 17:30:00'),
(22, 22, 'huy.ngo',      '$2a$12$hashUserHuyNgo000000000022',   'USER',    'Locked',   '2024-09-01 09:00:00', '2026-11-01 10:00:00'),
(23, 23, 'ngoc.truong',  '$2a$12$hashUserNgocTruong00000023',   'USER',    'Active',   '2024-09-18 09:00:00', '2026-11-19 08:20:00'),
(24, 24, 'tuan.phan',    '$2a$12$hashUserTuanPhan0000000024',   'USER',    'Active',   '2024-10-05 09:00:00', NULL),
(25, 29, 'thao.ta',      '$2a$12$hashUserThaoTa000000000025',   'USER',    'Disabled', '2025-01-10 09:00:00', '2025-06-15 17:00:00');

-- 8. Shift (15 rows - Shift 14 demonstrates overnight shift handling)
INSERT INTO Shift (ShiftID, ShiftName, StartTime, EndTime, IsOvernight) VALUES
(1,  'Morning Standard',      '08:00:00', '17:00:00', FALSE),
(2,  'Morning Early Store',   '06:30:00', '14:30:00', FALSE),
(3,  'Afternoon Store',       '14:00:00', '22:00:00', FALSE),
(4,  'Part-Time Morning A',   '08:00:00', '12:00:00', FALSE),
(5,  'Part-Time Afternoon A', '13:00:00', '17:00:00', FALSE),
(6,  'Part-Time Evening A',   '17:30:00', '21:30:00', FALSE),
(7,  'Cafe Opening Shift',    '06:00:00', '11:00:00', FALSE),
(8,  'Cafe Mid-Day Shift',    '11:00:00', '16:00:00', FALSE),
(9,  'Cafe Closing Shift',    '16:00:00', '22:00:00', FALSE),
(10, 'Warehouse Dawn Shift',  '05:30:00', '13:30:00', FALSE),
(11, 'Support Day Shift',     '08:30:00', '17:30:00', FALSE),
(12, 'Support Late Shift',    '13:30:00', '21:30:00', FALSE),
(13, 'Security Day Watch',    '06:00:00', '18:00:00', FALSE),
(14, 'Security Night Watch',  '18:00:00', '06:00:00', TRUE),
(15, 'Weekend Peak Shift',    '09:00:00', '18:00:00', FALSE);

-- 9. ShiftRequirement (16 rows)
INSERT INTO ShiftRequirement (ShiftID, WorkDate, RequiredEmployees) VALUES
(1,  '2026-11-16', 6),
(2,  '2026-11-16', 3),
(3,  '2026-11-16', 3),
(4,  '2026-11-16', 2),
(7,  '2026-11-16', 3),
(1,  '2026-11-17', 6),
(2,  '2026-11-17', 3),
(3,  '2026-11-17', 4),
(5,  '2026-11-17', 2),
(8,  '2026-11-17', 3),
(1,  '2026-11-18', 6),
(4,  '2026-11-18', 3),
(6,  '2026-11-18', 3),
(9,  '2026-11-18', 3),
(1,  '2026-11-19', 6),
(15, '2026-11-21', 5);

-- 10. ShiftAssignment (42 rows: covers Oct 2026, Nov 16-21, and Next Week Nov 23-26)
INSERT INTO ShiftAssignment (ShiftAssignmentID, EmployeeID, ShiftID, WorkDate, AssignmentStatus) VALUES
(1,  6,  1,  '2026-11-16', 'Confirmed'),
(2,  7,  1,  '2026-11-16', 'Confirmed'),
(3,  8,  1,  '2026-11-16', 'Confirmed'),
(4,  9,  1,  '2026-11-16', 'Confirmed'),
(5,  16, 2,  '2026-11-16', 'Confirmed'),
(6,  17, 2,  '2026-11-16', 'Confirmed'),
(7,  18, 3,  '2026-11-16', 'Confirmed'),
(8,  20, 7,  '2026-11-16', 'Confirmed'),
(9,  21, 7,  '2026-11-16', 'Confirmed'),
(10, 6,  1,  '2026-11-17', 'Confirmed'),
(11, 7,  1,  '2026-11-17', 'Confirmed'),
(12, 10, 1,  '2026-11-17', 'Confirmed'),
(13, 11, 1,  '2026-11-17', 'Confirmed'),
(14, 16, 4,  '2026-11-17', 'Confirmed'),
(15, 19, 5,  '2026-11-17', 'Confirmed'),
(16, 20, 8,  '2026-11-17', 'Confirmed'),
(17, 22, 8,  '2026-11-17', 'Cancelled'),
(18, 6,  1,  '2026-11-18', 'Confirmed'),
(19, 12, 10, '2026-11-18', 'Confirmed'),
(20, 14, 13, '2026-11-18', 'Confirmed'),
(21, 15, 11, '2026-11-19', 'Scheduled'),
(22, 23, 15, '2026-11-21', 'Scheduled'),
(23, 6,  1,  '2026-11-19', 'Confirmed'),
(24, 2,  1,  '2026-11-16', 'Confirmed'),
(25, 2,  1,  '2026-11-17', 'Confirmed'),
(26, 2,  1,  '2026-11-18', 'Confirmed'),
(27, 2,  1,  '2026-11-19', 'Confirmed'),
(28, 8,  1,  '2026-10-05', 'Confirmed'),
(29, 8,  1,  '2026-10-12', 'Confirmed'),
(30, 8,  1,  '2026-10-19', 'Confirmed'),
(31, 8,  1,  '2026-10-26', 'Confirmed'),
(32, 17, 2,  '2026-10-06', 'Confirmed'),
(33, 17, 2,  '2026-10-13', 'Confirmed'),
(34, 17, 2,  '2026-10-20', 'Confirmed'),
(35, 17, 2,  '2026-10-27', 'Confirmed'),
(36, 4,  2,  '2026-11-23', 'Scheduled'),
(37, 16, 2,  '2026-11-23', 'Scheduled'),
(38, 17, 3,  '2026-11-24', 'Scheduled'),
(39, 18, 3,  '2026-11-25', 'Scheduled'),
(40, 19, 5,  '2026-11-26', 'Scheduled'),
(41, 6,  1,  '2026-11-23', 'Scheduled'),
(42, 7,  1,  '2026-11-24', 'Scheduled');

-- 11. Attendance (31 rows)
INSERT INTO Attendance (AttendanceID, ShiftAssignmentID, ActualCheckIn, ActualCheckOut, AttendanceStatus, OvertimeHours, Notes) VALUES
(1,  1,  '2026-11-16 07:55:00', '2026-11-16 18:30:00', 'Present', 1.50, 'Deployed hotfix after hours'),
(2,  2,  '2026-11-16 07:58:00', '2026-11-16 17:05:00', 'Present', 0.00, NULL),
(3,  3,  '2026-11-16 08:18:00', '2026-11-16 17:15:00', 'Late',    0.00, 'Traffic congestion on Dien Bien Phu St.'),
(4,  4,  '2026-11-16 07:50:00', '2026-11-16 19:00:00', 'Present', 2.00, 'Completed monthly KPI report'),
(5,  5,  '2026-11-16 06:25:00', '2026-11-16 14:35:00', 'Present', 0.00, NULL),
(6,  6,  '2026-11-16 06:47:00', '2026-11-16 14:30:00', 'Late',    0.00, 'Arrived 17 mins late'),
(7,  7,  NULL,                  NULL,                  'Absent',  0.00, 'No-show without prior notice'),
(8,  8,  '2026-11-16 05:55:00', '2026-11-16 12:00:00', 'Present', 1.00, 'Covered peak morning rush'),
(9,  9,  '2026-11-16 05:58:00', '2026-11-16 11:02:00', 'Present', 0.00, NULL),
(10, 10, '2026-11-17 07:56:00', '2026-11-17 17:30:00', 'Present', 0.50, 'Database schema review'),
(11, 11, '2026-11-17 08:25:00', '2026-11-17 17:20:00', 'Late',    0.00, 'Heavy rain'),
(12, 12, '2026-11-17 07:52:00', '2026-11-17 17:00:00', 'Present', 0.00, NULL),
(13, 13, '2026-11-17 07:59:00', '2026-11-17 18:00:00', 'Present', 1.00, 'Month-end tax reconciliation'),
(14, 14, '2026-11-17 07:55:00', '2026-11-17 12:05:00', 'Present', 0.00, NULL),
(15, 15, NULL,                  NULL,                  'Excused', 0.00, 'Approved medical appointment'),
(16, 16, '2026-11-17 10:55:00', '2026-11-17 16:30:00', 'Present', 0.50, NULL),
(17, 17, NULL,                  NULL,                  'Absent',  0.00, 'Shift cancelled due to illness'),
(18, 18, '2026-11-18 07:54:00', '2026-11-18 19:00:00', 'Present', 2.00, 'Integration testing with Java team'),
(19, 23, '2026-11-19 07:55:00', '2026-11-19 17:05:00', 'Present', 0.00, 'On time'),
(20, 24, '2026-11-16 07:50:00', '2026-11-16 18:00:00', 'Present', 1.00, 'Weekly branch audit'),
(21, 25, '2026-11-17 07:52:00', '2026-11-17 17:00:00', 'Present', 0.00, 'On time'),
(22, 26, '2026-11-18 07:55:00', '2026-11-18 17:05:00', 'Present', 0.00, 'On time'),
(23, 27, '2026-11-19 07:53:00', '2026-11-19 17:10:00', 'Present', 0.00, 'On time'),
(24, 28, '2026-10-05 08:22:00', '2026-10-05 17:05:00', 'Late',    0.00, 'Late arrival #1 in Oct'),
(25, 29, '2026-10-12 08:25:00', '2026-10-12 17:00:00', 'Late',    0.00, 'Late arrival #2 in Oct'),
(26, 30, '2026-10-19 08:19:00', '2026-10-19 17:10:00', 'Late',    0.00, 'Late arrival #3 in Oct'),
(27, 31, '2026-10-26 08:30:00', '2026-10-26 17:30:00', 'Late',    0.50, 'Late arrival #4 in Oct'),
(28, 32, '2026-10-06 06:48:00', '2026-10-06 14:30:00', 'Late',    0.00, 'Late arrival #1 in Oct'),
(29, 33, '2026-10-13 06:50:00', '2026-10-13 14:30:00', 'Late',    0.00, 'Late arrival #2 in Oct'),
(30, 34, '2026-10-20 06:55:00', '2026-10-20 14:35:00', 'Late',    0.00, 'Late arrival #3 in Oct'),
(31, 35, '2026-10-27 06:49:00', '2026-10-27 14:30:00', 'Late',    0.00, 'Late arrival #4 in Oct');

-- 12. AttendanceEvent (24 rows - Weak Entity with DeviceLocation)
INSERT INTO AttendanceEvent (AttendanceID, EventNo, EventType, EventTimestamp, DeviceLocation) VALUES
(1,  1, 'ClockIn',    '2026-11-16 07:55:00', 'Biometric Scanner A1 - HQ Floor 5'),
(1,  2, 'BreakStart', '2026-11-16 12:00:00', 'Cafeteria Gate - HQ Floor 2'),
(1,  3, 'BreakEnd',   '2026-11-16 13:00:00', 'Cafeteria Gate - HQ Floor 2'),
(1,  4, 'ClockOut',   '2026-11-16 18:30:00', 'Biometric Scanner A1 - HQ Floor 5'),
(2,  1, 'ClockIn',    '2026-11-16 07:58:00', 'Biometric Scanner A1 - HQ Floor 5'),
(2,  2, 'ClockOut',   '2026-11-16 17:05:00', 'Biometric Scanner A1 - HQ Floor 5'),
(3,  1, 'ClockIn',    '2026-11-16 08:18:00', 'Biometric Scanner B2 - Building B'),
(3,  2, 'ClockOut',   '2026-11-16 17:15:00', 'Biometric Scanner B2 - Building B'),
(4,  1, 'ClockIn',    '2026-11-16 07:50:00', 'Biometric Scanner B3 - Building B'),
(4,  2, 'BreakStart', '2026-11-16 12:05:00', 'Cafeteria Gate - Building B'),
(4,  3, 'BreakEnd',   '2026-11-16 12:55:00', 'Cafeteria Gate - Building B'),
(4,  4, 'ClockOut',   '2026-11-16 19:00:00', 'Biometric Scanner B3 - Building B'),
(5,  1, 'ClockIn',    '2026-11-16 06:25:00', 'POS Terminal 01 - Flagship Store D1'),
(5,  2, 'ClockOut',   '2026-11-16 14:35:00', 'POS Terminal 01 - Flagship Store D1'),
(6,  1, 'ClockIn',    '2026-11-16 06:47:00', 'POS Terminal 02 - Flagship Store D1'),
(6,  2, 'ClockOut',   '2026-11-16 14:30:00', 'POS Terminal 02 - Flagship Store D1'),
(8,  1, 'ClockIn',    '2026-11-16 05:55:00', 'Tablet Kiosk 01 - Branch Phu Nhuan'),
(8,  2, 'ClockOut',   '2026-11-16 12:00:00', 'Tablet Kiosk 01 - Branch Phu Nhuan'),
(10, 1, 'ClockIn',    '2026-11-17 07:56:00', 'Biometric Scanner A1 - HQ Floor 5'),
(10, 2, 'ClockOut',   '2026-11-17 17:30:00', 'Biometric Scanner A1 - HQ Floor 5'),
(13, 1, 'ClockIn',    '2026-11-17 07:59:00', 'Biometric Scanner A2 - HQ Floor 4'),
(13, 2, 'ClockOut',   '2026-11-17 18:00:00', 'Biometric Scanner A2 - HQ Floor 4'),
(18, 1, 'ClockIn',    '2026-11-18 07:54:00', 'Biometric Scanner A1 - HQ Floor 5'),
(18, 2, 'ClockOut',   '2026-11-18 19:00:00', 'Biometric Scanner A1 - HQ Floor 5');

-- 13. LeaveBalance (20 rows)
INSERT INTO LeaveBalance (EmployeeID, LeaveYear, LeaveType, AllocatedDays, UsedDays) VALUES
(1,  2026, 'Annual',    15.00, 3.00),
(2,  2026, 'Annual',    15.00, 2.00),
(3,  2026, 'Annual',    15.00, 4.00),
(4,  2026, 'Annual',    14.00, 1.00),
(5,  2026, 'Annual',    14.00, 2.00),
(6,  2026, 'Annual',    12.00, 3.00),
(6,  2026, 'Sick',      10.00, 0.00),
(7,  2026, 'Annual',    12.00, 1.00),
(7,  2026, 'Sick',      10.00, 2.00),
(8,  2026, 'Annual',    12.00, 0.00),
(8,  2026, 'Personal',   5.00, 1.00),
(9,  2026, 'Annual',    12.00, 2.00),
(10, 2026, 'Annual',    12.00, 1.00),
(10, 2026, 'Sick',      10.00, 1.00),
(11, 2026, 'Annual',    12.00, 0.00),
(12, 2026, 'Unpaid',    30.00, 3.00),
(13, 2026, 'Maternity',180.00, 90.00),
(14, 2026, 'Annual',    12.00, 2.00),
(15, 2026, 'Sick',      10.00, 1.00),
(16, 2026, 'Personal',   5.00, 1.00);

-- 14. LeaveRequest (15 rows - IDs 3, 4, 7, 14 are Pending > 5 working days for Q4)
INSERT INTO LeaveRequest (LeaveRequestID, EmployeeID, LeaveType, StartDate, EndDate, Reason, SubmittedAt, RequestStatus, ReviewedBy, ReviewedAt) VALUES
(1,  6,  'Annual',    '2026-11-25', '2026-11-27', 'Family trip to Da Lat',                    '2026-11-12 09:00:00', 'Approved', 1,    '2026-11-15 10:15:00'),
(2,  7,  'Sick',      '2026-11-10', '2026-11-11', 'Seasonal flu and fever',                   '2026-11-09 08:00:00', 'Approved', 1,    '2026-11-09 18:30:00'),
(3,  8,  'Personal',  '2026-12-01', '2026-12-01', 'Renewing national identification card',    '2026-11-04 10:30:00', 'Pending',  NULL, NULL),
(4,  9,  'Annual',    '2026-12-10', '2026-12-12', 'Year-end personal vacation',               '2026-11-05 14:15:00', 'Pending',  NULL, NULL),
(5,  10, 'Sick',      '2026-11-05', '2026-11-05', 'Dental surgery recovery',                  '2026-11-03 11:00:00', 'Approved', 3,    '2026-11-04 14:20:00'),
(6,  11, 'Annual',    '2026-11-28', '2026-11-29', 'Attending sister wedding in Hue',          '2026-11-14 15:00:00', 'Rejected', 3,    '2026-11-16 09:45:00'),
(7,  12, 'Unpaid',    '2026-12-05', '2026-12-07', 'Personal family matters',                  '2026-11-06 09:20:00', 'Pending',  NULL, NULL),
(8,  13, 'Maternity', '2026-11-01', '2027-04-30', 'Statutory 6-month maternity leave',        '2026-10-15 08:30:00', 'Approved', 3,    '2026-10-20 11:00:00'),
(9,  14, 'Annual',    '2026-11-22', '2026-11-23', 'Short rest after night watch rotation',    '2026-11-15 16:00:00', 'Approved', 2,    '2026-11-17 15:10:00'),
(10, 15, 'Sick',      '2026-11-12', '2026-11-12', 'Migraine headache',                        '2026-11-12 07:00:00', 'Approved', 3,    '2026-11-12 07:40:00'),
(11, 16, 'Personal',  '2026-11-19', '2026-11-19', 'University midterm examination',           '2026-11-14 10:00:00', 'Approved', 4,    '2026-11-16 16:00:00'),
(12, 17, 'Unpaid',    '2026-11-24', '2026-11-25', 'Hometown visit',                           '2026-11-16 13:00:00', 'Rejected', 4,    '2026-11-18 11:25:00'),
(13, 19, 'Sick',      '2026-11-17', '2026-11-17', 'Hospital check-up',                        '2026-11-16 08:45:00', 'Approved', 4,    '2026-11-16 19:00:00'),
(14, 20, 'Annual',    '2026-12-02', '2026-12-04', 'Personal travel',                          '2026-11-09 11:10:00', 'Pending',  NULL, NULL),
(15, 21, 'Personal',  '2026-11-26', '2026-11-26', 'Thesis defense preparation at university', '2026-11-18 16:30:00', 'Pending',  NULL, NULL);

-- 15. PublicHoliday (15 rows)
INSERT INTO PublicHoliday (HolidayDate, HolidayName, IsPaidHoliday, Description) VALUES
('2026-01-01', 'New Year Day',                    TRUE,  'International New Year public holiday'),
('2026-02-16', 'Lunar New Year Eve (Tet)',        TRUE,  'Vietnamese Lunar New Year Eve'),
('2026-02-17', 'Lunar New Year Day 1 (Tet)',      TRUE,  'First day of Tet holiday'),
('2026-02-18', 'Lunar New Year Day 2 (Tet)',      TRUE,  'Second day of Tet holiday'),
('2026-02-19', 'Lunar New Year Day 3 (Tet)',      TRUE,  'Third day of Tet holiday'),
('2026-02-20', 'Lunar New Year Day 4 (Tet)',      TRUE,  'Fourth day of Tet holiday'),
('2026-04-26', 'Hung Kings Commemoration Day',    TRUE,  '10th day of 3rd Lunar Month'),
('2026-04-27', 'Hung Kings Compensatory Leave',   TRUE,  'Compensatory day off for Sunday holiday'),
('2026-04-30', 'Reunification Day',               TRUE,  'Liberation of the South and National Reunification'),
('2026-05-01', 'International Labour Day',        TRUE,  'May Day public holiday'),
('2026-09-02', 'National Day',                    TRUE,  'Vietnam National Independence Day'),
('2026-09-03', 'National Day Additional Holiday', TRUE,  'Adjacent National Day holiday'),
('2026-10-20', 'Company Foundation Anniversary',  TRUE,  'Annual company-wide paid holiday'),
('2026-11-20', 'Vietnamese Teachers Day Event',   FALSE, 'Company training observance'),
('2026-12-25', 'Christmas Company Appreciation',  FALSE, 'Year-end company celebration day');
