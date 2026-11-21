package com.group9.attendance.ui;

import com.group9.attendance.db.DBConnection;
import com.group9.attendance.db.SqlErrorHandler;
import com.group9.attendance.util.TableModelUtil.ComboItem;
import com.group9.attendance.util.UiHelpers;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
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
 * Editing Form + Multi-Table Transaction Demo (Section 2.3 & 2.4):
 * Supports LeaveRequest INSERT, UPDATE, DELETE (with confirmation) and
 * atomic approval inside a COMMIT / ROLLBACK transaction updating both
 * LeaveRequest and LeaveBalance.
 */
public class LeaveRequestCrudPanel extends JPanel {

    private static final String LIST_SQL =
        "SELECT lr.LeaveRequestID, lr.EmployeeID, e.FullName AS Applicant, lr.LeaveType, "
        + "lr.StartDate, lr.EndDate, lr.Reason, lr.SubmittedAt, lr.RequestStatus, "
        + "COALESCE(r.FullName, '') AS ReviewerName, lr.ReviewedAt "
        + "FROM LeaveRequest lr "
        + "JOIN Employee e ON lr.EmployeeID = e.EmployeeID "
        + "LEFT JOIN Employee r ON lr.ReviewedBy = r.EmployeeID "
        + "ORDER BY lr.LeaveRequestID";

    private final JTable table = UiHelpers.createDataTable();
    private final JLabel countLabel = new JLabel("Total rows: 0");
    private final JTextField idField = new JTextField(6);
    private final JComboBox<ComboItem> employeeCombo = new JComboBox<>();
    private final JComboBox<String> typeCombo = new JComboBox<>(new String[] {
        "Annual", "Sick", "Unpaid", "Maternity", "Personal"
    });
    private final JTextField startDateField = new JTextField(10);
    private final JTextField endDateField = new JTextField(10);
    private final JTextField reasonField = new JTextField(28);
    private final JTextArea sqlPreview = UiHelpers.createSqlPreviewArea(4);

    public LeaveRequestCrudPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        idField.setEditable(false);
        startDateField.setText(LocalDate.now().plusDays(1).toString());
        endDateField.setText(LocalDate.now().plusDays(2).toString());

        JPanel formGrid = new JPanel(new GridLayout(3, 4, 8, 6));
        formGrid.setBorder(BorderFactory.createTitledBorder("Leave Request Details & Multi-Table Approval Transaction"));
        formGrid.add(new JLabel("LeaveRequestID (Auto):"));
        formGrid.add(idField);
        formGrid.add(new JLabel("Applicant *:"));
        formGrid.add(employeeCombo);

        formGrid.add(new JLabel("Leave Type *:"));
        formGrid.add(typeCombo);
        formGrid.add(new JLabel("Start Date (YYYY-MM-DD) *:"));
        formGrid.add(startDateField);

        formGrid.add(new JLabel("End Date (YYYY-MM-DD) *:"));
        formGrid.add(endDateField);
        formGrid.add(new JLabel("Reason *:"));
        formGrid.add(reasonField);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton clearBtn = new JButton("New / Clear");
        JButton insertBtn = new JButton("Insert Request");
        JButton updateBtn = new JButton("Update Request");
        JButton deleteBtn = new JButton("Delete Request");
        JButton approveTxBtn = new JButton("Approve Selected (Transaction)");
        JButton refreshBtn = new JButton("Refresh");

