package com.group9.attendance.ui;

import com.group9.attendance.util.UiHelpers;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

/**
 * Report Screen (Section 2.4): Executes aggregate SQL reports (department
 * attendance & overtime totals, understaffed shifts, and ISA compensation summary).
 */
public class ReportPanel extends JPanel {

    private static final Map<String, String> REPORTS = new LinkedHashMap<>();

    static {
        REPORTS.put(
            "1. Department Attendance & Overtime Summary",
            "SELECT d.DepartmentID, d.DepartmentName, "
            + "COUNT(a.AttendanceID) AS TotalAttendanceRecords, "
            + "SUM(CASE WHEN a.AttendanceStatus = 'Present' THEN 1 ELSE 0 END) AS PresentCount, "
            + "SUM(CASE WHEN a.AttendanceStatus = 'Late' THEN 1 ELSE 0 END) AS LateCount, "
            + "SUM(CASE WHEN a.AttendanceStatus = 'Absent' THEN 1 ELSE 0 END) AS AbsentCount, "
            + "COALESCE(SUM(a.OvertimeHours), 0.00) AS TotalOvertimeHours "
            + "FROM Department d "
            + "LEFT JOIN Employee e ON d.DepartmentID = e.DepartmentID "
            + "LEFT JOIN ShiftAssignment sa ON e.EmployeeID = sa.EmployeeID "
            + "LEFT JOIN Attendance a ON sa.ShiftAssignmentID = a.ShiftAssignmentID "
            + "GROUP BY d.DepartmentID, d.DepartmentName "
            + "ORDER BY TotalAttendanceRecords DESC, d.DepartmentName ASC"
        );
        REPORTS.put(
            "2. Understaffed Shift Requirements (GROUP BY ... HAVING)",
            "SELECT sr.WorkDate, s.ShiftName, sr.RequiredEmployees, "
            + "COUNT(sa.ShiftAssignmentID) AS AssignedEmployees, "
            + "(sr.RequiredEmployees - COUNT(sa.ShiftAssignmentID)) AS StaffingGap "
            + "FROM ShiftRequirement sr "
            + "JOIN Shift s ON sr.ShiftID = s.ShiftID "
            + "LEFT JOIN ShiftAssignment sa ON sr.ShiftID = sa.ShiftID "
            + "AND sr.WorkDate = sa.WorkDate AND sa.AssignmentStatus <> 'Cancelled' "
            + "GROUP BY sr.WorkDate, s.ShiftName, sr.RequiredEmployees "
            + "HAVING COUNT(sa.ShiftAssignmentID) < sr.RequiredEmployees "
            + "ORDER BY sr.WorkDate, s.ShiftName"
        );
        REPORTS.put(
            "3. Full-Time vs Part-Time Compensation Overview (UNION ALL)",
            "SELECT e.EmployeeID, e.FullName, 'Full-Time' AS EmploymentType, "
            + "ft.MonthlyBaseSalary AS EstimatedMonthlyPay, ft.StandardMonthlyHours AS ExpectedMonthlyHours "
            + "FROM Employee e JOIN FullTime ft ON e.EmployeeID = ft.EmployeeID "
            + "UNION ALL "
            + "SELECT e.EmployeeID, e.FullName, 'Part-Time' AS EmploymentType, "
            + "(pt.HourlyRate * pt.ExpectedWeeklyHours * 4) AS EstimatedMonthlyPay, (pt.ExpectedWeeklyHours * 4) AS ExpectedMonthlyHours "
            + "FROM Employee e JOIN PartTime pt ON e.EmployeeID = pt.EmployeeID "
            + "ORDER BY EmployeeID"
        );
    }

    private final JComboBox<String> reportCombo = new JComboBox<>(REPORTS.keySet().toArray(new String[0]));
    private final JLabel countLabel = new JLabel("Total rows: 0");
    private final JTable reportTable = UiHelpers.createDataTable();
    private final JTextArea sqlPreview = UiHelpers.createSqlPreviewArea(5);

    public ReportPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        JButton runBtn = new JButton("Generate Report");
        topBar.add(new JLabel("Aggregate Report:"));
        topBar.add(reportCombo);
        topBar.add(runBtn);
        topBar.add(countLabel);

        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(reportTable), BorderLayout.CENTER);
        add(UiHelpers.wrapSqlPreview(sqlPreview), BorderLayout.SOUTH);

        runBtn.addActionListener(e -> runSelectedReport());
        reportCombo.addActionListener(e -> runSelectedReport());
    }

    public void runSelectedReport() {
        String key = (String) reportCombo.getSelectedItem();
        if (key == null) {
            return;
        }
        String sql = REPORTS.get(key);
        if (sql == null) {
            return;
        }
        sqlPreview.setText(sql);
        UiHelpers.executeQueryToTable(this, reportTable, countLabel, "run report: " + key, sql);
    }
}
