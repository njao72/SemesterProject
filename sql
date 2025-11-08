-- University Admissions Database Schema
-- Created for Data Analysis Application

-- Create database
CREATE DATABASE university_admissions;

-- Applicants table
CREATE TABLE applicants (
    applicant_id VARCHAR(10) PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    dob DATE NOT NULL,
    gender CHAR(1) NOT NULL CHECK (gender IN ('M', 'F')),
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(15),
    city VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Applications table
CREATE TABLE applications (
    application_id VARCHAR(10) PRIMARY KEY,
    applicant_id VARCHAR(10) NOT NULL,
    program VARCHAR(50) NOT NULL,
    admission_year INT NOT NULL,
    status ENUM('Accepted', 'Rejected', 'Pending') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (applicant_id) REFERENCES applicants(applicant_id) ON DELETE CASCADE
);

-- Exam scores table
CREATE TABLE exam_scores (
    score_id VARCHAR(10) PRIMARY KEY,
    applicant_id VARCHAR(10) NOT NULL,
    subject VARCHAR(50) NOT NULL,
    score INT NOT NULL CHECK (score >= 0 AND score <= 100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (applicant_id) REFERENCES applicants(applicant_id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_applications_program ON applications(program);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applicants_city ON applicants(city);
CREATE INDEX idx_applicants_gender ON applicants(gender);
CREATE INDEX idx_exam_scores_subject ON exam_scores(subject);
CREATE INDEX idx_exam_scores_applicant ON exam_scores(applicant_id);

-- Create views for common queries
CREATE VIEW v_acceptance_rates AS
SELECT 
    program,
    COUNT(*) as total_applications,
    SUM(CASE WHEN status = 'Accepted' THEN 1 ELSE 0 END) as accepted_count,
    ROUND(
        (SUM(CASE WHEN status = 'Accepted' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)), 
        2
    ) as acceptance_rate
FROM applications
GROUP BY program;

CREATE VIEW v_avg_scores_by_program AS
SELECT 
    app.program,
    AVG(es.score) as avg_score,
    COUNT(DISTINCT es.applicant_id) as applicant_count
FROM applications app
JOIN exam_scores es ON app.applicant_id = es.applicant_id
WHERE app.status = 'Accepted'
GROUP BY app.program;

CREATE VIEW v_applicant_demographics AS
SELECT 
    city,
    gender,
    COUNT(*) as count,
    ROUND((COUNT(*) * 100.0 / (SELECT COUNT(*) FROM applicants)), 2) as percentage
FROM applicants
GROUP BY city, gender
ORDER BY city, gender;
