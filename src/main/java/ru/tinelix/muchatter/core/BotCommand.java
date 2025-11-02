package ru.tinelix.muchatter.core;

import java.util.HashMap;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.tinelix.muchatter.core.CommandSearch;
import ru.tinelix.muchatter.db.DatabaseEngine;

public class BotCommand {

    protected static final String COMMAND_NAME = "BotCommandExample";

    protected MuChatter mChatter;
    protected DatabaseEngine mDatabase;
    private CommandSearch mSearch;

    protected String mMsgText;
    protected String mCmdText;
    protected String mArgsText;
    protected Chat   mTgChat;
    protected User   mTgFrom;

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

        // Parse command input text
        mMsgText = msgText;

        mTgFrom = tgFrom;

        if(mMsgText.length() > 0)
            mCmdText = mMsgText.split(" ")[0];

        // Parse arguments text
        int firstDelimIndex = msgText.indexOf(" ");

        if(firstDelimIndex > 0)
            mArgsText = msgText.substring(firstDelimIndex + 1);
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

    protected InlineKeyboardButton createInlineButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    protected void update(long msgId) {

    }
}
