import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;


public class PrintServer{

    public PrintServer() {
        PrintApplication printApp;
        try {
            printApp = new PrintApplication();
        } catch (RemoteException e){
            System.out.println("Could not create Printer application");
            return;
        }
        // Load printers
        for (Printer p : loadPrintersFromFile(printApp)){
            printApp.registerPrinter(p);
        }

        // Load users
        for (User u : loadUsers()){
            printApp.addUser(u);
        }
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            try {
                registry = LocateRegistry.getRegistry(1099);
            } catch (RemoteException ex) {
                System.out.println("Can't get Registry");
                return;
            }
        }

        try {
            registry.bind("PrinterServer", printApp);
        } catch (AlreadyBoundException | RemoteException e) {
            System.out.println("Could not bind application");
        }

        System.out.println("Server is ready.");
    }
    public static void main(String[] args) throws RemoteException {
        new PrintServer();
    }
    public static ArrayList<Printer> loadPrintersFromFile(PrintApplication server) {
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