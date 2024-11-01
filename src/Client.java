import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Queue;
import java.util.Scanner;

public class Client {
    private static PrintRMI printRMI;

    public static void main(String[] args) {
        try {
            // Connect to the RMI Registry
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            
            PrinterInterface printRMI = (PrinterInterface) registry.lookup("PrinterServer");
            System.out.println("Connected to the Print Server.");

            // Scanner for user input
            Scanner scanner = new Scanner(System.in);
            String command;

            System.out.println("Available commands:");
            System.out.println("1. print <filename> <printer>");
            System.out.println("2. queue <printer>");
            System.out.println("3. top <printer> <job>");
            System.out.println("4. start <printer>");
            System.out.println("5. stop <printer>");
            System.out.println("6. status <printer>");
            System.out.println("7. restart");
            System.out.println("8. exit");

            while (true) {
                System.out.print("> ");
                command = scanner.nextLine();

                // Process commands
                if (command.startsWith("print")) {
                    String[] parts = command.split(" ");
                    if (parts.length == 3) {
                        
                        printRMI.print(parts[1], parts[2]);
                    } else {
                        System.out.println("Usage: print <filename> <printer>");
                    }
                } else if (command.startsWith("queue")) {
                    String[] parts = command.split(" ");
                    if (parts.length == 2) {
                        Queue<PrintJob> queue = printRMI.queue(parts[1]);
                        try{
                            System.out.println("Queue for " + parts[1] + ": " + queue.toString());
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Usage: queue <printer>");
                    }
                } else if (command.startsWith("top")) {
                    String[] parts = command.split(" ");
                    if (parts.length == 3) {
                        int job = Integer.parseInt(parts[2]);
                        printRMI.TopQueue(parts[1], job);
                    } else {
                        System.out.println("Usage: top <printer> <job>");
                    }
                } else if (command.startsWith("start")) {
                    String[] parts = command.split(" ");
                    if (parts.length == 2) {
                        printRMI.start();
                        System.out.println(parts[1] + " started.");
                    } else {
                        System.out.println("Usage: start <printer>");
                    }
                } else if (command.startsWith("stop")) {
                    String[] parts = command.split(" ");
                    if (parts.length == 2) {
                        printRMI.stop();
                        System.out.println(parts[1] + " stopped.");
                    } else {
                        System.out.println("Usage: stop <printer>");
                    }
                } else if (command.startsWith("status")) {
                    String[] parts = command.split(" ");
                    if (parts.length == 2) {
                        String status = printRMI.status(parts[1]);
                        System.out.println(status);
                    } else {
                        System.out.println("Usage: status <printer>");
                    }
                } else if (command.equals("restart")) {
                    printRMI.restart();
                    System.out.println("Print server restarted.");
                } else if (command.equals("exit")) {
                    System.out.println("Exiting...");
                    break;
                } else {
                    System.out.println("Unknown command. Please try again.");
                }
            }

            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
