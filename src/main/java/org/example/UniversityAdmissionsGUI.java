package org.example;
// Swing UI components for GUI windows, buttons, text areas, tables, etc.
import javax.swing.*;
// AWT for layouts, graphics, colors, dimensions, events, etc.
import java.awt.*;
// JDBC for database connectivity (Connection, Statement, ResultSet, SQLException)
import java.sql.*;
// Java utilities (ArrayList, Vector, etc.)
import java.util.*;
// Text formatting for numbers (e.g., "0.00" format for percentages)
import java.text.DecimalFormat;
// JFreeChart library for creating bar, pie, and histogram charts
import org.jfree.chart.*;
// JFreeChart data structures for bar/line charts
import org.jfree.data.category.DefaultCategoryDataset;
// JFreeChart data structure for pie charts
import org.jfree.data.general.DefaultPieDataset;
// JFreeChart data structure for histograms
import org.jfree.data.statistics.HistogramDataset;
// JFreeChart orientation enum (VERTICAL, HORIZONTAL)
import org.jfree.chart.plot.PlotOrientation;
// JFreeChart pie plot customization
import org.jfree.chart.plot.PiePlot;
// JFreeChart label generator for pie chart labels
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;

// Main GUI window for the University Admissions Dashboard
public class UniversityAdmissionsGUI extends JFrame {
    // JDBC connection to the database (MySQL, PostgreSQL, or MariaDB)
    private Connection connection;
    // Text area that displays top 10 applicants by average score
    private JTextArea outputArea;
    // Table model for Acceptance Rates table tab (stores data rows dynamically)
    private javax.swing.table.DefaultTableModel acceptanceTableModel;
    // Table model for Average Scores table tab (stores data rows dynamically)
    private javax.swing.table.DefaultTableModel avgScoresTableModel;
    // JTable widget displaying acceptance rates per program
    private JTable acceptanceTable;
    // JTable widget displaying average exam scores per program
    private JTable avgScoresTable;

