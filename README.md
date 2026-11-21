# Employee Attendance and Shift Scheduling System (PDM — Group 9)

**Course:** IT079IU – Principles of Database Management (Semester 1, 2026–2027)  
**Institution:** International University – Vietnam National University HCMC  
**Instructor:** Assoc. Prof. Dr. Nguyễn Thị Thúy Loan  
**Topic:** Topic 15 — Employee Attendance and Shift Scheduling

---

## Group 9 Members

| No. | Full Name | Student ID |
| :-: | :--- | :--- |
| 1 | Đỗ Phan Minh Trí *(Leader)* | ITITMQ25003 |
| 2 | Đinh Tường Ngân | ITITIU21252 |
| 3 | Trần Huy Trường | ITITIU24071 |
| 4 | Nguyễn Phạm Nhật Minh | ITDSIU25022 |
| 5 | Lê Văn Nhân | ITITIU24041 |
| 6 | Đinh Quốc Bình | ITDSIU24008 |
| 7 | Trần Hoàng Doanh | ITITWE23016 |
| 8 | Huỳnh Đỗ Đình Nguyên | ITDSIU25024 |
| 9 | Lê Tấn Thịnh | ITCSIU22239 |
| 10 | Lý Khải Minh | ITITWE22173 |

---

## Repository Structure

```text
PDM-Group9-Employee-Attendance-System/
├── java-app/                        # Java Swing + JDBC Desktop Application (JDK 27 + MySQL Connector/J 8.2.0)
│   ├── src/com/group9/attendance/   # UI panels, DAOs, DBConnection, SqlErrorHandler
│   ├── lib/mysql-connector-j-8.2.0.jar
│   ├── config.properties.example    # Template for local DB credentials
│   └── README.md                    # Detailed Java app guide
├── sql/                             # MySQL 8.4 DDL, Triggers, Sample Data & Queries
│   ├── PDM_Database_Group9.sql      # All-in-one script (15 tables + sample data + 6 triggers + view)
│   ├── 01_schema_and_data.sql       # DDL, secondary indexes, and 314 sample rows (>= 15 rows/table)
│   ├── 01b_triggers.sql             # 6 business-rule triggers (ISA disjointness, cycle guard, reviewer role)
│   └── 02_queries_and_updates.sql   # DML, COMMIT/ROLLBACK transactions, View, and Q1–Q12
```

---

## Quick Start

### 1. Database Setup (MySQL 8.0 / 8.4)
Run the all-in-one script in MySQL CLI or DBeaver/Workbench:

```bash
mysql -u root -p < sql/PDM_Database_Group9.sql
```

*(Alternatively, run `sql/01_schema_and_data.sql` followed by `sql/01b_triggers.sql`, then explore `sql/02_queries_and_updates.sql`.)*

### 2. Configure & Run Java Desktop Application
```bash
cd java-app
cp config.properties.example config.properties
# Edit config.properties with your local MySQL username and password

mkdir -p bin
javac -encoding UTF-8 -cp "lib/mysql-connector-j-8.2.0.jar" -d bin $(find src -name "*.java")
java -cp "bin:lib/mysql-connector-j-8.2.0.jar" com.group9.attendance.App
```
