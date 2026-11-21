-- ============================================================================
-- IT079IU - PRINCIPLES OF DATABASE MANAGEMENT (Semester 1, 2026-2027)
-- GROUP 9 - TOPIC 15: EMPLOYEE ATTENDANCE AND SHIFT SCHEDULING
-- FILE 1B: BUSINESS RULE TRIGGERS (Run in MySQL Workbench / CLI, or select each
--          CREATE TRIGGER ... END; block in DBeaver and press Command + Enter)
-- ============================================================================

USE employee_attendance_db;

DELIMITER $$

-- 1. ISA Disjointness: Employee cannot be both FullTime and PartTime
CREATE TRIGGER trg_fulltime_disjoint_insert
BEFORE INSERT ON FullTime
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM PartTime WHERE EmployeeID = NEW.EmployeeID) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'ISA Disjointness Violation: Employee is already registered as PartTime.';
    END IF;
END $$

CREATE TRIGGER trg_parttime_disjoint_insert
BEFORE INSERT ON PartTime
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM FullTime WHERE EmployeeID = NEW.EmployeeID) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'ISA Disjointness Violation: Employee is already registered as FullTime.';
    END IF;
END $$

-- 2. Supervises Hierarchy: Prevent self-supervision and multi-level supervisory cycles
CREATE TRIGGER trg_employee_prevent_self_mgr_insert
BEFORE INSERT ON Employee
FOR EACH ROW
BEGIN
    IF NEW.ManagerID IS NOT NULL AND NEW.ManagerID = NEW.EmployeeID THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Hierarchy Error: An employee cannot supervise themselves.';
    END IF;
END $$

CREATE TRIGGER trg_employee_prevent_cycle
BEFORE UPDATE ON Employee
FOR EACH ROW
BEGIN
    DECLARE curr_mgr INT;
    DECLARE hop_count INT DEFAULT 0;
    IF NEW.ManagerID IS NOT NULL AND NEW.ManagerID = NEW.EmployeeID THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Hierarchy Error: An employee cannot supervise themselves.';
    END IF;
    SET curr_mgr = NEW.ManagerID;
    WHILE curr_mgr IS NOT NULL AND hop_count < 100 DO
        IF curr_mgr = NEW.EmployeeID THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Hierarchy Error: Supervisory cycle detected.';
        END IF;
        SET curr_mgr = (SELECT ManagerID FROM Employee WHERE EmployeeID = curr_mgr LIMIT 1);
        SET hop_count = hop_count + 1;
    END WHILE;
END $$

-- 3. LeaveRequest Authorization: Prevent self-review & enforce active MANAGER/ADMIN role
CREATE TRIGGER trg_leaverequest_validate_insert
BEFORE INSERT ON LeaveRequest
FOR EACH ROW
BEGIN
    DECLARE reviewer_role VARCHAR(20);
    IF NEW.ReviewedBy IS NOT NULL THEN
        IF NEW.ReviewedBy = NEW.EmployeeID THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Authorization Error: An employee cannot review their own leave request.';
        END IF;
        SET reviewer_role = (
            SELECT Role FROM UserAccount
            WHERE EmployeeID = NEW.ReviewedBy AND AccountStatus = 'Active'
            LIMIT 1
        );
        IF reviewer_role IS NULL OR reviewer_role NOT IN ('MANAGER', 'ADMIN') THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Authorization Error: Reviewer must have an active MANAGER or ADMIN account.';
        END IF;
    END IF;
END $$

CREATE TRIGGER trg_leaverequest_validate_update
BEFORE UPDATE ON LeaveRequest
FOR EACH ROW
BEGIN
    DECLARE reviewer_role VARCHAR(20);
    IF NEW.ReviewedBy IS NOT NULL THEN
        IF NEW.ReviewedBy = NEW.EmployeeID THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Authorization Error: An employee cannot review their own leave request.';
        END IF;
        SET reviewer_role = (
            SELECT Role FROM UserAccount
            WHERE EmployeeID = NEW.ReviewedBy AND AccountStatus = 'Active'
            LIMIT 1
        );
        IF reviewer_role IS NULL OR reviewer_role NOT IN ('MANAGER', 'ADMIN') THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Authorization Error: Reviewer must have an active MANAGER or ADMIN account.';
        END IF;
    END IF;
END $$

DELIMITER ;
