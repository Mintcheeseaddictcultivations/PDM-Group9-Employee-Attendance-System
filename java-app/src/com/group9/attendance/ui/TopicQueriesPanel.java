package com.group9.attendance.ui;

import com.group9.attendance.util.TableModelUtil.ComboItem;
import com.group9.attendance.util.UiHelpers;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Topic 15 Queries Screen (Section 2.4 & Part 3): Exposes all 5/5 required
 * queries for Topic 15 (Employee Attendance and Shift Scheduling) through
 * parameterized PreparedStatements.
 */
public class TopicQueriesPanel extends JPanel {

    private final JComboBox<ComboItem> deptCombo = new JComboBox<>();
    private final JTextField q1StartField = new JTextField("2026-11-23", 10);
    private final JTextField q1EndField = new JTextField("2026-11-29", 10);
    private final JTextField q2MonthField = new JTextField("2026-10", 8);
    private final JTextField q2MinLateField = new JTextField("3", 4);
    private final JTextField q3StartField = new JTextField("2026-10-01", 10);
    private final JTextField q3EndField = new JTextField("2026-12-31", 10);
    private final JTextField q4RefDateField = new JTextField("2026-11-19", 10);
    private final JTextField q4WaitDaysField = new JTextField("5", 4);
    private final JTextField q5StartField = new JTextField("2026-11-01", 10);
    private final JTextField q5EndField = new JTextField("2026-11-19", 10);

    private final JLabel countLabel = new JLabel("Total rows: 0");
    private final JTable resultTable = UiHelpers.createDataTable();
    private final JTextArea sqlPreview = UiHelpers.createSqlPreviewArea(6);

    public TopicQueriesPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel queriesPanel = new JPanel(new GridLayout(5, 1, 4, 4));
        queriesPanel.setBorder(BorderFactory.createTitledBorder("Topic 15 Required Queries (All 5/5 Accessible via PreparedStatement)"));

        // Q1 row
        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JButton b1 = new JButton("Run Q1: Next-Week Roster");
        r1.add(b1);
        r1.add(new JLabel("Department:"));
        r1.add(deptCombo);
        r1.add(new JLabel("From:"));
        r1.add(q1StartField);
        r1.add(new JLabel("To:"));
        r1.add(q1EndField);
        queriesPanel.add(r1);

        // Q2 row
        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JButton b2 = new JButton("Run Q2: Late > N Times Last Month");
        r2.add(b2);
        r2.add(new JLabel("Month (YYYY-MM):"));
        r2.add(q2MonthField);
        r2.add(new JLabel("More than N times:"));
        r2.add(q2MinLateField);
        queriesPanel.add(r2);

        // Q3 row
        JPanel r3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JButton b3 = new JButton("Run Q3: Total Overtime per Department");
        r3.add(b3);
        r3.add(new JLabel("Quarter Start:"));
        r3.add(q3StartField);
        r3.add(new JLabel("Quarter End:"));
        r3.add(q3EndField);
        queriesPanel.add(r3);

        // Q4 row
        JPanel r4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JButton b4 = new JButton("Run Q4: Pending Leaves > N Working Days");
        r4.add(b4);
        r4.add(new JLabel("As-Of Date:"));
        r4.add(q4RefDateField);
        r4.add(new JLabel("Min Working Days:"));
        r4.add(q4WaitDaysField);
        queriesPanel.add(r4);

        // Q5 row
        JPanel r5 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JButton b5 = new JButton("Run Q5: Present Every Working Day (NOT EXISTS)");
        r5.add(b5);
        r5.add(new JLabel("Month Start:"));
        r5.add(q5StartField);
        r5.add(new JLabel("Up To:"));
        r5.add(q5EndField);
        r5.add(countLabel);
        queriesPanel.add(r5);

