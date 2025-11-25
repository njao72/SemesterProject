GROUP 1
Memebers:
1. Adrian Osogo
2. James Njao
3. Terrence Katua
4. Emmanuel Kanyiri
5. Jeff Muroki


University Admissions Data Analysis

Project Goal:

Develop a java desktop application (Swing) that imports CSV admissions data into a normalized relational DB, performs admissions statistics and ranking, and displays results with visualizations.

The university application analyzes university admission data from 3 csv files:
1. applicants.csv
2. applications.csv
3. exma_scores.csv

Features

1. Database design:
   
   i.Normalised databses schema
   ii.Relational tables

2.Java application with GUI
   
   i. Swing based graphical interface
   ii. Database connection and management
   iii. CSV data import*

3.Statistical analysis
   
   i.Acceptance rates per program
   ii.Average exam scores per program
   iii.Distribution of applicants by city and gender
   iv.Top 10 applicants by average exam score


4.Data visualisation
  a. Bar chart
  
    i.Acceptance rates per program
    ii. Average scores by program
    
  b. Histogram
  c. Pie chart

Project structure

Semesterproject
  src/main/java/org/example
  
    main.java                  #Main program
    DatabseLoginLauncher       #Database login
    UniversityAdmissionsApp    #University admissions
    UniversityAdmissionsGUI    #GUI
 sql
 
   schema.sql                  #SQL Schema
   
 pom.xml
 
 readme.md
 
 readmeAssets                  #Slides
 
 resources                     #Table and visualisations
    Tables
    Charts
