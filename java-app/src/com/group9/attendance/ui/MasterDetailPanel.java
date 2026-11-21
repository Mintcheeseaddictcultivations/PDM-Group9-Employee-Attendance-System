package com.group9.attendance.ui;

import com.group9.attendance.util.UiHelpers;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

/**
 * Master-Detail Form (Section 2.4): Selecting an employee in the upper master
 * grid dynamically loads that employee's shift assignments and attendance rows
 * into the lower detail grid.
 */
public class MasterDetailPanel extends JPanel {

    private static final String MASTER_SQL =
        "SELECT e.EmployeeID, e.FullName, d.DepartmentName, p.PositionName, e.EmploymentStatus "
        + "FROM Employee e "
        + "JOIN Department d ON e.DepartmentID = d.DepartmentID "
        + "JOIN `Position` p ON e.PositionID = p.PositionID "
        + "ORDER BY e.EmployeeID";

    private static final String DETAIL_SQL =
        "SELECT sa.ShiftAssignmentID, sa.WorkDate, s.ShiftName, s.StartTime, s.EndTime, "
        + "sa.AssignmentStatus, COALESCE(a.AttendanceStatus, 'Not Recorded') AS AttendanceStatus, "
        + "a.ActualCheckIn, a.ActualCheckOut, COALESCE(a.OvertimeHours, 0.00) AS OvertimeHours "
        + "FROM ShiftAssignment sa "
        + "JOIN Shift s ON sa.ShiftID = s.ShiftID "
        + "LEFT JOIN Attendance a ON sa.ShiftAssignmentID = a.ShiftAssignmentID "
        + "WHERE sa.EmployeeID = ? "
        + "ORDER BY sa.WorkDate DESC, s.StartTime";

    private final JTable masterTable = UiHelpers.createDataTable();
    private final JTable detailTable = UiHelpers.createDataTable();
    private final JLabel masterCountLabel = new JLabel("Employees: 0");
    private final JLabel detailCountLabel = new JLabel("Select an employee above to view shifts & attendance");
    private final JTextArea sqlPreview = UiHelpers.createSqlPreviewArea(4);

    public MasterDetailPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel masterHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        JButton refreshBtn = new JButton("Refresh Employees");
        masterHeader.add(new JLabel("Master — Employees:"));
        masterHeader.add(refreshBtn);
        masterHeader.add(masterCountLabel);

        JPanel masterPanel = new JPanel(new BorderLayout(4, 4));
        masterPanel.add(masterHeader, BorderLayout.NORTH);
        masterPanel.add(new JScrollPane(masterTable), BorderLayout.CENTER);

        JPanel detailPanel = new JPanel(new BorderLayout(4, 4));
        detailPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        detailPanel.add(detailCountLabel, BorderLayout.NORTH);
        detailPanel.add(new JScrollPane(detailTable), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, masterPanel, detailPanel);
        splitPane.setResizeWeight(0.5);

        add(splitPane, BorderLayout.CENTER);
        add(UiHelpers.wrapSqlPreview(sqlPreview), BorderLayout.SOUTH);

        refreshBtn.addActionListener(e -> loadMaster());
        masterTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedEmployeeDetails();
            }
        });
    }

    public void loadMaster() {
        sqlPreview.setText(MASTER_SQL);
        UiHelpers.executeQueryToTable(this, masterTable, masterCountLabel, "load employee master list", MASTER_SQL);
        if (masterTable.getRowCount() > 0) {
            masterTable.setRowSelectionInterval(0, 0);
        }
    }

    private void loadSelectedEmployeeDetails() {
        int viewRow = masterTable.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        int modelRow = masterTable.convertRowIndexToModel(viewRow);
        int empId = ((Number) masterTable.getModel().getValueAt(modelRow, 0)).intValue();
        String empName = String.valueOf(masterTable.getModel().getValueAt(modelRow, 1));

        sqlPreview.setText(DETAIL_SQL + "  [? = " + empId + "]");
        UiHelpers.executeQueryToTable(
            this,
            detailTable,
            detailCountLabel,
            "load shift assignments for employee " + empName,
            DETAIL_SQL,
            empId
        );
        detailCountLabel.setText(
            "Detail — Shift Assignments & Attendance for " + empName
            + " (ID " + empId + ") | " + detailCountLabel.getText()
        );
    }
}
