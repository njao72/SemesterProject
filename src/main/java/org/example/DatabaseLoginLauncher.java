package org.example;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseLoginLauncher {
    // The main entry point of the java application//
    public static void main(String[] args) {
        //Schedules the showLoginDialog  method to run the AWFT  Event dispatch Thread//
        SwingUtilities.invokeLater(() -> showLoginDialog());
    }
//Defines a private helper method  to create and display the login dialog//
    private static void showLoginDialog() {
        //Creates a JPanel//
        //Uses GridBagLayout for flexible,grid based component positioning//
        JPanel panel = new JPanel(new GridBagLayout());
        //Creates a Gridbaglayout constraint object.Object specifies constraints like objects e.t.c//
        GridBagConstraints gbc = new GridBagConstraints();
        //Sets padding(inserts)of 4 pixels on one side of the components//
        gbc.insets = new Insets(4, 4, 4, 4);
        //Configures components to stretch horizontally to fill their grid cells//
        gbc.fill = GridBagConstraints.HORIZONTAL;
        //Define the components for the UI login form//
        //Creates a string array containing  the names of the supported database types//
        String[] dbTypes = new String[] {"MySQL", "PostgreSQL", "MariaDB"};
        //Creates a dropdown menu(JComboBox) populated with the database types//
        JComboBox<String> dbTypeCombo = new JComboBox<>(dbTypes);
        //Creates a text field for the host ,prefilled with localhost and setting a preferred width(20 columns)//
        JTextField hostField = new JTextField("localhost", 20);
        //Creates a textfield for the port prefilled with 3306//
        JTextField portField = new JTextField("3306", 6);
        //creates a textfield for the database prefiiled with University Admissions//
        JTextField dbNameField = new JTextField("University_admissions", 20);
        //creates a textfield for the user field prefilled with root
        JTextField userField = new JTextField("root", 12);
        //creates a textfield for the password field//
        JPasswordField passwordField = new JPasswordField(12);
//initializes a counter variable to keep track of the grid of the current grid row//
        int row = 0;
        //Add database types: at (row 0,col 0)//
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Database Type:"), gbc);
        //add database dropdown at (row 0,col 1)//
        gbc.gridx = 1; panel.add(dbTypeCombo, gbc);
       //move to the next row//
        row++;
        // add host at (row 0, col 1)
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Host:"), gbc);
        //add the host text field at (row 1, col 1)
        gbc.gridx = 1; panel.add(hostField, gbc);
       //move to next row//
        row++;
        // add port label at( row 2, col 0)
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Port:"), gbc);
        //add port text field at (row 2, col 1)
        gbc.gridx = 1; panel.add(portField, gbc);

        //move to next row//
        row++;
        // add database label at( row 3, col 0)
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Database:"), gbc);
        // add database field at (row 3, col 1)
        gbc.gridx = 1; panel.add(dbNameField, gbc);

        //move to next row//
        row++;
        // add username label at( row 3, col 0)
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Username:"), gbc);
        // add username field at ( row 4, Col 1)
        gbc.gridx = 1; panel.add(userField, gbc);

        //move to next row//
        row++;
        // add database label at( row 3, col 0)
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Password:"), gbc);
        // add password field at (row 5, col 1)
        gbc.gridx = 1; panel.add(passwordField, gbc);

        //Shows the dialog and the get user input//
        //Displays a modal dialog window containing custom panel//
        //The potion variable will store which button the user pressed(ok,cancel)//
        int option = JOptionPane.showConfirmDialog(null, panel, "Database Login",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // checks if user clicked ok button//
        if (option == JOptionPane.OK_OPTION) {
            // retrieves user inputs from data fields//
            String dbType = (String) dbTypeCombo.getSelectedItem();
            String host = hostField.getText().trim();
            String port = portField.getText().trim();
            String db = dbNameField.getText().trim();
            String user = userField.getText().trim();
            //Securely retrieves the password as (char) and converts it to string
            String password = new String(passwordField.getPassword());
          // configure JDBC driver and URL based on datatype//
            //Declares variable to hold the JDBC driver class name and connection URL//
            String driverClass;
            String url;
            //Use a switch statement to set the correct driver and URL format//
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

            // Attempt to connect to the database//
            //Declare a connecton  object (from java.sql) and initializes it to null
            Connection conn = null;
            try {
                //Load the specified JDBC class driver to memory//
                Class.forName(driverClass);
                //Attempt to connect to the database using the URL, user and password//
                conn = DriverManager.getConnection(url, user, password);
            } catch (ClassNotFoundException cnfe) {
                //This catch block runs if the JDBC driver (the .jar file)is not found//
                JOptionPane.showMessageDialog(null, "JDBC Driver not found: " + driverClass + "\nPlease add the JDBC driver JAR to the classpath.", "Driver Error", JOptionPane.ERROR_MESSAGE);
                return;
            } catch (SQLException sqle) {
                //This catch block runs for any related SQL exceptions e.g(wrong password)//
                JOptionPane.showMessageDialog(null, "Failed to connect: " + sqle.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // On success, open the dashboard and pass the connection
            final Connection finalConn = conn;
            SwingUtilities.invokeLater(() -> {
                try {
                    //Creates a new instance of the main application window(UniversityAdmissionsGUI)//
                    // Pass the active database connection to its constructor//
                    UniversityAdmissionsGUI gui = new UniversityAdmissionsGUI(finalConn);
                    // Make the main application window visible to the user//
                    gui.setVisible(true);
                } catch (Exception e) {
                    //catch any unexpected errors during the main GUI admissions initialization
                    JOptionPane.showMessageDialog(null, "Error launching dashboard: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    //print the full stack error to console for debugging//
                    e.printStackTrace();
                    //Try to close database connection if  dashboard fails to launch//
                    try { if (finalConn != null) finalConn.close(); } catch (SQLException ignored) {}
                }
            });
        }
    }
}
