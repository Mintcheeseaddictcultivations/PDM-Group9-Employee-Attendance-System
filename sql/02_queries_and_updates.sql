USE employee_attendance_db;

-- PART 1: DML OPERATIONS (INSERT, UPDATE, DELETE) ON MAIN TABLES

-- 1.1 INSERT: Schedule a new shift assignment for Employee 25 on 2026-11-25
INSERT INTO ShiftAssignment (ShiftAssignmentID, EmployeeID, ShiftID, WorkDate, AssignmentStatus)
VALUES (43, 25, 9, '2026-11-25', 'Scheduled');

SELECT * FROM ShiftAssignment WHERE ShiftAssignmentID = 43;

-- 1.2 UPDATE: Confirm the newly scheduled shift assignment
UPDATE ShiftAssignment
SET AssignmentStatus = 'Confirmed'
WHERE ShiftAssignmentID = 43;

SELECT * FROM ShiftAssignment WHERE ShiftAssignmentID = 43;

-- 1.3 DELETE: Remove the temporary shift assignment
DELETE FROM ShiftAssignment
WHERE ShiftAssignmentID = 43;

SELECT COUNT(*) AS RemainingAfterDelete FROM ShiftAssignment WHERE ShiftAssignmentID = 43;

-- 1.4 DML on Employee (INSERT, UPDATE, DELETE)
INSERT INTO Employee (EmployeeID, FullName, Ssn, Sex, Phone, Email, HireDate, EmploymentStatus, DepartmentID, PositionID, ManagerID)
VALUES (99, 'Nguyen Van Test', '999999999', 'M', '0909999999', 'test.nv@company.vn', '2026-11-19', 'Active', 2, 5, 1);
UPDATE Employee SET Phone = '0908888888', EmploymentStatus = 'OnLeave' WHERE EmployeeID = 99;
DELETE FROM Employee WHERE EmployeeID = 99;

-- 1.5 DML on Attendance (INSERT, UPDATE, DELETE)
INSERT INTO Attendance (AttendanceID, ShiftAssignmentID, ActualCheckIn, ActualCheckOut, AttendanceStatus, OvertimeHours, Notes)
VALUES (99, 21, '2026-11-19 08:28:00', '2026-11-19 17:35:00', 'Present', 0.50, 'Completed support tickets');
UPDATE Attendance SET OvertimeHours = 1.00, Notes = 'Updated overtime after manager verification' WHERE AttendanceID = 99;
DELETE FROM Attendance WHERE AttendanceID = 99;

-- 1.6 DML on LeaveRequest (INSERT, UPDATE, DELETE)
INSERT INTO LeaveRequest (LeaveRequestID, EmployeeID, LeaveType, StartDate, EndDate, Reason, SubmittedAt, RequestStatus)
VALUES (99, 10, 'Personal', '2026-12-15', '2026-12-15', 'Family errand', '2026-11-19 10:00:00', 'Pending');
UPDATE LeaveRequest SET Reason = 'Updated reason: medical checkup' WHERE LeaveRequestID = 99;
DELETE FROM LeaveRequest WHERE LeaveRequestID = 99;

-- PART 2: TRANSACTIONS (COMMIT & ROLLBACK ACROSS MULTIPLE TABLES)

-- 2.1 COMMIT Example: Manager (EmployeeID = 1) approves LeaveRequestID = 4
-- (EmployeeID = 9, Annual leave from 2026-12-10 to 2026-12-12 = 3 days)
-- and updates LeaveBalance.UsedDays for Employee 9 in 2026 simultaneously.
START TRANSACTION;

UPDATE LeaveRequest
SET RequestStatus = 'Approved',
    ReviewedBy = 1,
    ReviewedAt = '2026-11-21 10:00:00'
WHERE LeaveRequestID = 4
  AND RequestStatus = 'Pending';

UPDATE LeaveBalance
SET UsedDays = UsedDays + (DATEDIFF('2026-12-12', '2026-12-10') + 1)
WHERE EmployeeID = 9
  AND LeaveYear = 2026
  AND LeaveType = 'Annual';

COMMIT;

-- Verify committed changes in both tables
SELECT LeaveRequestID, EmployeeID, LeaveType, RequestStatus, ReviewedBy, ReviewedAt
FROM LeaveRequest WHERE LeaveRequestID = 4;

SELECT EmployeeID, LeaveYear, LeaveType, AllocatedDays, UsedDays,
       (AllocatedDays - UsedDays) AS RemainingDays
FROM LeaveBalance WHERE EmployeeID = 9 AND LeaveYear = 2026;

-- 2.2 ROLLBACK Example: Attempt to approve LeaveRequestID = 7 (Unpaid, 3 days)
-- for Employee 12, then roll back before committing.
START TRANSACTION;

