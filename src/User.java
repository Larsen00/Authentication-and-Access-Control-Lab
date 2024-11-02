public class User {

    private String name;
    public String password;

    public User(String name, String password){
        this.name = name;
        this.password = password;
    }

    public boolean comparePassword(String password){
        return password == this.password;
    }

}
