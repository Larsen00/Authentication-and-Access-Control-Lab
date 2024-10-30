import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Queue;

public Interface Printer extends Remote {

    public void print(String filename) throws RemoteException;
    public Queue<String> queue() throws RemoteException;
    public void TopQueue(int job) throws RemoteException;
    public void start() throws RemoteException;
    public void stop() throws RemoteException;
    public void restart() throws RemoteException;
    public String status() throws RemoteException;
    public String readConfig(String parameter) throws RemoteException;
    public void setConfig(String parameter, String value) throws RemoteException;


}