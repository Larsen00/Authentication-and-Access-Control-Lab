import java.io.*;
import java.rmi.AlreadyBoundException;
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
        accessControl("print", sessionToken);
        Printer printer = getPrinter(printerName);
        if (printer != null){
            printer.print(filename);
        }
     }

    public Queue<PrintJob> queue(String printerName, SessionToken sessionToken) throws RemoteException, PrintAppException {
        accessControl("queue", sessionToken);
        Printer printer = getPrinter(printerName);
        if (printer != null){
            return printer.queue();
        }
        return null;
    }

    public void TopQueue(String printerName, int job, SessionToken sessionToken) throws RemoteException, PrintAppException {
        accessControl("topQueue", sessionToken);
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
        accessControl("setConfig", sessionToken);
        config.put(parameter, value);
    }


    public String readConfig(String parameter, SessionToken sessionToken) throws RemoteException, PrintAppException {
        accessControl("readConfig", sessionToken);
        return config.get(parameter);
    }

    public String status(String printerName, SessionToken sessionToken) throws RemoteException, PrintAppException {
        accessControl("status", sessionToken);
        Printer printer = getPrinter(printerName);
        if (printer != null){
            return printer.status();
        }
        return "Printer: "+printerName+" does not exist!";
    }

    public void logAction(String action, String username, Boolean accessGranted) {
        String filepath = "logs/actionLog.txt";
        new java.io.File("logs").mkdirs();
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

        try (FileWriter writer = new FileWriter(filepath, true)) { // Open in append mode
            if (accessGranted) {
                writer.write(action + " by: " + username + " at " + timestamp + " - Access Granted\n");
            } else {
                writer.write(action + " by: " + username + " at " + timestamp + " - Access Denied\n");
            }
            writer.flush(); // Ensure data is written to the file
        } catch (IOException e) {
            throw new RuntimeException("Error writing job to file", e);
        }
    }

    public boolean checkAccessControl(String methodName, String username) {
        // A match is access granted, otherwise access denied

        String filePath = "dummyData/ACL.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Split each line by colon
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String subjectName = parts[0].trim();
                    String accessRight = parts[1].trim();
                    if (methodName.equals(subjectName) && accessRight.equals(username)) {
                        logAction(methodName, username, true);
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            logAction(methodName, username, false);
            e.printStackTrace();
        }
        return false;
    }

    public void accessControl(String methodName, SessionToken sessionToken) throws PrintAppException {

        validateSession(sessionToken);

        if (methodName == null) {
            return;
        }

        User user = sessionToken.getUser();

        // access control for the user
        if (checkAccessControl(methodName, user.getName())) {
            return;
        }

        // If the subjectName and accessRight pair match the method name and username is not found, throw an exception
        throw new PrintAppException("This Action ("+ methodName+") is not authorized");
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
        accessControl(action, sessionToken);
        return sessionToken;
    }

    public boolean isAllowedToStartServer(String username) throws RemoteException {
        User user = usersMap.get(username);
        return checkAccessControl("start", user.getName());
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
            logAction("login", userName, false);
            return new Response<>(null, "Password is incorrect", null);
        }
        logAction("login", userName, true);
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
        logAction("connected to server", username, true);
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
        accessControl(action, sessionToken);

        try {
            // Get the current registry
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            killServer(registry);
            logAction("Stopped the server", sessionToken.getUser().getName(), true);
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
        logAction("Started the server", username, true);
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
                // Create a new User and add it to the list
                User user = new User(name, password);
                users.add(user);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return users;
    }
}