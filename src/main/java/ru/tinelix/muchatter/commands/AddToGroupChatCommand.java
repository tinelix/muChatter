package ru.tinelix.muchatter.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.sql.ResultSet;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
import ru.tinelix.muchatter.core.MuChatter;
import ru.tinelix.muchatter.db.DatabaseEngine;
import ru.tinelix.muchatter.db.SQLProcessor;

import ru.tinelix.muchatter.core.Locale;

public class AddToGroupChatCommand extends BotCommand {

    public static final String COMMAND_NAME = "AddToGroupChat";

    public AddToGroupChatCommand(MuChatter chatter, DatabaseEngine dbEngine) {
        super(chatter, dbEngine);
    }

    public void run() {
        try {
            ResultSet userSettingsDbResult =
                    SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, "settings");

            String lang = userSettingsDbResult.getString("ui_language");

            SendMessage message = new SendMessage(
                Long.toString(mTgChat.getId()),
                Locale.translate(lang, "add_to_groupchat")
            );

            InlineKeyboardMarkup markupInline = InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        createInlineButton(
                            Locale.translate(lang, "add_to_groupchat_buttons", 0),
                            String.format("https://t.me/%sbot?startgroup=start", mChatter.mConfig.bot_username),
                            true
                        )
                    )
                ).build();

            message.setReplyMarkup(markupInline);
            message.setParseMode("HTML");
            mChatter.getTelegramClient().execute(message);

        } catch (Exception e) {
            mChatter.onError(e.getMessage());
            e.printStackTrace();
        }
    }

}
