package ru.tinelix.muchatter.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.sql.ResultSet;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.InlineKeyboardMarkupBuilder;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.tinelix.muchatter.commands.ChatBridgeCommand;
import ru.tinelix.muchatter.core.BotCommand;
import ru.tinelix.muchatter.core.Locale;
import ru.tinelix.muchatter.core.MuChatter;
import ru.tinelix.muchatter.db.DatabaseEngine;
import ru.tinelix.muchatter.db.SQLCreator;
import ru.tinelix.muchatter.db.SQLCreator.ExtendedColumn;
import ru.tinelix.muchatter.db.SQLProcessor;
import ru.tinelix.muchatter.utils.DoubleArrayList;

public class SettingsCommand extends BotCommand {

    public static final String COMMAND_NAME = "Settings";

    public static final String CHANGE_SETTINGS_CALLBACK                     = "changeSettings";
    public static final String SET_SETTINGS_CALLBACK                        = "setSettings";
    public static final String SHOW_ADDITIONAL_SETTINGS_CALLBACK            = "showAdditionalSettings";

    public SettingsCommand(MuChatter chatter, DatabaseEngine dbEngine) {
        super(chatter, dbEngine);
    }

    public void run() {
        try {

            ArrayList<ExtendedColumn> columns = getSettingsColumns();

            ResultSet userDbResult = SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, null);
            String forms = "";

            DoubleArrayList<InlineKeyboardButton> buttons = new DoubleArrayList<>();

            SendMessage message = null;

            String msgText = "";

            int formsLength = 0;

            if(mIsGroupChat) {

                GetChatMember getChatMember = GetChatMember.builder()
                                                        .userId(mTgFrom.getId())
                                                        .chatId(mTgChat.getId())
                                                        .build();

                ChatMember member = mChatter.getTelegramClient().execute(getChatMember);

                if(!member.getStatus().equals("administrator") && !member.getStatus().equals("creator")) {
                    sendErrorMessage(2);
                    return;
                }
            }

            for(int i = 0; i < columns.size(); i++) {

                String type = columns.get(i).getType();

                String value = "";

                if(columns.get(i).readOnly)
                    continue;

                switch(type) {
                    case "lang":
                        value = Locale.getLanguageName(mUiLanguage);
                        break;
                    case "utc":
                        int tz = mPrimarySettings.getInt(i + 2);

                        value = String.format(
                                    "UTC%s%02d:%02d",
                                    tz >= 0 ? "+" : "-",
                                    (int)Math.floor(tz / 60), tz % 60
                                );
                        break;
                    case "bool":
                    case "set":
                        value = Locale.translate(mUiLanguage, "on_off", mPrimarySettings.getInt(i + 2));
                        break;
                    default:
                        value = mPrimarySettings.getString(i + 2);
                        break;
                }

                formsLength++;

                forms += String.format(
                    "%s: <I>%s</I>\r\n",
                    Locale.translate(
                        mUiLanguage, mIsGroupChat ?
                                "group_settings_forms" : "user_settings_forms",
                        i
                    ),
                    value
                );

                msgText = forms;
            }

            buttons.addToInnerArray(
                0, 0, createInlineButton(
                        Locale.translate(mUiLanguage, "settings_buttons", 0),
                        SettingsCommand.CHANGE_SETTINGS_CALLBACK
                      )
            );

            InlineKeyboardMarkup markupInline = InlineKeyboardMarkup
                .builder()
                .keyboardRow(new InlineKeyboardRow(buttons.get(0)))
                .build();

            message = SendMessage.builder()
                    .chatId(mTgChat.getId())
                    .text(msgText)
                    .build();

            if(markupInline != null)
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
            ArrayList<ExtendedColumn> columns = getSettingsColumns();

            String forms = "";
            String msgText = "";

            DoubleArrayList<InlineKeyboardButton> buttons = new DoubleArrayList<>();

            EditMessageText message = null;

            InlineKeyboardMarkup markupInline = null;

            int formsLength = 0;

            if(mIsGroupChat) {

                GetChatMember getChatMember = GetChatMember.builder()
                                                        .userId(mTgFrom.getId())
                                                        .chatId(mTgChat.getId())
                                                        .build();

                ChatMember member = mChatter.getTelegramClient().execute(getChatMember);

                if(!member.getStatus().equals("administrator") && !member.getStatus().equals("creator")) {
                    sendErrorMessage(2);
                    return;
                }
            }

            if(mMsgText.startsWith(SettingsCommand.CHANGE_SETTINGS_CALLBACK)) {

                for(int i = 0; i < columns.size(); i++) {
                    if(columns.get(i).readOnly)
                        continue;

                    buttons.addToInnerArray(
                        (int)Math.floor(formsLength / 3), formsLength % 3,
                        createInlineButton(
                            Locale.translate(mUiLanguage, mIsGroupChat ?
                                                        "group_settings_forms" : "user_settings_forms",
                                             i),
                            SettingsCommand.SET_SETTINGS_CALLBACK + "_" + Integer.toString(i)
                        )
                    );

                    formsLength++;
                }

                msgText = Locale.translate(mUiLanguage, "settings_edit_areas");

            } else if(mMsgText.startsWith(SettingsCommand.SET_SETTINGS_CALLBACK + "_cancel")) {

            } else if(mMsgText.startsWith(SettingsCommand.SET_SETTINGS_CALLBACK + "_")) {

                int index = Integer.parseInt(mMsgText.split("_")[1]);

                String columnType = columns.get(index).getType();

                String columnAdditionalCallback = columns.get(index).getAdditionalSettingsCallback();

                boolean hasBoolValue = mMsgText.split("_").length >= 3 &&
                                       (mMsgText.split("_")[2].equals("on") || mMsgText.split("_")[2].equals("off"));

                boolean hasNumberValue = mMsgText.split("_").length >= 3 && parseInt(mMsgText.split("_")[2]) >= 0;

                int numberValue = -1;

                if(hasNumberValue) {
                    numberValue = parseInt(mMsgText.split("_")[2]);
                }

                if(columnType.equals("bool")) {

                    if(hasBoolValue) {
                        if(mIsGroupChat)
                            SQLProcessor.updateGroupChatIntoDb(mChatter, mDatabase, mTgChat,
                                index, !(mPrimarySettings.getBoolean(index + 1)), "settings"
                            );
                        else
                            SQLProcessor.updateUserIntoDb(mChatter, mDatabase, mTgFrom,
                                    index, !(mPrimarySettings.getBoolean(index + 1)), "settings"
                            );
                    }

                    mPrimarySettings = mIsGroupChat ?
                            SQLProcessor.getGroupChatFromDb(mChatter, mDatabase, mTgChat, "settings") :
                            SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, "settings");

                    msgText = String.format("<B>%s</B>\r\n\r\n", Locale.translate(mUiLanguage, "settings_title"))
                            + String.format(Locale.translate(mUiLanguage, "settings_parameter"),
                                            Locale.translate(mUiLanguage, mIsGroupChat ? "group_settings_forms" : "user_settings_forms", index),
                                            Locale.translate(mUiLanguage, "on_off", mPrimarySettings.getInt(index + 1))
                              );
                    buttons.addToInnerArray(
                            0, 0,
                            createInlineButton(
                                mPrimarySettings.getBoolean(index + 1) ?
                                            Locale.translate(mUiLanguage, "settings_buttons", 2) :
                                            Locale.translate(mUiLanguage, "settings_buttons", 1),
                                            SettingsCommand.SET_SETTINGS_CALLBACK + "_" + index +
                                                (mPrimarySettings.getBoolean(index + 1) ? "_on" : "_off")
                            )
                    );

                } else if(columnType.equals("set")) {

                    mPrimarySettings = mIsGroupChat ?
                            SQLProcessor.getGroupChatFromDb(mChatter, mDatabase, mTgChat, "settings") :
                            SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, "settings");

                    msgText = String.format("<B>%s</B>\r\n\r\n", Locale.translate(mUiLanguage, "settings_title"))
                            + String.format(Locale.translate(mUiLanguage, "settings_parameter"),
                                            Locale.translate(mUiLanguage, mIsGroupChat ? "group_settings_forms" : "user_settings_forms", index),
                                            Locale.translate(mUiLanguage, "on_off", mPrimarySettings.getInt(index + 2))
                              );
                    buttons.addToInnerArray(
                        0, 0,
                        createInlineButton(
                            mPrimarySettings.getBoolean(index + 2) ?
                                Locale.translate(mUiLanguage, "settings_buttons", 2) :
                                Locale.translate(mUiLanguage, "settings_buttons", 1),
                                columnAdditionalCallback
                        )
                    );

                } else if(columnType.equals("lang")) {

                    String[] locales = Locale.getAvailableLocales();

                    if(!hasNumberValue) {
                        msgText = String.format("<B>%s</B>\r\n\r\n", Locale.translate(mUiLanguage, "settings_title"))
                                + String.format(Locale.translate(mUiLanguage, "settings_parameter"),
                                                Locale.translate(mUiLanguage, "user_settings_forms", index),
                                                Locale.getLanguageName(mUiLanguage)
                                ) + "\r\n\r\n<B>Available languages:</B>\r\n";

                        for(int i = 0; i < locales.length; i++) {
                            msgText += (i + 1) + ". " + Locale.getRemoteLanguageName(locales[i]) + "\r\n";
                        }

                        markupInline = createT9Layout(mMsgText, 1, 2, false);
                    } else {
                        String newLangCode = locales[numberValue - 1].split("\\.")[0];

                        if(mIsGroupChat)
                            SQLProcessor.updateGroupChatIntoDb(mChatter, mDatabase, mTgChat,
                                index, newLangCode, "settings"
                            );
                        else
                            SQLProcessor.updateUserIntoDb(mChatter, mDatabase, mTgFrom,
                                index, newLangCode, "settings"
                            );

                        msgText = String.format("<B>%s</B>\r\n\r\n", Locale.translate(newLangCode, "settings_done"));
                        run();
                    }
                } else {
                    msgText = String.format("%s\r\n\r\n", Locale.translate(mUiLanguage, "not_implemented"));
                }
            }

            InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();

            for(int i = 0; i < buttons.size(); i++) {
                builder.keyboardRow(new InlineKeyboardRow(buttons.get(i)));
            }

            message = EditMessageText.builder()
                    .chatId(mTgChat.getId())
                    .messageId((int)msgId)
                    .text(msgText)
                    .build();

            if(markupInline == null && builder != null)
                message.setReplyMarkup(builder.build());
            else if(markupInline != null)
                message.setReplyMarkup(markupInline);

            message.setParseMode("HTML");

            mChatter.getTelegramClient().execute(message);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<ExtendedColumn> getSettingsColumns() {
        ArrayList<ExtendedColumn> columns;
        if(mIsGroupChat) {
            columns = SQLCreator.getPublicGroupChatSettingsColumns();
            columns.get(2).readOnly = true;
            columns.get(3).readOnly = true;
            columns.get(5).setAdditionalSettingsCallback(ChatBridgeCommand.RUN_IRC_BRIDGE_WIZARD);
        } else {
            columns = SQLCreator.getPublicUserSettingsColumns();
        }

        return columns;
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
