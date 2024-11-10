import java.sql.Timestamp;
import java.security.SecureRandom;

public class SessionManager {

    public SessionToken newSessionToken(User user) {
        Timestamp timeStamp = new Timestamp(System.currentTimeMillis());

        // Generate a session ID (random byte array of length 32)
        byte[] sessionID = new byte[32];
        new SecureRandom().nextBytes(sessionID);

        String signature = "True";

        // Create a new SessionToken using the constructor
        return new SessionToken(timeStamp, sessionID, user, signature);
    }

    // Method to validate if a session token is still valid
    public Boolean validateSessionToken(SessionToken sessionToken) {
        // Check if the signature is valid
        if (!sessionToken.getSignature().equals("True")) {
            return false;
        }

        // Check if the token is not older than 2 minutes
        long currentTimeMillis = System.currentTimeMillis();
        long tokenTimeMillis = sessionToken.getTimeStamp().getTime();
        long elapsedMillis = currentTimeMillis - tokenTimeMillis;

        // If the session token is older than 2 minutes (120,000 milliseconds), it’s invalid
        return elapsedMillis <= 120000;
    }

    public User getUser(SessionToken sessionToken) {
        // Access the user through the getter
        return sessionToken.getUser();
    }
}
