package ru.tinelix.muchatter.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.InlineKeyboardMarkupBuilder;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.tinelix.muchatter.core.BotCommand;
import ru.tinelix.muchatter.core.Locale;
import ru.tinelix.muchatter.core.MuChatter;
import ru.tinelix.muchatter.db.DatabaseEngine;
import ru.tinelix.muchatter.db.SQLProcessor;
import ru.tinelix.muchatter.utils.DoubleArrayList;

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

            int formsLength = Locale.getLocaleArrayLength(mUiLanguage, "profile_forms");
            String forms = "";

            boolean notFilledIn = false;

            for(int i = 0; i < formsLength; i++) {

                int columnIndex = i + 5;

                if(i >= 1)
                    columnIndex = i + 6;

                if(userDbResult.getString(columnIndex) == null)
                    notFilledIn = true;

                forms += String.format(
                    "%s:\r\n%s\r\n\r\n",
                    Locale.translate(mUiLanguage, "profile_forms", i),
                    userDbResult.getString(columnIndex) == null ?
                        "<I>" + Locale.translate(mUiLanguage, "not_specified") + "</I>" :
                        userDbResult.getString(columnIndex)
                );
            }

            InlineKeyboardMarkup markupInline = InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        createInlineButton(
                            Locale.translate(mUiLanguage, "profile_buttons", notFilledIn ? 1 : 0),
                            "editProfile"
                        )
                    )
                ).build();

            SendMessage message = new SendMessage(
                Long.toString(mTgChat.getId()),
                String.format(
                    "<B>%s - %s%s%s</B>\r\n\r\n%s<I>ID: <CODE>%d</CODE> / В базе данных с %s</I>",
                    Locale.translate(mUiLanguage, "profile_info_title"),
                    mTgFrom.getFirstName(),
                    mTgFrom.getLastName() == null ? "" : (" " + mTgFrom.getLastName()),
                    mTgFrom.getUserName() == null ? "" : (" (@" + mTgFrom.getUserName() + ")"),
                    forms,
                    mTgFrom.getId(), userDbResult.getString(6)
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

        String cbDataNumber = mMsgText.split("_")[1];
        int columnIndex = Integer.parseInt(cbDataNumber) + 3;

        SQLProcessor.updateUserIntoDb(mChatter, mDatabase, mTgFrom, columnIndex, msgText);

        run();

        mChatter.removeTemporaryCallback(mMsgText, mTgChat, mTgFrom);
    }

    public void update(long msgId) {
        try {
            ResultSet userDbResult = SQLProcessor.getUserFromDb(mChatter, mDatabase, mTgFrom, null);

            int formsLength = Locale.getLocaleArrayLength(mUiLanguage, "profile_forms");
            String forms = "";

            boolean notFilledIn = false;

            EditMessageText message = null;

            InlineKeyboardMarkup markupInline = null;

            DoubleArrayList<InlineKeyboardButton> buttons = new DoubleArrayList<>();

            if(mMsgText.equals(ProfileCommand.EDIT_PROFILE_CALLBACK)) {
                message = EditMessageText.builder()
                    .chatId(mTgChat.getId())
                    .messageId((int)msgId)
                    .text(Locale.translate(mUiLanguage, "profile_edit_areas"))
                    .build();

                for(int i = 1; i < formsLength; i++) {
                    buttons.addToInnerArray(
                        (int)Math.floor((i - 1) / 2), (i - 1) % 2,
                        createInlineButton(
                            Locale.translate(mUiLanguage, "profile_forms", i),
                            ProfileCommand.FILL_PROFILE_TEXTAREA_CALLBACK + "_" + Integer.toString(i)
                        )
                    );
                }

            } else if(mMsgText.startsWith(ProfileCommand.FILL_PROFILE_TEXTAREA_CALLBACK + "_cancel")) {

                for(int i = 0; i < formsLength; i++) {
                    int columnIndex = i + 5;

                    if(i >= 1)
                        columnIndex = i + 6;

                    if(userDbResult.getString(columnIndex) == null)
                        notFilledIn = true;

                    forms += String.format(
                        "%s:\r\n%s\r\n\r\n",
                        Locale.translate(mUiLanguage, "profile_forms", i),
                        userDbResult.getString(columnIndex) == null ?
                            "<I>" + Locale.translate(mUiLanguage, "not_specified") + "</I>" :
                            userDbResult.getString(columnIndex)
                    );
                }

                markupInline = InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(
                            createInlineButton(
                                Locale.translate(mUiLanguage, "profile_buttons", notFilledIn ? 1 : 0),
                                "editProfile"
                            )
                        )
                    ).build();

                message = EditMessageText.builder()
                    .chatId(mTgChat.getId())
                    .messageId((int)msgId)
                    .text(
                        String.format(
                            "<B>%s - %s%s%s</B>\r\n\r\n%s<I>ID: <CODE>%d</CODE></I>",
                            Locale.translate(mUiLanguage, "profile_info_title"),
                            mTgFrom.getFirstName(),
                            mTgFrom.getLastName() == null ? "" : (" " + mTgFrom.getLastName()),
                            mTgFrom.getUserName() == null ? "" : (" (@" + mTgFrom.getUserName() + ")"),
                            forms,
                            mTgFrom.getId()
                        )
                    ).build();

                mChatter.removeTemporaryCallback(mMsgText, mTgChat, mTgFrom);

            } else if(mMsgText.startsWith(ProfileCommand.FILL_PROFILE_TEXTAREA_CALLBACK + "_")) {

                mChatter.makeTemporaryCallback(mMsgText, mTgChat, mTgFrom);

                message = EditMessageText.builder()
                    .chatId(mTgChat.getId())
                    .messageId((int)msgId)
                    .text(
                        String.format(
                            Locale.translate(mUiLanguage, "profile_edit_areas_2"),
                            Locale.translate(mUiLanguage, "profile_forms", Integer.parseInt(mMsgText.split("_")[1]))
                        )
                    ).build();

                buttons.addToInnerArray(
                    0, 0,
                    createInlineButton(
                        Locale.translate(mUiLanguage, "profile_buttons", 2),
                        ProfileCommand.FILL_PROFILE_TEXTAREA_CALLBACK + "_cancel"
                    )
                );

            }

            if(markupInline == null) {
                InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();

                for(int i = 0; i < buttons.size(); i++) {
                    builder.keyboardRow(new InlineKeyboardRow(buttons.get(i)));
                }

                markupInline = builder.build();

                message.setReplyMarkup(markupInline);
            }

            message.setParseMode("HTML");

            mChatter.getTelegramClient().execute(message);
        } catch (Exception e) {
            mChatter.onError(e.getMessage());
            e.printStackTrace();
        }
    }


}
