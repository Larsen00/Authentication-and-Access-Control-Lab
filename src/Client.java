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
        try {
            Client client = new Client();
            Scanner scanner = new Scanner(System.in);

            while(client.screen != Client.ScreenState.EXIT) {

                if (client.screen == Client.ScreenState.LOGIN_SCREEN) {

                    client.login(scanner);
                    client.printApplicationMessage();
                    client.setUpCommands();

                } else if (client.screen == Client.ScreenState.MENU) {
                    System.out.println();
                    client.displayCommands();
                    client.printAppResponse = new ArrayList<>();
                    System.out.print("\n> ");
                    String input = scanner.nextLine();
                    String[] parts = input.trim().split("\\s+");
                    String command = parts[0];
                    System.out.println();

                    RemoteCommand action = client.commands.get(command);
                    if (action != null) {
                        try {
                            if (client.printServerCheck(command)) {
                                action.execute(parts);
                                client.printApplicationMessage();
                            }
                        } catch (RemoteException e) {
                            System.err.println("An error occurred while executing the command:");
                            e.printStackTrace();
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid number format: " + e.getMessage());
                        } catch (PrintAppException e) {
                            client.printApplicationMessage();
                            System.err.println(e.getMessage());
                            System.out.println();
                            if (e.getMessage().equals("Session expired. Please login again.")) {
                                client.screen = ScreenState.LOGIN_SCREEN;
                            }
                        }
                    } else {
                        System.out.println("Unknown command. Please try again.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    public void login(Scanner scanner) throws  RemoteException{
        while(true){
            System.out.println("User Name");
            System.out.print(">");
            String username = scanner.nextLine();

            System.out.println("Password");
            System.out.print(">");

            String password = scanner.nextLine();
            try {
                if (this.printApp == null && handleStart(username, password, false)) {
                    this.sessionToken = printApp.login(username, password);
                    this.screen = ScreenState.MENU;
                    break;
                }
                System.out.println("Login successful.");
            } catch (PrintAppException e) {
                System.out.println(e.getMessage());
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

    private void handleLogout() {
        this.sessionToken = null;
        this.screen = ScreenState.LOGIN_SCREEN;
    }

    public boolean printServerCheck(String action) {
        if (printApp == null && !action.equals("start") && !action.equals("stop") && !action.equals("restart") && !action.equals("exit")) {
            System.out.println("Error: The server is not running. Please start the server first.");
            return false;
        }
        return true;
    }

    public void displayCommands() {
        System.out.println("Available commands:");
        int index = 1;
        for (String command : commands.keySet()) {
            System.out.println(index + ". " + command);
            index++;
        }
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