UPDATE LeaveRequest
SET RequestStatus = 'Approved',
    ReviewedBy = 2,
    ReviewedAt = '2026-11-21 10:15:00'
WHERE LeaveRequestID = 7;

UPDATE LeaveBalance
SET UsedDays = UsedDays + 3.00
WHERE EmployeeID = 12 AND LeaveYear = 2026 AND LeaveType = 'Unpaid';

ROLLBACK;

-- Verify LeaveRequestID = 7 remains 'Pending'
SELECT LeaveRequestID, EmployeeID, RequestStatus, ReviewedBy
FROM LeaveRequest WHERE LeaveRequestID = 7;

-- PART 3: VIEW CREATION & QUERY

CREATE OR REPLACE VIEW vw_EmployeeAttendanceSummary AS
SELECT e.EmployeeID,
       e.FullName,
       d.DepartmentName,
       COUNT(a.AttendanceID) AS TotalShiftsRecorded,
       SUM(CASE WHEN a.AttendanceStatus = 'Present' THEN 1 ELSE 0 END) AS PresentDays,
       SUM(CASE WHEN a.AttendanceStatus = 'Late' THEN 1 ELSE 0 END) AS LateDays,
       SUM(CASE WHEN a.AttendanceStatus = 'Absent' THEN 1 ELSE 0 END) AS AbsentDays,
       COALESCE(SUM(a.OvertimeHours), 0.00) AS TotalOvertimeHours
FROM Employee e
JOIN Department d ON e.DepartmentID = d.DepartmentID
LEFT JOIN ShiftAssignment sa ON e.EmployeeID = sa.EmployeeID
LEFT JOIN Attendance a ON sa.ShiftAssignmentID = a.ShiftAssignmentID
GROUP BY e.EmployeeID, e.FullName, d.DepartmentName;

SELECT * FROM vw_EmployeeAttendanceSummary
WHERE TotalShiftsRecorded > 0
ORDER BY TotalOvertimeHours DESC, LateDays DESC;

-- PART 4: 12 SELECT QUERIES (5 MANDATORY TOPIC 15 + 7 CUSTOM QUERIES)

-- Q1 [Mandatory 1 - Multi-table JOIN]:
-- Retrieve the shift roster of one department ('Store Operations', DepartmentID = 3)
-- for next week ('2026-11-23' to '2026-11-29').
SELECT sa.WorkDate, d.DepartmentName, e.EmployeeID, e.FullName,
       p.PositionName, s.ShiftName, s.StartTime, s.EndTime, sa.AssignmentStatus
FROM ShiftAssignment sa
JOIN Employee e ON sa.EmployeeID = e.EmployeeID
JOIN Department d ON e.DepartmentID = d.DepartmentID
JOIN `Position` p ON e.PositionID = p.PositionID
JOIN Shift s ON sa.ShiftID = s.ShiftID
WHERE d.DepartmentID = 3
  AND sa.WorkDate BETWEEN '2026-11-23' AND '2026-11-29'
ORDER BY sa.WorkDate, s.StartTime, e.EmployeeID;

-- Q2 [Mandatory 2 - GROUP BY ... HAVING]:
-- Find employees who clocked in late more than three times (> 3) last month (October 2026).
SELECT e.EmployeeID, e.FullName, d.DepartmentName, COUNT(a.AttendanceID) AS LateCount
FROM Attendance a
JOIN ShiftAssignment sa ON a.ShiftAssignmentID = sa.ShiftAssignmentID
JOIN Employee e ON sa.EmployeeID = e.EmployeeID
JOIN Department d ON e.DepartmentID = d.DepartmentID
WHERE a.AttendanceStatus = 'Late'
  AND sa.WorkDate BETWEEN '2026-10-01' AND '2026-10-31'
GROUP BY e.EmployeeID, e.FullName, d.DepartmentName
HAVING COUNT(a.AttendanceID) > 3
ORDER BY LateCount DESC;

-- Q3 [Mandatory 3 - Multi-table JOIN + Aggregation]:
-- Calculate total overtime hours per department for this quarter (Q4 2026: Oct–Dec 2026).
SELECT d.DepartmentID, d.DepartmentName,
       COUNT(a.AttendanceID) AS RecordedShifts,
       COALESCE(SUM(a.OvertimeHours), 0.00) AS TotalOvertimeHours
