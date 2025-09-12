package ru.tinelix.microbot;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.*;

import ru.tinelix.microbot.core.Microchatter;

public class BotApplication {
	public static void main(String[] args) {
		printLogo();
		Microchatter bot = new Microchatter();
	}
	
	public static void printLogo() {
		System.out.printf(
			"\r\nMicrochatter for Telegram\r\n" +
			"Copyright (c) 2025 Dmitry Tretyakov (aka. Tinelix).\r\n" +
			"https://github.com/tinelix/microchatter\r\n\r\n"
		);

		System.out.printf(
			"Working directory: %s\r\n\r\n", 
			Paths.get(".").toAbsolutePath().normalize()
		);
	}
}
