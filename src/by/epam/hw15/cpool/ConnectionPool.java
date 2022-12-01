package by.epam.hw15.cpool;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

public final class ConnectionPool {

	// основная очередь соединений
	private BlockingQueue<Connection> connectionQueue;

	// сюда дублируем то, соединение, с которым работаем
	private BlockingQueue<Connection> givenAwayQueue;

	private String url;
	private String user;
	private String password;
	private String poolSize;

	public ConnectionPool() {
		// извлекаем значения из db.properties
		DBResourceManager resourceManager = DBResourceManager.getInstance();
		this.url = resourceManager.getValue(DBParameter.DB_URL);
		this.user = resourceManager.getValue(DBParameter.DB_USER);
		this.password = resourceManager.getValue(DBParameter.DB_PASSWORD);
		this.poolSize = resourceManager.getValue(DBParameter.DB_POOL_SIZE);
	}

	// заполняем пул
	public void initPoolData() throws ConnectionPoolException {
		int pSize = Integer.parseInt(poolSize);
		connectionQueue = new ArrayBlockingQueue<Connection>(pSize);
		givenAwayQueue = new ArrayBlockingQueue<Connection>(pSize);

		try {
			// создаем соединения и передаем в основную очередь
			for (int i = 0; i < pSize; i++) {
				Connection connection = DriverManager.getConnection(url, user, password);
				// оборачиваем в PooledConnection
				PooledConnection pooledConnection = new PooledConnection(connection);
				// добавляем
				connectionQueue.add(pooledConnection);
			}
		} catch (SQLException e) {
			throw new ConnectionPoolException("SQLException in ConnectionPool", e);
		}
	}

	// получаем соединение
	public Connection takeConnection() throws ConnectionPoolException {
		Connection connection = null;

		try {
			// получаем и удаляем соединение из основной очереди
			connection = connectionQueue.take();
			// сразу дублируем его во вторую очередь
			givenAwayQueue.add(connection);
		} catch (InterruptedException e) {
			throw new ConnectionPoolException("Error connecting to the data source", e);
		}

		return connection;
	}

	public void closeConnection(Connection con, Statement st, ResultSet rs) {
		try {
			con.close();
		} catch (SQLException e) {
			// logger.log(Level.ERROR, "Connection isn't return to the pool.");
		}

		try {
			rs.close();
		} catch (SQLException e) {
			// logger.log(Level.ERROR, "ResultSet isn't closed.");
		}

		try {
			st.close();
		} catch (SQLException e) {
			// logger.log(Level.ERROR, "Statement isn't closed.");
		}
	}

	public void closeConnection(Connection con, Statement st) {
		try {
			con.close();
		} catch (SQLException e) {
			// logger.log(Level.ERROR, "Connection isn't return to the pool.");
		}

		try {
			st.close();
		} catch (SQLException e) {
			// logger.log(Level.ERROR, "Statement isn't closed.");
		}
	}

	// проходка по двум очередям с целью закрытия активных соединений
	public void dispose() {
		clearConnectionQueue();
	}

	private void clearConnectionQueue() {
		try {
			closeConnectionsQueue(givenAwayQueue);
			closeConnectionsQueue(connectionQueue);
		} catch (SQLException e) {
			// logger.log(Level.ERROR, "Error closing the connection.", e);
		}
	}

	private void closeConnectionsQueue(BlockingQueue<Connection> queue) throws SQLException {
		Connection connection;
		while ((connection = queue.peek()) != null) {
			if (!connection.getAutoCommit()) {
				connection.commit();
			}
			((PooledConnection) connection).reallyClose();
		}
	}

	/*--------------------------------------------------------------------------------------------*/

	// объекты этого класса -> элементы очередей -> соединения
	private class PooledConnection implements Connection {

		private Connection connection;

		public PooledConnection(Connection connection) {
			this.connection = connection;
		}

		/*
		 * закрываем ресурс(соединение)
		 */
		public void reallyClose() throws SQLException {
			connection.close();
		}

