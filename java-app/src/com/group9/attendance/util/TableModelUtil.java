package com.group9.attendance.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

/**
 * Shared Swing helpers for building read-only table models from JDBC ResultSets
 * and representing ID + Label pairs inside JComboBox controls.
 */
public final class TableModelUtil {

    private TableModelUtil() {
    }

    /**
     * Converts a ResultSet into a non-editable DefaultTableModel.
     */
    public static DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        Vector<String> columns = new Vector<>(colCount);
        for (int i = 1; i <= colCount; i++) {
            columns.add(meta.getColumnLabel(i));
        }

        Vector<Vector<Object>> rows = new Vector<>();
        while (rs.next()) {
            Vector<Object> row = new Vector<>(colCount);
            for (int i = 1; i <= colCount; i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
        }

        return new DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * Lightweight value object for populating JComboBox dropdowns with foreign keys.
     */
    public record ComboItem(int id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
