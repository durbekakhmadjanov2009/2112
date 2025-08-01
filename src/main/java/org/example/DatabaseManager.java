package org.example;

import org.example.model.AcceptedUser;
import org.example.model.ApplicationData;
import org.example.model.JobPosition;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.sql.DriverManager.getConnection;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/nis_vacancies";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "1";

    public List<JobPosition> getJobPositions() {
        List<JobPosition> positions = new ArrayList<>();
        String sql = "SELECT id, name, description, requirements FROM job_positions WHERE is_active = true";

        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                positions.add(new JobPosition(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("requirements")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return positions;
    }

    public List<ApplicationData> getApplicationsByDate(LocalDateTime threshold) {
        List<ApplicationData> applications = new ArrayList<>();
        String query = "SELECT * FROM user_applications WHERE submitted_at >= ?";

        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setObject(1, threshold);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    applications.add(mapToApplicationData(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch applications by date", e);
        }
        return applications;
    }

    // New mapToApplicationData method
    private ApplicationData mapToApplicationData(ResultSet rs) throws SQLException {
        ApplicationData app = new ApplicationData();
        app.setId(rs.getLong("id"));
        app.setUserId(rs.getLong("user_id"));
        app.setFullName(rs.getString("full_name"));
        app.setBirthDate(rs.getString("birth_date"));
        app.setPhone(rs.getString("phone"));
        app.setEmail(rs.getString("email"));
        app.setAddress(rs.getString("address"));
        app.setEducation(rs.getString("education"));
        app.setExperience(rs.getString("experience"));
        app.setCertificates(rs.getString("certificates"));
        app.setBranch(rs.getString("branch"));
        app.setCvFileId(rs.getString("cv_file_id"));
        app.setDiplomaFileId(rs.getString("diploma_file_id"));
        app.setCertificateFileId(rs.getString("certificate_file_id"));
        app.setVideoLink(rs.getString("video_link"));
        app.setExtraNotes(rs.getString("extra_notes"));
        app.setSubmittedAt(rs.getTimestamp("submitted_at") != null ? rs.getTimestamp("submitted_at").toLocalDateTime() : null);
        app.setJobPosition(rs.getString("job_position"));
        app.setJobId(rs.getString("job_position")); // Mapping job_position to jobId for consistency
        app.setUsername(rs.getString("username"));
        return app;
    }

    public JobPosition getJobPositionById(int id) {
        String sql = "SELECT id, name, description, requirements FROM job_positions WHERE id = ?";

        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new JobPosition(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("requirements")
                    );
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void saveUserApplication(long userId, Map<String, String> data, String username) {
        String sql = """
                INSERT INTO user_applications (
                    user_id, full_name, phone, certificates, branch, cv_file_id, job_position, username, submitted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET
                    full_name = EXCLUDED.full_name,
                    phone = EXCLUDED.phone,
                    certificates = EXCLUDED.certificates,
                    branch = EXCLUDED.branch,
                    cv_file_id = EXCLUDED.cv_file_id,
                    job_position = EXCLUDED.job_position,
                    username = EXCLUDED.username,
                    submitted_at = EXCLUDED.submitted_at
                """;

        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, data.getOrDefault("full_name", ""));
            pstmt.setString(3, data.getOrDefault("phone", ""));
            pstmt.setString(4, data.getOrDefault("certificates", ""));
            pstmt.setString(5, data.getOrDefault("branch", ""));
            pstmt.setString(6, data.getOrDefault("cv_file_id", ""));
            pstmt.setString(7, data.getOrDefault("job_position", ""));
            pstmt.setString(8, username);
            pstmt.setObject(9, LocalDateTime.now()); // Set submitted_at to current timestamp

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getUserApplicationsForAdmin() {
        List<String> applications = new ArrayList<>();
        String sql = "SELECT * FROM user_applications";

        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                long userId = rs.getLong("user_id");
                String fullName = rs.getString("full_name");
                String birthDate = rs.getString("birth_date");
                String phone = rs.getString("phone");
                String email = rs.getString("email");
                String address = rs.getString("address");
                String education = rs.getString("education");
                String experience = rs.getString("experience");
                String certificates = rs.getString("certificates");
                String branch = rs.getString("branch");
                String cvFileId = rs.getString("cv_file_id");
                String diplomaFileId = rs.getString("diploma_file_id");
                String certificateFileId = rs.getString("certificate_file_id");
                String videoLink = rs.getString("video_link");
                String extraNotes = rs.getString("extra_notes");

                StringBuilder sb = new StringBuilder();
                sb.append("<b>👤 Foydalanuvchi ma'lumotlari</b>\n");
                sb.append("🆔 User ID: <code>").append(userId).append("</code>\n");
                sb.append("👨‍💼 Ism: <b>").append(fullName).append("</b>\n");
                sb.append("🎂 Tug‘ilgan sana: ").append(birthDate != null ? birthDate : "Kiritilmagan").append("\n");
                sb.append("📞 Telefon: ").append(phone != null ? phone : "Kiritilmagan").append("\n");
                sb.append("📧 Email: ").append(email != null ? email : "Kiritilmagan").append("\n");
                sb.append("📍 Manzil: ").append(address != null ? address : "Kiritilmagan").append("\n\n");
                sb.append("<b>🏫 Ta'lim va Tajriba</b>\n");
                sb.append("🎓 Ma'lumoti: ").append(education != null ? education : "Kiritilmagan").append("\n");
                sb.append("💼 Tajribasi: ").append(experience != null ? experience : "Kiritilmagan").append("\n");
                sb.append("🏅 Sertifikatlari: ").append(certificates != null ? certificates : "Kiritilmagan").append("\n");
                sb.append("🏢 Filial: ").append(branch != null ? branch : "Kiritilmagan").append("\n\n");
                sb.append("<b>📄 Fayllar</b>\n");
                if (cvFileId != null && !cvFileId.isEmpty()) sb.append("📄 <a href=\"https://t.me/@GoogleMapsbot_bot?start=").append(cvFileId).append("\">CV fayl</a>\n");
                if (diplomaFileId != null && !diplomaFileId.isEmpty()) sb.append("🎓 Diplom: <code>").append(diplomaFileId).append("</code>\n");
                if (certificateFileId != null && !certificateFileId.isEmpty()) sb.append("🏅 Sertifikat: <code>").append(certificateFileId).append("</code>\n");
                if (videoLink != null && !videoLink.isEmpty()) sb.append("🎥 Video: ").append(videoLink).append("\n");
                if (extraNotes != null && !extraNotes.isEmpty()) sb.append("📝 Izoh: ").append(extraNotes).append("\n");

                applications.add(sb.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return applications;
    }

    public List<ApplicationData> getAllApplications() {
        List<ApplicationData> applications = new ArrayList<>();
        String sql = "SELECT * FROM user_applications"; // Updated to select all columns

        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                applications.add(mapToApplicationData(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return applications;
    }

    public String getJobPositionNameById(String jobId) {
        String sql = "SELECT name FROM job_positions WHERE id = ?";
        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(jobId));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Noma'lum lavozim";
    }

    public ApplicationData getApplicationByUserId(Long userId) {
        String query = "SELECT * FROM user_applications WHERE user_id = ?";

        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapToApplicationData(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveAcceptedApplication(Long userId, String fullName, String phone, String username, String certificates, String branch, String cvFileId, String jobPosition) {
        String sql = """
                INSERT INTO accepted_applications (
                    user_id, full_name, phone_number, username, certificates, branch, cv_file_id, job_position
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, fullName);
            pstmt.setString(3, phone);
            pstmt.setString(4, username);
            pstmt.setString(5, certificates);
            pstmt.setString(6, branch);
            pstmt.setString(7, cvFileId);
            pstmt.setString(8, jobPosition);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteUserApplication(Long userId) {
        String sql = "DELETE FROM user_applications WHERE user_id = ?";

        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<AcceptedUser> getAllAcceptedUsers() {
        List<AcceptedUser> list = new ArrayList<>();
        String sql = "SELECT * FROM accepted_applications";

        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                AcceptedUser user = new AcceptedUser();
                user.setUserId(rs.getLong("user_id"));
                user.setFullName(rs.getString("full_name"));
                user.setPhoneNumber(rs.getString("phone_number"));
                user.setUsername(rs.getString("username"));
                user.setCertificates(rs.getString("certificates"));
                user.setBranch(rs.getString("branch"));
                user.setCvFileId(rs.getString("cv_file_id"));
                user.setJobPosition(rs.getString("job_position"));
                list.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public AcceptedUser getAcceptedUserById(Long userId) {
        String sql = "SELECT * FROM accepted_applications WHERE user_id = ?";

        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    AcceptedUser user = new AcceptedUser();
                    user.setUserId(rs.getLong("user_id"));
                    user.setFullName(rs.getString("full_name"));
                    user.setPhoneNumber(rs.getString("phone_number"));
                    user.setUsername(rs.getString("username"));
                    user.setCertificates(rs.getString("certificates"));
                    user.setBranch(rs.getString("branch"));
                    user.setCvFileId(rs.getString("cv_file_id"));
                    user.setJobPosition(rs.getString("job_position"));
                    return user;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Map<String, Object>> getAllJobPositionsWithStatus() {
        List<Map<String, Object>> jobList = new ArrayList<>();
        String sql = "SELECT id, name, is_active FROM job_positions";

        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> jobMap = new HashMap<>();
                jobMap.put("id", rs.getInt("id"));
                jobMap.put("name", rs.getString("name"));
                jobMap.put("is_active", rs.getBoolean("is_active"));
                jobList.add(jobMap);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return jobList;
    }

    public boolean getJobPositionStatusById(int jobId) {
        String sql = "SELECT is_active FROM job_positions WHERE id = ?";
        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, jobId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_active");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateJobPositionStatus(int jobId, boolean isActive) {
        String sql = "UPDATE job_positions SET is_active = ? WHERE id = ?";
        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, isActive);
            stmt.setInt(2, jobId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveJobPosition(String name, String description, String requirements, boolean isActive) {
        String sql = """
                INSERT INTO job_positions (name, description, requirements, is_active)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setString(3, requirements);
            pstmt.setBoolean(4, isActive);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    // FAQ contentni saqlash
    public void saveFAQ(String content) {
        String query = "INSERT INTO faqs (content, updated_at) VALUES (?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (id) DO UPDATE SET content = ?, updated_at = CURRENT_TIMESTAMP";
        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, content);
            stmt.setString(2, content);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("FAQ saqlashda xatolik: " + e.getMessage());
        }
    }

    // FAQ contentni olish
    public String getFAQ() {
        String query = "SELECT content FROM faqs ORDER BY updated_at DESC LIMIT 1";
        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getString("content");
            }
            return "❗ Hozircha ko'p so'raladigan savollar mavjud emas.";
        } catch (SQLException e) {
            throw new RuntimeException("FAQ olishda xatolik: " + e.getMessage());
        }
    }

    // Admin parolini olish
    public String getAdminPassword() {
        String query = "SELECT password FROM admin_credentials ORDER BY updated_at DESC LIMIT 1";
        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getString("password");
            }
            return "2"; // Dastlabki parol, agar jadval bo'sh bo'lsa
        } catch (SQLException e) {
            throw new RuntimeException("Parolni olishda xatolik: " + e.getMessage());
        }
    }

    // Admin parolini yangilash
    public void updateAdminPassword(String newPassword) {
        String query = "INSERT INTO admin_credentials (password, updated_at) VALUES (?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (id) DO UPDATE SET password = ?, updated_at = CURRENT_TIMESTAMP";
        try (Connection conn = getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newPassword);
            stmt.setString(2, newPassword);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Parolni yangilashda xatolik: " + e.getMessage());
        }
    }
}