import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

public class Client {

    // Custom functional interface that allows RemoteException
    @FunctionalInterface
    interface RemoteCommand {
        void execute(String[] args) throws RemoteException, NotBoundException;
    }
    private static PrinterInterface printApp;
    private Map<String, RemoteCommand> commands;

    public static void main(String[] args) {
        try {

            Client client = new Client();
            client.setUpCommands();



            Scanner scanner = new Scanner(System.in);

            while (true) {
                client.displayCommands();
                System.out.print("\n> ");
                String input = scanner.nextLine();
                String[] parts = input.trim().split("\\s+");
                String command = parts[0];
                System.out.println();

                RemoteCommand action = client.commands.get(command);
                if (action != null) {
                    try {
                        if (client.printServerCheck(command)){
                            action.execute(parts);
                        }
                    } catch (RemoteException e) {
                        System.err.println("An error occurred while executing the command:");
                        e.printStackTrace();
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid number format: " + e.getMessage());
                    }
                } else {
                    System.out.println("Unknown command. Please try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setUpCommands() {
        // Map of commands to corresponding methods
        this.commands = new HashMap<>();
        commands.put("print", Client::handlePrint);
        commands.put("queue", Client::handleQueue);
        commands.put("top", Client::handleTopQueue);
        commands.put("start", args -> handleStart());
        commands.put("stop", args -> handleStop());
        commands.put("status", Client::handleStatus);
        commands.put("restart", args -> handleRestart());
        commands.put("readConfig", Client::handleReadConfig);
        commands.put("setConfig", Client::handleSetConfig);
        commands.put("printers", args -> handlePrinters());
        commands.put("exit", args -> handleExit());
        commands.put("check",args-> checkPrivilege());
    }

    public void checkPrivilege() {
        try {
            printApp.accessControl("restart","user");
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
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

    private static void handlePrint(String[] args) throws RemoteException {
        if (args.length == 3) {
            printApp.print(args[1], args[2]);
        } else {
            System.out.println("Usage: print <filename> <printer>");
        }
    }

    private static void handleQueue(String[] args) throws RemoteException {
        if (args.length == 2) {
            Queue<PrintJob> queue = printApp.queue(args[1]);
            System.out.println("Queue for " + args[1] + ": " + queue);
        } else {
            System.out.println("Usage: queue <printer>");
        }
    }

    private static void handleTopQueue(String[] args) throws RemoteException {
        if (args.length == 3) {
            int job = Integer.parseInt(args[2]);
            printApp.TopQueue(args[1], job);
        } else {
            System.out.println("Usage: top <printer> <job>");
        }
    }

    private static void handleStart() {
        Registry registry;
        try {
            // Connect to the registry
            registry = LocateRegistry.getRegistry("localhost", 1099);
            printApp = (PrinterInterface) registry.lookup("PrinterServer");
            System.out.println("Server was already running");
        } catch (RemoteException | NotBoundException err) {
            System.out.println("..starting server");
            try {
                PrintServer printServer = new PrintServer(); // Will start the new server
                registry = LocateRegistry.getRegistry("localhost", 1099);
                printApp = (PrinterInterface) registry.lookup("PrinterServer");
            } catch (RemoteException e) {
                System.out.println("Could not find port");
            } catch (NotBoundException e) {
                System.out.println(("Could not find Server"));
            }
        }
    }


    private static void handleStop() {

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            registry.unbind("PrinterServer");
            printApp = null;
            System.out.println("Server is shutting down...");
        } catch (NotBoundException | RemoteException e) {
            System.out.println("Server was not running");
        }
    }

    private static void handleStatus(String[] args) throws RemoteException {
        if (args.length == 2) {
            String status = printApp.status(args[1]);
            System.out.println("Status: " + status);
        } else {
            System.out.println("Usage: status <printer>");
        }
    }

    private static void handleRestart() throws RemoteException {
        handleStop();
        handleStart();
    }

    private static void handleReadConfig(String[] args) throws RemoteException {
        if (args.length == 2) {
            String config = printApp.readConfig(args[1]);
            System.out.println("Config value: " + config);
        } else {
            System.out.println("Usage: readConfig <parameter>");
        }
    }

    private static void handleSetConfig(String[] args) throws RemoteException {
        if (args.length == 3) {
            printApp.setConfig(args[1], args[2]);
            System.out.println("Config set successfully.");
        } else {
            System.out.println("Usage: setConfig <parameter> <value>");
        }
    }

    private static void handleExit() {
        System.out.println("Exiting...");
        System.exit(0);
    }

    private static void handlePrinters() throws RemoteException {
        System.out.println(printApp.displayPrinters());
    }
}
