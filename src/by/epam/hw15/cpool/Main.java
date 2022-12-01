package by.epam.hw15.cpool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {
	
	public static final String SQL = "SELECT * FROM users";

	public static void main(String[] args) throws ConnectionPoolException, SQLException {
		
		ConnectionPool connectionPool = new ConnectionPool();
		connectionPool.initPoolData();	
		
		Connection connection = connectionPool.takeConnection();	
		
		Statement statement = connection.createStatement();
		
		ResultSet resultSet = statement.executeQuery(SQL);		
		
		while(resultSet.next()) {
			int id = resultSet.getInt("user_id");
			String firstName = resultSet.getString("first_name");
			String lastName = resultSet.getString("last_name");
			String login = resultSet.getString("login");
			String password = resultSet.getString("password");
			String email = resultSet.getString("email"); 
			
			System.out.printf("%d, %s, %s, %s, %s, %s", id, firstName, lastName, login, password, email);
		}
		
		
		
		
		
	}

}
