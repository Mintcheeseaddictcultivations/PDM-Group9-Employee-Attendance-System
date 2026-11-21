package com.group9.attendance.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Loads external JDBC connection settings from config.properties without
 * hardcoding credentials in Java source code.
 */
public final class DBConnection {

    private static final String[] CONFIG_CANDIDATES = {
        "config.properties",
        "java-app/config.properties"
    };

    private DBConnection() {
    }

    private static File resolveConfigFile() throws SQLException {
        for (String path : CONFIG_CANDIDATES) {
            File file = new File(path);
            if (file.isFile()) {
                return file;
            }
        }
        try {
            File codeLoc = new File(DBConnection.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File baseDir = codeLoc.isDirectory() ? codeLoc.getParentFile() : codeLoc.getParentFile();
            if (baseDir != null) {
                File nearby = new File(baseDir, "config.properties");
                if (nearby.isFile()) {
                    return nearby;
                }
            }
        } catch (Exception ignored) {
        }
        throw new SQLException(
            "Configuration file 'config.properties' not found in working directory ("
            + new File(".").getAbsolutePath()
            + "). Copy 'config.properties.example' to 'config.properties' and set db.user / db.password."
        );
    }

    /**
     * Opens a new JDBC Connection using settings from config.properties.
     */
    public static Connection getConnection() throws SQLException {
        File configFile = resolveConfigFile();
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
        } catch (IOException ex) {
            throw new SQLException("Failed to read config.properties: " + ex.getMessage(), ex);
        }

        String url = props.getProperty("db.url", "").trim();
        String user = props.getProperty("db.user", "").trim();
        String password = props.getProperty("db.password", "");

        if (url.isEmpty() || user.isEmpty()) {
            throw new SQLException("Missing 'db.url' or 'db.user' in " + configFile.getPath());
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException(
                "MySQL JDBC Driver not found. Place mysql-connector-j-*.jar inside the 'lib/' folder.",
                ex
            );
        }

        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Verifies database connectivity and returns a human-readable status message.
     */
    public static String testConnection() throws SQLException {
        try (Connection conn = getConnection()) {
            return "Connected to " + conn.getCatalog()
                + " (" + conn.getMetaData().getDatabaseProductName()
                + " " + conn.getMetaData().getDatabaseProductVersion() + ")";
        }
    }
}