        add(queriesPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultTable), BorderLayout.CENTER);
        add(UiHelpers.wrapSqlPreview(sqlPreview), BorderLayout.SOUTH);

        b1.addActionListener(e -> runQ1());
        b2.addActionListener(e -> runQ2());
        b3.addActionListener(e -> runQ3());
        b4.addActionListener(e -> runQ4());
        b5.addActionListener(e -> runQ5());
    }

    public void loadDepartments() {
        UiHelpers.loadComboItems(this, deptCombo, "SELECT DepartmentID, DepartmentName FROM Department ORDER BY DepartmentID", null);
        // Default to Store Operations (DepartmentID = 3) for Q1 demo data
        UiHelpers.selectComboById(deptCombo, 3);
    }

    private LocalDate[] parseDateRange(String startText, String endText, String label) {
        try {
            LocalDate s = LocalDate.parse(startText.trim());
            LocalDate e = LocalDate.parse(endText.trim());
            if (e.isBefore(s)) {
                JOptionPane.showMessageDialog(this, label + ": End date must be on or after start date.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return new LocalDate[] {s, e};
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, label + ": Use YYYY-MM-DD date format.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private void runQ1() {
        ComboItem dept = (ComboItem) deptCombo.getSelectedItem();
        LocalDate[] range = parseDateRange(q1StartField.getText(), q1EndField.getText(), "Q1");
        if (dept == null || range == null) {
            return;
        }
        String sql = "SELECT d.DepartmentName, sa.WorkDate, e.EmployeeID, e.FullName, "
            + "s.ShiftName, s.StartTime, s.EndTime, sa.AssignmentStatus "
            + "FROM ShiftAssignment sa "
            + "JOIN Employee e ON sa.EmployeeID = e.EmployeeID "
            + "JOIN Department d ON e.DepartmentID = d.DepartmentID "
            + "JOIN Shift s ON sa.ShiftID = s.ShiftID "
            + "WHERE d.DepartmentID = ? AND sa.WorkDate BETWEEN ? AND ? "
            + "AND sa.AssignmentStatus <> 'Cancelled' "
            + "ORDER BY sa.WorkDate, s.StartTime, e.EmployeeID";
        sqlPreview.setText(sql);
        UiHelpers.executeQueryToTable(
            this, resultTable, countLabel, "run Q1",
            sql, dept.id(), java.sql.Date.valueOf(range[0]), java.sql.Date.valueOf(range[1])
        );
    }

    private void runQ2() {
        String month = q2MonthField.getText().trim();
        if (!month.matches("\\d{4}-\\d{2}")) {
            JOptionPane.showMessageDialog(this, "Q2: Month must follow YYYY-MM format (e.g., 2026-10).", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int minLate;
        try {
            minLate = Integer.parseInt(q2MinLateField.getText().trim());
            if (minLate < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Q2: Minimum late count must be a non-negative integer.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sql = "SELECT e.EmployeeID, e.FullName, d.DepartmentName, COUNT(a.AttendanceID) AS LateCount "
            + "FROM Attendance a "
            + "JOIN ShiftAssignment sa ON a.ShiftAssignmentID = sa.ShiftAssignmentID "
            + "JOIN Employee e ON sa.EmployeeID = e.EmployeeID "
            + "JOIN Department d ON e.DepartmentID = d.DepartmentID "
            + "WHERE a.AttendanceStatus = 'Late' AND DATE_FORMAT(sa.WorkDate, '%Y-%m') = ? "
            + "GROUP BY e.EmployeeID, e.FullName, d.DepartmentName "
            + "HAVING COUNT(a.AttendanceID) > ? "
            + "ORDER BY LateCount DESC";
        sqlPreview.setText(sql);
        UiHelpers.executeQueryToTable(this, resultTable, countLabel, "run Q2", sql, month, minLate);
    }

    private void runQ3() {
        LocalDate[] range = parseDateRange(q3StartField.getText(), q3EndField.getText(), "Q3");
        if (range == null) {
            return;
        }
        String sql = "SELECT d.DepartmentID, d.DepartmentName, "
            + "COUNT(DISTINCT e.EmployeeID) AS ActiveStaffCount, "
            + "SUM(a.OvertimeHours) AS TotalOvertimeHours "
            + "FROM Department d "
            + "JOIN Employee e ON d.DepartmentID = e.DepartmentID "
            + "JOIN ShiftAssignment sa ON e.EmployeeID = sa.EmployeeID "
            + "JOIN Attendance a ON sa.ShiftAssignmentID = a.ShiftAssignmentID "
            + "WHERE sa.WorkDate BETWEEN ? AND ? "
            + "GROUP BY d.DepartmentID, d.DepartmentName "
            + "HAVING SUM(a.OvertimeHours) > 0 "
            + "ORDER BY TotalOvertimeHours DESC";
        sqlPreview.setText(sql);
        UiHelpers.executeQueryToTable(
            this, resultTable, countLabel, "run Q3",
            sql, java.sql.Date.valueOf(range[0]), java.sql.Date.valueOf(range[1])
        );
    }

    private void runQ4() {
        LocalDate refDate;
        try {
            refDate = LocalDate.parse(q4RefDateField.getText().trim());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Q4: As-Of Date must use YYYY-MM-DD format.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int minWorkingDays;
        try {
            minWorkingDays = Integer.parseInt(q4WaitDaysField.getText().trim());
            if (minWorkingDays < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Q4: Min working days must be a non-negative integer.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "WITH RECURSIVE CalendarDates AS ("
            + "  SELECT DATE_SUB(?, INTERVAL 180 DAY) AS CalDate "
            + "  UNION ALL "
            + "  SELECT DATE_ADD(CalDate, INTERVAL 1 DAY) FROM CalendarDates WHERE CalDate < ?"
            + "), WorkingDays AS ("
            + "  SELECT c.CalDate FROM CalendarDates c "
            + "  WHERE DAYOFWEEK(c.CalDate) NOT IN (1, 7) "
            + "    AND NOT EXISTS (SELECT 1 FROM PublicHoliday ph WHERE ph.HolidayDate = c.CalDate AND ph.IsPaidHoliday = TRUE)"
            + ") "
            + "SELECT lr.LeaveRequestID, e.FullName AS Applicant, d.DepartmentName, "
            + "lr.LeaveType, lr.SubmittedAt, lr.StartDate, lr.EndDate, "
            + "(SELECT COUNT(*) FROM WorkingDays wd WHERE wd.CalDate > DATE(lr.SubmittedAt) AND wd.CalDate <= ?) AS WorkingDaysWaiting "
            + "FROM LeaveRequest lr "
            + "JOIN Employee e ON lr.EmployeeID = e.EmployeeID "
            + "JOIN Department d ON e.DepartmentID = d.DepartmentID "
            + "WHERE lr.RequestStatus = 'Pending' "
            + "  AND (SELECT COUNT(*) FROM WorkingDays wd WHERE wd.CalDate > DATE(lr.SubmittedAt) AND wd.CalDate <= ?) > ? "
            + "ORDER BY lr.SubmittedAt ASC";
        sqlPreview.setText(sql);
        java.sql.Date sqlRef = java.sql.Date.valueOf(refDate);
        UiHelpers.executeQueryToTable(
            this, resultTable, countLabel, "run Q4",
            sql, sqlRef, sqlRef, sqlRef, sqlRef, minWorkingDays
        );
    }

    private void runQ5() {
        LocalDate[] range = parseDateRange(q5StartField.getText(), q5EndField.getText(), "Q5");
        if (range == null) {
            return;
        }
        String sql = "SELECT e.EmployeeID, e.FullName, d.DepartmentName "
            + "FROM Employee e "
            + "JOIN Department d ON e.DepartmentID = d.DepartmentID "
            + "WHERE e.EmploymentStatus = 'Active' "
            + "  AND EXISTS ("
            + "      SELECT 1 FROM ShiftAssignment sa_check "
            + "      WHERE sa_check.WorkDate BETWEEN ? AND ? "
            + "        AND DAYOFWEEK(sa_check.WorkDate) NOT IN (1, 7) "
            + "        AND NOT EXISTS (SELECT 1 FROM PublicHoliday ph2 WHERE ph2.HolidayDate = sa_check.WorkDate AND ph2.IsPaidHoliday = TRUE)"
            + "  ) "
            + "  AND NOT EXISTS ("
            + "      SELECT DISTINCT sa_days.WorkDate "
            + "      FROM ShiftAssignment sa_days "
            + "      WHERE sa_days.WorkDate BETWEEN ? AND ? "
            + "        AND DAYOFWEEK(sa_days.WorkDate) NOT IN (1, 7) "
            + "        AND NOT EXISTS (SELECT 1 FROM PublicHoliday ph WHERE ph.HolidayDate = sa_days.WorkDate AND ph.IsPaidHoliday = TRUE) "
            + "        AND NOT EXISTS ("
            + "            SELECT 1 FROM ShiftAssignment sa "
            + "            JOIN Attendance a ON sa.ShiftAssignmentID = a.ShiftAssignmentID "
            + "            WHERE sa.EmployeeID = e.EmployeeID "
            + "              AND sa.WorkDate = sa_days.WorkDate "
            + "              AND a.AttendanceStatus = 'Present'"
            + "        )"
            + "  )";
        sqlPreview.setText(sql);
        java.sql.Date sqlStart = java.sql.Date.valueOf(range[0]);
        java.sql.Date sqlEnd = java.sql.Date.valueOf(range[1]);
        UiHelpers.executeQueryToTable(
            this, resultTable, countLabel, "run Q5",
            sql, sqlStart, sqlEnd, sqlStart, sqlEnd
        );
    }
}
