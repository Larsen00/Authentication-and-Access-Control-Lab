
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;



public class PrintServer{

    public static void main(String[] args) {
        
        try {

            PrintRMI prmi = new PrintRMI();
            Registry registry = LocateRegistry.createRegistry(1099);

            registry.bind("PrinterServer", prmi);
            System.out.println("Server is ready.");
            
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}