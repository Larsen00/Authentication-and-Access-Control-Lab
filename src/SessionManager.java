import java.sql.Timestamp;

public class SessionManager {

    public SessionToken newSessionToken(User user) {
        SessionToken sessionToken = new SessionToken();
        sessionToken.TimeStamp = new Timestamp(System.currentTimeMillis());
        sessionToken.validationKey = new byte[32];
        sessionToken.user = user;
        sessionToken.signature = "True";

        // These values are used as the payload
        // A signature is made with all the values

        return sessionToken;
    }

    public Boolean validateSessionToken(SessionToken sessionToken){
       
        return sessionToken.signature.equals("True");
    }

    public User getUser(SessionToken sessionToken) {
        return sessionToken.user;
    }

}
