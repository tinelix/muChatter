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

public class ProfileCommand extends BotCommand {

    public static final String COMMAND_NAME = "Profile";

    public ProfileCommand(MuChatter chatter, DatabaseEngine dbEngine) {
        super(chatter, dbEngine);
    }

    public void run() {
        try {
            ResultSet userDbResult =
                    SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, null);
            ResultSet userSettingsDbResult =
                    SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, "settings");

            String lang = userSettingsDbResult.getString("ui_language");
            int formsLength = Locale.getLocaleArrayLength(lang, "profile_forms");
            String forms = "";

            for(int i = 0; i < formsLength; i++) {
                forms += String.format(
                    "%s:\r\n%s\r\n\r\n",
                    Locale.translate(lang, "profile_forms", i),
                    userDbResult.getString(i + 5) == null ?
                        Locale.translate(lang, "not_specified") :
                        userDbResult.getString(i + 5)
                );
            }

            InlineKeyboardMarkup markupInline = InlineKeyboardMarkup.builder()
                .keyboardRow(
                    new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text(Locale.translate(lang, "profile_buttons", 0))
                            .callbackData("editProfile")
                            .build()
                    )
            ).build();

            SendMessage message = new SendMessage(
                Long.toString(mTgChat.getId()),
                String.format(
                    "<B>%s%s</B>\r\n\r\n%s",
                    mTgFrom.getFirstName(),
                    mTgFrom.getLastName() == null ? "" : (" " + mTgFrom.getLastName()),
                    forms
                )
            );

            message.setReplyMarkup(
                markupInline
            );

            message.setParseMode("HTML");



            mChatter.getTelegramClient().execute(message);
        } catch (Exception e) {
            mChatter.onError(e.getMessage());
            e.printStackTrace();
        }
    }
}
