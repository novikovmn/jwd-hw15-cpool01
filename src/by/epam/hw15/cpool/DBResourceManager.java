package by.epam.hw15.cpool;

import java.util.ResourceBundle;

public final class DBResourceManager {
	
	private final static DBResourceManager instance = new DBResourceManager();
	
	private ResourceBundle bundle = ResourceBundle.getBundle("db");
	
	private DBResourceManager() {
		
	}

	public static DBResourceManager getInstance() {
		return instance;
	}
	
	public String getValue(String key) {
		return bundle.getString(key);
	}
	
	

}


