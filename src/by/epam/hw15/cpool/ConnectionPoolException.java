package by.epam.hw15.cpool;

public class ConnectionPoolException extends Exception {

	private static final long serialVersionUID = -5091120782685249283L;

	public ConnectionPoolException() {
		super();
	}

	public ConnectionPoolException(String msg) {
		super(msg);
	}

	public ConnectionPoolException(Exception e) {
		super(e);
	}

	public ConnectionPoolException(String msg, Exception e) {
		super(msg, e);
	}

}