    // Constructor that creates its own database connection (hardcoded credentials)
    // Rarely used; most of the time the launcher provides a connection
    public UniversityAdmissionsGUI() {
        try {
            // Load MySQL driver and connect to local database
            initializeDatabase();
            // Build and display the GUI
            buildUI();
        } catch (Exception e) {
            // Show error dialog if initialization fails
            JOptionPane.showMessageDialog(null, "Error initializing application: " + e.getMessage(),
                    "Initialization Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(); // Print stack trace to console for debugging
        }
    }

    // Constructor that accepts an existing JDBC connection from DatabaseLoginLauncher
    // This is the primary way to instantiate the GUI (with user-provided DB credentials)
    public UniversityAdmissionsGUI(Connection connection) {
        // Store the database connection for later queries
        this.connection = connection;
        try {
            // Build and display the GUI using the provided connection
            buildUI();
        } catch (Exception e) {
            // Show error dialog if UI building fails
            JOptionPane.showMessageDialog(null, "Error initializing UI: " + e.getMessage(),
                    "Initialization Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(); // Print stack trace to console for debugging
        }
    }

    // Build the main GUI window with tabs, text area, and buttons
    // This method creates all visual components and arranges them using GridBagLayout
    private void buildUI() {
        // Set the window title that appears in the title bar
        setTitle("University Admissions Dashboard");
        // Exit the application when the window is closed
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Use BorderLayout as the main container layout manager
        setLayout(new BorderLayout());

        // Create the main panel that holds all components with GridBagLayout for precise positioning
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints(); // Constraints for positioning in grid

        // Create a tabbed pane (JTabbedPane) to hold multiple analysis views
        JTabbedPane tabbedPane = new JTabbedPane();

        try {
            // Add each analysis tab to the tabbed pane
            tabbedPane.addTab("City & Gender", createCityGenderPanel()); // Show applicants by city and gender
            tabbedPane.addTab("Acceptance Rates (Chart)", createAcceptanceRatesChart()); // Bar chart of acceptance rates
            tabbedPane.addTab("Acceptance Rates (Table)", createAcceptanceRatesTablePanel()); // Table view of acceptance rates
            tabbedPane.addTab("Average Scores (Chart)", createAverageScoresChart()); // Bar chart of avg exam scores
            tabbedPane.addTab("Average Scores (Table)", createAverageScoresTablePanel()); // Table view of avg scores
            tabbedPane.addTab("Exam Score Distribution", createExamScoresHistogram()); // Histogram of all exam scores
            tabbedPane.addTab("Gender Distribution", createGenderDistributionChart()); // Pie chart of gender breakdown

            // Create a read-only text area to display top 10 applicants
            outputArea = new JTextArea(10, 40); // 10 rows, 40 columns
            outputArea.setEditable(false); // Users cannot edit this text area
            JScrollPane scrollPane = new JScrollPane(outputArea); // Add scrollbar if text overflows

            // Add tabbed pane to main panel at grid position (0, 0) taking 70% of window height
            gbc.gridx = 0; // Column 0
            gbc.gridy = 0; // Row 0
            gbc.weightx = 1.0; // Take all horizontal space
            gbc.weighty = 0.7; // Take 70% of vertical space
            gbc.fill = GridBagConstraints.BOTH; // Expand in both directions
            mainPanel.add(tabbedPane, gbc);

            // Add text output area to main panel at row 1 taking 30% of window height
            gbc.gridy = 1; // Move to next row
            gbc.weighty = 0.3; // Take 30% of vertical space
            mainPanel.add(scrollPane, gbc);

            // Create a button panel with refresh, clear, and logout buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5)); // Center buttons with spacing

            // "Refresh Data" button: reload data from database and update all views
            JButton refreshButton = new JButton("Refresh Data");
            refreshButton.addActionListener(e -> refreshData()); // When clicked, call refreshData()
            buttonPanel.add(refreshButton);

            // "Clear Database" button: delete all data from the three tables
            JButton clearDataButton = new JButton("Clear Database");
            clearDataButton.addActionListener(e -> clearDatabase()); // When clicked, call clearDatabase()
            buttonPanel.add(clearDataButton);

            // "Logout" button: close connection and return to login screen
            JButton logoutButton = new JButton("Logout");
            logoutButton.addActionListener(e -> logout()); // When clicked, call logout()
            buttonPanel.add(logoutButton);

            // Add button panel to main panel at row 2 taking 5% of window height
            gbc.gridy = 2; // Move to next row
            gbc.weighty = 0.05; // Take 5% of vertical space
            gbc.fill = GridBagConstraints.HORIZONTAL; // Only stretch horizontally
            mainPanel.add(buttonPanel, gbc);

            // Add the main panel to the frame
            add(mainPanel);

            // Load initial data on the Event Dispatch Thread (EDT) to avoid blocking UI
            SwingUtilities.invokeLater(this::refreshData);
        } catch (Exception e) {
            // If any tab creation fails, show error and print stack trace
            JOptionPane.showMessageDialog(this, "Error building UI: " + e.getMessage(), "UI Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(); // Print stack trace to console for debugging
        }

        // Set the window size (width, height) and center it on the screen
        setSize(1000, 800); // 1000 pixels wide, 800 pixels tall
        setLocationRelativeTo(null); // Center on screen
    }

    // Connect to the MySQL database using hardcoded credentials
    // This method is only used in the no-argument constructor
    private void initializeDatabase() throws ClassNotFoundException, SQLException {
        // Load the MySQL JDBC driver class into memory
        Class.forName("com.mysql.cj.jdbc.Driver");
        // Create a connection to the local MySQL database using hardcoded credentials
        connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/University_admissions", // MySQL JDBC URL (localhost, port 3306, database name)
                "root", // Username
                "Ken@2018" // Password
        );
    }

    // Create a bar chart showing acceptance rate for each program
    // Acceptance Rate = (Number Accepted / Total Applications) * 100
    private JPanel createAcceptanceRatesChart() throws SQLException {
        // Create a panel to hold the chart
        JPanel panel = new JPanel(new BorderLayout());
        // Create an empty dataset for the bar chart (category = program names, values = acceptance rates)
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try {
            try (Statement stmt = connection.createStatement()) {
                // SQL query: for each program, count accepted vs total applications, calculate percentage
                String query = "SELECT b.program, COUNT(CASE WHEN b.status='Accepted' THEN 1 END) AS Accepted, " +
                        "COUNT(b.application_id) AS totalCount, " +
                        "(COUNT(CASE WHEN b.status='Accepted' THEN 1 END)*100/COUNT(b.application_id)) AS acceptanceRate " +
                        "FROM applicants AS a LEFT JOIN applications AS b ON a.applicant_id=b.applicant_id " +
                        "GROUP BY b.program";
                ResultSet rs = stmt.executeQuery(query); // Execute the query

                // Loop through each program and add its acceptance rate to the dataset
                while (rs.next()) {
                    // addValue(value, rowName, columnName/category)
                    dataset.addValue(rs.getDouble("acceptanceRate"), // The acceptance rate percentage
                            "Acceptance Rate", // Row label (series name)
                            rs.getString("program")); // Column label (category/X-axis)
                }
            }
        } catch (Exception e) {
            // If query fails (e.g., no data), just continue with empty dataset
        }

        // If no data was loaded, add a dummy "No Data" entry to prevent JFreeChart rendering errors
        if (dataset.getColumnCount() == 0) {
            dataset.addValue(0, "Acceptance Rate", "No Data");
        }

        // Create a bar chart from the dataset
        JFreeChart chart = ChartFactory.createBarChart(
                "Acceptance Rate per Program", // Chart title
                "Program", // X-axis label
                "Acceptance Rate (%)", // Y-axis label
                dataset, // Data to display
                PlotOrientation.VERTICAL, // Bars go up and down (not left-right)
                true, // Show legend
                true, // Show tooltips on hover
                false // No URLs
        );

        // Wrap the chart in a ChartPanel (Swing component) so it can be displayed
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400)); // Set chart size (width, height)
        panel.add(chartPanel, BorderLayout.CENTER); // Add chart to the panel

        return panel; // Return the panel for display in a tab
    }

