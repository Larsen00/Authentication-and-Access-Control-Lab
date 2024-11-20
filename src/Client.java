import java.io.FileWriter;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;

public class Client {

    // Custom functional interface that allows RemoteException
    @FunctionalInterface
    interface RemoteCommand {
        void execute(String[] args) throws RemoteException, NotBoundException, PrintAppException;
    }
    private PrinterInterface printApp;
    private Map<String, RemoteCommand> commands;
    private SessionToken sessionToken;
    private ArrayList<String> printAppResponse = new ArrayList<>();
    enum ScreenState {
        LOGIN_SCREEN,
        MENU,
        EXIT
    }


    private ScreenState screen = ScreenState.LOGIN_SCREEN;

    public static void main(String[] args) {
        Client client = new Client();


            while (client.screen != Client.ScreenState.EXIT) {
                try {
                    client.printAppResponse = new ArrayList<>();
                    if (client.screen == Client.ScreenState.LOGIN_SCREEN) {
                        client.setUpLoginCommands();
                        client.displayCommands();
                        client.handleCommand();
                    } else if (client.screen == Client.ScreenState.MENU) {
                        client.setUpCommands();
                        client.displayCommands();
                        client.handleCommand();
                    }
                } catch(RemoteException e){
                    System.err.println("An error occurred while executing the command:");
                    e.printStackTrace();
                } catch(NumberFormatException e){
                    System.err.println("Invalid number format: " + e.getMessage());
                } catch(PrintAppException e){
                    System.err.println(e.getMessage());
                    System.out.println();
                    if (e.getMessage().equals("Session expired. Please login again.")) {
                        client.screen = ScreenState.LOGIN_SCREEN;
                    }
                } catch (NotBoundException e) {
                    System.err.println("Error when trying to bind the server: " + e.getMessage());
                }
            }
        System.exit(0); //Exit application
    }

    private void handleCommand() throws NotBoundException, PrintAppException, RemoteException {
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        String[] parts = input.trim().split("\\s+");
        String command = parts[0];
        RemoteCommand action = this.commands.get(command);
        if (action != null) {
            if (this.screen == ScreenState.LOGIN_SCREEN || this.printServerCheck(command) ) {
                action.execute(parts);
                this.printApplicationMessage();
            }
        } else {
            System.out.println("Unknown command. Please try again.");
        }
    }

    public void printApplicationMessage() {
        if (this.printAppResponse.isEmpty()) {
            return;
        }
        System.out.println("\nApplication Message:");
        for (String message : this.printAppResponse) {
            System.out.println(message);
        }
    }

    public void login() throws RemoteException, PrintAppException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("User Name");
        System.out.print(">");
        String username = scanner.nextLine();

        System.out.println("Password");
        System.out.print(">");

        String password = scanner.nextLine();
        PrinterInterface printAppInstance;
        if (printApp == null) {
            printAppInstance = new PrintApplication();
        } else {
            printAppInstance = this.printApp;
        }

