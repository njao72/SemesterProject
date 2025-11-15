package org.example;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseLoginLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> showLoginDialog());
    }

    private static void showLoginDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        //Define the components for the UI login form//
        //Creates a string array containing  the names of the supported database types//
        String[] dbTypes = new String[] {"MySQL", "PostgreSQL", "MariaDB"};
        JComboBox<String> dbTypeCombo = new JComboBox<>(dbTypes);
        JTextField hostField = new JTextField("localhost", 20);
        JTextField portField = new JTextField("3306", 6);
        JTextField dbNameField = new JTextField("University_admissions", 20);
        JTextField userField = new JTextField("root", 12);
        JPasswordField passwordField = new JPasswordField(12);
//initializes a counter variable to keep track of the grid of the current grid row//
        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Database Type:"), gbc);
        gbc.gridx = 1; panel.add(dbTypeCombo, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1; panel.add(hostField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1; panel.add(portField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Database:"), gbc);
        gbc.gridx = 1; panel.add(dbNameField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; panel.add(userField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; panel.add(passwordField, gbc);

        int option = JOptionPane.showConfirmDialog(null, panel, "Database Login",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String dbType = (String) dbTypeCombo.getSelectedItem();
            String host = hostField.getText().trim();
            String port = portField.getText().trim();
            String db = dbNameField.getText().trim();
            String user = userField.getText().trim();
            String password = new String(passwordField.getPassword());

            String driverClass;
            String url;
            switch (dbType) {
                case "PostgreSQL":
                    driverClass = "org.postgresql.Driver";
                    url = String.format("jdbc:postgresql://%s:%s/%s", host, port, db);
                    break;
                case "MariaDB":
                    driverClass = "org.mariadb.jdbc.Driver";
                    url = String.format("jdbc:mariadb://%s:%s/%s", host, port, db);
                    break;
                case "MySQL":
                default:
                    driverClass = "com.mysql.cj.jdbc.Driver";
                    url = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC", host, port, db);
                    break;
            }

            // Attempt to connect
            Connection conn = null;
            try {
                Class.forName(driverClass);
                conn = DriverManager.getConnection(url, user, password);
            } catch (ClassNotFoundException cnfe) {
                JOptionPane.showMessageDialog(null, "JDBC Driver not found: " + driverClass + "\nPlease add the JDBC driver JAR to the classpath.", "Driver Error", JOptionPane.ERROR_MESSAGE);
                return;
            } catch (SQLException sqle) {
                JOptionPane.showMessageDialog(null, "Failed to connect: " + sqle.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // On success, open the dashboard and pass the connection
            final Connection finalConn = conn;
            SwingUtilities.invokeLater(() -> {
                try {
                    UniversityAdmissionsGUI gui = new UniversityAdmissionsGUI(finalConn);
                    gui.setVisible(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Error launching dashboard: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    try { if (finalConn != null) finalConn.close(); } catch (SQLException ignored) {}
                }
            });
        }
    }
}
