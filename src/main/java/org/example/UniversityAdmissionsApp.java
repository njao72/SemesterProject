package org.example;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.text.DecimalFormat;
import java.util.List;

import org.jfree.chart.*;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;

public class UniversityAdmissionsApp extends JFrame {
    private Connection connection;
    private JTextArea outputArea;
    private JTabbedPane tabbedPane;
    private JPanel cityGenderPanel;
    private JTable cityGenderTable;

    public UniversityAdmissionsApp() {
        initializeUI();
        connectToDatabase();
        loadData();
    }

    private void initializeUI() {
        setTitle("University Admissions Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create main tabbed pane
        tabbedPane = new JTabbedPane();

        // Create panels for different sections
        createCityGenderPanel();
        createChartsPanel();
        createTopApplicantsPanel();

        // Add main components
        add(tabbedPane, BorderLayout.CENTER);

        // Add refresh button at bottom
        JButton refreshButton = new JButton("Refresh Data");
        refreshButton.addActionListener(e -> refreshData());
        add(refreshButton, BorderLayout.SOUTH);

        // Set window properties
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/University_admissions",
                    "root",
                    "Ken@2018"
            );
        } catch (Exception e) {
            showError("Database Connection Error", e);
        }
    }

    private void createCityGenderPanel() {
        cityGenderPanel = new JPanel(new BorderLayout());
        cityGenderTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(cityGenderTable);
        cityGenderPanel.add(scrollPane, BorderLayout.CENTER);
        tabbedPane.addTab("City & Gender Distribution", cityGenderPanel);
    }

    private void createChartsPanel() {
        JPanel chartsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        tabbedPane.addTab("Charts", chartsPanel);

        try {
            chartsPanel.add(createAcceptanceRatesChart());
            chartsPanel.add(createExamScoresHistogram());
            chartsPanel.add(createGenderDistributionChart());
            chartsPanel.add(createAverageScoresChart());
        } catch (Exception e) {
            showError("Error Creating Charts", e);
        }
    }

    private void createTopApplicantsPanel() {
        JPanel topApplicantsPanel = new JPanel(new BorderLayout());
        outputArea = new JTextArea(20, 40);
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        topApplicantsPanel.add(scrollPane, BorderLayout.CENTER);
        tabbedPane.addTab("Top Applicants", topApplicantsPanel);
    }

    private JPanel createAcceptanceRatesChart() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try (Statement stmt = connection.createStatement()) {
            String query = "SELECT b.program, " +
                    "COUNT(CASE WHEN b.status='Accepted' THEN 1 END)*100.0/COUNT(b.application_id) AS acceptanceRate " +
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
        panel.add(chartPanel);
        return panel;
    }

    private JPanel createExamScoresHistogram() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        List<Double> scores = new ArrayList<>();

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT score FROM exam_scores");
            while (rs.next()) {
                scores.add(rs.getDouble("score"));
            }
        }

        double[] scoreArray = scores.stream().mapToDouble(d -> d).toArray();
        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("Scores", scoreArray, 20);

        JFreeChart chart = ChartFactory.createHistogram(
                "Distribution of Exam Scores",
                "Score",
                "Frequency",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        panel.add(chartPanel);
        return panel;
    }

    private JPanel createGenderDistributionChart() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultPieDataset dataset = new DefaultPieDataset();

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT gender, COUNT(*) as count FROM applicants GROUP BY gender"
            );
            while (rs.next()) {
                dataset.setValue(rs.getString("gender"), rs.getInt("count"));
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
        panel.add(chartPanel);
        return panel;
    }

    private JPanel createAverageScoresChart() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT b.program, AVG(score) as avg_score " +
                            "FROM exam_scores e " +
                            "JOIN applications b ON e.applicant_id = b.applicant_id " +
                            "GROUP BY b.program"
            );

            while (rs.next()) {
                dataset.addValue(rs.getDouble("avg_score"),
                        "Average Score",
                        rs.getString("program"));
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Average Exam Scores by Program",
                "Program",
                "Average Score",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        panel.add(chartPanel);
        return panel;
    }

    private void loadData() {
        try {
            loadCityGenderData();
            loadTopApplicants();
        } catch (Exception e) {
            showError("Error Loading Data", e);
        }
    }

    private void loadCityGenderData() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT city, gender, COUNT(*) as count " +
                            "FROM applicants GROUP BY city, gender ORDER BY city, gender"
            );

            // Create table model and update the table
            Vector<String> columnNames = new Vector<>(Arrays.asList("City", "Gender", "Count"));
            Vector<Vector<Object>> data = new Vector<>();

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("city"));
                row.add(rs.getString("gender"));
                row.add(rs.getInt("count"));
                data.add(row);
            }

            cityGenderTable.setModel(new DefaultTableModel(data, columnNames));
        }
    }

    private void loadTopApplicants() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT a.first_name, a.last_name, AVG(score) as avg_score " +
                            "FROM exam_scores e " +
                            "JOIN applicants a ON e.applicant_id = a.applicant_id " +
                            "GROUP BY a.applicant_id, a.first_name, a.last_name " +
                            "ORDER BY avg_score DESC LIMIT 10"
            );

            StringBuilder sb = new StringBuilder();
            sb.append("Top 10 Applicants by Average Exam Score:\n\n");

            while (rs.next()) {
                sb.append(String.format("%s %s: %.2f\n",
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getDouble("avg_score")
                ));
            }

            outputArea.setText(sb.toString());
        }
    }

    private void refreshData() {
        try {
            loadData();
            tabbedPane.removeAll();
            createCityGenderPanel();
            createChartsPanel();
            createTopApplicantsPanel();
        } catch (Exception e) {
            showError("Error Refreshing Data", e);
        }
    }

    private void showError(String title, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this,
                "Error: " + e.getMessage(),
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        EventQueue.invokeLater(() -> {
            try {
                UniversityAdmissionsApp app = new UniversityAdmissionsApp();
                app.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Error starting application: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}
