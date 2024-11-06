public class User {

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
    public String getPassword(){
        return this.password;
    }

}
