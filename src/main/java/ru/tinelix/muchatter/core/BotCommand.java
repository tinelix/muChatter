package ru.tinelix.muchatter.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.sql.ResultSet;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton.InlineKeyboardButtonBuilder;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.InlineKeyboardMarkupBuilder;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.tinelix.muchatter.core.CommandSearch;
import ru.tinelix.muchatter.db.DatabaseEngine;
import ru.tinelix.muchatter.db.SQLProcessor;
import ru.tinelix.muchatter.utils.DoubleArrayList;

public class BotCommand {

    protected static final String COMMAND_NAME = "BotCommandExample";

    protected MuChatter mChatter;
    protected DatabaseEngine mDatabase;
    private CommandSearch mSearch;

    protected String        mMsgText;
    protected String        mCmdText;
    protected String        mArgsText;
    protected Chat          mTgChat;
    protected User          mTgFrom;
    protected boolean       mIsGroupChat;
    protected ResultSet     mPrimarySettings;
    protected ResultSet     mUserSettings;
    protected ResultSet     mChatSettings;
    protected String        mUiLanguage;
    protected int           mTzOffset;

    public BotCommand(MuChatter chatter, DatabaseEngine dbEngine) {
        this.mChatter = chatter;
        this.mDatabase = dbEngine;
    }

    public static BotCommand resolve(
        MuChatter chatter,
        DatabaseEngine dbEngine,
        Chat tgChat,
        User tgFrom,
        String msgText
    ) {
        String cmdText = "";
        String argsText = "";

        if(msgText.length() > 0)
            cmdText = msgText.split(" ")[0];

        // Parse arguments text
        int firstDelimIndex = msgText.indexOf(" ");

        if(firstDelimIndex > 0)
            argsText = msgText.substring(firstDelimIndex + 1);

        BotCommand cmd = CommandSearch.find(chatter, dbEngine, cmdText);

        if(cmd != null)
            cmd.parse(tgChat, tgFrom, msgText);

        return cmd;
    }

    public static BotCommand resolveByCallback(
        MuChatter chatter,
        DatabaseEngine dbEngine,
        Chat tgChat,
        User tgFrom,
        String cbData
    ) {

        BotCommand cmd = CommandSearch.findByCallback(chatter, dbEngine, cbData);

        if(cmd != null)
            cmd.parse(tgChat, tgFrom, cbData);

        return cmd;
    }

    protected void parse(Chat tgChat, User tgFrom, String msgText) {
        // Getting Telegram chat info
        mTgChat = tgChat;

        mIsGroupChat = mTgChat.getId() < 0;

        // Parse command input text
        mMsgText = msgText;

        mTgFrom = tgFrom;

        if(mMsgText.length() > 0)
            mCmdText = mMsgText.split(" ")[0];

        // Parse arguments text
        int firstDelimIndex = msgText.indexOf(" ");

        if(firstDelimIndex > 0)
            mArgsText = msgText.substring(firstDelimIndex + 1);

        SQLProcessor.updateUserIntoDb(mChatter, mDatabase, mTgFrom);

        try {
            mChatSettings = SQLProcessor.getGroupChatFromDb(mChatter, mDatabase, mTgChat, "settings");
            mUserSettings = SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, "settings");

            mPrimarySettings = mIsGroupChat ? mChatSettings : mUserSettings;

            mUiLanguage = mPrimarySettings.getString("ui_language");
            mTzOffset = mPrimarySettings.getInt("timezone");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    protected void run() {
        SendMessage message = new SendMessage(
            Long.toString(mTgChat.getId()),
            "BotCommand class test!"
        );
        try {
            mChatter.getTelegramClient().execute(message);
        } catch (TelegramApiException e) {
            mChatter.onError(e.getMessage());
        }
    }

    protected void runFromCallback(String msgText) {
        SendMessage message = new SendMessage(
            Long.toString(mTgChat.getId()),
            "BotCommand class test!"
        );
        try {
            mChatter.getTelegramClient().execute(message);
        } catch (TelegramApiException e) {
            mChatter.onError(e.getMessage());
        }
    }

    public static HashMap<String, String> getInformation() {
        HashMap<String, String> info = new HashMap<String, String>();

        info.put("syntax",    "/cmdname [arguments]");
        info.put("cmd_desc",  "Command description placeholder");
        info.put("args_desc", "Arguments description placeholder");
        info.put("av_rights", "Available rights placeholder");

        return info;
    }

    protected InlineKeyboardButton createInlineButton(String text, String data) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(data)
                .build();
    }

    protected InlineKeyboardButton createInlineButton(String text, String data, boolean useAsUrl) {
        InlineKeyboardButtonBuilder builder = InlineKeyboardButton.builder();

        builder.text(text);

        if(useAsUrl)
            builder.url(data);
        else
            builder.callbackData(data);

        return builder.build();
    }

    protected InlineKeyboardMarkup createT9Layout(String callbackData, int startPos, int size, boolean pagination) {
        DoubleArrayList<InlineKeyboardButton> buttons = new DoubleArrayList<>();

        String startPosNum = Integer.toString(startPos);

        if(size > 10) {
            return null;
        }

        if(startPosNum.endsWith("0")) {
            startPos++;
        }

        for(int i = startPos; i < (size + startPos); i++) {
            String emojiNum = Integer.toString(i);

            emojiNum
                .replace("0", "0️⃣")
                .replace("1", "1️⃣")
                .replace("2", "2️⃣")
                .replace("3", "3️⃣")
                .replace("4", "4️⃣")
                .replace("5", "5️⃣")
                .replace("6", "6️⃣")
                .replace("7", "7️⃣")
                .replace("8", "8️⃣")
                .replace("9", "9️⃣");

            buttons.addToInnerArray(
                (int)Math.floor((i - startPos) / 3),
                (i - startPos) % 3,
                createInlineButton(emojiNum, callbackData + "_" + i)
            );
        }

        if(pagination && size == 10)
            buttons.addToInnerArray(
                3,
                0,
                createInlineButton("⏭️", callbackData + "_next")
            );
        else if(startPos > 10)
            buttons.addToInnerArray(
                3,
                0,
                createInlineButton("⏮️", callbackData + "_prev")
            );


        InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();

        for(int i = 0; i < buttons.size(); i++) {
            builder.keyboardRow(new InlineKeyboardRow(buttons.get(i)));
        }

        return builder.build();
    }

    protected void update(long msgId) {

    }

    protected void sendErrorMessage(int errorCode) {
        try {
            ResultSet dbSettingsResult =
                mIsGroupChat ?
                    SQLProcessor.getGroupChatFromDb(mChatter, mDatabase, mTgChat, "settings") :
                    SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, "settings");

            String lang = dbSettingsResult.getString("ui_language");

            String msgText = "🚫 " + Locale.translate(lang, "error_reasons", errorCode);

            SendMessage message = SendMessage.builder()
                        .chatId(mTgChat.getId())
                        .text(msgText)
                        .build();

            message.setParseMode("HTML");
            mChatter.getTelegramClient().execute(message);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
