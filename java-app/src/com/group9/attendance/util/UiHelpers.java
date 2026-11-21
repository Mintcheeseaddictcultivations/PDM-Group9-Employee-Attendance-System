package com.group9.attendance.util;

import com.group9.attendance.db.DBConnection;
import com.group9.attendance.db.SqlErrorHandler;
import com.group9.attendance.util.TableModelUtil.ComboItem;
import java.awt.Component;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Shared Swing and JDBC helpers used across panels to avoid duplicated query
 * execution, combo loading, and table configuration logic.
 */
public final class UiHelpers {

    private UiHelpers() {
    }

    public static JTable createDataTable() {
        JTable table = new JTable();
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    public static JTextArea createSqlPreviewArea(int rows) {
        JTextArea area = new JTextArea(rows, 60);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    public static JScrollPane wrapSqlPreview(JTextArea area) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createTitledBorder("Underlying SQL Statement"));
        return scroll;
    }

    /**
     * Executes a parameterized SELECT query and fills the target JTable and row-count label.
     */
    public static void executeQueryToTable(
        Component parent,
        JTable table,
        JLabel countLabel,
        String actionDescription,
        String sql,
        Object... params
    ) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                DefaultTableModel model = TableModelUtil.buildTableModel(rs);
                table.setModel(model);
                if (countLabel != null) {
                    countLabel.setText("Total rows: " + model.getRowCount());
                }
            }
        } catch (SQLException ex) {
            SqlErrorHandler.showError(parent, ex, actionDescription);
        }
    }

    /**
     * Loads two-column (INT id, VARCHAR label) lookup rows into a JComboBox.
     */
    public static void loadComboItems(
        Component parent,
        JComboBox<ComboItem> combo,
        String sql,
        String optionalAllLabel
    ) {
        combo.removeAllItems();
        if (optionalAllLabel != null) {
            combo.addItem(new ComboItem(-1, optionalAllLabel));
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                combo.addItem(new ComboItem(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException ex) {
            SqlErrorHandler.showError(parent, ex, "load dropdown options");
        }
    }

    public static void selectComboById(JComboBox<ComboItem> combo, int targetId) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            ComboItem item = combo.getItemAt(i);
            if (item != null && item.id() == targetId) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }
}
