import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class PrintApplication extends UnicastRemoteObject implements PrinterInterface {

    Map<String, Printer> printers;
    Map<String, User> usersMap;
    Queue<String> receivedMessages;
    private Map<String, String> config;
    public PrintApplication() throws RemoteException{
        printers = new HashMap<>();
        usersMap = new HashMap<>();
        receivedMessages = new LinkedList<>();
        config = new HashMap<>();
    }

    @Override
    public void print(String filename, String printerName) throws RemoteException {
        Printer printer = getPrinter(printerName);
        if (printer != null){
            printer.print(filename);
        }
     }
    @Override
    public Queue<PrintJob> queue(String printerName) throws RemoteException{
        Printer printer = getPrinter(printerName);
        if (printer != null){
            return printer.queue();
        }
        return null;
    }
    @Override
    public void TopQueue(String printerName, int job) throws RemoteException{
        Printer printer = getPrinter(printerName);
        if (printer != null){
            try {
                printer.topQueue(job);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    @Override
    public void setConfig(String parameter, String value) throws RemoteException{
         config.put(parameter, value);
    }

    @Override
    public String readConfig(String parameter) throws RemoteException{
         return config.get(parameter);
    }

    @Override
    public void start() throws RemoteException{
        for (Printer p : printers.values()){
            p.run();
        }
    }
    @Override
    public void stop() throws RemoteException{
        for (Printer p : printers.values()){
            p.stop();
        }
    }
    @Override
    public void restart() throws RemoteException{
        printers.clear();
    }
    @Override
    public String status(String printerName) throws RemoteException{
        
        Printer printer = getPrinter(printerName);
        if (printer != null){
            return printer.status();
        }
        return "Printer: "+printerName+" does not exist!";
    }

    @Override
    public void accessControl(String methodName, String userRole) throws Exception {
        
            Path filePath = Paths.get("src/hierarchy-access-control.json");
            String content = new String(Files.readAllBytes(filePath));
            JSONObject jsonObject = new JSONObject(content);

            // Check if the action is allowed for the role
            Set<String> allowedActions = getActionsForRole(userRole, jsonObject);
            if (!allowedActions.contains(methodName)) {
                throw new AccessDeniedException();
            }

        
    }

    private Set<String> getActionsForRole(String roleName, JSONObject rolesJson) throws AccessDeniedException {
        Set<String> actions = new HashSet<>();
        JSONObject role = rolesJson.optJSONObject(roleName);

        if (role == null) {
            throw new AccessDeniedException();
        }

        // Add actions of the current role
        JSONArray actionsArray = role.optJSONArray("actions");
        if (actionsArray != null) {
            for (int i = 0; i < actionsArray.length(); i++) {
                actions.add(actionsArray.getString(i));
            }
        }

        // Recursively add actions from extended roles
        JSONArray extendsArray = role.optJSONArray("extends");
        if (extendsArray != null) {
            for (int i = 0; i < extendsArray.length(); i++) {
                String parentRoleName = extendsArray.optString(i);
                if (parentRoleName != null) {
                    actions.addAll(getActionsForRole(parentRoleName, rolesJson));
                }
            }
        }

        return actions;
    }

    public void receiveMessage(String message){
        receivedMessages.add(message);
        printMessages();
    }
    public void printMessages(){
        while(!receivedMessages.isEmpty()){
            System.err.println(receivedMessages.poll());
        }
    }

    public void login(String userName, String password){
        if (this.usersMap.containsKey(userName)){
            User user = usersMap.get(userName);
            if (user.getPassword().equals(password)){
                System.out.println("Logged in as " + userName);
                // ToDO retuner en sesseion token
            } else {
                System.out.println("Incorrect password");
            }
        } else {
            System.out.println("Username not found");
        }
    }

    public void logout(){
        // TODO should take in a session token and logout the user so other user cant logout eachother
    }
    public void createUser(String userName, String password, String userType) {
        if (this.usersMap.containsKey(userName)) {
            System.out.println("User already exists.");
            return;
        }
        this.usersMap.put(userName, new User(userName, password,userType));
    }

    public boolean validSession(){
        // Todo logout user return false
        return true;
    }


    public void registerPrinter(Printer p) {
        printers.put(p.name, p);
    }

    public Printer getPrinter(String name){
        if (printers.containsKey(name)){
            return printers.get(name);
        } else {
            System.out.println("Could not find printer");
            return null;
        }
    }
 

    public String displayPrinters(){
        String s = "";
        s += "Displaying Printers:";
        int i = 1;
        for (Printer p : printers.values()){
            s += "\n" + i + ". " + p.name ;
            i++;
        }
        return s;
    }
}