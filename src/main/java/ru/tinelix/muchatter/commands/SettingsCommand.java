package ru.tinelix.muchatter.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.sql.ResultSet;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.tinelix.muchatter.core.BotCommand;
import ru.tinelix.muchatter.core.Locale;
import ru.tinelix.muchatter.core.MuChatter;
import ru.tinelix.muchatter.db.DatabaseEngine;
import ru.tinelix.muchatter.db.SQLProcessor;
import ru.tinelix.muchatter.utils.DoubleArrayList;

public class SettingsCommand extends BotCommand {

    public static final String COMMAND_NAME = "Settings";
    public static final String EDIT_USER_SETTINGS_CALLBACK            = "editUserSettings";
    public static final String SET_USER_SETTINGS_CALLBACK             = "setUserSettings";

    public SettingsCommand(MuChatter chatter, DatabaseEngine dbEngine) {
        super(chatter, dbEngine);
    }

    public void run() {
        try {
            ResultSet userDbResult = SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, null);
            ResultSet userDbSettingsResult = SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, "settings");

            String lang = userDbSettingsResult.getString("ui_language");
            int tzOffset = userDbSettingsResult.getInt("timezone");
            int formsLength = Locale.getLocaleArrayLength(lang, "user_settings_forms");
            String user_forms = "";

            SendMessage message = null;

            if(!mTgChat.getId().equals(mTgFrom.getId())) {
                message = new SendMessage(
                    Long.toString(mTgChat.getId()),
                    String.format(
                        "<B>%s</B>\r\n\r\n🚫 %s",
                        Locale.translate(lang, "error_title"),
                        Locale.translate(lang, "error_reasons", 0)
                    )
                );
            } else {

                for(int i = 0; i < formsLength; i++) {

                    String value = "";

                    switch(i) {
                        case 0:
                            value = Locale.getLanguageName(lang);
                            break;
                        case 1:
                            int tz = userDbSettingsResult.getInt(i + 2);

                            value = String.format(
                                        "UTC%s%02d:%02d",
                                        tz >= 0 ? "+" : "-",
                                        (int)Math.floor(tz / 60), tz % 60
                                    );
                            break;
                        case 2:
                        case 3:
                            value = Locale.translate(lang, "on_off", userDbSettingsResult.getInt(i + 2));
                            break;
                        default:
                            value = userDbSettingsResult.getString(i + 2);
                            break;
                    }

                    user_forms += String.format(
                        "%s: <I>%s</I>\r\n",
                        Locale.translate(lang, "user_settings_forms", i),
                        value
                    );
                }

                message = new SendMessage(
                    Long.toString(mTgChat.getId()),
                    String.format(
                        "<B>%s</B>\r\n\r\n%s",
                        Locale.translate(lang, "settings_categories", 0),
                        user_forms
                    )
                );

            }