		/*
		 * здесь, после работы с соединением, удалем его из резервной очереди и
		 * сохраняем обратно в основную
		 */
		@Override
		public void close() throws SQLException {
			if (connection.isClosed()) {
				throw new SQLException("Attempting to close closed connection");
			}

			if (connection.isReadOnly()) {
				connection.setReadOnly(false);
			}

			if (!givenAwayQueue.remove(this)) {
				throw new SQLException("Error deleting connection from the givenAwayQueue");
			}

			if (!connectionQueue.offer(this)) {
				throw new SQLException("Error allocating connection in the pool.");

			}
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return null;
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return false;
		}

		@Override
		public Statement createStatement() throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql) throws SQLException {
			return null;
		}

		@Override
		public CallableStatement prepareCall(String sql) throws SQLException {
			return null;
		}

		@Override
		public String nativeSQL(String sql) throws SQLException {
			return null;
		}

		@Override
		public void setAutoCommit(boolean autoCommit) throws SQLException {

		}

		@Override
		public boolean getAutoCommit() throws SQLException {
			return false;
		}

		@Override
		public void commit() throws SQLException {

		}

		@Override
		public void rollback() throws SQLException {

		}

		@Override
		public boolean isClosed() throws SQLException {
			return false;
		}

		@Override
		public DatabaseMetaData getMetaData() throws SQLException {
			return null;
		}

		@Override
		public void setReadOnly(boolean readOnly) throws SQLException {

		}

		@Override
		public boolean isReadOnly() throws SQLException {
			return false;
		}

		@Override
		public void setCatalog(String catalog) throws SQLException {

		}

		@Override
		public String getCatalog() throws SQLException {
			return null;
		}

		@Override
		public void setTransactionIsolation(int level) throws SQLException {

		}

		@Override
		public int getTransactionIsolation() throws SQLException {
			return 0;
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			return null;
		}

		@Override
		public void clearWarnings() throws SQLException {

		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
				throws SQLException {
			return null;
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
				throws SQLException {
			return null;
		}

		@Override
		public Map<String, Class<?>> getTypeMap() throws SQLException {
			return null;
		}

		@Override
		public void setTypeMap(Map<String, Class<?>> map) throws SQLException {

		}

		@Override
		public void setHoldability(int holdability) throws SQLException {

		}

		@Override
		public int getHoldability() throws SQLException {
			return 0;
		}

		@Override
		public Savepoint setSavepoint() throws SQLException {
			return null;
		}

		@Override
		public Savepoint setSavepoint(String name) throws SQLException {
			return null;
		}

		@Override
		public void rollback(Savepoint savepoint) throws SQLException {

		}

		@Override
		public void releaseSavepoint(Savepoint savepoint) throws SQLException {

		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
				throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
				int resultSetHoldability) throws SQLException {
			return null;
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
				int resultSetHoldability) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
			return null;
		}

		@Override
		public Clob createClob() throws SQLException {
			return null;
		}

		@Override
		public Blob createBlob() throws SQLException {
			return null;
		}

		@Override
		public NClob createNClob() throws SQLException {
			return null;
		}

		@Override
		public SQLXML createSQLXML() throws SQLException {
			return null;
		}

		@Override
		public boolean isValid(int timeout) throws SQLException {
			return false;
		}

		@Override
		public void setClientInfo(String name, String value) throws SQLClientInfoException {

		}

		@Override
		public void setClientInfo(Properties properties) throws SQLClientInfoException {

		}

		@Override
		public String getClientInfo(String name) throws SQLException {
			return null;
		}

		@Override
		public Properties getClientInfo() throws SQLException {
			return null;
		}

		@Override
		public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
			return null;
		}

		@Override
		public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
			return null;
		}

		@Override
		public void setSchema(String schema) throws SQLException {

		}

		@Override
		public String getSchema() throws SQLException {
			return null;
		}

		@Override
		public void abort(Executor executor) throws SQLException {

		}

		@Override
		public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {

		}

		@Override
		public int getNetworkTimeout() throws SQLException {
			return 0;
		}

	}
	/*--------------------------------------------------------------------------------------------*/

}