FROM Department d
JOIN Employee e ON d.DepartmentID = e.DepartmentID
JOIN ShiftAssignment sa ON e.EmployeeID = sa.EmployeeID
JOIN Attendance a ON sa.ShiftAssignmentID = a.ShiftAssignmentID
WHERE sa.WorkDate BETWEEN '2026-10-01' AND '2026-12-31'
GROUP BY d.DepartmentID, d.DepartmentName
HAVING SUM(a.OvertimeHours) > 0
ORDER BY TotalOvertimeHours DESC;

-- Q4 [Mandatory 4 - Subquery + PublicHoliday exclusion]:
-- List leave requests still awaiting approval ('Pending') after more than 5 working days
-- (excluding Saturdays, Sundays, and PublicHoliday dates) as of '2026-11-21'.
SELECT lr.LeaveRequestID, e.EmployeeID, e.FullName, d.DepartmentName,
       lr.LeaveType, lr.SubmittedAt, lr.StartDate, lr.EndDate,
       DATEDIFF('2026-11-21', DATE(lr.SubmittedAt)) AS CalendarDaysWaiting
FROM LeaveRequest lr
JOIN Employee e ON lr.EmployeeID = e.EmployeeID
JOIN Department d ON e.DepartmentID = d.DepartmentID
WHERE lr.RequestStatus = 'Pending'
  AND (
      SELECT COUNT(*)
      FROM (
          SELECT DATE_ADD(DATE(lr.SubmittedAt), INTERVAL seq.n DAY) AS CheckDate
          FROM (
              SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
              UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
              UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11
              UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
              UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19
              UNION ALL SELECT 20
          ) seq
          WHERE DATE_ADD(DATE(lr.SubmittedAt), INTERVAL seq.n DAY) > DATE(lr.SubmittedAt)
            AND DATE_ADD(DATE(lr.SubmittedAt), INTERVAL seq.n DAY) <= '2026-11-21'
      ) dates
      WHERE DAYOFWEEK(dates.CheckDate) NOT IN (1, 7)
        AND dates.CheckDate NOT IN (SELECT HolidayDate FROM PublicHoliday)
  ) > 5
ORDER BY lr.SubmittedAt ASC;

-- Q5 [Mandatory 5 - Relational Division using Double NOT EXISTS ("for all")]:
-- Find employees who were present ('Present') on every working day of the month
-- (all scheduled working days in November 2026 up to '2026-11-20', excluding PublicHoliday).
SELECT e.EmployeeID, e.FullName, d.DepartmentName
FROM Employee e
JOIN Department d ON e.DepartmentID = d.DepartmentID
WHERE NOT EXISTS (
    SELECT DISTINCT sa_cal.WorkDate
    FROM ShiftAssignment sa_cal
    WHERE sa_cal.WorkDate BETWEEN '2026-11-01' AND '2026-11-20'
      AND DAYOFWEEK(sa_cal.WorkDate) NOT IN (1, 7)
      AND sa_cal.WorkDate NOT IN (SELECT HolidayDate FROM PublicHoliday)
      AND NOT EXISTS (
          SELECT 1
          FROM ShiftAssignment sa_emp
          JOIN Attendance a ON sa_emp.ShiftAssignmentID = a.ShiftAssignmentID
          WHERE sa_emp.EmployeeID = e.EmployeeID
            AND sa_emp.WorkDate = sa_cal.WorkDate
            AND a.AttendanceStatus = 'Present'
      )
);

-- Q6 [LEFT OUTER JOIN + GROUP BY ... HAVING]:
-- Detect understaffed shifts where active assignments are fewer than RequiredEmployees.
SELECT sr.WorkDate, s.ShiftName, sr.RequiredEmployees,
       COUNT(sa.ShiftAssignmentID) AS AssignedCount,
       (sr.RequiredEmployees - COUNT(sa.ShiftAssignmentID)) AS StaffingShortage
FROM ShiftRequirement sr
JOIN Shift s ON sr.ShiftID = s.ShiftID
LEFT JOIN ShiftAssignment sa
       ON sr.ShiftID = sa.ShiftID
      AND sr.WorkDate = sa.WorkDate
      AND sa.AssignmentStatus <> 'Cancelled'
GROUP BY sr.WorkDate, s.ShiftName, sr.RequiredEmployees
HAVING COUNT(sa.ShiftAssignmentID) < sr.RequiredEmployees
ORDER BY sr.WorkDate, StaffingShortage DESC;

-- Q7 [Set Operation - UNION ALL]:
-- Compare estimated monthly base pay and expected monthly hours across
-- Full-Time and Part-Time employees.
SELECT e.EmployeeID, e.FullName, d.DepartmentName, 'Full-Time' AS ContractType,
       ft.MonthlyBaseSalary AS EstimatedMonthlyPay,
       ft.StandardMonthlyHours AS ExpectedMonthlyHours
