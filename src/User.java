import java.io.Serial;
import java.io.Serializable;

public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String name;
    private String password;

    public User(String name, String password){
        this.name = name;
        this.password = password;
    }

    public boolean comparePassword(String password){
        return this.password.equals(password);
    }

    public String getName(){
        return this.name;
    }

}
