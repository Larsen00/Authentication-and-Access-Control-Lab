import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serial;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
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
    private Map<String, String> config;
    private SessionManager sessionManager;
    private Boolean hasLoadedDummyData = false;

    // Constructor
    public PrintApplication() throws RemoteException{
        this.printers = new HashMap<>();
        this.usersMap = new HashMap<>();
        this.config = new HashMap<>();
        this.sessionManager = new SessionManager();
    }

    @Override
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

    public void accessControl(String methodName, String[] userRole) throws PrintAppException {
           
        Path filePath = Paths.get("src/hierarchy-access-control.json");
        String content = null;
        try {
            content = new String(Files.readAllBytes(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = new JSONObject(content);
           
        // Check if the action is allowed for the role
        Set<String> allowedActions =new HashSet<>();
        for(String role: userRole){
            allowedActions.addAll(getActionsForRole(role, jsonObject));
        }

        // System.out.println(allowedActions);
        if (!allowedActions.contains(methodName)) {
            throw new PrintAppException("This Action ("+ methodName+") is not authorized");
        }
    }
    public void validateSession(SessionToken sessionToken) throws PrintAppException {
        if (!this.sessionManager.validateSessionToken(sessionToken)) {
            throw new PrintAppException("Session expired. Please login again.");
        }
    }

    // Should be used as a dynamic function to check if the user is authenticated either by session token or by username and password
    public SessionToken authenticateUser(String username, String password, SessionToken sessionToken, String action) throws PrintAppException, RemoteException {
        if (sessionToken == null) {
           Response<PrinterInterface> response = login(username, password);
           if (response.getData() != null) { // means login was successful and a server is running
               sessionToken = sessionManager.newSessionToken(usersMap.get(username));
           } else {
               return null;
           }
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

    public boolean isAllowedToStartServer(String username) throws RemoteException {
        User user = usersMap.get(username);
        try {
            accessControl("start", user.getUserType());
            return true;
        } catch (PrintAppException e) {
            return false;
        }
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
                    
                    Set<String> new_actions = getActionsForRole(parentRoleName,rolesJson);
                    if (new_actions!=null){
                        actions.addAll(new_actions);
                    }
                }
            }
        }
        return actions;
    }

    private void loadData(){
        if (!this.hasLoadedDummyData) {
            // Load users
            for (User u : loadUsers()) {
                this.addUser(u);
            }
            // Load printers
            for (Printer p : loadPrintersFromFile()) {
                this.registerPrinter(p);
            }
            this.hasLoadedDummyData = true;
        }
    }
    // Login method that returns a session token if the user is authenticated
    public Response<PrinterInterface> login(String userName, String password) throws PrintAppException {
        this.loadData();

        if (!this.usersMap.containsKey(userName)) {
            return new Response<>(null, "User does not exist", null);
        }

        User user = usersMap.get(userName);

        if (!user.comparePassword(password)) {
            return new Response<>(null, "Password is incorrect", null);
        }

        return new Response<>(this, "Login successful", null);
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
    
    public String displayPrinters(SessionToken sessionToken) throws PrintAppException {
        validateSession(sessionToken);
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

    public Response<PrinterInterface> connect(String username, String password, SessionToken sessionToken) throws RemoteException, PrintAppException {

        // Case the server is running but the user has not connected to it (different client started the server)
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        PrinterInterface printApp = null;
        try {
            printApp = (PrinterInterface) registry.lookup("PrinterServer");
        } catch (NotBoundException e) {
            throw new PrintAppException("Server could not be found");
        }
        sessionToken = printApp.authenticateUser(username, password, sessionToken, null);
        return new Response<>(printApp, "Connected to already running server", sessionToken);
    }

    public Response<PrinterInterface> start(String username, String password, SessionToken sessionToken, boolean restart) throws PrintAppException, RemoteException {
        Registry registry;
        String action = restart ? "restart" : "start";
        return this.printServer(username, password, sessionToken, action);
    }

    public Response<Void> stop(SessionToken sessionToken, Boolean restart) throws PrintAppException, RemoteException {
        String action = restart ? "restart" : "stop";

        // Authenticate the action
        authenticateAction(action, sessionToken);

        try {
            // Get the current registry
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            killServer(registry);

            return new Response<>(null, "Server stopped successfully", null);


        } catch (NotBoundException e) {
            throw new PrintAppException("Server was not running");
        } catch (Exception e) {
            throw new PrintAppException("Error while stopping the server: " + e.getMessage());
        }
    }

    private void killServer(Registry registry) throws NotBoundException, RemoteException {
        // Unbind the server from the registry
        registry.unbind("PrinterServer");
        boolean forceUnexport = true;
        UnicastRemoteObject.unexportObject(this, forceUnexport);
    }

    public Response<PrinterInterface> printServer(String username, String password,SessionToken sessionToken, String action) throws PrintAppException, RemoteException {

        this.loadData();
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            try {
                registry = LocateRegistry.getRegistry(1099);
            } catch (RemoteException ex) {
                return new Response<>(null, "Can't get Registry", null);
            }
        }

        try {
            registry.bind("PrinterServer", this);
        } catch (AlreadyBoundException | RemoteException e) {
            return new Response<>(null, "Could not bind application", null);
        }
        sessionToken = this.authenticateUser(username, password, sessionToken, action);
        return new Response<>(this, "Server started successfully", sessionToken);
    }

    public static ArrayList<Printer> loadPrintersFromFile() {
        ArrayList<Printer> printers = new ArrayList<>();
        String fileName = "dummyData/dummyPrinters.txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                printers.add(new Printer(line.trim()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return printers;
    }

    public static ArrayList<User> loadUsers() {
        ArrayList<User> users = new ArrayList<>();

        try {
            Path filePath = Paths.get("dummyData/users.json");
            String content = new String(Files.readAllBytes(filePath));

            // Start by parsing the content as a JSONArray, not JSONObject
            JSONArray usersArray = new JSONArray(content);

            // Loop through each user object in the array
            for (int i = 0; i < usersArray.length(); i++) {
                JSONObject userObj = usersArray.getJSONObject(i);
                String name = userObj.getString("name");
                String password = userObj.getString("password");
                JSONArray userTypeArray = userObj.getJSONArray("userType");
                String[] userType = new String[userTypeArray.length()];
                for (int j = 0; j < userTypeArray.length(); j++) {
                    userType[j] = userTypeArray.getString(j);
                }
                // Create a new User and add it to the list
                User user = new User(name, password, userType);
                users.add(user);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return users;
    }
}