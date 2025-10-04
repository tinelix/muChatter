package ru.tinelix.muchatter;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.*;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.tinelix.muchatter.core.MuChatter;

public class BotApplication {
	public static void main(String[] args) {
		printLogo();
		MuChatter bot = new MuChatter();
		
		try {
			TelegramBotsLongPollingApplication app = 
				new TelegramBotsLongPollingApplication();
		
			app.registerBot(bot.getBotToken(), bot);
		
			System.out.println("muChatter successfully started!\r\n");
		
			Thread.currentThread().join();
		} catch(Exception ex) {
			
		}
	}
	
	public static void printLogo() {
		System.out.printf(
			"\r\nmuChatter for Telegram\r\n" +
			"Copyright (c) 2025 Dmitry Tretyakov (aka. Tinelix)\r\n" +
			"https://github.com/tinelix/muChatter\r\n\r\n"
		);

		System.out.printf(
			"Working directory: %s\r\n\r\n", 
			Paths.get(".").toAbsolutePath().normalize()
		);
	}
}