    private JPanel createCityGenderPanel() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        javax.swing.table.DefaultTableModel model = null;

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT city, gender, COUNT(*) as count FROM applicants GROUP BY city, gender ORDER BY city, gender"
            );

            java.util.Vector<String> columnNames = new java.util.Vector<>();
            columnNames.add("City");
            columnNames.add("Gender");
            columnNames.add("Count");

            java.util.Vector<java.util.Vector<Object>> data = new java.util.Vector<>();
            while (rs.next()) {
                java.util.Vector<Object> row = new java.util.Vector<>();
                row.add(rs.getString("city"));
                row.add(rs.getString("gender"));
                row.add(rs.getInt("count"));
                data.add(row);
            }

            model = new javax.swing.table.DefaultTableModel(data, columnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAcceptanceRatesTablePanel() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        java.util.Vector<String> cols = new java.util.Vector<>();
        cols.add("Program");
        cols.add("Accepted");
        cols.add("Total");
        cols.add("AcceptanceRate(%)");

        acceptanceTableModel = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        acceptanceTable = new JTable(acceptanceTableModel);
        panel.add(new JScrollPane(acceptanceTable), BorderLayout.CENTER);
        // populate
        populateAcceptanceTable();
        return panel;
    }

    private JPanel createAverageScoresTablePanel() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        java.util.Vector<String> cols = new java.util.Vector<>();
        cols.add("Program");
        cols.add("AverageScore");

        avgScoresTableModel = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        avgScoresTable = new JTable(avgScoresTableModel);
        panel.add(new JScrollPane(avgScoresTable), BorderLayout.CENTER);
        populateAverageScoresTable();
        return panel;
    }

    private void populateAcceptanceTable() {
        if (connection == null || acceptanceTableModel == null) return;
        acceptanceTableModel.setRowCount(0);
        try (Statement stmt = connection.createStatement()) {
            String q = "SELECT b.program, COUNT(CASE WHEN b.status='Accepted' THEN 1 END) AS Accepted, " +
                    "COUNT(b.application_id) AS totalCount, " +
                    "(COUNT(CASE WHEN b.status='Accepted' THEN 1 END)*100/COUNT(b.application_id)) AS acceptanceRate " +
                    "FROM applicants AS a LEFT JOIN applications AS b ON a.applicant_id=b.applicant_id " +
                    "GROUP BY b.program";
            ResultSet rs = stmt.executeQuery(q);
            while (rs.next()) {
                String program = rs.getString("program");
                int accepted = rs.getInt("Accepted");
                int total = rs.getInt("totalCount");
                double rate = rs.getDouble("acceptanceRate");
                acceptanceTableModel.addRow(new Object[] { program, accepted, total, String.format("%.2f", rate) });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading acceptance table: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void populateAverageScoresTable() {
        if (connection == null || avgScoresTableModel == null) return;
        avgScoresTableModel.setRowCount(0);
        try (Statement stmt = connection.createStatement()) {
            String q = "SELECT b.program, AVG(e.score) AS avg_score FROM exam_scores e LEFT JOIN applications b ON e.applicant_id=b.applicant_id GROUP BY b.program";
            ResultSet rs = stmt.executeQuery(q);
            while (rs.next()) {
                String program = rs.getString("program");
                double avg = rs.getDouble("avg_score");
                avgScoresTableModel.addRow(new Object[] { program, String.format("%.2f", avg) });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading average scores table: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private JPanel createExamScoresHistogram() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        java.util.List<Double> scores = new ArrayList<>();

        try {
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT score FROM exam_scores");
                while (rs.next()) {
                    scores.add(rs.getDouble("score"));
                }
            }
        } catch (Exception e) {
            // Handle empty data
        }

        // Create histogram with actual data or show empty message
        HistogramDataset dataset = new HistogramDataset();
        if (!scores.isEmpty()) {
            double[] scoreArray = scores.stream().mapToDouble(d -> d).toArray();
            dataset.addSeries("Exam Scores", scoreArray, 10);
        }

        JFreeChart chart = ChartFactory.createHistogram(
                "Distribution of Exam Scores",
                "Score",
                "Frequency",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        // If no data, add a message to the chart
        if (scores.isEmpty()) {
            chart.getPlot().setNoDataMessage("No exam score data available");
        }

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        panel.add(chartPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGenderDistributionChart() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultPieDataset dataset = new DefaultPieDataset();

        try {
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                        "SELECT gender, COUNT(gender) AS Gendercount FROM applicants GROUP BY gender"
                );
                while (rs.next()) {
                    dataset.setValue(rs.getString("gender"), rs.getInt("Gendercount"));
                }
            }
        } catch (Exception e) {
            // Handle empty data
        }

        // If dataset is empty, add a dummy value to prevent null binding error
        if (dataset.getItemCount() == 0) {
            dataset.setValue("No Data", 0);
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Gender Distribution",
                dataset,
                true, true, false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
                "{0}: {1} ({2})",
                new DecimalFormat("0"),
                new DecimalFormat("0.0%")
        ));

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        panel.add(chartPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Create a bar chart that shows average exam score per program.
     */
    private JPanel createAverageScoresChart() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try {
            try (Statement stmt = connection.createStatement()) {
                String sql = "SELECT b.program, AVG(e.score) AS avg_score " +
                        "FROM exam_scores e LEFT JOIN applications b ON e.applicant_id = b.applicant_id " +
                        "GROUP BY b.program";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String program = rs.getString("program");
                    double avg = rs.getDouble("avg_score");
                    if (program == null) program = "(No program)";
                    dataset.addValue(avg, "Average Score", program);
                }
            }
        } catch (Exception e) {
            // Handle empty data
        }

        // If dataset is empty, add a dummy value to prevent null binding error
        if (dataset.getColumnCount() == 0) {
            dataset.addValue(0, "Average Score", "No Data");
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Average Exam Score per Program",
                "Program",
                "Average Score",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        panel.add(chartPanel, BorderLayout.CENTER);
        return panel;
    }

    // Clear all data from the database tables (with confirmation)
    private void clearDatabase() {
        // Show a warning dialog with YES/NO confirmation
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all data from the database?\nThis action cannot be undone.", // Warning message
                "Confirm Clear Database", // Dialog title
                JOptionPane.YES_NO_OPTION, // Show Yes and No buttons
                JOptionPane.WARNING_MESSAGE); // Use warning icon

        // Only proceed if user clicks "Yes"
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                try (Statement stmt = connection.createStatement()) {
                    // Delete from exam_scores first (it depends on applications and applicants)
                    stmt.execute("DELETE FROM exam_scores");
                    // Delete from applications (it depends on applicants)
                    stmt.execute("DELETE FROM applications");
                    // Delete from applicants (no dependencies)
                    stmt.execute("DELETE FROM applicants");
                }
                // Show success message
                JOptionPane.showMessageDialog(this,
                        "Database cleared successfully!", // Success message
                        "Success", // Dialog title
                        JOptionPane.INFORMATION_MESSAGE); // Use info icon
                // Refresh all views to show empty data
                refreshData();
            } catch (SQLException e) {
                // Show error message if deletion fails
                JOptionPane.showMessageDialog(this,
                        "Error clearing database: " + e.getMessage(), // Error description
                        "Database Error", // Dialog title
                        JOptionPane.ERROR_MESSAGE); // Use error icon
                e.printStackTrace(); // Print stack trace to console for debugging
            }
        }
    }

    // Logout: close the database connection and return to login screen
    private void logout() {
        // Show a confirmation dialog to prevent accidental logout
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?", // Dialog message
                "Confirm Logout", // Dialog title
                JOptionPane.YES_NO_OPTION, // Show Yes and No buttons
                JOptionPane.QUESTION_MESSAGE); // Use question icon

        // If user clicked "Yes", proceed with logout
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Close the database connection if it's still open
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                // If connection close fails, just print error and continue
                e.printStackTrace();
            }
            // Close this GUI window
            this.dispose();
            // Show the login dialog again so user can log back in with different credentials
            SwingUtilities.invokeLater(() -> DatabaseLoginLauncher.showLoginDialogStatic());
        }
    }

    // Refresh all data from the database and update all views
    // This includes: top 10 applicants display, table population, and chart refreshes
    private void refreshData() {
        try {
            // Clear the text output area before adding new content
            outputArea.setText("");

            // Query the database for top 10 applicants by average exam score
            try (Statement stmt = connection.createStatement()) {
                // SQL: join exam_scores with applicants, calculate average score, order by highest
                ResultSet rs = stmt.executeQuery(
                        "SELECT a.first_name, a.last_name, AVG(score) as avg_score " +
                                "FROM exam_scores AS c " +
                                "LEFT JOIN applicants AS a ON c.applicant_id=a.applicant_id " +
                                "GROUP BY c.applicant_id " +
                                "ORDER BY AVG(score) DESC LIMIT 10" // Get top 10 with highest average scores
                );

                // Add header text to the output area
                outputArea.append("Top 10 Applicants by Average Exam Score:\n\n");
                // Loop through each row in the result set
                while (rs.next()) {
                    // Format: "FirstName LastName: AverageScore"
                    outputArea.append(String.format("%s %s: %.2f\n",
                            rs.getString("first_name"), // Get first name
                            rs.getString("last_name"), // Get last name
                            rs.getDouble("avg_score") // Get average score formatted to 2 decimals
                    ));
                }
            }
            // Refresh the acceptance rates table tab
            populateAcceptanceTable();
            // Refresh the average scores table tab
            populateAverageScoresTable();

        } catch (SQLException e) {
            // If any database query fails, show error dialog
            JOptionPane.showMessageDialog(this,
                    "Error refreshing data: " + e.getMessage(), // Error description
                    "Database Error", // Dialog title
                    JOptionPane.ERROR_MESSAGE); // Use error icon
            e.printStackTrace(); // Print stack trace to console for debugging
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                UniversityAdmissionsGUI frame = new UniversityAdmissionsGUI();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Error starting application: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
