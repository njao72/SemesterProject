package org.example;


import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.text.DecimalFormat;
import org.jfree.chart.*;
        import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;

public class UniversityAdmissionsGUI extends JFrame {
    private Connection connection;
    private JTextArea outputArea;
    // Table models so we can refresh data without rebuilding UI
    private javax.swing.table.DefaultTableModel acceptanceTableModel;
    private javax.swing.table.DefaultTableModel avgScoresTableModel;
    private JTable acceptanceTable;
    private JTable avgScoresTable;

    /**
     * Default constructor - will attempt to initialize the database using
     * the built-in initializeDatabase() method (hardcoded connection info).
     */
    public UniversityAdmissionsGUI() {
        try {
            initializeDatabase();
            buildUI();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error initializing application: " + e.getMessage(),
                    "Initialization Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Constructor that accepts an existing JDBC Connection. Useful when a
     * launcher dialog creates the connection (for example after prompting
     * the user for host/user/password).
     */
    public UniversityAdmissionsGUI(Connection connection) {
        this.connection = connection;
        try {
            buildUI();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error initializing UI: " + e.getMessage(),
                    "Initialization Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Shared UI construction (assumes 'connection' is set).
     */
    private void buildUI() {
        setTitle("University Admissions Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create main panel with GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Create tabbed pane for charts
        JTabbedPane tabbedPane = new JTabbedPane();

        try {
            // Add tabs
            tabbedPane.addTab("City & Gender", createCityGenderPanel());
            tabbedPane.addTab("Acceptance Rates (Chart)", createAcceptanceRatesChart());
            tabbedPane.addTab("Acceptance Rates (Table)", createAcceptanceRatesTablePanel());
            tabbedPane.addTab("Average Scores (Chart)", createAverageScoresChart());
            tabbedPane.addTab("Average Scores (Table)", createAverageScoresTablePanel());
            tabbedPane.addTab("Exam Score Distribution", createExamScoresHistogram());
            tabbedPane.addTab("Gender Distribution", createGenderDistributionChart());

            // Create text output area
            outputArea = new JTextArea(10, 40);
            outputArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(outputArea);

            // Add components to main panel
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.weighty = 0.7;
            gbc.fill = GridBagConstraints.BOTH;
            mainPanel.add(tabbedPane, gbc);

            gbc.gridy = 1;
            gbc.weighty = 0.3;
            mainPanel.add(scrollPane, gbc);

            // Add refresh button
            JButton refreshButton = new JButton("Refresh Data");
            refreshButton.addActionListener(e -> refreshData());
            gbc.gridy = 2;
            gbc.weighty = 0.05;
            gbc.fill = GridBagConstraints.NONE;
            mainPanel.add(refreshButton, gbc);

            add(mainPanel);

            // Initial data load (off the EDT)
            SwingUtilities.invokeLater(this::refreshData);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error building UI: " + e.getMessage(), "UI Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        // Set window properties
        setSize(1000, 800);
        setLocationRelativeTo(null);
    }

    private void initializeDatabase() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/University_admissions",
                "root",
                "Ken@2018"
        );
    }

    private JPanel createAcceptanceRatesChart() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try (Statement stmt = connection.createStatement()) {
            String query = "SELECT b.program, COUNT(CASE WHEN b.status='Accepted' THEN 1 END) AS Accepted, " +
                    "COUNT(b.application_id) AS totalCount, " +
                    "(COUNT(CASE WHEN b.status='Accepted' THEN 1 END)*100/COUNT(b.application_id)) AS acceptanceRate " +
                    "FROM applicants AS a LEFT JOIN applications AS b ON a.applicant_id=b.applicant_id " +
                    "GROUP BY b.program";
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                dataset.addValue(rs.getDouble("acceptanceRate"),
                        "Acceptance Rate",
                        rs.getString("program"));
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Acceptance Rate per Program",
                "Program",
                "Acceptance Rate (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        panel.add(chartPanel, BorderLayout.CENTER);

        return panel;
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

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT score FROM exam_scores");
            while (rs.next()) {
                scores.add(rs.getDouble("score"));
            }
        }

        double[] scoreArray = scores.stream().mapToDouble(d -> d).toArray();
        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("Exam Scores", scoreArray, 10);

        JFreeChart chart = ChartFactory.createHistogram(
                "Distribution of Exam Scores",
                "Score",
                "Frequency",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        panel.add(chartPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGenderDistributionChart() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultPieDataset dataset = new DefaultPieDataset();

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT gender, COUNT(gender) AS Gendercount FROM applicants GROUP BY gender"
            );
            while (rs.next()) {
                dataset.setValue(rs.getString("gender"), rs.getInt("Gendercount"));
            }
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

    private void refreshData() {
        try {
            outputArea.setText(""); // Clear previous output

            // Display top 10 applicants
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                        "SELECT a.first_name, a.last_name, AVG(score) as avg_score " +
                                "FROM exam_scores AS c " +
                                "LEFT JOIN applicants AS a ON c.applicant_id=a.applicant_id " +
                                "GROUP BY c.applicant_id " +
                                "ORDER BY AVG(score) DESC LIMIT 10"
                );

                outputArea.append("Top 10 Applicants by Average Exam Score:\n\n");
                while (rs.next()) {
                    outputArea.append(String.format("%s %s: %.2f\n",
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getDouble("avg_score")
                    ));
                }
            }
            // Refresh the tables as well
            populateAcceptanceTable();
            populateAverageScoresTable();
            // Also show the acceptance rates and average scores in the output area
            outputArea.append("Acceptance rates per program:\n");
            outputArea.append(String.format("%-40s | %-8s | %-6s | %-8s\n", "Program", "Accepted", "Total", "Rate(%)"));
            if (acceptanceTableModel != null) {
                for (int r = 0; r < acceptanceTableModel.getRowCount(); r++) {
                    Object prog = acceptanceTableModel.getValueAt(r, 0);
                    Object accepted = acceptanceTableModel.getValueAt(r, 1);
                    Object total = acceptanceTableModel.getValueAt(r, 2);
                    Object rate = acceptanceTableModel.getValueAt(r, 3);
                    outputArea.append(String.format("%-40s | %-8s | %-6s | %8s\n", prog == null ? "(null)" : prog.toString(),
                            accepted == null ? "0" : accepted.toString(),
                            total == null ? "0" : total.toString(),
                            rate == null ? "0.00" : rate.toString()));
                }
            }

            outputArea.append("\nAverage exam score per program:\n");
            outputArea.append(String.format("%-40s | %-12s\n", "Program", "AverageScore"));
            if (avgScoresTableModel != null) {
                for (int r = 0; r < avgScoresTableModel.getRowCount(); r++) {
                    Object prog = avgScoresTableModel.getValueAt(r, 0);
                    Object avg = avgScoresTableModel.getValueAt(r, 1);
                    outputArea.append(String.format("%-40s | %12s\n", prog == null ? "(null)" : prog.toString(),
                            avg == null ? "0.00" : avg.toString()));
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error refreshing data: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
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
