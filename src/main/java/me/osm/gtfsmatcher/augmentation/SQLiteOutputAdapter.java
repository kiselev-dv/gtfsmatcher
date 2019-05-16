package me.osm.gtfsmatcher.augmentation;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class SQLiteOutputAdapter implements OutputAdapter {
	
	private static final int BATCH_SIZE = 10_000;
	private static final HashMap<String, String> columnTypes = new HashMap<>();
	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
		
		columnTypes.put("lat", "REAL");
		columnTypes.put("lng", "REAL");
		columnTypes.put("osm_id", "INTEGER");
		columnTypes.put("stop_sequence", "INTEGER");
		columnTypes.put("direction_id", "INTEGER");
		columnTypes.put("bikes_allowed", "INTEGER");
		columnTypes.put("wheelchair_accessible", "INTEGER");
		columnTypes.put("route_type", "INTEGER");
	}
	
	private Connection connection;
	private PreparedStatement prepStatement;
	private List<String> columns;
	
	private int batches = 0;
	private int records = 0;

	SQLiteOutputAdapter(File out) {
		if (out.exists()) {
			out.delete();
		}
		
        String url = "jdbc:sqlite:" + out.getPath();
        try {
            this.connection = DriverManager.getConnection(url);
            this.connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

	@Override
	public void close() throws Exception {
		this.connection.close();
	}

	@Override
	public void newEntry(String name, List<String> columns) {
		try {
			Statement stmnt = this.connection.createStatement();
			
			List<String> columnDefinitions = getDefinitions(name, columns);
			
			String tableName = StringUtils.remove(name, ".txt").toLowerCase();
			String tableSQL = "create table if not exists " 
					+ tableName 
					+ " (" + StringUtils.join(columns, ", \n") + ");";

			stmnt.closeOnCompletion();
			stmnt.executeUpdate(tableSQL);
			this.connection.commit();
			
			prepStatement = this.connection.prepareStatement("insert into " + 
					tableName + 
					" values(" + StringUtils.join(Collections.nCopies(columns.size(), "?"), ", ") + ")");

			
		} catch(Exception e) {
			throw new Error(e);
		}
	}

	private List<String> getDefinitions(String table, List<String> columns) {
		this.columns = columns;
		return columns.stream()
				.map(column -> column + " " + columnTypes.getOrDefault(column, "TEXT"))
				.collect(Collectors.toList());
	}

	@Override
	public void closeEntry() {
		try {
			if (batches > 0) {
				prepStatement.executeBatch();
				this.connection.commit();
				System.out.println(records + " records writen");
				records = 0;
				batches = 0;
			}
		}
		catch (Exception e) {
			throw new Error(e);
		}
	}

	@Override
	public void printRecord(List<String> rowStrings) {
		try {
			int i = 0;
			for(String s : rowStrings) {
				String column = columns.get(i);
				String type = columnTypes.getOrDefault(column, "TEXT");
				
				switch(type) {
				case "REAL":
					if (StringUtils.stripToNull(s) == null) {
						prepStatement.setNull(i + 1, java.sql.Types.REAL);
					}
					else {
						prepStatement.setDouble(i + 1, Double.valueOf(s));
					}
					
					break;
				case "INTEGER":
					if (StringUtils.stripToNull(s) == null) {
						prepStatement.setNull(i + 1, java.sql.Types.INTEGER);
					}
					else {
						prepStatement.setLong(i + 1, Long.valueOf(s));
					}

					break;
				default:
					prepStatement.setString(i + 1, StringUtils.stripToNull(s));
				}
				
				i++;
			}
			prepStatement.addBatch();
			batches++;
			records++;
			
			if (batches % BATCH_SIZE == 0) {
				prepStatement.executeBatch();
				this.connection.commit();
				batches = 0;
				prepStatement.clearBatch();
				System.out.println(records + " records writen");
			}
			
		}
		catch (Exception e) {
			throw new Error(e);
		}
	}
	

}
