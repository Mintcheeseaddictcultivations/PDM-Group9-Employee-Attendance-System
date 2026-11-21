# Employee Attendance and Shift Scheduling — Java Desktop Application

**Course:** IT079IU – Principles of Database Management (Semester 1, 2026–2027)  
**Group 9 – Topic 15:** Employee Attendance and Shift Scheduling

---

## 1. Environment & Dependencies

* **JDK Version:** JDK 17 or newer (compiled and verified on **JDK 27**, Oracle HotSpot 64-Bit).
* **GUI Framework:** Java Swing (included in the standard JDK; no external UI runtime required).
* **DBMS:** MySQL 8.0+ (`employee_attendance_db`, created using `../01_schema_and_data.sql`).
* **JDBC Driver:** MySQL Connector/J **8.2.0** (`lib/mysql-connector-j-8.2.0.jar`, already bundled in `lib/`).

---

## 2. Database Setup & Configuration

1. Run `../01_schema_and_data.sql` against your MySQL 8.0+ server to create `employee_attendance_db` and populate sample data.
   * Optionally run `../01b_triggers.sql` using MySQL Workbench or the MySQL CLI to activate database-level trigger checks (`trg_fulltime_disjoint_insert`, `trg_employee_prevent_cycle`, `trg_leaverequest_validate_*`).
2. Edit **`config.properties`** in `java-app/` (external to the compiled code, so no recompilation is needed when switching servers):
   ```properties
   db.url=jdbc:mysql://localhost:3306/employee_attendance_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
   db.user=root
   db.password=YOUR_MYSQL_PASSWORD
   ```

---

## 3. Build & Run Instructions

### Option A: Visual Studio Code (Recommended)
1. Install **Extension Pack for Java** in VS Code.
2. Open the **`java-app`** folder (`File -> Open Folder... -> .../pdm-group9-final/java-app`).
3. Ensure `.vscode/settings.json` detects `src/` and `lib/*.jar`.
4. Open `src/com/group9/attendance/App.java` and click **Run** (or press `F5`).

### Option B: Command Line / Terminal

#### macOS / Linux
```bash
cd java-app
mkdir -p bin
javac -d bin -cp "lib/*" $(find src -name "*.java")
java -cp "bin:lib/*" com.group9.attendance.App
```

#### Windows (PowerShell / CMD)
```powershell
cd java-app
mkdir bin
dir /s /B src\*.java > sources.txt
javac -d bin -cp "lib\*" @sources.txt
java -cp "bin;lib\*" com.group9.attendance.App
```

---

## 4. Application Screens & Rubric Compliance (Section 2.4)

1. **Browse Tables (`BrowsePanel`)**: Select any of the 15 database tables (validated against an allowlist), view all rows in a `JTable`, and inspect total row count.
2. **Search Employees (`SearchPanel`)**: Parameterized filtering by keyword (`FullName` / `Email` / `Ssn`), department, and employment status; safe against apostrophes (e.g., `O'Connor`).
3. **Master–Detail (`MasterDetailPanel`)**: Selecting an employee in the upper grid dynamically loads their scheduled `ShiftAssignment` and `Attendance` records in the lower grid.
4. **Employee CRUD (`EmployeeCrudPanel`)**: `INSERT`, `UPDATE`, and `DELETE` (with confirmation prompt) plus client-side validation for required fields, 9-digit SSN, email format, and `YYYY-MM-DD` dates.
5. **Leave Request CRUD & Transaction (`LeaveRequestCrudPanel`)**: `INSERT` (checks `EndDate >= StartDate`), `UPDATE`, `DELETE` (with confirmation), and **Approve Selected (Transaction)** updating both `LeaveRequest` and `LeaveBalance` inside a single `COMMIT` / `ROLLBACK` transaction.
6. **Reports (`ReportPanel`)**: Aggregate statistics per department (`Present`, `Late`, `Absent`, `SUM(OvertimeHours)`), understaffed shifts (`GROUP BY ... HAVING`), and Full-Time vs Part-Time overview (`UNION ALL`).
7. **Topic 15 Queries (`TopicQueriesPanel`)**: Interactive execution for all **5/5** queries required under Topic 15 in Part 3, displaying SQL, row counts, and live results.

All database calls use `PreparedStatement` with `?` placeholders, close resources via `try-with-resources`, and translate `SQLException` constraint violations into plain-language dialogs.

---

## 5. Live Demo & Grading Test Tips (Section 2.4)

* **SQL Injection / Apostrophe Safety:** On Tab **2. Search Employees**, type `O'Connor` and click **Search** — `PreparedStatement` safely binds the quote as data with no syntax error.
* **Constraint Violation Messages:** On Tab **4. Employee CRUD**, try deleting Employee `#6` (`Dinh Quoc Binh`) — MySQL enforces `ON DELETE RESTRICT` (code `1451`) and the UI displays a plain-language dialog instead of a stack trace. Changing Department + Position to an invalid pair triggers `DepartmentPosition` composite FK error (`1452`).
* **Transaction `COMMIT` vs `ROLLBACK`:** On Tab **5. Leave CRUD & Transaction**:
  * Selecting Pending requests **`#3`, `#4`, or `#7`** (which have matching `LeaveBalance` entries) and clicking **Approve Selected (Transaction)** executes `COMMIT` across both `LeaveRequest` and `LeaveBalance`.
  * Selecting Pending request **`#14`** (Employee `#20`, who has no `LeaveBalance` row) automatically triggers `ROLLBACK` so `LeaveRequest #14` safely remains `Pending`.
