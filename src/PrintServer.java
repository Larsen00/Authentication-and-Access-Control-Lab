
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Scanner;


public class PrintServer{

    public PrintServer() {
        PrintApplication printApp;
        try {
            printApp = new PrintApplication();
        } catch (RemoteException e){
            System.out.println("Could not create Printer application");
            return;
        }

        for (Printer p : dummyPrinters(printApp)){
            printApp.registerPrinter(p);
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
    public static ArrayList<Printer> dummyPrinters(PrintApplication server) {
        ArrayList<Printer> printers = new ArrayList<>();

        // Create four dummy printers with unique names
        printers.add(new Printer("Printer1", server));
        printers.add(new Printer("Printer2", server));
        printers.add(new Printer("Printer3", server));
        printers.add(new Printer("Printer4", server));

        return printers;
    }

}