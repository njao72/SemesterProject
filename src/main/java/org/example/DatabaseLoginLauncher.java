package org.example;
// Swing UI components
import javax.swing.*; // JFrame, JPanel, JButton, JOptionPane, JFileChooser, etc.
// AWT for layout and events
import java.awt.*; // GridBagLayout, GridBagConstraints, Insets, EventQueue
import java.awt.event.*; // ActionEvent, ActionListener
// JDBC classes
import java.sql.Connection; // JDBC Connection
import java.sql.DriverManager; // DriverManager for obtaining connections
import java.sql.SQLException; // Exception type for SQL errors
// IO and utilities for CSV import
import java.io.*; // File, FileReader, BufferedReader, IOException
import java.util.*; // Collections utilities
// JDBC helpers for prepared statements and resultsets
import java.sql.PreparedStatement; // For parameterized INSERTs
import java.sql.ResultSet; // For reading query results (not used here but imported)
import java.sql.Statement; // For executing SQL statements (not used here but imported)

public class DatabaseLoginLauncher {
    // Program entry point. Launches the Swing UI on the Event Dispatch Thread.
    public static void main(String[] args) {
        // Ensure Swing components are created on the EDT
        SwingUtilities.invokeLater(() -> showLoginDialog());
    }

    // Display a modal dialog prompting the user for DB connection details.
    private static void showLoginDialog() {
        // Panel with GridBagLayout to arrange labels and input fields
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4); // padding around components
        gbc.fill = GridBagConstraints.HORIZONTAL; // make components stretch horizontally

        // Dropdown for database type
        String[] dbTypes = new String[] {"MySQL", "PostgreSQL", "MariaDB"};
        JComboBox<String> dbTypeCombo = new JComboBox<>(dbTypes); // choose driver/url
        // Text fields for connection parameters with sensible defaults
        JTextField hostField = new JTextField("localhost", 20);
        JTextField portField = new JTextField("3306", 6);
        JTextField dbNameField = new JTextField("University_admissions", 20);
        JTextField userField = new JTextField("root", 12);
        JPasswordField passwordField = new JPasswordField(12);

        // Place components into grid rows
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

        // Show the dialog and get the user's choice (OK/CANCEL)
        int option = JOptionPane.showConfirmDialog(null, panel, "Database Login",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // If user clicked OK, read inputs and attempt a JDBC connection
        if (option == JOptionPane.OK_OPTION) {
            String dbType = (String) dbTypeCombo.getSelectedItem(); // chosen DB
            String host = hostField.getText().trim(); // host text field
            String port = portField.getText().trim(); // port text field
            String db = dbNameField.getText().trim(); // database name
            String user = userField.getText().trim(); // username
            String password = new String(passwordField.getPassword()); // password

            // Resolve driver class and URL template based on selection
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
                    // Use MySQL Connector/J driver and a typical JDBC URL
                    driverClass = "com.mysql.cj.jdbc.Driver";
                    url = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC", host, port, db);
                    break;
            }

            // Attempt to load the driver and connect
            Connection conn = null;
            try {
                Class.forName(driverClass); // ensure driver class available
                conn = DriverManager.getConnection(url, user, password); // open connection
            } catch (ClassNotFoundException cnfe) {
                // Driver jar missing
                JOptionPane.showMessageDialog(null, "JDBC Driver not found: " + driverClass + "\nPlease add the JDBC driver JAR to the classpath.", "Driver Error", JOptionPane.ERROR_MESSAGE);
                return;
            } catch (SQLException sqle) {
                // Connection failed
                JOptionPane.showMessageDialog(null, "Failed to connect: " + sqle.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // On success: optionally import CSV files, then launch the GUI with the open connection
            final Connection finalConn = conn; // capture for inner runnable
            SwingUtilities.invokeLater(() -> {
                try {
                    showImportDialog(finalConn); // optional CSV import step
                    UniversityAdmissionsGUI gui = new UniversityAdmissionsGUI(finalConn); // create dashboard with live connection
                    gui.setVisible(true); // show the dashboard window
                } catch (Exception e) {
                    // If anything goes wrong, notify user and close connection
                    JOptionPane.showMessageDialog(null, "Error launching dashboard: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    try { if (finalConn != null) finalConn.close(); } catch (SQLException ignored) {}
                }
            });
        }
    }

    /**
     * Show a small dialog allowing the user to import CSV files into three tables.
     * The CSV must have a header row with column names matching the DB table columns.
     */
    private static void showImportDialog(Connection conn) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4,4,4,4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField applicantsField = new JTextField(30);
        JTextField applicationsField = new JTextField(30);
        JTextField examScoresField = new JTextField(30);

        JButton browseApplicants = new JButton("Browse...");
        JButton browseApplications = new JButton("Browse...");
        JButton browseExam = new JButton("Browse...");

        browseApplicants.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                applicantsField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        browseApplications.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                applicationsField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        browseExam.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                examScoresField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Applicants CSV:"), gbc);
        gbc.gridx = 1; panel.add(applicantsField, gbc);
        gbc.gridx = 2; panel.add(browseApplicants, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Applications CSV:"), gbc);
        gbc.gridx = 1; panel.add(applicationsField, gbc);
        gbc.gridx = 2; panel.add(browseApplications, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Exam Scores CSV:"), gbc);
        gbc.gridx = 1; panel.add(examScoresField, gbc);
        gbc.gridx = 2; panel.add(browseExam, gbc);

        int option = JOptionPane.showConfirmDialog(null, panel, "Import CSV data (optional)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            // For each non-empty field, attempt import
            if (!applicantsField.getText().trim().isEmpty()) {
                importCsvToTable(conn, applicantsField.getText().trim(), "applicants");
            }
            if (!applicationsField.getText().trim().isEmpty()) {
                importCsvToTable(conn, applicationsField.getText().trim(), "applications");
            }
            if (!examScoresField.getText().trim().isEmpty()) {
                importCsvToTable(conn, examScoresField.getText().trim(), "exam_scores");
            }
        }
    }

    /**
     * Import a CSV file into the specified table. The CSV's first row must be column names.
     * This method builds an INSERT statement using the header names and inserts all rows.
     */
    private static void importCsvToTable(Connection conn, String csvPath, String tableName) {
        File f = new File(csvPath);
        if (!f.exists()) {
            JOptionPane.showMessageDialog(null, "File not found: " + csvPath, "Import Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String header = br.readLine();
            if (header == null) {
                JOptionPane.showMessageDialog(null, "Empty CSV file: " + csvPath, "Import Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String[] cols = header.split(",");
            for (int i = 0; i < cols.length; i++) cols[i] = cols[i].trim();

            String placeholders = String.join(",", Collections.nCopies(cols.length, "?"));
            String colList = String.join(",", cols);
            String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, colList, placeholders);

            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                String line;
                int batch = 0;
                while ((line = br.readLine()) != null) {
                    // simple CSV split – does not fully support quoted commas
                    String[] values = line.split(",");
                    for (int i = 0; i < cols.length; i++) {
                        String val = i < values.length ? values[i].trim() : null;
                        if (val != null && val.equalsIgnoreCase("NULL")) val = null;
                        ps.setString(i+1, val);
                    }
                    ps.addBatch();
                    batch++;
                    if (batch % 500 == 0) ps.executeBatch();
                }
                if (batch % 500 != 0) ps.executeBatch();
                conn.commit();
                JOptionPane.showMessageDialog(null, "Imported " + f.getName() + " into table " + tableName, "Import Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                conn.rollback();
                JOptionPane.showMessageDialog(null, "Failed to import " + f.getName() + ": " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (IOException | SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error reading or importing file: " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}
