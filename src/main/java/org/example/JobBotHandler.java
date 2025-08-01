package org.example;

import org.example.model.JobPosition;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class JobBotHandler extends TelegramLongPollingBot {

    private final DatabaseManager databaseManager = new DatabaseManager();
    private final Map<Long, Map<String, String>> userApplicationData = new HashMap<>();
    private final Map<Long, Integer> userStepMap = new HashMap<>();
    private final Map<Long, String> adminStepMap = new HashMap<>();
    private final Map<Long, List<String>> tempJobData = new HashMap<>();
    private final AdminCommandHandler adminCommandHandler = new AdminCommandHandler(this, databaseManager);

    public Map<Long, String> getAdminStepMap() {
        return adminStepMap;
    }

    public Map<Long, List<String>> getTempJobData() {
        return tempJobData;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Long chatId = update.getMessage().getChatId();
            long userId = update.getMessage().getFrom().getId();
            String text = update.getMessage().hasText() ? update.getMessage().getText() : "";
            int step = userStepMap.getOrDefault(userId, 1);

            if (text.equals("/admin") || adminStepMap.containsKey(userId)) {
                adminCommandHandler.handleAdminCommand(update);
                return;
            }

            if (userStepMap.containsKey(userId)) {
                Map<String, String> data = userApplicationData.getOrDefault(userId, new HashMap<>());

                switch (step) {
                    case 1 -> {
                        if (!text.isEmpty()) {
                            data.put("full_name", text);
                            userApplicationData.put(userId, data);
                            userStepMap.put(userId, 2);
                            sendQuestionByStep(chatId, userId);
                        }
                    }
                    case 2 -> {
                        if (update.getMessage().hasContact()) {
                            String phone = update.getMessage().getContact().getPhoneNumber();
                            data.put("phone", phone);
                            userApplicationData.put(userId, data);
                            userStepMap.put(userId, 3);
                            sendQuestionByStep(chatId, userId);
                            return;
                        }
                        if (text.equals("✏️ Raqamni qo‘lda kiritish")) {
                            SendMessage askPhone = new SendMessage();
                            askPhone.setChatId(chatId);
                            askPhone.setText("📞 Iltimos, telefon raqamingizni quyidagi formatda kiriting:\n\n*+998901234567*\n\nYoki *998901234567* ko‘rinishida.");
                            askPhone.setParseMode("Markdown");
                            try {
                                execute(askPhone);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                        if (!text.isEmpty()) {
                            String regex = "^(\\+998|998)\\d{9}$";
                            if (text.matches(regex)) {
                                data.put("phone", text);
                                userApplicationData.put(userId, data);
                                userStepMap.put(userId, 3);
                                sendQuestionByStep(chatId, userId);
                            } else {
                                SendMessage warn = new SendMessage();
                                warn.setChatId(chatId);
                                warn.setText("❗️ Telefon raqam formati noto‘g‘ri!\n\nTo‘g‘ri format: *+998901234567* yoki *998901234567*");
                                warn.setParseMode("Markdown");
                                try {
                                    execute(warn);
                                } catch (TelegramApiException e) {
                                    e.printStackTrace();
                                }
                            }
                            return;
                        }
                    }
                    case 3 -> {
                        if (!text.isEmpty()) {
                            data.put("address", text);
                            userApplicationData.put(userId, data);
                            userStepMap.put(userId, 4);
                            sendQuestionByStep(chatId, userId);
                        }
                    }
                    case 4 -> {
                        if (!text.isEmpty()) {
                            data.put("certificates", text);
                            userApplicationData.put(userId, data);
                            userStepMap.put(userId, 5);
                            sendQuestionByStep(chatId, userId);
                        }
                    }
                    case 5 -> {
                        if (update.getMessage().hasPhoto()) {
                            String fileId = update.getMessage().getPhoto().get(update.getMessage().getPhoto().size() - 1).getFileId();
                            data.put("cv_file_id", fileId);
                            userApplicationData.put(userId, data);
                            userStepMap.put(userId, 6);
                            sendQuestionByStep(chatId, userId);
                        } else if (update.getMessage().hasDocument()) {
                            String mimeType = update.getMessage().getDocument().getMimeType();
                            String fileId = update.getMessage().getDocument().getFileId();
                            if (mimeType.equals("application/pdf") || mimeType.equals("image/jpeg") || mimeType.equals("image/png")) {
                                data.put("cv_file_id", fileId);
                                userApplicationData.put(userId, data);
                                userStepMap.put(userId, 6);
                                sendQuestionByStep(chatId, userId);
                            } else {
                                SendMessage warn = new SendMessage(chatId.toString(), "❗️ Faqat PDF, JPG yoki PNG formatdagi CV yuboring.");
                                try {
                                    execute(warn);
                                } catch (TelegramApiException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            SendMessage warn = new SendMessage(chatId.toString(), "❗️ Iltimos, CV faylini yuboring (PDF, JPG yoki PNG).");
                            try {
                                execute(warn);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                        return;
                    }
                    case 6 -> {
                        if (!text.isEmpty()) {
                            data.put("branch", text);
                            userApplicationData.put(userId, data);
                            sendQuestionByStep(chatId, userId);
                        }
                    }
                }
                return;
            }

            switch (text) {
                case "/start" -> {
                    userStepMap.remove(userId);
                    userApplicationData.remove(userId);
                    sendStartMessage(chatId);
                }
                case "📢 Bo‘sh ish o‘rinlari" -> sendJobPositions(chatId);
                case "❓ Ko‘p so‘raladigan savollar" -> sendFAQMessage(chatId);
                case "ℹ️ Ma’lumot" -> sendInfoMessage(chatId);
                case "🌐 Tilni o‘zgartirish" -> sendLanguageMessage(chatId);
                default -> sendUnknownCommand(chatId);
            }
        }

        if (update.hasCallbackQuery()) {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            String callbackData = update.getCallbackQuery().getData();
            long userId = update.getCallbackQuery().getFrom().getId();

            if (callbackData.startsWith("job_")) {
                String jobId = callbackData.replace("job_", "");
                Map<String, String> data = userApplicationData.getOrDefault(userId, new HashMap<>());
                data.put("job_position", jobId);
                userApplicationData.put(userId, data);
                sendJobSelectedMessage(chatId, jobId);
            } else if (callbackData.startsWith("ariza_")) {
                String jobId = callbackData.replace("ariza_", "");
                handleApplicationStart(chatId, userId, jobId);
            } else if (callbackData.startsWith("confirm_save_")) {
                userId = Long.parseLong(callbackData.replace("confirm_save_", ""));
                Map<String, String> data = userApplicationData.getOrDefault(userId, new HashMap<>());
                String username = update.getCallbackQuery().getFrom().getUserName();
                if (username == null) username = "no_username";
                databaseManager.saveUserApplication(userId, data, username);

                SendMessage doneMsg = new SendMessage();
                doneMsg.setChatId(chatId);
                doneMsg.setText("🎉 Ma'lumotlaringiz muvaffaqiyatli saqlandi! Rahmat.");
                doneMsg.setParseMode("HTML");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                InlineKeyboardButton backToMenu = InlineKeyboardButton.builder()
                        .text("🏠 Bosh menyuga qaytish")
                        .callbackData("back_to_menu")
                        .build();
                rows.add(List.of(backToMenu));
                markup.setKeyboard(rows);
                doneMsg.setReplyMarkup(markup);

                try {
                    execute(doneMsg);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }

                userApplicationData.remove(userId);
                userStepMap.remove(userId);
            } else if (callbackData.equals("cert_ielts")) {
                InlineKeyboardMarkup ieltsKeyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<String> levels = List.of("6.5", "7.0", "7.5", "8.0", "8.5", "9.0");

                for (String level : levels) {
                    rows.add(List.of(InlineKeyboardButton.builder()
                            .text(level)
                            .callbackData("ielts_" + level)
                            .build()));
                }

                ieltsKeyboard.setKeyboard(rows);

                EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
                edit.setChatId(chatId.toString());
                edit.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
                edit.setReplyMarkup(ieltsKeyboard);

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if (callbackData.equals("cert_cefr")) {
                InlineKeyboardMarkup cefrKeyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<String> levels = List.of("B2", "C1", "C2");

                for (String level : levels) {
                    rows.add(List.of(InlineKeyboardButton.builder()
                            .text(level)
                            .callbackData("cefr_" + level)
                            .build()));
                }

                cefrKeyboard.setKeyboard(rows);

                EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
                edit.setChatId(chatId.toString());
                edit.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
                edit.setReplyMarkup(cefrKeyboard);

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if (callbackData.startsWith("ielts_")) {
                String selectedLevel = callbackData.replace("ielts_", "");
                Map<String, String> data = userApplicationData.getOrDefault(userId, new HashMap<>());
                data.put("certificates", "IELTS - " + selectedLevel);
                userApplicationData.put(userId, data);
                userStepMap.put(userId, 5);

                EditMessageReplyMarkup clearMarkup = new EditMessageReplyMarkup();
                clearMarkup.setChatId(chatId.toString());
                clearMarkup.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
                InlineKeyboardMarkup emptyMarkup = new InlineKeyboardMarkup();
                emptyMarkup.setKeyboard(new ArrayList<>());
                clearMarkup.setReplyMarkup(emptyMarkup);

                try {
                    execute(clearMarkup);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                SendMessage selectedMsg = new SendMessage();
                selectedMsg.setChatId(chatId.toString());
                selectedMsg.setText("✅ Sertifikat: IELTS daraja " + selectedLevel + " tanlandi!");
                try {
                    execute(selectedMsg);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                sendQuestionByStep(chatId, userId);
            } else if (callbackData.startsWith("cefr_")) {
                String selectedLevel = callbackData.replace("cefr_", "").toUpperCase();
                Map<String, String> data = userApplicationData.getOrDefault(userId, new HashMap<>());
                data.put("certificates", "CEFR - " + selectedLevel);
                userApplicationData.put(userId, data);
                userStepMap.put(userId, 5);

                EditMessageReplyMarkup clearMarkup = new EditMessageReplyMarkup();
                clearMarkup.setChatId(chatId.toString());
                clearMarkup.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
                InlineKeyboardMarkup emptyMarkup = new InlineKeyboardMarkup();
                emptyMarkup.setKeyboard(new ArrayList<>());
                clearMarkup.setReplyMarkup(emptyMarkup);

                try {
                    execute(clearMarkup);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                SendMessage selectedMsg = new SendMessage();
                selectedMsg.setChatId(chatId.toString());
                selectedMsg.setText("✅ Sertifikat: CEFR daraja " + selectedLevel + " tanlandi!");
                try {
                    execute(selectedMsg);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                sendQuestionByStep(chatId, userId);
            } else if (callbackData.startsWith("branch_")) {
                String selectedBranch = callbackData.replace("branch_", "");
                Map<String, String> data = userApplicationData.getOrDefault(userId, new HashMap<>());
                data.put("branch", selectedBranch);
                userApplicationData.put(userId, data);

                EditMessageReplyMarkup clearMarkup = new EditMessageReplyMarkup();
                clearMarkup.setChatId(chatId.toString());
                clearMarkup.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
                InlineKeyboardMarkup emptyMarkup = new InlineKeyboardMarkup();
                emptyMarkup.setKeyboard(new ArrayList<>());
                clearMarkup.setReplyMarkup(emptyMarkup);

                try {
                    execute(clearMarkup);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                SendMessage branchSelectedMsg = new SendMessage();
                branchSelectedMsg.setChatId(chatId.toString());
                branchSelectedMsg.setText("✅ Siz tanlagan filial: <b>" + selectedBranch + "</b>");
                branchSelectedMsg.setParseMode("HTML");

                try {
                    execute(branchSelectedMsg);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                sendConfirmationButtons(chatId, userId);
            } else if (callbackData.equals("back_to_menu")) {
                sendStartMessage(chatId);
            } else if (callbackData.equals("skip_step_4")) {
                userStepMap.put(userId, 5);

                EditMessageText clearedText = new EditMessageText();
                clearedText.setChatId(chatId);
                clearedText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
                clearedText.setText("☑️ Sertifikat yuborish bosqichi o‘tkazib yuborildi.");
                clearedText.setParseMode("HTML");

                try {
                    execute(clearedText);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                sendQuestionByStep(chatId, userId);
            } else if (callbackData.startsWith("cancel_save_")) {
                long cancelUserId = Long.parseLong(callbackData.replace("cancel_save_", ""));
                userStepMap.remove(cancelUserId);
                userApplicationData.remove(cancelUserId);

                EditMessageText cancelMessage = new EditMessageText();
                cancelMessage.setChatId(chatId);
                cancelMessage.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
                cancelMessage.setParseMode("HTML");
                cancelMessage.setText("""
                        ❌ <b>Ariza bekor qilindi.</b>
                        
                        Agar xohlasangiz, arizani boshidan to‘ldirishingiz yoki bosh menyuga qaytishingiz mumkin.
                        """);

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<InlineKeyboardButton> row = List.of(
                        InlineKeyboardButton.builder()
                                .text("🔄 Boshidan boshlash")
                                .callbackData("restart_application_" + cancelUserId)
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🏠 Bosh menyu")
                                .callbackData("back_to_menu")
                                .build()
                );
                markup.setKeyboard(List.of(row));
                cancelMessage.setReplyMarkup(markup);

                try {
                    execute(cancelMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (callbackData.startsWith("restart_application_")) {
                sendJobPositions(chatId);
            }

            adminCommandHandler.handleAdminCallback(update);
        }
    }

    private void sendStartMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setParseMode("HTML");
        message.setText("""
                👋 <b>Assalomu alaykum!</b>
                
                Namangan International School ishga qabul qilish botiga xush kelibsiz!
                
                Quyidagi bo‘limlardan birini tanlang:
                """);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📢 Bo‘sh ish o‘rinlari");
        row1.add("❓ Ko‘p so‘raladigan savollar");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("ℹ️ Ma’lumot");
        row2.add("🌐 Tilni o‘zgartirish");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFAQMessage(Long chatId) {
        String faqContent = databaseManager.getFAQ();
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setParseMode("HTML");
        message.setText("<b>❓ Ko‘p so‘raladigan savollar:</b>\n\n" + faqContent);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendConfirmationButtons(Long chatId, long userId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("✅ Hamma ma'lumotlar tayyor. Saqlashni tasdiqlaysizmi?");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> row = List.of(
                InlineKeyboardButton.builder().text("✅ Ha, saqlash").callbackData("confirm_save_" + userId).build(),
                InlineKeyboardButton.builder().text("❌ Bekor qilish").callbackData("cancel_save_" + userId).build()
        );
        markup.setKeyboard(List.of(row));
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendJobPositions(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setParseMode("HTML");

        List<JobPosition> positions = databaseManager.getJobPositions();

        StringBuilder textBuilder = new StringBuilder("📋 <b>Bo‘sh ish o‘rinlari</b>\n\n");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        int index = 1;
        for (JobPosition job : positions) {
            textBuilder.append(index++).append("️⃣ <b>")
                    .append(job.getName())
                    .append("</b> — ").append(job.getDescription()).append("\n");

            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(job.getName())
                    .callbackData("job_" + job.getId())
                    .build();
            keyboard.add(Collections.singletonList(button));
        }

        message.setText(textBuilder.toString());

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        inlineKeyboard.setKeyboard(keyboard);
        message.setReplyMarkup(inlineKeyboard);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleApplicationStart(Long chatId, long userId, String jobId) {
        Map<String, String> data = userApplicationData.getOrDefault(userId, new HashMap<>());
        data.put("job_id", jobId);
        userApplicationData.put(userId, data);
        userStepMap.put(userId, 1);
        sendQuestionByStep(chatId, userId);
    }

    private void sendQuestionByStep(Long chatId, long userId) {
        int step = userStepMap.getOrDefault(userId, 1);
        String question = "";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        switch (step) {
            case 1 -> question = "1️⃣ Iltimos, to‘liq ismingizni yozing (F.I.Sh):";
            case 2 -> {
                question = "2️⃣ Telefon raqamingizni yuboring:";
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);

                KeyboardRow row = new KeyboardRow();
                org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton contactButton =
                        new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton("📱 Kontakt ulashish");
                contactButton.setRequestContact(true);
                row.add(contactButton);

                KeyboardRow row2 = new KeyboardRow();
                row2.add("✏️ Raqamni qo‘lda kiritish");

                List<KeyboardRow> keyboard = new ArrayList<>();
                keyboard.add(row);
                keyboard.add(row2);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
            }
            case 3 -> question = "3️⃣ Manzilingizni kiriting (viloyat, tuman):";
            case 4 -> {
                question = "4️⃣ IELTS, CEFR yoki boshqa sertifikatlaringiz bo‘lsa yozing yoki quyidagilardan tanlang:";

                InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> row1 = new ArrayList<>();
                row1.add(InlineKeyboardButton.builder().text("🎓 IELTS").callbackData("cert_ielts").build());
                row1.add(InlineKeyboardButton.builder().text("📝 CEFR").callbackData("cert_cefr").build());
                rows.add(row1);
                List<InlineKeyboardButton> row2 = new ArrayList<>();
                row2.add(InlineKeyboardButton.builder()
                        .text("⏭ O‘tkazib yuborish")
                        .callbackData("skip_step_4")
                        .build());
                rows.add(row2);
                inlineKeyboard.setKeyboard(rows);
                message.setReplyMarkup(inlineKeyboard);
            }
            case 5 -> question = "5️⃣ Ozingiz haqingizdagi malumotni(Rezume)  PDF, JPG yoki PNG formatda yuboring:";
            case 6 -> {
                question = "6️⃣ Qaysi filialda ishlamoqchisiz?";
                InlineKeyboardMarkup branchKeyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                rows.add(List.of(InlineKeyboardButton.builder()
                        .text("📍 Namangan 1")
                        .callbackData("branch_Filial 1")
                        .build()));
                rows.add(List.of(InlineKeyboardButton.builder()
                        .text("📍 Pop 2")
                        .callbackData("branch_Filial 2")
                        .build()));
                rows.add(List.of(InlineKeyboardButton.builder()
                        .text("📍 Uychi 3")
                        .callbackData("branch_Filial 3")
                        .build()));
                rows.add(List.of(InlineKeyboardButton.builder()
                        .text("📍 Chortoq 4")
                        .callbackData("branch_Filial 4")
                        .build()));
                rows.add(List.of(InlineKeyboardButton.builder()
                        .text("📍 Chust 5")
                        .callbackData("branch_Filial 5")
                        .build()));

                branchKeyboard.setKeyboard(rows);
                message.setReplyMarkup(branchKeyboard);
            }



            default -> question = "❗️ Noma'lum bosqich.";
        }

        message.setText(question);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendInfoMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setParseMode("HTML");
        message.setText("""
                ℹ️ <b>Namangan International School</b>
                
                📚 Biz haqimizda batafsil ma’lumot olish uchun rasmiy veb-saytimizga tashrif buyuring.
                """);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendLanguageMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setParseMode("HTML");
        message.setText("""
                🌐 <b>Tilni tanlang:</b>
                
                🇺🇿 O‘zbekcha
                🇬🇧 English
                """);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendJobSelectedMessage(Long chatId, String jobId) {
        JobPosition job = databaseManager.getJobPositionById(Integer.parseInt(jobId));
        if (job == null) {
            sendUnknownCommand(chatId);
            return;
        }

        String text = "✅ Siz <b>" + job.getName() + "</b> ish o‘rnini tanladingiz.\n\n" +
                "<b>Yo‘nalish:</b> " + job.getDescription() + "\n\n" +
                job.getRequirements();

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setParseMode("HTML");
        message.setText(text);

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton applyButton = InlineKeyboardButton.builder()
                .text("📄 Ariza topshirish")
                .callbackData("ariza_" + jobId)
                .build();

        inlineKeyboard.setKeyboard(List.of(List.of(applyButton)));
        message.setReplyMarkup(inlineKeyboard);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendUnknownCommand(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("❗ Noma'lum buyruq. Iltimos, menyudan tanlang.");
        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "https://t.me/easyVocabularystart_bot";
    }

    @Override
    public String getBotToken() {
        return "8162079159:AAF6v6gupfLyopT5xzd60ZOZRvDkPTg8Mfc";
    }
}