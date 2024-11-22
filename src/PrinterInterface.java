import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Queue;

public interface PrinterInterface extends Remote {

    public void print(String filename, String printer, SessionToken sessionToken) throws RemoteException, PrintAppException;
    public Queue<PrintJob> queue(String printer, SessionToken sessionToken) throws RemoteException, PrintAppException;
    public void TopQueue(String printer, int job, SessionToken sessionToken) throws RemoteException, PrintAppException;
    public Response<PrinterInterface> start(String username, String password, SessionToken sessionToken, boolean restart) throws PrintAppException, RemoteException;
    public Response<Void> stop(SessionToken sessionToken, Boolean restart) throws PrintAppException, RemoteException;
    public String status(String printer, SessionToken sessionToken) throws RemoteException, PrintAppException;
    public String readConfig(String parameter, SessionToken sessionToken) throws RemoteException, PrintAppException;
    public void setConfig(String parameter, String value, SessionToken sessionToken) throws RemoteException, PrintAppException;
    public String displayPrinters(SessionToken sessionToken) throws  RemoteException, PrintAppException;
    public void accessControl(String methodName, SessionToken sessionToken) throws Exception;
    public Response<PrinterInterface> login(String username, String password) throws RemoteException, PrintAppException;
    SessionToken authenticateUser(String username, String password, SessionToken sessionToken, String action) throws PrintAppException, RemoteException;

    public boolean isAllowedToStartServer(String username) throws RemoteException;
   public Response<PrinterInterface> connect(String username, String password, SessionToken sessionToken) throws RemoteException, PrintAppException;
}