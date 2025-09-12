package ru.tinelix.microbot.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

import ru.tinelix.microbot.db.DatabaseEngine;
import ru.tinelix.microbot.core.interfaces.LogColorFormatter;

public class SQLProcessor implements LogColorFormatter {
	
	private DatabaseEngine dbEngine;
	private Connection conn;
	public int last_error_code;
	public String last_error_desc;
	
	public SQLProcessor(DatabaseEngine dbEngine) {
		this.dbEngine = dbEngine;
		this.conn = dbEngine.conn;
	}
	
	public int createTables() {
		int sql_conn = dbEngine.checkSQLConnection();
		if(sql_conn < 0)
			return sql_conn;
			
        //try (Statement stmt = conn.createStatement()) {
        //     String sqlTelevisionsTable = "" +
		// 		"CREATE TABLE IF NOT EXISTS televisions (" +
		// 			   "id INT PRIMARY KEY, " +
		// 			   "name VARCHAR(60) UNIQUE NOT NULL, " +
		// 			   "serial VARCHAR(100) UNIQUE NOT NULL," +
		// 			   "color VARCHAR(25) NOT NULL, " +
		// 			   "price INT NOT NULL, " +
		// 			   "category VARCHAR(25) NOT NULL, " +
		// 			   "technology VARCHAR(18) NOT NULL, " +
		// 			   "inches INT NOT NULL, " +
		// 			   "on_sale BOOLEAN NOT NULL " +
		// 		")";
        //     stmt.executeUpdate(sqlTelevisionsTable);
        		//    
        //     String sqlVacuumsTable = "" +
		// 		"CREATE TABLE IF NOT EXISTS vacuums (" +
		// 			   "id INT PRIMARY KEY, " +
		// 			   "name VARCHAR(60) UNIQUE NOT NULL, " +
		// 			   "serial VARCHAR(100) UNIQUE NOT NULL," +
		// 			   "color VARCHAR(25) NOT NULL, " +
		// 			   "price INT NOT NULL, " +
		// 			   "dust_volume INT NOT NULL, " +
		// 			   "modes_amount INT NOT NULL, " +
		// 			   "on_sale BOOLEAN NOT NULL " +
		// 		")";
        //     stmt.executeUpdate(sqlVacuumsTable);
        		//    
        //     String sqlRefrigeratorsTable = "" +
		// 		"CREATE TABLE IF NOT EXISTS refrigerators (" +
		// 			   "id INT PRIMARY KEY, " +
		// 			   "name VARCHAR(60) UNIQUE NOT NULL, " +
		// 			   "serial VARCHAR(100) UNIQUE NOT NULL," +
		// 			   "color VARCHAR(25) NOT NULL, " +
		// 			   "price INT NOT NULL, " +
		// 			   "doors_amount INT NOT NULL, " +
		// 			   "chambers_amount INT NOT NULL, " +
		// 			   "on_sale BOOLEAN NOT NULL " +
        //          ")";
        //     stmt.executeUpdate(sqlRefrigeratorsTable);
        		//    
        //     String sqlPhonesTable = "" +
		// 		"CREATE TABLE IF NOT EXISTS phones (" +
		// 			   "id INT PRIMARY KEY, " +
		// 			   "name VARCHAR(60) UNIQUE NOT NULL, " +
		// 			   "serial VARCHAR(100) UNIQUE NOT NULL," +
		// 			   "color VARCHAR(25) NOT NULL, " +
		// 			   "price INT NOT NULL, " +
		// 			   "rom_gb INT NOT NULL, " +
		// 			   "ram_gb INT NOT NULL, " +
		// 			   "cameras_amount INT NOT NULL,  " +
		// 			   "on_sale BOOLEAN NOT NULL " +
        //         ")";
        //     stmt.executeUpdate(sqlPhonesTable);
        		//    
        //     String sqlPCTable = "" +
        //         "CREATE TABLE IF NOT EXISTS computers (" +
		// 			   "id INT PRIMARY KEY, " +
		// 			   "name VARCHAR(60) UNIQUE NOT NULL, " +
		// 			   "serial VARCHAR(100) UNIQUE NOT NULL," +
		// 			   "color VARCHAR(24) NOT NULL, " +
		// 			   "price INT NOT NULL, " +
		// 			   "category VARCHAR(16) NOT NULL, " +
		// 			   "cpu_type VARCHAR(10) NOT NULL, " +
		// 			   "cpu_model VARCHAR(40) NOT NULL, " +
		// 			   "ssd_gb INT NOT NULL, " +
		// 			   "ram_gb INT NOT NULL, " +
		// 			   "dvd_drive BOOLEAN NOT NULL, " +
		// 			   "on_sale BOOLEAN NOT NULL " +
        //         ")";
        //     stmt.executeUpdate(sqlPCTable);
        		//    
        //     String sqlUsersTable = "" +
        //         "CREATE TABLE IF NOT EXISTS users (" +
		// 			   "id INT PRIMARY KEY, " +
		// 			   "first_name VARCHAR(20) UNIQUE NOT NULL, " +
		// 			   "last_name VARCHAR(40) UNIQUE NOT NULL, " +
		// 			   "age INT UNIQUE NOT NULL, " +
		// 			   "dest_address VARCHAR(160) NOT NULL, " +
		// 			   "blocked BOOLEAN NOT NULL, " +
		// 			   "email VARCHAR(120) NOT NULL, " +
		// 			   "access_token VARCHAR(500) NOT NULL " +
        //         ")";
        //     stmt.executeUpdate(sqlUsersTable);
        //     onSuccess("Created four tables successfully.");
        // } catch (SQLException e) {
        // 	dbEngine.last_exception = e;
        //     onError("Cannot create tables. Please try again.");
        //     last_error_code = -1;
		// 	return -3;
        // }
        return 0;
	}
    
    @Override
    public boolean onSuccess(String message) {
        System.out.println(
        	DatabaseEngine.SUCCESS_COLOR + "[SUCC] " + DatabaseEngine.RESET_COLOR + message
        );
        return true;
    }
    
    @Override
    public boolean onPadding(String message) {
        System.out.println(
        	DatabaseEngine.RESET_COLOR + "       " + message
        );
        return true;
    }
	
	@Override
    public boolean onInfo(String message) {
        System.out.println(
        	DatabaseEngine.INFO_COLOR + "[INFO] " + DatabaseEngine.RESET_COLOR + message
        );
        return true;
    }

    @Override
    public boolean onWarning(String message) {
    	System.out.println(
			DatabaseEngine.WARNING_COLOR + "[WARN] " + DatabaseEngine.RESET_COLOR + message
		);
		return true;
    }

    @Override
    public boolean onError(String message) {
        System.out.println(
        	DatabaseEngine.ERROR_COLOR + "[ERR ] " + DatabaseEngine.RESET_COLOR + message
        );
        if(dbEngine.config != null)
				onPadding(
					String.format("PostgreSQL URL: %s", dbEngine.config.postgresql_url)
				);
			onPadding(
				String.format("Error Message: %s", dbEngine.last_exception.getMessage())
			);
        return true;
    }
}