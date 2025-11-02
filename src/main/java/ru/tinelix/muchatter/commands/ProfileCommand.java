package ru.tinelix.muchatter.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.sql.ResultSet;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
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

    public static final String COMMAND_NAME                     = "Profile";
    public static final String EDIT_PROFILE_CALLBACK            = "editProfile";
    public static final String FILL_PROFILE_TEXTAREA_CALLBACK   = "fillProfileTextArea";

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

            boolean notFilledIn = false;

            for(int i = 0; i < formsLength; i++) {

                if(userDbResult.getString(i + 5) == null)
                    notFilledIn = true;

                forms += String.format(
                    "%s:\r\n%s\r\n\r\n",
                    Locale.translate(lang, "profile_forms", i),
                    userDbResult.getString(i + 5) == null ?
                        "<I>" + Locale.translate(lang, "not_specified") + "</I>" :
                        userDbResult.getString(i + 5)
                );
            }

            InlineKeyboardMarkup markupInline = InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        createInlineButton(
                            Locale.translate(lang, "profile_buttons", notFilledIn ? 1 : 0),
                            "editProfile"
                        )
                    )
                ).build();

            SendMessage message = new SendMessage(
                Long.toString(mTgChat.getId()),
                String.format(
                    "<B>%s - %s%s%s</B>\r\n\r\n%s<I>ID: <CODE>%d</CODE></I>",
                    Locale.translate(lang, "profile_info_title"),
                    mTgFrom.getFirstName(),
                    mTgFrom.getLastName() == null ? "" : (" " + mTgFrom.getLastName()),
                    mTgFrom.getUserName() == null ? "" : (" (@" + mTgFrom.getUserName() + ")"),
                    forms,
                    mTgFrom.getId()
                )
            );

            message.setReplyMarkup(markupInline);

            message.setParseMode("HTML");

            mChatter.getTelegramClient().execute(message);
        } catch (Exception e) {
            mChatter.onError(e.getMessage());
            e.printStackTrace();
        }
    }

    public void runFromCallback(String msgText) {

    }

    public void update(long msgId) {
        try {
            ResultSet userDbSettingsResult = SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, "settings");

            String lang = userDbSettingsResult.getString("ui_language");
            int formsLength = Locale.getLocaleArrayLength(lang, "profile_forms");

            ArrayList<InlineKeyboardButton> buttons1 = new ArrayList<>();
            ArrayList<InlineKeyboardButton> buttons2 = new ArrayList<>();
            ArrayList<InlineKeyboardButton> buttons3 = new ArrayList<>();

            if(mMsgText.equals(ProfileCommand.EDIT_PROFILE_CALLBACK)) {
                EditMessageText message = EditMessageText.builder()
                    .chatId(mTgChat.getId())
                    .messageId((int)msgId)
                    .text(Locale.translate(lang, "profile_edit_areas"))
                    .build();

                for(int i = 1; i < formsLength; i++) {
                    if(i < 3) {
                        buttons1.add(
                            createInlineButton(
                                Locale.translate(lang, "profile_forms", i),
                                ProfileCommand.FILL_PROFILE_TEXTAREA_CALLBACK + "_" + Integer.toString(i)
                            )
                        );
                    } else if(i < 5) {
                        buttons2.add(
                            createInlineButton(
                                Locale.translate(lang, "profile_forms", i),
                                ProfileCommand.FILL_PROFILE_TEXTAREA_CALLBACK + "_" + Integer.toString(i)
                            )
                        );
                    } else {
                        buttons3.add(
                            createInlineButton(
                                Locale.translate(lang, "profile_forms", i),
                                ProfileCommand.FILL_PROFILE_TEXTAREA_CALLBACK + "_" + Integer.toString(i)
                            )
                        );
                    }
                }

                InlineKeyboardMarkup markupInline = InlineKeyboardMarkup
                    .builder()
                    .keyboardRow(new InlineKeyboardRow(buttons1))
                    .keyboardRow(new InlineKeyboardRow(buttons2))
                    .keyboardRow(new InlineKeyboardRow(buttons3))
                    .build();

                message.setReplyMarkup(markupInline);

                mChatter.getTelegramClient().execute(message);

            } else if(mMsgText.startsWith(ProfileCommand.FILL_PROFILE_TEXTAREA_CALLBACK + "_")) {

                mChatter.makeTemporaryCallback(ProfileCommand.FILL_PROFILE_TEXTAREA_CALLBACK, mTgChat, mTgFrom);

                EditMessageText message = EditMessageText.builder()
                    .chatId(mTgChat.getId())
                    .messageId((int)msgId)
                    .text(
                        String.format(
                            Locale.translate(lang, "profile_edit_areas_2"),
                            Locale.translate(lang, "profile_forms", Integer.parseInt(mMsgText.split("_")[1]))
                        )
                    ).build();

                message.setParseMode("HTML");

                mChatter.getTelegramClient().execute(message);
            }
        } catch (Exception e) {
            mChatter.onError(e.getMessage());
            e.printStackTrace();
        }
    }


}
