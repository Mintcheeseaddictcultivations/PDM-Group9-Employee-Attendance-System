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
import java.sql.SQLException;
import java.sql.Types;
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
 * Editing Form (Section 2.4): Provides INSERT, UPDATE, and DELETE (with
 * confirmation dialog) for Employee records, validating inputs client-side
 * before hitting MySQL and explaining database constraint errors cleanly.
 */
public class EmployeeCrudPanel extends JPanel {

    private static final String LIST_SQL =
        "SELECT e.EmployeeID, e.FullName, e.Ssn, e.Sex, e.Phone, e.Email, e.HireDate, "
        + "e.EmploymentStatus, e.DepartmentID, d.DepartmentName, e.PositionID, p.PositionName, e.ManagerID "
        + "FROM Employee e "
        + "JOIN Department d ON e.DepartmentID = d.DepartmentID "
        + "JOIN `Position` p ON e.PositionID = p.PositionID "
        + "ORDER BY e.EmployeeID";

    private final JTable table = UiHelpers.createDataTable();
    private final JLabel countLabel = new JLabel("Total rows: 0");
    private final JTextField idField = new JTextField(8);
    private final JTextField nameField = new JTextField(18);
    private final JTextField ssnField = new JTextField(12);
    private final JComboBox<String> sexCombo = new JComboBox<>(new String[] {"M", "F", "Other"});
    private final JTextField phoneField = new JTextField(12);
    private final JTextField emailField = new JTextField(18);
    private final JTextField hireDateField = new JTextField(12);
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[] {"Active", "Inactive", "OnLeave", "Terminated"});
    private final JComboBox<ComboItem> deptCombo = new JComboBox<>();
    private final JComboBox<ComboItem> posCombo = new JComboBox<>();
    private final JComboBox<ComboItem> mgrCombo = new JComboBox<>();
    private final JTextArea sqlPreview = UiHelpers.createSqlPreviewArea(3);

    public EmployeeCrudPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        idField.setEditable(false);
        hireDateField.setText(LocalDate.now().toString());

        JPanel formGrid = new JPanel(new GridLayout(4, 4, 8, 6));
        formGrid.setBorder(BorderFactory.createTitledBorder("Employee Details (Required fields validated before DB)"));
        formGrid.add(new JLabel("EmployeeID (Auto):"));
        formGrid.add(idField);
        formGrid.add(new JLabel("Full Name *:"));
        formGrid.add(nameField);

        formGrid.add(new JLabel("SSN (9 digits) *:"));
        formGrid.add(ssnField);
        formGrid.add(new JLabel("Sex *:"));
        formGrid.add(sexCombo);

        formGrid.add(new JLabel("Phone *:"));
        formGrid.add(phoneField);
        formGrid.add(new JLabel("Email *:"));
        formGrid.add(emailField);

        formGrid.add(new JLabel("Hire Date (YYYY-MM-DD) *:"));
        formGrid.add(hireDateField);
        formGrid.add(new JLabel("Status *:"));
        formGrid.add(statusCombo);

        JPanel fkRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        fkRow.add(new JLabel("Department *:"));
        fkRow.add(deptCombo);
        fkRow.add(new JLabel("Position *:"));
        fkRow.add(posCombo);
        fkRow.add(new JLabel("Manager:"));
        fkRow.add(mgrCombo);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton newBtn = new JButton("New / Clear");
        JButton insertBtn = new JButton("Insert");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");
        btnRow.add(newBtn);
        btnRow.add(insertBtn);
        btnRow.add(updateBtn);
        btnRow.add(deleteBtn);
        btnRow.add(refreshBtn);
        btnRow.add(countLabel);

        JPanel bottomPanel = new JPanel(new BorderLayout(4, 4));
        JPanel formWrapper = new JPanel(new BorderLayout(4, 4));
        formWrapper.add(formGrid, BorderLayout.NORTH);
        formWrapper.add(fkRow, BorderLayout.CENTER);
        formWrapper.add(btnRow, BorderLayout.SOUTH);
        bottomPanel.add(formWrapper, BorderLayout.NORTH);
        bottomPanel.add(UiHelpers.wrapSqlPreview(sqlPreview), BorderLayout.SOUTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFormFromSelectedRow();
            }
        });
        newBtn.addActionListener(e -> clearForm());
        refreshBtn.addActionListener(e -> refreshData());
        insertBtn.addActionListener(e -> insertEmployee());
        updateBtn.addActionListener(e -> updateEmployee());
        deleteBtn.addActionListener(e -> deleteEmployee());
    }

    public void refreshData() {
        UiHelpers.loadComboItems(this, deptCombo, "SELECT DepartmentID, DepartmentName FROM Department ORDER BY DepartmentName", null);
        UiHelpers.loadComboItems(this, posCombo, "SELECT PositionID, PositionName FROM `Position` ORDER BY PositionName", null);
        UiHelpers.loadComboItems(this, mgrCombo, "SELECT EmployeeID, CONCAT(EmployeeID, ' - ', FullName) FROM Employee ORDER BY EmployeeID", "None (Top Management)");
        sqlPreview.setText(LIST_SQL);
        UiHelpers.executeQueryToTable(this, table, countLabel, "load employees", LIST_SQL);
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        ssnField.setText("");
        phoneField.setText("");
        emailField.setText("");
        hireDateField.setText(LocalDate.now().toString());
        sexCombo.setSelectedIndex(0);
        statusCombo.setSelectedIndex(0);
        if (mgrCombo.getItemCount() > 0) {
            mgrCombo.setSelectedIndex(0);
        }
        table.clearSelection();
    }

    private void populateFormFromSelectedRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        int r = table.convertRowIndexToModel(viewRow);
        idField.setText(String.valueOf(table.getModel().getValueAt(r, 0)));
        nameField.setText(String.valueOf(table.getModel().getValueAt(r, 1)));
        ssnField.setText(String.valueOf(table.getModel().getValueAt(r, 2)));
        sexCombo.setSelectedItem(String.valueOf(table.getModel().getValueAt(r, 3)));
        phoneField.setText(String.valueOf(table.getModel().getValueAt(r, 4)));
        emailField.setText(String.valueOf(table.getModel().getValueAt(r, 5)));
        hireDateField.setText(String.valueOf(table.getModel().getValueAt(r, 6)));
        statusCombo.setSelectedItem(String.valueOf(table.getModel().getValueAt(r, 7)));
        UiHelpers.selectComboById(deptCombo, ((Number) table.getModel().getValueAt(r, 8)).intValue());
        UiHelpers.selectComboById(posCombo, ((Number) table.getModel().getValueAt(r, 10)).intValue());
        Object mgrObj = table.getModel().getValueAt(r, 12);
        if (mgrObj instanceof Number mgrNum) {
            UiHelpers.selectComboById(mgrCombo, mgrNum.intValue());
        } else {
            mgrCombo.setSelectedIndex(0);
        }
    }

    private LocalDate validateInputs() {
        String fullName = nameField.getText().trim();
        String ssn = ssnField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String hireText = hireDateField.getText().trim();

        if (fullName.isEmpty() || ssn.isEmpty() || phone.isEmpty() || email.isEmpty() || hireText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All required fields (*) must be filled in.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if (!ssn.matches("\\d{9}")) {
            JOptionPane.showMessageDialog(this, "SSN must be exactly 9 numeric digits (e.g., 100000031).", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if (!email.contains("@") || !email.contains(".")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address containing '@' and '.'.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try {
            return LocalDate.parse(hireText);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Hire Date must follow YYYY-MM-DD format (e.g., 2026-11-20).", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private void insertEmployee() {
        LocalDate hireDate = validateInputs();
        if (hireDate == null) {
            return;
        }
        ComboItem dept = (ComboItem) deptCombo.getSelectedItem();
        ComboItem pos = (ComboItem) posCombo.getSelectedItem();
        ComboItem mgr = (ComboItem) mgrCombo.getSelectedItem();
        if (dept == null || pos == null) {
            JOptionPane.showMessageDialog(this, "Department and Position must be selected.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO Employee (FullName, Ssn, Sex, Phone, Email, HireDate, EmploymentStatus, DepartmentID, PositionID, ManagerID) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        sqlPreview.setText(sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindEmployeeParams(ps, hireDate, dept.id(), pos.id(), mgr);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Employee inserted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
            clearForm();
        } catch (SQLException ex) {
            SqlErrorHandler.showError(this, ex, "insert employee");
        }
    }

    private void updateEmployee() {
        if (idField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select an existing employee row to update.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate hireDate = validateInputs();
        if (hireDate == null) {
            return;
        }
        int empId = Integer.parseInt(idField.getText().trim());
        ComboItem dept = (ComboItem) deptCombo.getSelectedItem();
        ComboItem pos = (ComboItem) posCombo.getSelectedItem();
        ComboItem mgr = (ComboItem) mgrCombo.getSelectedItem();
        if (dept == null || pos == null) {
            JOptionPane.showMessageDialog(this, "Department and Position must be selected.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (mgr != null && mgr.id() == empId) {
            JOptionPane.showMessageDialog(this, "An employee cannot supervise themselves.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "UPDATE Employee SET FullName = ?, Ssn = ?, Sex = ?, Phone = ?, Email = ?, "
            + "HireDate = ?, EmploymentStatus = ?, DepartmentID = ?, PositionID = ?, ManagerID = ? "
            + "WHERE EmployeeID = ?";
        sqlPreview.setText(sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindEmployeeParams(ps, hireDate, dept.id(), pos.id(), mgr);
            ps.setInt(11, empId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Employee #" + empId + " updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (SQLException ex) {
            SqlErrorHandler.showError(this, ex, "update employee #" + empId);
        }
    }

    private void deleteEmployee() {
        if (idField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select an employee row to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int empId = Integer.parseInt(idField.getText().trim());
        String empName = nameField.getText().trim();
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete employee #" + empId + " (" + empName + ")?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "DELETE FROM Employee WHERE EmployeeID = ?";
        sqlPreview.setText(sql + "  [? = " + empId + "]");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Employee #" + empId + " deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
            clearForm();
        } catch (SQLException ex) {
            SqlErrorHandler.showError(this, ex, "delete employee #" + empId);
        }
    }

    private void bindEmployeeParams(PreparedStatement ps, LocalDate hireDate, int deptId, int posId, ComboItem mgr) throws SQLException {
        ps.setString(1, nameField.getText().trim());
        ps.setString(2, ssnField.getText().trim());
        ps.setString(3, (String) sexCombo.getSelectedItem());
        ps.setString(4, phoneField.getText().trim());
        ps.setString(5, emailField.getText().trim());
        ps.setDate(6, Date.valueOf(hireDate));
        ps.setString(7, (String) statusCombo.getSelectedItem());
        ps.setInt(8, deptId);
        ps.setInt(9, posId);
        if (mgr != null && mgr.id() > 0) {
            ps.setInt(10, mgr.id());
        } else {
            ps.setNull(10, Types.INTEGER);
        }
    }
}
