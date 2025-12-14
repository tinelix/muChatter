package ru.tinelix.muchatter.db;

import java.io.FileInputStream;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.tinelix.muchatter.core.interfaces.LogColorFormatter;
import ru.tinelix.muchatter.db.SQLProcessor;

public class DatabaseEngine implements LogColorFormatter {
	public DatabaseConfig config;
	public Connection conn;
	public Exception last_exception;
	public SQLProcessor sql_proc;
	
	public static final String RESET_COLOR 		= "\u001B[0m";
    public static final String SUCCESS_COLOR 	= "\u001B[32m"; // Green
    public static final String WARNING_COLOR 	= "\u001B[33m"; // Yellow
    public static final String ERROR_COLOR 		= "\u001B[31m"; // Red
    public static final String INFO_COLOR      	= "\u001B[36m"; // Cyan
	
	public static class DatabaseConfig {
		public String sql_addr;
		public String username;
		public String password;
	}

	public DatabaseEngine() {
		try {
			FileInputStream inputStream = new FileInputStream("config/db.json");
			ObjectMapper mapper = new ObjectMapper();
        		config = mapper.readValue(
				inputStream, DatabaseConfig.class
			);
		} catch(java.io.IOException | java.lang.NullPointerException e) {
			last_exception = e;
			onError("Please create 'config/db.json' file and try again.");
		}
	}

	public int connect() {
		try {
            conn = DriverManager.getConnection(
				config.sql_addr, config.username, config.password
			);
			conn.setAutoCommit(true);
			onSuccess("Database connected successfully.");

			sql_proc = new SQLProcessor(this);
			sql_proc.createTables();

			return 0;
		} catch(SQLException | java.lang.NullPointerException e) {
			last_exception = e;
			onError("Connection refused. Please try again.\r\nMessage: " + e.getMessage());
			return -1;
		}
	}
	
	public int checkSQLConnection() {
		try {
			if (conn == null || conn.isClosed()) {
				onError("Connection refused. Please try again.");
				return -1;
			}
        } catch(SQLException e) {
        	onError("Cannot check SQL connection. Please try again.");
        	return -2;
        }
        return 0;
	}

	protected PreparedStatement escapeSQLValues(
		PreparedStatement pstmt, LinkedHashMap<String, Object> values
	) throws SQLException {
		for(int i = 0; i < values.size(); i++) {
			String key = values.keySet().toArray()[i].toString();

			if(values.get(key) == null)
				pstmt.setNull(i + 1, Types.VARCHAR);
			else if(values.get(key) instanceof String)
				pstmt.setString(i + 1, (String)values.get(key));
			else if(values.get(key) instanceof Integer)
				pstmt.setInt(i + 1, (Integer)values.get(key));
			else if(values.get(key) instanceof Long)
				pstmt.setLong(i + 1, (Long)values.get(key));
			else if(values.get(key) instanceof Boolean)
				pstmt.setBoolean(i + 1, (Boolean)values.get(key));
        }

        return pstmt;
	}

	protected PreparedStatement escapeSQLValue(
		PreparedStatement pstmt, Object value
	) throws SQLException {
		if(value == null)
			pstmt.setNull(1, Types.VARCHAR);
		else if(value instanceof String) {
			pstmt.setString(1, (String)value);
		} else if(value instanceof Integer) {
			pstmt.setInt(1, (Integer)value);
		} else if(value instanceof Long) {
			pstmt.setLong(1, (Long)value);
		} else if(value instanceof Boolean) {
			pstmt.setBoolean(1, (Boolean)value);
		}

        return pstmt;
	}

	protected PreparedStatement escapeSQLValue(
		PreparedStatement pstmt, int index, Object value
	) throws SQLException {
		if(value == null)
			pstmt.setNull(index + 1, Types.VARCHAR);
		else if(value instanceof String) {
			pstmt.setString(index + 1, (String)value);
		} else if(value instanceof Integer) {
			pstmt.setInt(index + 1, (Integer)value);
		} else if(value instanceof Long) {
			pstmt.setLong(index + 1, (Long)value);
		} else if(value instanceof Boolean) {
			pstmt.setBoolean(index + 1, (Boolean)value);
		}

        return pstmt;
	}

