import java.io.IOException;
import java.io.Serial;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class PrintApplication extends UnicastRemoteObject implements PrinterInterface {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, Printer> printers;
    private final Map<String, User> usersMap;
    private Queue<String> receivedMessages;
    private Map<String, String> config;
    private SessionManager sessionManager;

    // Constructor
    public PrintApplication() throws RemoteException{
        this.printers = new HashMap<>();
        this.usersMap = new HashMap<>();
        this.receivedMessages = new LinkedList<>();
        this.config = new HashMap<>();
        this.sessionManager = new SessionManager();
    }

    public void print(String filename, String printerName, SessionToken sessionToken) throws RemoteException, PrintAppException {
        authenticateAction("print", sessionToken);
        Printer printer = getPrinter(printerName);
        if (printer != null){
            printer.print(filename);
        }
     }

    public Queue<PrintJob> queue(String printerName, SessionToken sessionToken) throws RemoteException, PrintAppException {
        authenticateAction("queue", sessionToken);
        Printer printer = getPrinter(printerName);
        if (printer != null){
            return printer.queue();
        }
        return null;
    }

    public void TopQueue(String printerName, int job, SessionToken sessionToken) throws RemoteException, PrintAppException {
        authenticateAction("topQueue", sessionToken);
        Printer printer = getPrinter(printerName);
        if (printer != null){
            try {
                printer.topQueue(job);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void setConfig(String parameter, String value, SessionToken sessionToken) throws RemoteException, PrintAppException {
        authenticateAction("setConfig", sessionToken);
        config.put(parameter, value);
    }


    public String readConfig(String parameter, SessionToken sessionToken) throws RemoteException, PrintAppException {
        authenticateAction("readConfig", sessionToken);
        return config.get(parameter);
    }

    public String status(String printerName, SessionToken sessionToken) throws RemoteException, PrintAppException {
        authenticateAction("status", sessionToken);
        Printer printer = getPrinter(printerName);
        if (printer != null){
            return printer.status();
        }
        return "Printer: "+printerName+" does not exist!";
    }

    public void accessControl(String methodName, String userRole) throws PrintAppException {
           
        Path filePath = Paths.get("src/hierarchy-access-control.json");
        String content = null;
        try {
            content = new String(Files.readAllBytes(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = new JSONObject(content);
           
        // Check if the action is allowed for the role
        Set<String> allowedActions = null;
        allowedActions = getActionsForRole(userRole, jsonObject);
        if (!allowedActions.contains(methodName)) {
            throw new PrintAppException("Incorrect password.");
        }
    }
    public void validateSession(SessionToken sessionToken) throws PrintAppException {
        if (!this.sessionManager.validateSessionToken(sessionToken)) {
            throw new PrintAppException("Session token is invalid");
        }
    }

    // Should be used as a dynamic function to check if the user is authenticated either by session token or by username and password
    public SessionToken authenticateUser(String username, String password, SessionToken sessionToken, String action) throws PrintAppException {
        if (sessionToken == null) {
           sessionToken = login(username, password);
        }
        validateSession(sessionToken);
        User user = sessionToken.getUser();
        if (action != null) {
            accessControl(action, user.getUserType());
        }
        return sessionToken;
    }

    public void authenticateAction(String action, SessionToken sessionToken) throws PrintAppException {
        validateSession(sessionToken);
        User user = sessionToken.getUser();
        accessControl(action, user.getUserType());
    }


    private Set<String> getActionsForRole(String roleName, JSONObject rolesJson)   {
        Set<String> actions = new HashSet<>();
        JSONObject role = rolesJson.optJSONObject(roleName);
        
        if (role == null) {
            return actions;
        }

        // Add actions of the current role
        JSONArray actionsArray = role.optJSONArray("actions");
        if (actionsArray.length() != 0 ) {
            for (int i = 0; i < actionsArray.length(); i++) {
                actions.add(actionsArray.getString(i));
            }
        }

        // Recursively add actions from extended roles
        JSONArray extendsArray = role.optJSONArray("extends");
        if (extendsArray.length() !=0) {
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

    // Login method that returns a session token if the user is authenticated
    public SessionToken login(String userName, String password) throws PrintAppException {
        if (this.usersMap.containsKey(userName)) {
            User user = usersMap.get(userName);
            if (user.comparePassword(password)) {
                return sessionManager.newSessionToken(user);
            } else {
                throw new PrintAppException("Incorrect password.");
            }
        } else {
            throw new PrintAppException("Username not found.");
        }
    }

    public void registerPrinter(Printer p) {
        printers.put(p.name, p);
    }

    public Printer getPrinter(String name) throws PrintAppException {
        if (printers.containsKey(name)){
            return printers.get(name);
        } else {
            throw new PrintAppException("Printer: "+name+" does not exist!");
        }
    }

    public String displayPrinters(){
        StringBuilder s = new StringBuilder();
        s.append("Displaying Printers:");
        int i = 1;
        for (Printer p : printers.values()){
            s.append("\n").append(i).append(". ").append(p.name);
            i++;
        }
        return s.toString();
    }

    public void addUser(User user) {
        usersMap.put(user.getName(), user);
    }

    public Response<PrinterInterface> start(String username, String password, SessionToken sessionToken, boolean restart) throws PrintAppException {
        Registry registry;
        String action = restart ? "restart" : "start";
        try {
            // Connect to the registry
            registry = LocateRegistry.getRegistry("localhost", 1099);
            PrinterInterface printApp = (PrinterInterface) registry.lookup("PrinterServer");
            sessionToken = printApp.authenticateUser(username, password, sessionToken, action);
            return new Response<>(printApp, "Server was already running", sessionToken);
        } catch (RemoteException | NotBoundException err) {
            try {
                PrintServer printServer = new PrintServer(); // Will start the new server
                registry = LocateRegistry.getRegistry("localhost", 1099);
                PrinterInterface printApp = (PrinterInterface) registry.lookup("PrinterServer");
                try {
                    sessionToken = printApp.authenticateUser(username, password, sessionToken, action);
                } catch (RemoteException e) {
                    return new Response<>(printApp, "Could not authenticate User", sessionToken);
                }
                return new Response<>(printApp, "Server started successfully", sessionToken);
            } catch (RemoteException e) {
                return new Response<>(null, "Server could not be started", sessionToken);
            } catch (NotBoundException e) {
                return new Response<>(null, "Could not find Server", sessionToken);
            }
        }
    }

    public Response<Void> stop(SessionToken sessionToken, Boolean restart) throws PrintAppException, RemoteException {
        String action = restart ? "restart" : "stop";
        authenticateAction(action, sessionToken);
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            registry.unbind("PrinterServer");
            return new Response<>(null, "Server stopped successfully", null);
        } catch (NotBoundException | RemoteException e) {
            throw new PrintAppException("Server was not running");
        }
    }
}