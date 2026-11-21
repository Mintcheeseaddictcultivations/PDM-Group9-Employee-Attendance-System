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
 * Browse Form (Section 2.4): Displays all rows of any selected database table
 * in a JTable grid along with the total row count.
 */
public class BrowsePanel extends JPanel {

    private static final Map<String, String> ALLOWED_TABLE_QUERIES = new LinkedHashMap<>();

    static {
        ALLOWED_TABLE_QUERIES.put("Department", "SELECT * FROM Department ORDER BY DepartmentID");
        ALLOWED_TABLE_QUERIES.put("Position", "SELECT * FROM `Position` ORDER BY PositionID");
        ALLOWED_TABLE_QUERIES.put("DepartmentPosition", "SELECT * FROM DepartmentPosition ORDER BY DepartmentID, PositionID");
        ALLOWED_TABLE_QUERIES.put("Employee", "SELECT * FROM Employee ORDER BY EmployeeID");
        ALLOWED_TABLE_QUERIES.put("FullTime", "SELECT * FROM FullTime ORDER BY EmployeeID");
        ALLOWED_TABLE_QUERIES.put("PartTime", "SELECT * FROM PartTime ORDER BY EmployeeID");
        ALLOWED_TABLE_QUERIES.put("UserAccount", "SELECT UserID, EmployeeID, Username, Role, AccountStatus, CreatedAt, LastLoginAt FROM UserAccount ORDER BY UserID");
        ALLOWED_TABLE_QUERIES.put("Shift", "SELECT * FROM Shift ORDER BY ShiftID");
        ALLOWED_TABLE_QUERIES.put("ShiftRequirement", "SELECT * FROM ShiftRequirement ORDER BY WorkDate, ShiftID");
        ALLOWED_TABLE_QUERIES.put("ShiftAssignment", "SELECT * FROM ShiftAssignment ORDER BY ShiftAssignmentID");
        ALLOWED_TABLE_QUERIES.put("Attendance", "SELECT * FROM Attendance ORDER BY AttendanceID");
        ALLOWED_TABLE_QUERIES.put("AttendanceEvent", "SELECT * FROM AttendanceEvent ORDER BY AttendanceID, EventNo");
        ALLOWED_TABLE_QUERIES.put("LeaveBalance", "SELECT * FROM LeaveBalance ORDER BY EmployeeID, LeaveYear, LeaveType");
        ALLOWED_TABLE_QUERIES.put("LeaveRequest", "SELECT * FROM LeaveRequest ORDER BY LeaveRequestID");
        ALLOWED_TABLE_QUERIES.put("PublicHoliday", "SELECT * FROM PublicHoliday ORDER BY HolidayDate");
    }

    private final JComboBox<String> tableCombo = new JComboBox<>(ALLOWED_TABLE_QUERIES.keySet().toArray(new String[0]));
    private final JLabel rowCountLabel = new JLabel("Total rows: 0");
    private final JTable dataTable = UiHelpers.createDataTable();
    private final JTextArea sqlPreview = UiHelpers.createSqlPreviewArea(3);

    public BrowsePanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        JButton loadBtn = new JButton("Load Table");
        topBar.add(new JLabel("Select Table:"));
        topBar.add(tableCombo);
        topBar.add(loadBtn);
        topBar.add(rowCountLabel);

        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(dataTable), BorderLayout.CENTER);
        add(UiHelpers.wrapSqlPreview(sqlPreview), BorderLayout.SOUTH);

        loadBtn.addActionListener(e -> loadSelectedTable());
        tableCombo.addActionListener(e -> loadSelectedTable());
    }

    public void loadSelectedTable() {
        String selected = (String) tableCombo.getSelectedItem();
        if (selected == null) {
            return;
        }
        String sql = ALLOWED_TABLE_QUERIES.get(selected);
        if (sql == null) {
            return;
        }
        sqlPreview.setText(sql);
        UiHelpers.executeQueryToTable(this, dataTable, rowCountLabel, "browse table " + selected, sql);
    }
}
