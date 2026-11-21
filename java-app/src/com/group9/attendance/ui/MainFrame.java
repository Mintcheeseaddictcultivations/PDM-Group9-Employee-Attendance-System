package com.group9.attendance.ui;

import com.group9.attendance.db.DBConnection;
import com.group9.attendance.db.SqlErrorHandler;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Main application window hosting all 6 required functional screens (plus
 * Leave Request CRUD & Multi-Table Transaction) over a shared JTabbedPane.
 */
public class MainFrame extends JFrame {

    private final JLabel statusLabel = new JLabel("Checking connection...");
    private boolean connected = false;
    private final BrowsePanel browsePanel = new BrowsePanel();
    private final SearchPanel searchPanel = new SearchPanel();
    private final MasterDetailPanel masterDetailPanel = new MasterDetailPanel();
    private final EmployeeCrudPanel employeeCrudPanel = new EmployeeCrudPanel();
    private final LeaveRequestCrudPanel leaveRequestCrudPanel = new LeaveRequestCrudPanel();
    private final ReportPanel reportPanel = new ReportPanel();
    private final TopicQueriesPanel topicQueriesPanel = new TopicQueriesPanel();

    public MainFrame() {
        super("IT079IU Group 9 — Employee Attendance and Shift Scheduling");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1160, 740);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(6, 6));

        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
        JLabel title = new JLabel("Employee Attendance & Shift Scheduling (JDBC + MySQL)");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        header.add(title, BorderLayout.WEST);

        JPanel connBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton testConnBtn = new JButton("Test Connection / Reload");
        connBar.add(statusLabel);
        connBar.add(testConnBtn);
        header.add(connBar, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1. Browse Tables", browsePanel);
        tabs.addTab("2. Search Employees", searchPanel);
        tabs.addTab("3. Master-Detail (Shifts)", masterDetailPanel);
        tabs.addTab("4. Employee CRUD", employeeCrudPanel);
        tabs.addTab("5. Leave CRUD & Transaction", leaveRequestCrudPanel);
        tabs.addTab("6. Aggregate Reports", reportPanel);
        tabs.addTab("7. Topic 15 Queries (5/5)", topicQueriesPanel);

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        testConnBtn.addActionListener(e -> checkAndLoadAll());
        tabs.addChangeListener(e -> onTabSelected(tabs.getSelectedIndex()));

        checkAndLoadAll();
    }

    private void checkAndLoadAll() {
        try {
            String msg = DBConnection.testConnection();
            connected = true;
            statusLabel.setText(msg);
            statusLabel.setForeground(new Color(0, 128, 0));
            browsePanel.loadSelectedTable();
            searchPanel.loadDepartmentsAndSearch();
            masterDetailPanel.loadMaster();
            employeeCrudPanel.refreshData();
            leaveRequestCrudPanel.refreshData();
            reportPanel.runSelectedReport();
            topicQueriesPanel.loadDepartments();
        } catch (SQLException ex) {
            connected = false;
            statusLabel.setText("Disconnected (check config.properties)");
            statusLabel.setForeground(Color.RED);
            SqlErrorHandler.showError(this, ex, "connect to database");
        }
    }

    private void onTabSelected(int index) {
        if (!connected) {
            return;
        }
        if (index == 0) {
            browsePanel.loadSelectedTable();
        } else if (index == 1) {
            searchPanel.executeSearch();
        } else if (index == 2) {
            masterDetailPanel.loadMaster();
        } else if (index == 3) {
            employeeCrudPanel.refreshData();
        } else if (index == 4) {
            leaveRequestCrudPanel.refreshData();
        } else if (index == 5) {
            reportPanel.runSelectedReport();
        }
    }
}
