import java.sql.Timestamp;

public class SessionToken {
    private Timestamp timeStamp;
    private byte[] sessionID;
    private User user;
    private String signature;

    // Constructor to initialize all fields
    public SessionToken(Timestamp timeStamp, byte[] sessionID, User user, String signature) {
        this.timeStamp = timeStamp;
        this.sessionID = sessionID;
        this.user = user;
        this.signature = signature;
    }

    // Getter methods for each field
    public Timestamp getTimeStamp() {
        return timeStamp;
    }

    public byte[] getSessionID() {
        return sessionID;
    }

    public User getUser() {
        return user;
    }

    public String getSignature() {
        return signature;
    }
}
