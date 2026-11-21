package com.group9.attendance.db;

import java.awt.Component;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 * Translates low-level MySQL SQLExceptions into plain-language messages so
 * raw stack traces are never exposed to the user.
 */
public final class SqlErrorHandler {

    private SqlErrorHandler() {
    }

    public static String toFriendlyMessage(SQLException ex, String action) {
        int code = ex.getErrorCode();
        String sqlState = ex.getSQLState() == null ? "" : ex.getSQLState();
        String raw = ex.getMessage() == null ? "" : ex.getMessage();

        if (code == 1062) {
            return "Duplicate entry while attempting to " + action
                + ". A unique value (such as SSN, Email, Username, or Employee + Shift + Date) already exists.";
        }
        if (code == 1451) {
            return "Cannot " + action
                + " because other records in the database depend on it (foreign key constraint).";
        }
        if (code == 1452) {
            if (raw.contains("fk_employee_dept_pos")) {
                return "Invalid Department + Position combination while attempting to " + action
                    + ". The selected position is not offered by the selected department (DepartmentPosition constraint).";
            }
            return "Invalid reference while attempting to " + action
                + ". A referenced parent record (Employee, Department, Position, or Shift) does not exist.";
        }
        if (code == 3819) {
            return "Validation constraint failed in the database while attempting to " + action
                + ". Please verify date order (EndDate >= StartDate), positive numbers, or leave balance limits.\nDetails: "
                + raw;
        }
        if (code == 1644) {
            // Custom SIGNAL SQLSTATE '45000' from triggers
            return "Business rule rejected this action: " + raw;
        }
        if (code == 1045) {
            return "Database authentication failed. Please check db.user and db.password in config.properties.";
        }
        if (code == 1049) {
            return "Database 'employee_attendance_db' was not found. Please run 01_schema_and_data.sql first.";
        }
        if ("08S01".equals(sqlState) || "08001".equals(sqlState) || code == 0) {
            return "Unable to reach the database while attempting to " + action
                + ".\nDetails: " + raw;
        }
        return "Database operation failed while attempting to " + action + ".\n" + raw;
    }

    public static void showError(Component parent, SQLException ex, String action) {
        JOptionPane.showMessageDialog(
            parent,
            toFriendlyMessage(ex, action),
            "Database Constraint / Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