            InlineKeyboardMarkup markupInline = InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        createInlineButton(
                            Locale.translate(lang, "settings_buttons", 0),
                            SettingsCommand.EDIT_USER_SETTINGS_CALLBACK
                        )
                    )
                ).build();

            message.setReplyMarkup(markupInline);

            message.setParseMode("HTML");

            mChatter.getTelegramClient().execute(message);
        } catch(Exception e) {
            mChatter.onError(e.getMessage());
            e.printStackTrace();
        }
    }

    public void update(long msgId) {
        try {
            ResultSet userDbResult = SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, null);
            ResultSet userDbSettingsResult = SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, "settings");

            String lang = userDbSettingsResult.getString("ui_language");
            int formsLength = Locale.getLocaleArrayLength(lang, "user_settings_forms");
            String forms = "";

            DoubleArrayList<InlineKeyboardButton> buttons = new DoubleArrayList<>();

            EditMessageText message = null;

            if(mMsgText.equals(SettingsCommand.EDIT_USER_SETTINGS_CALLBACK)) {

                for(int i = 0; i < formsLength; i++) {
                    buttons.addToInnerArray(
                        (int)Math.floor(i / 3), i % 3,
                        createInlineButton(
                            Locale.translate(lang, "user_settings_forms", i),
                            SettingsCommand.SET_USER_SETTINGS_CALLBACK + "_" + Integer.toString(i)
                        )
                    );
                }

                message = EditMessageText.builder()
                    .chatId(mTgChat.getId())
                    .messageId((int)msgId)
                    .text(Locale.translate(lang, "settings_edit_areas"))
                    .build();

                InlineKeyboardMarkup markupInline = InlineKeyboardMarkup
                    .builder()
                    .keyboardRow(new InlineKeyboardRow(buttons.get(0)))
                    .keyboardRow(new InlineKeyboardRow(buttons.get(1)))
                    .build();

                message.setReplyMarkup(markupInline);

            } else if(mMsgText.startsWith(SettingsCommand.SET_USER_SETTINGS_CALLBACK + "_cancel")) {

            } else if(mMsgText.startsWith(SettingsCommand.SET_USER_SETTINGS_CALLBACK + "_")) {

                String msgText = "";

                int index = Integer.parseInt(mMsgText.split("_")[1]);
                boolean hasBoolValue = mMsgText.split("_").length >= 3 &&
                                       (mMsgText.split("_")[2].equals("on") || mMsgText.split("_")[2].equals("off"));

                boolean hasNumberValue = mMsgText.split("_").length >= 3 && parseInt(mMsgText.split("_")[2]) >= 0;
                int numberValue = -1;

                if(hasNumberValue) {
                    numberValue = parseInt(mMsgText.split("_")[2]);
                }

                InlineKeyboardMarkup markupInline = null;

                if((index >= 2 && index <= 3)) {

                    if(hasBoolValue)
                        SQLProcessor.updateUserIntoDb(mChatter, mDatabase, mTgFrom,
                                index, !(userDbSettingsResult.getBoolean(index + 2)), "settings"
                        );

                    userDbSettingsResult = SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, "settings");

                    msgText = String.format("<B>%s</B>\r\n\r\n", Locale.translate(lang, "settings_title"))
                            + String.format(Locale.translate(lang, "settings_parameter"),
                                            Locale.translate(lang, "user_settings_forms", index),
                                            Locale.translate(lang, "on_off", userDbSettingsResult.getInt(index + 2))
                              );
                    buttons.addToInnerArray(
                            0, 0,
                            createInlineButton(
                                userDbSettingsResult.getBoolean(index + 2) ?
                                            Locale.translate(lang, "settings_buttons", 2) :
                                            Locale.translate(lang, "settings_buttons", 1),
                                            SettingsCommand.SET_USER_SETTINGS_CALLBACK + "_" + index +
                                                (userDbSettingsResult.getBoolean(index + 2) ? "_on" : "_off")
                            )
                        );

                    markupInline = InlineKeyboardMarkup
                        .builder()
                        .keyboardRow(new InlineKeyboardRow(buttons.get(0)))
                        .build();

                } else if(index == 0){

                    String[] locales = Locale.getAvailableLocales();

                    if(!hasNumberValue) {
                        msgText = String.format("<B>%s</B>\r\n\r\n", Locale.translate(lang, "settings_title"))
                                + String.format(Locale.translate(lang, "settings_parameter"),
                                                Locale.translate(lang, "user_settings_forms", index),
                                                Locale.getLanguageName(lang)
                                ) + "\r\n\r\n<B>Available languages:</B>\r\n";

                        for(int i = 0; i < locales.length; i++) {
                            msgText += (i + 1) + ". " + Locale.getRemoteLanguageName(locales[i]) + "\r\n";
                        }

                        markupInline = createT9Layout(mMsgText, 1, 2, false);
                    } else {
                        String newLangCode = locales[numberValue - 1].split("\\.")[0];

                        SQLProcessor.updateUserIntoDb(mChatter, mDatabase, mTgFrom,
                                index, newLangCode, "settings"
                        );
                        msgText = String.format("<B>%s</B>\r\n\r\n", Locale.translate(newLangCode, "settings_done"));
                        run();
                    }
                } else {
                    msgText = String.format("%s\r\n\r\n", Locale.translate(lang, "not_implemented"));
                }

                message = EditMessageText.builder()
                    .chatId(mTgChat.getId())
                    .messageId((int)msgId)
                    .text(msgText)
                    .build();

                message.setReplyMarkup(markupInline);
            }

            message.setParseMode("HTML");

            mChatter.getTelegramClient().execute(message);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private int parseInt(String numberStr) {
        int numberValue = 0;

        try {
            numberValue = Integer.parseInt(numberStr);
        } catch(Exception ex) {
            numberValue = -1;
        }
        return numberValue;
    }
}
