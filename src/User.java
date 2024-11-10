import java.io.Serial;
import java.io.Serializable;

public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String name;
    private String password;
    private String userType;

    public User(String name, String password, String userType){
        this.name = name;
        this.password = password;
        this.userType=userType;
    }

    public boolean comparePassword(String password){
        return this.password.equals(password);
    }
    public String getUserType(){
        return this.userType;
    }

    public String getName(){
        return this.name;
    }

}
