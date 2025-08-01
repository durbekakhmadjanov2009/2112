package org.example.model;  // Sening package nomingga qarab o'zgartir

public class AcceptedUser {
    private Long userId;
    private String fullName;
    private String phoneNumber;
    private String username;
    private String email;
    private String cvFileId;
    private String certificates;
    private String branch;
    private String jobPosition;


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName != null ? fullName : "Kiritilmagan";
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber != null ? phoneNumber : "Kiritilmagan";
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() {
        return username != null ? username : "no_username";
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email != null ? email : "Kiritilmagan";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCvFileId() {
        return cvFileId != null ? cvFileId : "Yuklanmagan";
    }

    public void setCvFileId(String cvFileId) {
        this.cvFileId = cvFileId;
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

    public String getJobPosition() {
        return jobPosition;
    }

    public void setJobPosition(String jobPosition) {
        this.jobPosition = jobPosition;
    }
}
