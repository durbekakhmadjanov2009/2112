package org.example.model;

import java.time.LocalDateTime;

public class ApplicationData {
    private long id; // Added to match table schema
    private long userId;
    private String fullName;
    private String birthDate;
    private String phone;
    private String email;
    private String address;
    private String education;
    private String experience;
    private String certificates;
    private String branch;
    private String cvFileId;
    private String diplomaFileId;
    private String certificateFileId;
    private String videoLink;
    private String extraNotes;
    private String jobPosition;
    private String jobId;
    private String username;
    private LocalDateTime submittedAt;

    // Getter and Setter for id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // Existing Getters and Setters
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getCertificates() {
        return certificates;
    }

    public void setCertificates(String certificates) {
        this.certificates = certificates;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCvFileId() {
        return cvFileId;
    }

    public void setCvFileId(String cvFileId) {
        this.cvFileId = cvFileId;
    }

    public String getDiplomaFileId() {
        return diplomaFileId;
    }

    public void setDiplomaFileId(String diplomaFileId) {
        this.diplomaFileId = diplomaFileId;
    }

    public String getCertificateFileId() {
        return certificateFileId;
    }

    public void setCertificateFileId(String certificateFileId) {
        this.certificateFileId = certificateFileId;
    }

    public String getVideoLink() {
        return videoLink;
    }

    public void setVideoLink(String videoLink) {
        this.videoLink = videoLink;
    }

    public String getExtraNotes() {
        return extraNotes;
    }

    public void setExtraNotes(String extraNotes) {
        this.extraNotes = extraNotes;
    }

    public String getJobPosition() {
        return jobPosition;
    }

    public void setJobPosition(String jobPosition) {
        this.jobPosition = jobPosition;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}