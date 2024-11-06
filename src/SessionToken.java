import java.sql.Timestamp;

public class SessionToken {
    Timestamp TimeStamp;
    byte[] validationKey;
    byte[] sessionID;
    User user;
    String signature;
}