FROM Employee e
JOIN Department d ON e.DepartmentID = d.DepartmentID
JOIN FullTime ft ON e.EmployeeID = ft.EmployeeID
UNION ALL
SELECT e.EmployeeID, e.FullName, d.DepartmentName, 'Part-Time' AS ContractType,
       (pt.HourlyRate * pt.ExpectedWeeklyHours * 4) AS EstimatedMonthlyPay,
       (pt.ExpectedWeeklyHours * 4) AS ExpectedMonthlyHours
FROM Employee e
JOIN Department d ON e.DepartmentID = d.DepartmentID
JOIN PartTime pt ON e.EmployeeID = pt.EmployeeID
ORDER BY EstimatedMonthlyPay DESC;

-- Q8 [Self-Join + LEFT OUTER JOIN]:
-- List every employee with their department, position, and direct supervisor's name.
SELECT e.EmployeeID, e.FullName AS EmployeeName, d.DepartmentName, p.PositionName,
       COALESCE(m.FullName, 'Executive / No Supervisor') AS SupervisorName
FROM Employee e
JOIN Department d ON e.DepartmentID = d.DepartmentID
JOIN `Position` p ON e.PositionID = p.PositionID
LEFT JOIN Employee m ON e.ManagerID = m.EmployeeID
ORDER BY e.EmployeeID;

-- Q9 [Subquery with IN]:
-- List employees who have at least one Approved leave request in 2026,
-- along with their remaining Annual leave balance.
SELECT e.EmployeeID, e.FullName, d.DepartmentName,
       lb.AllocatedDays, lb.UsedDays, (lb.AllocatedDays - lb.UsedDays) AS RemainingAnnualDays
FROM Employee e
JOIN Department d ON e.DepartmentID = d.DepartmentID
LEFT JOIN LeaveBalance lb
       ON e.EmployeeID = lb.EmployeeID AND lb.LeaveYear = 2026 AND lb.LeaveType = 'Annual'
WHERE e.EmployeeID IN (
    SELECT DISTINCT EmployeeID
    FROM LeaveRequest
    WHERE RequestStatus = 'Approved'
)
ORDER BY e.EmployeeID;

-- Q10 [Subquery with EXISTS]:
-- Find departments and positions that still have open headcount slots
-- (CurrentHeadcount < MaxHeadcount).
SELECT d.DepartmentID, d.DepartmentName, p.PositionName,
       dp.MaxHeadcount, COUNT(e.EmployeeID) AS CurrentHeadcount,
       (dp.MaxHeadcount - COUNT(e.EmployeeID)) AS AvailableSlots
FROM DepartmentPosition dp
JOIN Department d ON dp.DepartmentID = d.DepartmentID
JOIN `Position` p ON dp.PositionID = p.PositionID
LEFT JOIN Employee e ON e.DepartmentID = dp.DepartmentID AND e.PositionID = dp.PositionID
WHERE EXISTS (
    SELECT 1
    FROM Employee e2
    WHERE e2.DepartmentID = d.DepartmentID
      AND e2.EmploymentStatus = 'Active'
)
GROUP BY d.DepartmentID, d.DepartmentName, p.PositionName, dp.MaxHeadcount
HAVING COUNT(e.EmployeeID) < dp.MaxHeadcount
ORDER BY AvailableSlots DESC;

-- Q11 [LEFT OUTER JOIN - Anti-Join pattern]:
-- Identify active employees who do not yet have a system UserAccount.
SELECT e.EmployeeID, e.FullName, e.Email, d.DepartmentName, p.PositionName
FROM Employee e
JOIN Department d ON e.DepartmentID = d.DepartmentID
JOIN `Position` p ON e.PositionID = p.PositionID
LEFT JOIN UserAccount ua ON e.EmployeeID = ua.EmployeeID
WHERE e.EmploymentStatus = 'Active'
  AND ua.UserID IS NULL
ORDER BY e.EmployeeID;

-- Q12 [5-Table JOIN on Weak Entity]:
-- Retrieve the chronological clock-in and clock-out event log with device locations
-- for all shifts worked on '2026-11-16'.
SELECT a.AttendanceID, e.FullName, s.ShiftName,
       ae.EventNo, ae.EventType, ae.EventTimestamp, ae.DeviceLocation
FROM AttendanceEvent ae
JOIN Attendance a ON ae.AttendanceID = a.AttendanceID
JOIN ShiftAssignment sa ON a.ShiftAssignmentID = sa.ShiftAssignmentID
JOIN Employee e ON sa.EmployeeID = e.EmployeeID
JOIN Shift s ON sa.ShiftID = s.ShiftID
WHERE sa.WorkDate = '2026-11-16'
ORDER BY a.AttendanceID, ae.EventNo;