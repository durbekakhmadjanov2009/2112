package org.example.model;

public class UserApplication {
    private String fullName;
    private String birthDate;
    private String phoneNumber;
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
    private String additionalNotes;

    private int step = 1;

    public UserApplication(String fullName, String birthDate, String phoneNumber, String email, String address, String education, String experience, String certificates, String branch, String cvFileId, String diplomaFileId, String certificateFileId, String videoLink, String additionalNotes, int step) {
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
        this.education = education;
        this.experience = experience;
        this.certificates = certificates;
        this.branch = branch;
        this.cvFileId = cvFileId;
        this.diplomaFileId = diplomaFileId;
        this.certificateFileId = certificateFileId;
        this.videoLink = videoLink;
        this.additionalNotes = additionalNotes;
        this.step = step;
    }

    public UserApplication() {
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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

    public String getAdditionalNotes() {
        return additionalNotes;
    }

    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }
}
