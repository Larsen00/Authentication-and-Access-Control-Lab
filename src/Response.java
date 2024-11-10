import java.io.Serializable;

public class Response<T> implements Serializable {
    private static final long serialVersionUID = 1L; // Add a serialVersionUID for serialization

    private T data;
    private String message;
    private SessionToken sessionToken;

    public Response(T data, String message, SessionToken sessionToken) {
        this.data = data;
        this.message = message;
        this.sessionToken = sessionToken;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
    public SessionToken getSessionToken() {
        return sessionToken;
    }
}