        btnRow.add(clearBtn);
        btnRow.add(insertBtn);
        btnRow.add(updateBtn);
        btnRow.add(deleteBtn);
        btnRow.add(approveTxBtn);
        btnRow.add(refreshBtn);
        btnRow.add(countLabel);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.add(formGrid, BorderLayout.NORTH);
        bottom.add(btnRow, BorderLayout.CENTER);
        bottom.add(UiHelpers.wrapSqlPreview(sqlPreview), BorderLayout.SOUTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFromSelection();
            }
        });
        clearBtn.addActionListener(e -> clearForm());
        refreshBtn.addActionListener(e -> refreshData());
        insertBtn.addActionListener(e -> insertRequest());
        updateBtn.addActionListener(e -> updateRequest());
        deleteBtn.addActionListener(e -> deleteRequest());
        approveTxBtn.addActionListener(e -> approveSelectedWithTransaction());
    }

    public void refreshData() {
        UiHelpers.loadComboItems(
            this,
            employeeCombo,
            "SELECT EmployeeID, CONCAT(EmployeeID, ' - ', FullName) FROM Employee ORDER BY EmployeeID",
            null
        );
        sqlPreview.setText(LIST_SQL);
        UiHelpers.executeQueryToTable(this, table, countLabel, "load leave requests", LIST_SQL);
    }

    private void clearForm() {
        idField.setText("");
        startDateField.setText(LocalDate.now().plusDays(1).toString());
        endDateField.setText(LocalDate.now().plusDays(2).toString());
        reasonField.setText("");
        typeCombo.setSelectedIndex(0);
        if (employeeCombo.getItemCount() > 0) {
            employeeCombo.setSelectedIndex(0);
        }
        table.clearSelection();
    }

    private void populateFromSelection() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        int r = table.convertRowIndexToModel(viewRow);
        idField.setText(String.valueOf(table.getModel().getValueAt(r, 0)));
        UiHelpers.selectComboById(employeeCombo, ((Number) table.getModel().getValueAt(r, 1)).intValue());
        typeCombo.setSelectedItem(String.valueOf(table.getModel().getValueAt(r, 3)));
        startDateField.setText(String.valueOf(table.getModel().getValueAt(r, 4)));
        endDateField.setText(String.valueOf(table.getModel().getValueAt(r, 5)));
        reasonField.setText(String.valueOf(table.getModel().getValueAt(r, 6)));
    }

    private LocalDate[] validateDatesAndReason() {
        String reason = reasonField.getText().trim();
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Reason is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try {
            LocalDate start = LocalDate.parse(startDateField.getText().trim());
            LocalDate end = LocalDate.parse(endDateField.getText().trim());
            if (end.isBefore(start)) {
                JOptionPane.showMessageDialog(
                    this,
                    "Date order error: End Date must be on or after Start Date.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE
                );
                return null;
            }
            return new LocalDate[] {start, end};
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Start Date and End Date must use YYYY-MM-DD format.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            );
            return null;
        }
    }

    private void insertRequest() {
        LocalDate[] dates = validateDatesAndReason();
        ComboItem emp = (ComboItem) employeeCombo.getSelectedItem();
        if (dates == null || emp == null) {
            return;
        }
        String sql = "INSERT INTO LeaveRequest (EmployeeID, LeaveType, StartDate, EndDate, Reason, RequestStatus) "
            + "VALUES (?, ?, ?, ?, ?, 'Pending')";
        sqlPreview.setText(sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, emp.id());
            ps.setString(2, (String) typeCombo.getSelectedItem());
            ps.setDate(3, Date.valueOf(dates[0]));
            ps.setDate(4, Date.valueOf(dates[1]));
            ps.setString(5, reasonField.getText().trim());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Leave request submitted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
            clearForm();
        } catch (SQLException ex) {
            SqlErrorHandler.showError(this, ex, "insert leave request");
        }
    }

    private void updateRequest() {
        if (idField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a leave request row to update.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate[] dates = validateDatesAndReason();
        if (dates == null) {
            return;
        }
        int reqId = Integer.parseInt(idField.getText().trim());
        String sql = "UPDATE LeaveRequest SET LeaveType = ?, StartDate = ?, EndDate = ?, Reason = ? "
            + "WHERE LeaveRequestID = ? AND RequestStatus = 'Pending'";
        sqlPreview.setText(sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (String) typeCombo.getSelectedItem());
            ps.setDate(2, Date.valueOf(dates[0]));
            ps.setDate(3, Date.valueOf(dates[1]));
            ps.setString(4, reasonField.getText().trim());
            ps.setInt(5, reqId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "Leave request #" + reqId + " was not updated because only 'Pending' requests can be modified.",
                    "Update Not Allowed",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            JOptionPane.showMessageDialog(this, "Leave request #" + reqId + " updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (SQLException ex) {
            SqlErrorHandler.showError(this, ex, "update leave request #" + reqId);
        }
    }

    private void deleteRequest() {
        if (idField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a leave request row to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int reqId = Integer.parseInt(idField.getText().trim());
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete leave request #" + reqId + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        String sql = "DELETE FROM LeaveRequest WHERE LeaveRequestID = ?";
        sqlPreview.setText(sql + "  [? = " + reqId + "]");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reqId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Leave request #" + reqId + " deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
            clearForm();
        } catch (SQLException ex) {
            SqlErrorHandler.showError(this, ex, "delete leave request #" + reqId);
        }
    }

    private void approveSelectedWithTransaction() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a Pending leave request row first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int r = table.convertRowIndexToModel(viewRow);
        int reqId = ((Number) table.getModel().getValueAt(r, 0)).intValue();
        int empId = ((Number) table.getModel().getValueAt(r, 1)).intValue();
        String leaveType = String.valueOf(table.getModel().getValueAt(r, 3));
        LocalDate start = LocalDate.parse(String.valueOf(table.getModel().getValueAt(r, 4)));
        LocalDate end = LocalDate.parse(String.valueOf(table.getModel().getValueAt(r, 5)));
        String currentStatus = String.valueOf(table.getModel().getValueAt(r, 8));

        if (!"Pending".equals(currentStatus)) {
            JOptionPane.showMessageDialog(this, "Only 'Pending' requests can be approved (current status: " + currentStatus + ").", "Invalid State", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ComboItem reviewer = pickReviewer(empId);
        if (reviewer == null) {
            return;
        }

        long daysRequested = ChronoUnit.DAYS.between(start, end) + 1;
        int leaveYear = start.getYear();

        String updateReqSql = "UPDATE LeaveRequest SET RequestStatus = 'Approved', ReviewedBy = ?, ReviewedAt = NOW() "
            + "WHERE LeaveRequestID = ? AND RequestStatus = 'Pending'";
        String updateBalSql = "UPDATE LeaveBalance SET UsedDays = UsedDays + ? "
            + "WHERE EmployeeID = ? AND LeaveYear = ? AND LeaveType = ?";

        sqlPreview.setText("START TRANSACTION;\n" + updateReqSql + ";\n" + updateBalSql + ";\nCOMMIT; -- (or ROLLBACK on error)");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psReq = conn.prepareStatement(updateReqSql);
                 PreparedStatement psBal = conn.prepareStatement(updateBalSql)) {

                psReq.setInt(1, reviewer.id());
                psReq.setInt(2, reqId);
                int reqRows = psReq.executeUpdate();
                if (reqRows == 0) {
                    conn.rollback();
                    JOptionPane.showMessageDialog(this, "Leave request #" + reqId + " is no longer Pending. Transaction rolled back.", "Rolled Back", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                psBal.setDouble(1, (double) daysRequested);
                psBal.setInt(2, empId);
                psBal.setInt(3, leaveYear);
                psBal.setString(4, leaveType);
                int balRows = psBal.executeUpdate();
                if (balRows == 0) {
                    conn.rollback();
                    JOptionPane.showMessageDialog(
                        this,
                        "No LeaveBalance entry found for Employee #" + empId + " (" + leaveYear + " / " + leaveType + ").\nTransaction rolled back so LeaveRequest remains Pending.",
                        "Transaction Rolled Back",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                conn.commit();
                JOptionPane.showMessageDialog(
                    this,
                    "Transaction COMMITTED: Approved request #" + reqId + " and added " + daysRequested + " day(s) to LeaveBalance.",
                    "Transaction Committed",
                    JOptionPane.INFORMATION_MESSAGE
                );
                refreshData();
            } catch (SQLException txEx) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
                SqlErrorHandler.showError(this, txEx, "approve leave request in transaction (rolled back)");
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException ex) {
            SqlErrorHandler.showError(this, ex, "open connection for transaction");
        }
    }

    private ComboItem pickReviewer(int applicantEmpId) {
        JComboBox<ComboItem> reviewerCombo = new JComboBox<>();
        String sql = "SELECT e.EmployeeID, CONCAT(e.EmployeeID, ' - ', e.FullName, ' [', ua.Role, ']') "
            + "FROM Employee e JOIN UserAccount ua ON e.EmployeeID = ua.EmployeeID "
            + "WHERE ua.AccountStatus = 'Active' AND ua.Role IN ('ADMIN', 'MANAGER') AND e.EmployeeID <> ? "
            + "ORDER BY e.EmployeeID";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicantEmpId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviewerCombo.addItem(new ComboItem(rs.getInt(1), rs.getString(2)));
                }
            }
        } catch (SQLException ex) {
            SqlErrorHandler.showError(this, ex, "load eligible reviewers");
            return null;
        }
        if (reviewerCombo.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No active MANAGER/ADMIN reviewer available.", "Missing Reviewer", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int option = JOptionPane.showConfirmDialog(this, reviewerCombo, "Select Approving Manager / Admin", JOptionPane.OK_CANCEL_OPTION);
        return option == JOptionPane.OK_OPTION ? (ComboItem) reviewerCombo.getSelectedItem() : null;
    }
}
