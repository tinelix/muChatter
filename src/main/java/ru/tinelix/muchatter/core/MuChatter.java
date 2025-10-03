package ru.tinelix.muchatter.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.*;

import com.ea.async.Async;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.tinelix.muchatter.core.interfaces.LogColorFormatter;
import ru.tinelix.muchatter.core.BotCommand;

public class MuChatter implements LongPollingSingleThreadUpdateConsumer, LogColorFormatter {
		
	public static class ChatterConfig {
		public String 	tg_token;
		public String 	bot_username;
		public String 	bot_name;
		public long   	tg_bot_owner_id;
		public boolean	use_irc_bridge;
		public String 	irc_server;
		public int		irc_port;
		public String 	irc_encoding;
		public String 	irc_nickname;
		public String 	irc_ns_passwd;
		public String 	license;
	}	
		
	private static String VERSION = "0.0.0";
		
	public static final String RESET_COLOR 		= "\u001B[0m";
	public static final String SUCCESS_COLOR 	= "\u001B[32m"; // Green
	public static final String WARNING_COLOR 	= "\u001B[33m"; // Yellow
	public static final String ERROR_COLOR 		= "\u001B[31m"; // Red
	public static final String INFO_COLOR      	= "\u001B[36m"; // Cyan

	public ChatterConfig mConfig;

	public TelegramClient mClient;
		
	public MuChatter() {
			this.mConfig = new ChatterConfig();
			try {
				FileInputStream inputStream = new FileInputStream("config/bot.json");
				ObjectMapper mapper = new ObjectMapper();
				mConfig = mapper.readValue(
					inputStream, ChatterConfig.class
				);
				mClient = new OkHttpTelegramClient(getBotToken());
			} catch(java.io.IOException | java.lang.NullPointerException e) {
				onError("Please create 'config/bot.json' file and try again.\r\n");
				e.printStackTrace();
			}
	}
		
	public void onUpdateReceived(Update update) {
		
	}
	
	public String getBotToken() {
		return mConfig.tg_token;
	}
		
	public String getBotUsername() {
		return mConfig.bot_username;
	}

	public TelegramClient getClient() {
		return mClient;
	}
		
	@Override
	public boolean onSuccess(String message) {
		System.out.println(
			SUCCESS_COLOR + "[SUCC] " + RESET_COLOR + message
		);
		return true;
	}
		
	@Override
	public boolean onPadding(String message) {
		System.out.println(
			RESET_COLOR + "       " + message
		);
		return true;
	}
		
	@Override
	public boolean onInfo(String message) {
		System.out.println(
			INFO_COLOR + "[INFO] " + RESET_COLOR + message
		);
		return true;
	}

	@Override
	public boolean onWarning(String message) {
		System.out.println(
			WARNING_COLOR + "[WARN] " + RESET_COLOR + message
		);
		return true;
	}

	@Override
	public boolean onError(String message) {
		System.out.println(
			ERROR_COLOR + "[ERR ] " + RESET_COLOR + message
		);
		return true;
	}

	@Override
	public void consume(Update update) {
		// We check if the update has a message and the message has text
		if (update.hasMessage() && update.getMessage().hasText()) {
			User tgFrom = update.getMessage().getFrom();
			Chat tgChat = update.getMessage().getChat();

			BotCommand command = BotCommand.resolve(
				this, tgChat, tgFrom, update.getMessage().getText()
			);

			if(command != null)
				command.run();
		}
	}
}