	public boolean ifExist(String table, String column, int value) {
		int sql_conn = checkSQLConnection();
		if(sql_conn < 0)
			return false;
			
		if (!sql_proc.validTables.contains(table.toLowerCase())) {
            onError("Invalid table name provided for ifExist function.");
            return false;
        }
        
        String safeTableName = table.replace("\"", "\"\"")
									.replace("=", "").replace(" ", "");

        String safeColumnName = column.replace("\"", "\"\"")
									.replace("=", "").replace(" ", "");
        
        String sqlIfExist = "" +
			"SELECT EXISTS(SELECT 1 FROM " + safeTableName + " WHERE " +
			safeColumnName + " = ?)";

		try (PreparedStatement pstmt = conn.prepareStatement(sqlIfExist)) {
			pstmt.setInt(1, value);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getBoolean(1);
			}
		} catch (SQLException e) {
			last_exception = e;
			onError("Cannot check values if exist. Please try again.");
			return false;
		}
		
		return false;
	}
	
	public boolean ifExist(String table, String column, String value) {
		int sql_conn = checkSQLConnection();
		if(sql_conn < 0)
			return false;
			
		if (!sql_proc.validTables.contains(table.toLowerCase())) {
            onError("Invalid table name provided for ifExist function.");
            return false;
        }

        column = column.replace("\"", "\"\"");
        
        String safeTableName = table.replace("\"", "\"\"")
									.replace("=", "").replace(" ", "");

        String safeColumnName = '"' + column.replace("\"", "\"\"")
									  .replace("=", "").replace(" ", "");
        
        String sqlIfExist = "" +
			"SELECT EXISTS(SELECT 1 FROM " + safeTableName + 
			" WHERE " + safeColumnName + " = ?)";

		try (PreparedStatement pstmt = conn.prepareStatement(sqlIfExist)) {
			pstmt.setString(1, value);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getBoolean(1);
			}
		} catch (SQLException e) {
			last_exception = e;
			onError("Cannot check values if exist. Please try again.");
			return false;
		}
		
		return false;
	}

	public boolean ifExist(String table, String column, long value) {
		int sql_conn = checkSQLConnection();
		if(sql_conn < 0)
			return false;

		if (!sql_proc.validTables.contains(table.toLowerCase())) {
            onError("Invalid table name provided for ifExist function.");
            return false;
        }

        String safeTableName = table.replace("\"", "\"\"")
									.replace("=", "").replace(" ", "");

        String safeColumnName = column.replace("\"", "\"\"").replace("\"", "\"\"")
									  .replace("=", "").replace(" ", "");

        String sqlIfExist = "" +
			"SELECT EXISTS(SELECT 1 FROM " + safeTableName +
			" WHERE " + safeColumnName + " = ?)";

		try (PreparedStatement pstmt = conn.prepareStatement(sqlIfExist)) {
			pstmt.setLong(1, value);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getBoolean(1);
			}
		} catch (SQLException e) {
			last_exception = e;
			onError("Cannot check values if exist. Please try again.");
			return false;
		}

		return false;
	}
	
	public ResultSet selectEquals(
		String columns, String table, 
		String whereColumn, Object whereValue
	) throws SQLException {

        StringBuilder query = new StringBuilder("SELECT ");
        query.append(columns).append(" FROM ").append(table);

        if (!whereColumn.trim().isEmpty()) {
            query.append(" WHERE ").append(whereColumn)
				 .append(" = ?");
        }

        PreparedStatement pstmt = conn.prepareStatement(query.toString());
        pstmt = escapeSQLValue(pstmt, whereValue);

        return pstmt.executeQuery();
    }
    
    public ResultSet selectEquals(
		String columns, String table, 
		String whereColumn, Object whereValue,
		String orderByClause
	) throws SQLException {
		
        StringBuilder query = new StringBuilder("SELECT ");
        query.append(columns).append(" FROM ").append(table);
        
        if (!whereColumn.trim().isEmpty()) {
            query.append(" WHERE ").append(whereColumn)
				 .append(" = ?");
        }

        if (!orderByClause.trim().isEmpty()) {
			String[] orderSplitted = orderByClause.split("_");
			if(orderSplitted.length > 1) {
				query.append(" ORDER BY ").append(orderSplitted[0]);
				if(orderSplitted[1].equals("asc")) {
					query.append(" ASC");
				} else {
					query.append(" DESC");
				}
			}
		}

        PreparedStatement pstmt = conn.prepareStatement(query.toString());
        pstmt = escapeSQLValue(pstmt, whereValue);

        return pstmt.executeQuery();
    }
    
    public boolean add(String table, LinkedHashMap<String, Object> values) throws SQLException {
        if (values != null && values.size() == 0) {
            return false;
        }

		StringBuilder query = new StringBuilder("INSERT INTO ");
		query.append(table).append("(");

		for(int i = 0; i < values.keySet().size(); i++) {
			if(i < values.keySet().size() - 1) {
				query.append("" + values.keySet().toArray()[i] + ", ");
			} else {
				query.append(values.keySet().toArray()[i]);
			}
		}

		query.append(") VALUES (");

		for(int i = 0; i < values.size(); i++) {
			if(i < values.size() - 1)
				query.append("?, ");
			else
				query.append("?");
		}

		query.append(");");

		onInfo("SQL Query Mask: " + query);

        PreparedStatement pstmt = conn.prepareStatement(query.toString());

        pstmt = escapeSQLValues(pstmt, values);
        
        return pstmt.executeUpdate() > 0;
    }

    public boolean update(
		String table, String column, Object value,
		String whereColumn, Object whereValue
	) throws SQLException {
        if (value == null)
            return false;

		StringBuilder query = new StringBuilder("UPDATE ");
		query.append(table).append(" SET ")
		     .append(column).append(" = ?");

		if (!whereColumn.trim().isEmpty()) {
            query.append(" WHERE ").append(whereColumn)
				 .append(" = ?");
        }

        PreparedStatement pstmt = conn.prepareStatement(query.toString());

        pstmt = escapeSQLValue(pstmt, value);
        if(whereValue != null)
			pstmt = escapeSQLValue(pstmt, 1, whereValue);
		boolean result = pstmt.executeUpdate() > 0;

		onInfo(query.toString());
        return result;
    }
    
    public int getEntryCount(String table) {
    	try {
			int count = 0;
			String sqlCountQuery = "SELECT COUNT(*) FROM " + table + ";";
			Statement statement = conn.createStatement();
				
			ResultSet resultSet = statement.executeQuery(sqlCountQuery);
			if (resultSet.next()) { 
				count = resultSet.getInt(1);
			}
			return count;
        } catch(SQLException e) {
        	return -1;
        }
    }
    
    public String convertHTTPParams(LinkedHashMap<String, Object> map, int req_type) {
    	StringBuilder sqlClause = new StringBuilder();
    	int map_size = map.size();
		int map_count = 0;
    	
    	switch(req_type) {
    		case 0: // WHERE
				for (var entry : map.entrySet()) {
					if(entry.getValue() instanceof Integer) {
						if((Integer) entry.getValue() == 0)
							continue;
							
						if(map_count > 0) 
							sqlClause.append(" AND ");
							
						if(entry.getKey().endsWith("_start")) {
							sqlClause.append(
								entry.getKey().replace("_start", "") + " >= "
							);
						} else if(entry.getKey().endsWith("_end")) {
							sqlClause.append(
								entry.getKey().replace("_end", "")  + " <= "
							);
						} else {
							sqlClause.append(
								entry.getKey() + " = "
							);
						}
					
						sqlClause.append("" + (Integer)entry.getValue());	
					} else if(entry.getValue() instanceof Boolean) {
						if(!((Boolean) entry.getValue()))
							continue;
							
						if(map_count > 0) 
							sqlClause.append(" AND ");
							
						sqlClause.append(
								entry.getKey() + " = "
						);
					
						sqlClause.append("TRUE");	
					} else if(entry.getValue() instanceof String) {
						if(((String)entry.getValue()).length() == 0)
							continue;
						
						if(map_count > 0) 
							sqlClause.append(" AND ");
						
						sqlClause.append("LOWER(" + entry.getKey() + ") = ");
						
						sqlClause.append("LOWER(\'" + 
							(String)entry.getValue()
							+ "\')"
						);	
					}
					map_count++;
				}
				break;
			case 1:			// INSERT INTO VALUES
				if(map_size > 0) 
					sqlClause.append("(");
				
				for (var entry : map.entrySet()) {
					if(entry.getValue() instanceof Integer) {
						if(map_count > 0) 
							sqlClause.append(", ");
						
						sqlClause.append("" + (Integer)entry.getValue()); 
					} else if(entry.getValue() instanceof String) {
						if(map_count > 0) 
							sqlClause.append(", ");
							
						sqlClause.append(
							"'" + ((String)entry.getValue()) + "'"
						); 
					} else if(entry.getValue() instanceof Boolean) {
						if(map_count > 0) 
							sqlClause.append(", ");
							
						sqlClause.append("" + ((Boolean)entry.getValue() ? "TRUE" : "FALSE") + ""); 
					}
					
					map_count++;
				}
				
				if(map_count > 0)
					sqlClause.append(")");
				break;
    	}
        
        return sqlClause.toString();
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
        if(config != null)
			onPadding(
				String.format("SQL Database URL: %s",  config.sql_addr)
			);
			onPadding(
				String.format("Error Message: %s", last_exception.getMessage())
			);
        return true;
    }

}
