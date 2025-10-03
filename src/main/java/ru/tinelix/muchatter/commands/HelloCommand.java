package ru.tinelix.muchatter.commands;

import java.util.HashMap;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.tinelix.muchatter.core.BotCommand;
import ru.tinelix.muchatter.core.MuChatter;

public class HelloCommand extends BotCommand {

    public static final String COMMAND_NAME = "Hello";

    public HelloCommand(MuChatter chatter) {
        super(chatter);
    }

    public void run() {
        SendMessage message = new SendMessage(
            Long.toString(mTgChat.getId()),
            String.format(
                "From %s %s / %d",
                mTgFrom.getFirstName(),
                mTgFrom.getLastName(),
                mTgChat.getId()
            )
        );

        try {
            mChatter.mClient.execute(message);
        } catch (TelegramApiException e) {
            mChatter.onError(e.getMessage());
        }
    }

}
