import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Queue;

public interface PrinterInterface extends Remote {

    public void print(String filename, String printer) throws RemoteException;
    public Queue<PrintJob> queue(String printer) throws RemoteException;
    public void TopQueue(String printer, int job) throws RemoteException;
    public void start() throws RemoteException;
    public void stop() throws RemoteException;
    public void restart() throws RemoteException;
    public String status(String printer) throws RemoteException;
    public String readConfig(String parameter) throws RemoteException;
    public void setConfig(String parameter, String value) throws RemoteException;
    public String displayPrinters() throws  RemoteException;
    public void accessControl(String methodName, String userRole) throws Exception;
    public SessionToken login(String username, String password) throws PrintAppException;

}