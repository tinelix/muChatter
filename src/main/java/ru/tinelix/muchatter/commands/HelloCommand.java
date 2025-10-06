package ru.tinelix.muchatter.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.tinelix.muchatter.core.BotCommand;
import ru.tinelix.muchatter.core.MuChatter;
import ru.tinelix.muchatter.db.DatabaseEngine;

import ru.tinelix.muchatter.core.Locale;

public class HelloCommand extends BotCommand {

    public static final String COMMAND_NAME = "Hello";

    public HelloCommand(MuChatter chatter, DatabaseEngine dbEngine) {
        super(chatter, dbEngine);
    }

    public void run() {

        List<Object> locale_args = new ArrayList<>();

        locale_args.add(mTgFrom.getFirstName());

        SendMessage message = new SendMessage(
            Long.toString(mTgChat.getId()),
            Locale.translate("en_Int", "greetings", locale_args)
        );

        try {
            mChatter.mClient.execute(message);
        } catch (TelegramApiException e) {
            mChatter.onError(e.getMessage());
        }
    }

}
