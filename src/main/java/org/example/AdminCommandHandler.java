package org.example;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.model.AcceptedUser;
import org.example.model.ApplicationData;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AdminCommandHandler {

    private final JobBotHandler bot;
    private final DatabaseManager databaseManager;
    private final Set<Long> adminUsers = new HashSet<>();
    private Map<Long, Map<Integer, Boolean>> adminJobStatusMap = new HashMap<>();

    public AdminCommandHandler(JobBotHandler bot, DatabaseManager databaseManager) {
        this.bot = bot;
        this.databaseManager = databaseManager;
    }

    public void handleAdminCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();
        String text = update.getMessage().getText();

        if (text.equals("/admin")) {
            SendMessage askPassword = new SendMessage(chatId.toString(), "🔑 Iltimos, parolni kiriting:");
            try {
                bot.execute(askPassword);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            bot.getAdminStepMap().put(userId, "WAITING_FOR_PASSWORD");
            return;
        }

        String currentStep = bot.getAdminStepMap().get(userId);

        if ("WAITING_FOR_PASSWORD".equals(currentStep)) {
            String currentPassword = databaseManager.getAdminPassword();
            if (text.equals(currentPassword)) {
                adminUsers.add(userId);
                bot.getAdminStepMap().remove(userId);

                sendAdminPanel(chatId);
            } else {
                try {
                    bot.execute(new SendMessage(chatId.toString(), "❌ Noto‘g‘ri parol. Qayta urinib ko‘ring."));
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
            return;
        }

        if (!adminUsers.contains(userId)) {
            try {
                bot.execute(new SendMessage());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        if ("WAITING_FOR_FAQ".equals(currentStep)) {
            databaseManager.saveFAQ(text);
            bot.getAdminStepMap().remove(userId);

            SendMessage successMsg = new SendMessage(chatId.toString(), "✅ FAQ muvaffaqiyatli yangilandi!");
            try {
                bot.execute(successMsg);
                sendAdminPanel(chatId);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if ("WAITING_FOR_CURRENT_PASSWORD".equals(currentStep)) {
            String currentPassword = databaseManager.getAdminPassword();
            if (text.equals(currentPassword)) {
                bot.getAdminStepMap().put(userId, "WAITING_FOR_NEW_PASSWORD");
                SendMessage askNewPassword = new SendMessage(chatId.toString(), "🔑 Yangi parolni kiriting:");
                try {
                    bot.execute(askNewPassword);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else {
                SendMessage errorMsg = new SendMessage(chatId.toString(), "❌ Joriy parol noto‘g‘ri. Qayta urinib ko‘ring.");
                try {
                    bot.execute(errorMsg);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if ("WAITING_FOR_NEW_PASSWORD".equals(currentStep)) {
            databaseManager.updateAdminPassword(text);
            bot.getAdminStepMap().remove(userId);

            SendMessage successMsg = new SendMessage(chatId.toString(), "✅ Parol muvaffaqiyatli yangilandi!");
            try {
                bot.execute(successMsg);
                sendAdminPanel(chatId);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if ("ADDING_JOB_NAME".equals(currentStep)) {
            bot.getTempJobData().put(userId, new ArrayList<>(List.of(text)));
            bot.getAdminStepMap().put(userId, "ADDING_JOB_DESCRIPTION");

            SendMessage askDescription = new SendMessage(chatId.toString(), "📝 Endi ish sohasining qisqacha tavsifini kiriting:");
            try {
                bot.execute(askDescription);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if ("ADDING_JOB_DESCRIPTION".equals(currentStep)) {
            List<String> jobData = bot.getTempJobData().get(userId);
            jobData.add(text);
            bot.getAdminStepMap().put(userId, "ADDING_JOB_REQUIREMENTS");

            SendMessage askRequirements = new SendMessage(chatId.toString(), "📝 Endi bu ish uchun talablarni yozing:");
            try {
                bot.execute(askRequirements);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if ("ADDING_JOB_REQUIREMENTS".equals(currentStep)) {
            List<String> jobData = bot.getTempJobData().get(userId);
            jobData.add(text);

            String jobName = jobData.get(0);
            String description = jobData.get(1);
            String requirements = jobData.get(2);

            String preview = """
                    ✅ Yangi ish soha ma'lumotlari:
                    
                    🔹 <b>Nomi:</b> %s
                    🔹 <b>Tavsifi:</b> %s
                    🔹 <b>Talablar:</b> %s
                    
                    Saqlash uchun 'Saqlash' tugmasini bosing.
                    """.formatted(jobName, description, requirements);

            SendMessage confirmMsg = new SendMessage(chatId.toString(), preview);
            confirmMsg.setParseMode("HTML");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            rows.add(List.of(InlineKeyboardButton.builder().text("💾 Saqlash").callbackData("save_new_job").build()));
            rows.add(List.of(InlineKeyboardButton.builder().text("❌ Bekor qilish").callbackData("cancel_new_job").build()));
            markup.setKeyboard(rows);
            confirmMsg.setReplyMarkup(markup);

            bot.getAdminStepMap().put(userId, "WAITING_TO_SAVE_JOB");
            try {
                bot.execute(confirmMsg);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void handleAdminCallback(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Long userId = update.getCallbackQuery().getFrom().getId();

        if (!adminUsers.contains(userId)) {
            try {
                bot.execute(new SendMessage());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        if (callbackData.equals("update_faq")) {
            bot.getAdminStepMap().put(userId, "WAITING_FOR_FAQ");
            SendMessage askFAQ = new SendMessage(chatId.toString(), "📝 Ko‘p so‘raladigan savollar uchun yangi matn kiriting:");
            try {
                bot.execute(askFAQ);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if (callbackData.equals("change_password")) {
            bot.getAdminStepMap().put(userId, "WAITING_FOR_CURRENT_PASSWORD");
            SendMessage askCurrentPassword = new SendMessage(chatId.toString(), "🔑 Joriy parolni kiriting:");
            try {
                bot.execute(askCurrentPassword);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if (callbackData.equals("view_applications")) {
            SendMessage viewOptions = new SendMessage(chatId.toString(), "📋 Arizalarni qanday ko‘rishni tanlang:");
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            rows.add(List.of(InlineKeyboardButton.builder()
                    .text("🤖 Botda ko‘rish")
                    .callbackData("view_in_bot")
                    .build()));

            rows.add(List.of(InlineKeyboardButton.builder()
                    .text("📊 Excelda ko‘rish")
                    .callbackData("view_in_excel")
                    .build()));

            markup.setKeyboard(rows);
            viewOptions.setReplyMarkup(markup);

            try {
                bot.execute(viewOptions);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (callbackData.equals("view_in_bot")) {
            List<ApplicationData> applications = databaseManager.getAllApplications();

            if (applications.isEmpty()) {
                SendMessage noApps = new SendMessage(chatId.toString(), "❗ Hozircha hech qanday ariza mavjud emas.");
                try {
                    bot.execute(noApps);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                return;
            }

            SendMessage selectUserMsg = new SendMessage(chatId.toString(), "📋 Ko‘rish uchun arizachi nomini tanlang:");
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (ApplicationData app : applications) {
                String fullName = app.getFullName();
                if (fullName == null || fullName.trim().isEmpty()) {
                    fullName = "👤 Noma'lum foydalanuvchi";
                }

                InlineKeyboardButton btn = InlineKeyboardButton.builder()
                        .text(fullName)
                        .callbackData("view_application_" + app.getUserId())
                        .build();
                rows.add(List.of(btn));
            }

            markup.setKeyboard(rows);
            selectUserMsg.setReplyMarkup(markup);

            try {
                bot.execute(selectUserMsg);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (callbackData.equals("view_in_excel")) {
            SendMessage excelOptions = new SendMessage(chatId.toString(), "📊 Excelda ko‘rish uchun vaqt oralig‘ini tanlang:");
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            rows.add(List.of(InlineKeyboardButton.builder()
                    .text("📅 So‘nggi 1 kun")
                    .callbackData("excel_last_day")
                    .build()));

            rows.add(List.of(InlineKeyboardButton.builder()
                    .text("📅 So‘nggi 7 kun")
                    .callbackData("excel_last_week")
                    .build()));

            rows.add(List.of(InlineKeyboardButton.builder()
                    .text("📅 Hammasi")
                    .callbackData("excel_all")
                    .build()));

            markup.setKeyboard(rows);
            excelOptions.setReplyMarkup(markup);

            try {
                bot.execute(excelOptions);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (callbackData.equals("excel_last_day")) {
            sendExcelReport(chatId, 1);
        } else if (callbackData.equals("excel_last_week")) {
            sendExcelReport(chatId, 7);
        } else if (callbackData.equals("excel_all")) {
            sendExcelReport(chatId, null);
        } else if (callbackData.startsWith("view_application_")) {
            Long appUserId = Long.parseLong(callbackData.replace("view_application_", ""));
            ApplicationData app = databaseManager.getApplicationByUserId(appUserId);

            if (app == null) {
                SendMessage noApp = new SendMessage(chatId.toString(), "❗ Bu foydalanuvchi ariza topshirmagan.");
                try {
                    bot.execute(noApp);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<b>📝 Ariza Ma'lumotlari:</b>\n");

            String jobPositionName = databaseManager.getJobPositionNameById(app.getJobPosition());
            sb.append("💼 <b>Ish o‘rni:</b> ").append(jobPositionName).append("\n\n");

            sb.append("👤 <b>Ism:</b> ").append(app.getFullName()).append("\n");
            sb.append("📞 <b>Telefon:</b> ").append(app.getPhone()).append("\n");
            sb.append("👤 <b>Username:</b> @").append(isEmpty(app.getUsername()) ? "Kiritilmagan" : app.getUsername()).append("\n");

            String certificates = app.getCertificates();
            if (isEmpty(certificates) || certificates.equalsIgnoreCase("bosh")) {
                certificates = "Kiritilmagan";
            }
            sb.append("🏅 <b>Sertifikatlar:</b> ").append(certificates).append("\n");
            sb.append("🏢 <b>Filial:</b> ").append(app.getBranch()).append("\n");

            SendMessage applicationMessage = new SendMessage(chatId.toString(), sb.toString());
            applicationMessage.setParseMode("HTML");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton cvButton = InlineKeyboardButton.builder()
                    .text("📄 CV")
                    .callbackData("view_cv_" + app.getUserId())
                    .build();
            rows.add(List.of(cvButton));

            InlineKeyboardButton acceptButton = InlineKeyboardButton.builder()
                    .text("✅ Qabul qilish")
                    .callbackData("accept_" + app.getUserId())
                    .build();

            InlineKeyboardButton rejectButton = InlineKeyboardButton.builder()
                    .text("❌ Rad etish")
                    .callbackData("reject_" + app.getUserId())
                    .build();

            rows.add(List.of(acceptButton, rejectButton));

            markup.setKeyboard(rows);
            applicationMessage.setReplyMarkup(markup);

            try {
                bot.execute(applicationMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (callbackData.startsWith("accept_")) {
            Long acceptedUserId = Long.parseLong(callbackData.replace("accept_", ""));
            ApplicationData app = databaseManager.getApplicationByUserId(acceptedUserId);

            if (app != null) {
                String fullName = app.getFullName() != null ? app.getFullName() : "Kiritilmagan";
                String phone = app.getPhone() != null ? app.getPhone() : "Kiritilmagan";
                String username = app.getUsername() != null ? app.getUsername() : "no_username";
                String certificates = app.getCertificates() != null ? app.getCertificates() : "Kiritilmagan";
                String branch = app.getBranch() != null ? app.getBranch() : "Kiritilmagan";
                String cvFileId = app.getCvFileId() != null ? app.getCvFileId() : "";
                String jobPosition = app.getJobPosition() != null ? app.getJobPosition() : "Kiritilmagan";

                databaseManager.saveAcceptedApplication(
                        acceptedUserId,
                        fullName,
                        phone,
                        username,
                        certificates,
                        branch,
                        cvFileId,
                        jobPosition
                );

                databaseManager.deleteUserApplication(acceptedUserId);

                EditMessageText confirmedMsg = new EditMessageText();
                confirmedMsg.setChatId(chatId);
                confirmedMsg.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
                confirmedMsg.setText("✅ Ariza qabul qilindi va ro‘yxatga qo‘shildi!");
                try {
                    bot.execute(confirmedMsg);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                SendMessage userMessage = new SendMessage();
                userMessage.setChatId(acceptedUserId.toString());
                userMessage.setParseMode("HTML");
                userMessage.setText("""
                        🎉 <b>Siz qabul qilindingiz!</b>
                        
                        ✅ Sizning arizangiz ko‘rib chiqildi va siz tanlandingiz. Tez orada siz bilan bog‘lanishadi.
                        
                        📞 <i>Iltimos, telefoningizni doim yoqilgan holatda saqlang.</i>
                        """);

                InlineKeyboardMarkup menuMarkup = new InlineKeyboardMarkup();
                InlineKeyboardButton backToMenuButton = InlineKeyboardButton.builder()
                        .text("🏠 Bosh menyu")
                        .callbackData("back_to_menu")
                        .build();
                menuMarkup.setKeyboard(List.of(List.of(backToMenuButton)));
                userMessage.setReplyMarkup(menuMarkup);

                try {
                    bot.execute(userMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (callbackData.startsWith("reject_")) {
            Long rejectedUserId = Long.parseLong(callbackData.replace("reject_", ""));
            databaseManager.deleteUserApplication(rejectedUserId);

            EditMessageText rejectedMsg = new EditMessageText();
            rejectedMsg.setChatId(chatId);
            rejectedMsg.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
            rejectedMsg.setText("❌ Ariza rad etildi va o‘chirildi.");

            try {
                bot.execute(rejectedMsg);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

            SendMessage userMessage = new SendMessage();
            userMessage.setChatId(rejectedUserId.toString());
            userMessage.setParseMode("HTML");
            userMessage.setText("""
                    ❗ <b>Afsuski, arizangiz rad etildi.</b>
                    
                    ✉️ Biz bilan bog‘langaningiz uchun rahmat! Kelajakda omad tilaymiz.
                    """);

            InlineKeyboardMarkup menuMarkup = new InlineKeyboardMarkup();
            InlineKeyboardButton backToMenuButton = InlineKeyboardButton.builder()
                    .text("🏠 Bosh menyu")
                    .callbackData("back_to_menu")
                    .build();
            menuMarkup.setKeyboard(List.of(List.of(backToMenuButton)));
            userMessage.setReplyMarkup(menuMarkup);

            try {
                bot.execute(userMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if (callbackData.equals("view_accepted")) {
            List<AcceptedUser> acceptedUsers = databaseManager.getAllAcceptedUsers();

            if (acceptedUsers.isEmpty()) {
                SendMessage noAccepted = new SendMessage(chatId.toString(), "❗ Hozircha hech kim qabul qilinmagan.");
                try {
                    bot.execute(noAccepted);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                return;
            }

            SendMessage acceptedListMsg = new SendMessage(chatId.toString(), "✅ Qabul qilinganlar ro‘yxati:");
            InlineKeyboardMarkup acceptedMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> acceptedRows = new ArrayList<>();

            for (AcceptedUser user : acceptedUsers) {
                InlineKeyboardButton btn = InlineKeyboardButton.builder()
                        .text(user.getFullName())
                        .callbackData("accepted_user_" + user.getUserId())
                        .build();
                acceptedRows.add(List.of(btn));
            }

            acceptedMarkup.setKeyboard(acceptedRows);
            acceptedListMsg.setReplyMarkup(acceptedMarkup);

            try {
                bot.execute(acceptedListMsg);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (callbackData.startsWith("accepted_user_")) {
            Long selectedUserId = Long.parseLong(callbackData.replace("accepted_user_", ""));
            AcceptedUser accepted = databaseManager.getAcceptedUserById(selectedUserId);

            if (accepted == null) {
                SendMessage notFound = new SendMessage(chatId.toString(), "❗ Bu foydalanuvchi topilmadi.");
                try {
                    bot.execute(notFound);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                return;
            }

            String jobPositionId = accepted.getJobPosition();
            String jobName = "Kiritilmagan";
            if (jobPositionId != null && !jobPositionId.isEmpty()) {
                jobName = databaseManager.getJobPositionNameById(jobPositionId);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<b>✅ Qabul qilingan foydalanuvchi:</b>\n\n");
            sb.append("💼 <b>Lavozim:</b> ").append(jobName).append("\n\n");
            sb.append("🏢 <b>Filial:</b> ").append(accepted.getBranch() != null ? accepted.getBranch() : "Kiritilmagan").append("\n");
            sb.append("👤 <b>Ismi:</b> ").append(accepted.getFullName() != null ? accepted.getFullName() : "Kiritilmagan").append("\n");
            sb.append("📞 <b>Telefon:</b> ").append(accepted.getPhoneNumber() != null ? accepted.getPhoneNumber() : "Kiritilmagan").append("\n");
            sb.append("🔗 <b>Username:</b> @").append(accepted.getUsername() != null ? accepted.getUsername() : "no_username").append("\n");
            sb.append("🎓 <b>Sertifikat(lar):</b> ").append(accepted.getCertificates() != null ? accepted.getCertificates() : "Kiritilmagan").append("\n");

            SendMessage userInfo = new SendMessage(chatId.toString(), sb.toString());
            userInfo.setParseMode("HTML");

            try {
                bot.execute(userInfo);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

            String cvFileId = accepted.getCvFileId();
            if (cvFileId != null && !cvFileId.isEmpty()) {
                SendDocument cvDoc = new SendDocument();
                cvDoc.setChatId(chatId.toString());
                cvDoc.setDocument(new InputFile(cvFileId));
                cvDoc.setCaption("📄 <b>CV fayli:</b>");
                cvDoc.setParseMode("HTML");
                try {
                    bot.execute(cvDoc);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {
                SendMessage noCv = new SendMessage(chatId.toString(), "❗ CV mavjud emas.");
                noCv.setParseMode("HTML");
                try {
                    bot.execute(noCv);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        } else if (callbackData.equals("manage_jobs")) {
            List<Map<String, Object>> jobs = databaseManager.getAllJobPositionsWithStatus();

            if (jobs.isEmpty()) {
                SendMessage noJobs = new SendMessage(chatId.toString(), "❗ Hozircha ish o‘rinlari mavjud emas.");
                try {
                    bot.execute(noJobs);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return;
            }

            Map<Integer, Boolean> tempStatusMap = new HashMap<>();
            for (Map<String, Object> job : jobs) {
                int id = (int) job.get("id");
                boolean isActive = (boolean) job.get("is_active");
                tempStatusMap.put(id, isActive);
            }
            adminJobStatusMap.put(userId, tempStatusMap);

            SendMessage jobsMessage = new SendMessage(chatId.toString(), "⚙️ <b>Ish o‘rinlarini boshqarish:</b>");
            jobsMessage.setParseMode("HTML");

            InlineKeyboardMarkup markup = generateJobsInlineMarkup(tempStatusMap, jobs);
            jobsMessage.setReplyMarkup(markup);

            try {
                bot.execute(jobsMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (callbackData.startsWith("toggle_job_")) {
            int jobId = Integer.parseInt(callbackData.replace("toggle_job_", ""));
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

            Map<Integer, Boolean> statusMap = adminJobStatusMap.get(userId);
            if (statusMap != null && statusMap.containsKey(jobId)) {
                boolean current = statusMap.get(jobId);
                statusMap.put(jobId, !current);
            }

            List<Map<String, Object>> jobs = databaseManager.getAllJobPositionsWithStatus();

            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setParseMode("HTML");
            editMessage.setText("⚙️ <b>Ish o‘rinlarini boshqarish:</b>");

            InlineKeyboardMarkup markup = generateJobsInlineMarkup(statusMap, jobs);
            editMessage.setReplyMarkup(markup);

            try {
                bot.execute(editMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (callbackData.equals("save_jobs")) {
            Map<Integer, Boolean> statusMap = adminJobStatusMap.get(userId);
            if (statusMap != null) {
                for (Map.Entry<Integer, Boolean> entry : statusMap.entrySet()) {
                    databaseManager.updateJobPositionStatus(entry.getKey(), entry.getValue());
                }
            }

            adminJobStatusMap.remove(userId);

            EditMessageText savedMsg = new EditMessageText();
            savedMsg.setChatId(chatId);
            savedMsg.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
            savedMsg.setParseMode("HTML");
            savedMsg.setText("✅ O‘zgartirishlar muvaffaqiyatli saqlandi.");

            try {
                bot.execute(savedMsg);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if (callbackData.equals("cancel_jobs")) {
            adminJobStatusMap.remove(userId);

            EditMessageText cancelMsg = new EditMessageText();
            cancelMsg.setChatId(chatId);
            cancelMsg.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
            cancelMsg.setParseMode("HTML");
            cancelMsg.setText("❌ O‘zgartirishlar bekor qilindi.");

            try {
                bot.execute(cancelMsg);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if (callbackData.startsWith("view_cv_")) {
            long cvUserId = Long.parseLong(callbackData.replace("view_cv_", ""));
            ApplicationData app = databaseManager.getApplicationByUserId(cvUserId);

            if (app != null && app.getCvFileId() != null && !app.getCvFileId().isEmpty()) {
                String cvFileId = app.getCvFileId();

                try {
                    SendDocument cvDoc = new SendDocument();
                    cvDoc.setChatId(chatId.toString());
                    cvDoc.setDocument(new InputFile(cvFileId));
                    cvDoc.setCaption("📄 <b>CV (Document)</b>");
                    cvDoc.setParseMode("HTML");

                    bot.execute(cvDoc);
                } catch (TelegramApiException docEx) {
                    try {
                        SendPhoto cvPhoto = new SendPhoto();
                        cvPhoto.setChatId(chatId.toString());
                        cvPhoto.setPhoto(new InputFile(cvFileId));
                        cvPhoto.setCaption("🖼 <b>CV (Photo)</b>");
                        cvPhoto.setParseMode("HTML");

                        bot.execute(cvPhoto);
                    } catch (TelegramApiException photoEx) {
                        photoEx.printStackTrace();
                        SendMessage err = new SendMessage(chatId.toString(), "❗ CV ni yuborishda xatolik yuz berdi.");
                        try {
                            bot.execute(err);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                SendMessage noCv = new SendMessage(chatId.toString(), "❗ CV ni foydalanuvchi kiritmagan.");
                try {
                    bot.execute(noCv);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        } else if (callbackData.equals("add_new_job")) {
            SendMessage askName = new SendMessage(chatId.toString(), "📝 Iltimos, yangi ish sohasining nomini kiriting:");
            bot.getAdminStepMap().put(userId, "ADDING_JOB_NAME");
            try {
                bot.execute(askName);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if ("save_new_job".equals(callbackData)) {
            List<String> jobData = bot.getTempJobData().get(userId);

            if (jobData != null && jobData.size() == 3) {
                String jobName = jobData.get(0);
                String jobDescription = jobData.get(1);
                String jobRequirements = jobData.get(2);

                databaseManager.saveJobPosition(jobName, jobDescription, jobRequirements, true);

                bot.getTempJobData().remove(userId);
                bot.getAdminStepMap().remove(userId);

                EditMessageText savedMsg = new EditMessageText();
                savedMsg.setChatId(chatId);
                savedMsg.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
                savedMsg.setText("✅ Yangi ish so‘hasi muvaffaqiyatli saqlandi!");
                try {
                    bot.execute(savedMsg);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    bot.execute(new SendMessage(chatId.toString(), "❗ Ma'lumotlar to‘liq emas. Qaytadan urinib ko‘ring."));
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if ("cancel_new_job".equals(callbackData)) {
            bot.getTempJobData().remove(userId);
            bot.getAdminStepMap().remove(userId);

            EditMessageText cancelMsg = new EditMessageText();
            cancelMsg.setChatId(chatId);
            cancelMsg.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
            cancelMsg.setText("❌ Yangi ish so‘hasi qo‘shish bekor qilindi.");
            try {
                bot.execute(cancelMsg);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendAdminPanel(Long chatId) {
        SendMessage panel = new SendMessage(chatId.toString(), "✅ Admin panelga xush kelibsiz!");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(InlineKeyboardButton.builder()
                .text("📄 Arizalarni ko‘rish")
                .callbackData("view_applications")
                .build()));

        rows.add(List.of(InlineKeyboardButton.builder()
                .text("✅ Qabul qilinganlar")
                .callbackData("view_accepted")
                .build()));

        rows.add(List.of(InlineKeyboardButton.builder()
                .text("⚙️ Ish o‘rinlarini boshqarish")
                .callbackData("manage_jobs")
                .build()));

        rows.add(List.of(InlineKeyboardButton.builder()
                .text("❓ FAQni yangilash")
                .callbackData("update_faq")
                .build()));

        rows.add(List.of(InlineKeyboardButton.builder()
                .text("🔑 Parolni o‘zgartirish")
                .callbackData("change_password")
                .build()));

        markup.setKeyboard(rows);
        panel.setReplyMarkup(markup);

        try {
            bot.execute(panel);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendExcelReport(Long chatId, Integer days) {
        List<ApplicationData> applications;
        if (days != null) {
            LocalDateTime threshold = LocalDateTime.now(ZoneId.of("Asia/Tashkent")).minusDays(days);
            applications = databaseManager.getApplicationsByDate(threshold);
        } else {
            applications = databaseManager.getAllApplications();
        }

        if (applications.isEmpty()) {
            SendMessage noApps = new SendMessage(chatId.toString(), "❗ Tanlangan vaqt oralig‘ida arizalar mavjud emas.");
            try {
                bot.execute(noApps);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Applications");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Ism");
            headerRow.createCell(1).setCellValue("Telefon");
            headerRow.createCell(2).setCellValue("Filial");
            headerRow.createCell(3).setCellValue("Ish sohasi");
            headerRow.createCell(4).setCellValue("Sertifikatlar");
            headerRow.createCell(5).setCellValue("CV Link");

            for (int i = 0; i < applications.size(); i++) {
                ApplicationData app = applications.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(app.getFullName() != null ? app.getFullName() : "Kiritilmagan");
                row.createCell(1).setCellValue(app.getPhone() != null ? app.getPhone() : "Kiritilmagan");
                row.createCell(2).setCellValue(app.getBranch() != null ? app.getBranch() : "Kiritilmagan");
                row.createCell(3).setCellValue(databaseManager.getJobPositionNameById(app.getJobPosition()));
                row.createCell(4).setCellValue(app.getCertificates() != null && !app.getCertificates().equalsIgnoreCase("bosh") ? app.getCertificates() : "Kiritilmagan");
         //       String cvLink = "https://t.me/" + BOT_USERNAME + "?start=view_cv_" + app.getUserId();
             //   row.createCell(5).setCellValue(cvLink);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

            SendDocument document = new SendDocument();
            document.setChatId(chatId.toString());
            document.setDocument(new InputFile(bais, "Applications_" + LocalDateTime.now(ZoneId.of("Asia/Tashkent")).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx"));
            document.setCaption("📊 Arizalar ro‘yxati");

            try {
                bot.execute(document);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                SendMessage errorMsg = new SendMessage(chatId.toString(), "❗ Excel faylini yuborishda xatolik yuz berdi.");
                try {
                    bot.execute(errorMsg);
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            SendMessage errorMsg = new SendMessage(chatId.toString(), "❗ Excel faylini yaratishda xatolik yuz berdi.");
            try {
                bot.execute(errorMsg);
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private InlineKeyboardMarkup generateJobsInlineMarkup(Map<Integer, Boolean> statusMap, List<Map<String, Object>> jobs) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        jobs.sort(Comparator.comparing(job -> (int) job.get("id")));

        for (Map<String, Object> job : jobs) {
            int id = (int) job.get("id");
            String name = (String) job.get("name");
            boolean active = statusMap.getOrDefault(id, false);

            String emoji = active ? "✅" : "⬜️";
            String buttonText = emoji + " " + name;

            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(buttonText)
                    .callbackData("toggle_job_" + id)
                    .build();

            rows.add(List.of(button));
        }

        InlineKeyboardButton addJobButton = InlineKeyboardButton.builder()
                .text("➕ Yangi ish soha qo‘shish")
                .callbackData("add_new_job")
                .build();

        rows.add(List.of(addJobButton));

        InlineKeyboardButton saveButton = InlineKeyboardButton.builder()
                .text("✅ Saqlash")
                .callbackData("save_jobs")
                .build();

        InlineKeyboardButton cancelButton = InlineKeyboardButton.builder()
                .text("❌ Bekor qilish")
                .callbackData("cancel_jobs")
                .build();

        rows.add(List.of(saveButton, cancelButton));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
}