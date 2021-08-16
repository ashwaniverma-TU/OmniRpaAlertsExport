package exception;

public class OmniAlertsException extends Exception{
    public OmniAlertsException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    public OmniAlertsException() {
        super("Server error.");
    }

    public OmniAlertsException(String message) {
        super(message);
    }

    public OmniAlertsException(String message, Throwable cause) {
        super(message, cause);
    }
}
