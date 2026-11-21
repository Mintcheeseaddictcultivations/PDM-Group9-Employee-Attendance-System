package com.group9.attendance.ui;

import com.group9.attendance.util.TableModelUtil.ComboItem;
import com.group9.attendance.util.UiHelpers;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Search Form (Section 2.4): Filters employees using a parameterized
 * PreparedStatement query so special characters like apostrophes (') are safe.
 */
public class SearchPanel extends JPanel {

    private final JTextField keywordField = new JTextField(18);
    private final JComboBox<ComboItem> departmentCombo = new JComboBox<>();
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[] {
        "All Statuses", "Active", "Inactive", "OnLeave", "Terminated"
    });
    private final JLabel rowCountLabel = new JLabel("Total rows: 0");
    private final JTable resultTable = UiHelpers.createDataTable();
    private final JTextArea sqlPreview = UiHelpers.createSqlPreviewArea(5);

    public SearchPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton searchBtn = new JButton("Search");
        JButton resetBtn = new JButton("Reset");

        filterBar.add(new JLabel("Keyword (Name / Email / SSN):"));
        filterBar.add(keywordField);
        filterBar.add(new JLabel("Department:"));
        filterBar.add(departmentCombo);
        filterBar.add(new JLabel("Status:"));
        filterBar.add(statusCombo);
        filterBar.add(searchBtn);
        filterBar.add(resetBtn);
        filterBar.add(rowCountLabel);

        add(filterBar, BorderLayout.NORTH);
        add(new JScrollPane(resultTable), BorderLayout.CENTER);
        add(UiHelpers.wrapSqlPreview(sqlPreview), BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> executeSearch());
        keywordField.addActionListener(e -> executeSearch());
        resetBtn.addActionListener(e -> {
            keywordField.setText("");
            if (departmentCombo.getItemCount() > 0) {
                departmentCombo.setSelectedIndex(0);
            }
            statusCombo.setSelectedIndex(0);
            executeSearch();
        });
    }

    public void loadDepartmentsAndSearch() {
        UiHelpers.loadComboItems(
            this,
            departmentCombo,
            "SELECT DepartmentID, DepartmentName FROM Department ORDER BY DepartmentName",
            "All Departments"
        );
        executeSearch();
    }

    public void executeSearch() {
        StringBuilder sql = new StringBuilder(
            "SELECT e.EmployeeID, e.FullName, e.Ssn, e.Sex, e.Phone, e.Email, "
            + "d.DepartmentName, p.PositionName, e.EmploymentStatus, e.HireDate "
            + "FROM Employee e "
            + "JOIN Department d ON e.DepartmentID = d.DepartmentID "
            + "JOIN `Position` p ON e.PositionID = p.PositionID "
            + "WHERE 1 = 1"
        );
        List<Object> params = new ArrayList<>();

        String kw = keywordField.getText().trim();
        if (!kw.isEmpty()) {
            sql.append(" AND (e.FullName LIKE ? OR e.Email LIKE ? OR e.Ssn LIKE ?)");
            String pattern = "%" + kw + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        ComboItem dept = (ComboItem) departmentCombo.getSelectedItem();
        if (dept != null && dept.id() > 0) {
            sql.append(" AND e.DepartmentID = ?");
            params.add(dept.id());
        }

        String status = (String) statusCombo.getSelectedItem();
        if (status != null && !"All Statuses".equals(status)) {
            sql.append(" AND e.EmploymentStatus = ?");
            params.add(status);
        }

        sql.append(" ORDER BY e.EmployeeID");
        sqlPreview.setText(sql.toString());
        UiHelpers.executeQueryToTable(
            this,
            resultTable,
            rowCountLabel,
            "search employees",
            sql.toString(),
            params.toArray()
        );
    }
}