        Response<PrinterInterface> response = printAppInstance.login(username, password);
        this.printAppResponse.add(response.getMessage());
        if (response.getData() != null && this.printApp == null){
            this.printApp = response.getData();
        }
        if (response.getData() != null) {
            try {
                response = this.printApp.connect(username, password, null);
                this.printAppResponse.add(response.getMessage());
                this.printApp = response.getData();
                this.sessionToken = response.getSessionToken();
                this.screen = ScreenState.MENU;
            } catch (RemoteException e) {
                if (this.printApp.isAllowedToStartServer(username)) {
                    this.handleStart(username, password, false);
                    this.screen = ScreenState.MENU;
                } else {
                    printAppResponse.add("Server not running, contact user with admin rights to start the server.");
                }
            }
        }
    }

    public void setUpCommands() {
        // Map of commands to corresponding methods
        this.commands = new HashMap<>();
        commands.put("print", this::handlePrint);
        commands.put("queue", this::handleQueue);
        commands.put("top", this::handleTopQueue);
        commands.put("start", args -> handleStart(null, null, false));
        commands.put("stop", args -> handleStop(false));
        commands.put("status", this::handleStatus);
        commands.put("restart", args -> handleRestart());
        commands.put("readConfig", this::handleReadConfig);
        commands.put("setConfig", this::handleSetConfig);
        commands.put("printers", args -> handlePrinters());
        commands.put("exit", args -> handleExit());
        commands.put("logout", args -> handleLogout());
    }

    public void displayCommands() {
        System.out.println("Available commands:");
        int index = 1;
        for (String command : commands.keySet()) {
            System.out.println(index + ". " + command);
            index++;
        }
        System.out.println();
        System.out.print("\n> ");
    }

    public void setUpLoginCommands() {
        // Map of commands to corresponding methods
        this.commands = new HashMap<>();
        commands.put("login", args -> login());
        commands.put("exit", args -> handleExit());
    }

    private void handleLogout() {
        this.sessionToken = null;
        this.screen = ScreenState.LOGIN_SCREEN;
    }

    public boolean printServerCheck(String action) {
        if (printApp == null && !action.equals("start") && !action.equals("stop") && !action.equals("restart") && !action.equals("exit")) {
            printAppResponse.add("Error: The server is not running. Please start the server first.");
            return false;
        }
        return true;
    }


    private void handlePrint(String[] args) throws RemoteException, PrintAppException {
        if (args.length == 3) {
            printApp.print(args[1], args[2], this.sessionToken);
        } else {
            System.out.println("Usage: print <filename> <printer>");
        }
    }

    private void handleQueue(String[] args) throws RemoteException, PrintAppException {
        if (args.length == 2) {
            Queue<PrintJob> queue = printApp.queue(args[1], this.sessionToken);
            System.out.println("Queue for " + args[1] + ": " + queue);
        } else {
            System.out.println("Usage: queue <printer>");
        }
    }

    private void handleTopQueue(String[] args) throws RemoteException, PrintAppException {
        if (args.length == 3) {
            int job = Integer.parseInt(args[2]);
            printApp.TopQueue(args[1], job, this.sessionToken);
        } else {
            System.out.println("Usage: top <printer> <job>");
        }
    }

    private boolean handleStart(String username, String password, Boolean restart) throws RemoteException  {
        try {
            PrintApplication printAppInstance = new PrintApplication();
            Response<PrinterInterface> response = printAppInstance.start(username, password, this.sessionToken, restart);
            this.printAppResponse.add(response.getMessage());
            this.sessionToken = response.getSessionToken();
            printApp = response.getData();
            return true;
        } catch (PrintAppException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private void handleStop(Boolean restart) throws RemoteException, PrintAppException {
        Response<Void> response = printApp.stop(this.sessionToken, restart);
        this.printAppResponse.add(response.getMessage());
        this.printApp = null;
    }

    private void handleStatus(String[] args) throws RemoteException, PrintAppException {
        if (args.length == 2) {
            String status = this.printApp.status(args[1], this.sessionToken);
            System.out.println("Status: " + status);
        } else {
            System.out.println("Usage: status <printer>");
        }
    }

    private void handleRestart() throws RemoteException, PrintAppException {
        this.handleStop(true);
        if (!this.handleStart(null, null, true)) {
            System.out.println("Could not restart the server.");
        }
    }

    private void handleReadConfig(String[] args) throws RemoteException, PrintAppException {
        if (args.length == 2) {
            String config = this.printApp.readConfig(args[1], this.sessionToken);
            System.out.println("Config value: " + config);
        } else {
            System.out.println("Usage: readConfig <parameter>");
        }
    }

    private void handleSetConfig(String[] args) throws RemoteException, PrintAppException {
        if (args.length == 3) {
            this.printApp.setConfig(args[1], args[2], this.sessionToken);
            System.out.println("Config set successfully.");
        } else {
            System.out.println("Usage: setConfig <parameter> <value>");
        }
    }

    private void handleExit() {
        System.out.println("Exiting...");
        this.screen = ScreenState.EXIT;
    }

    private void handlePrinters() throws RemoteException, PrintAppException {
        System.out.println(this.printApp.displayPrinters(this.sessionToken));
    }
}
