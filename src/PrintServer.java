import java.rmi.RemoteException;

public class PrintServer implements PrinterInterface {

  
    public PrintServer(){
       
    }


    @Override
     public void print(String filename, String printer) throws RemoteException {
            
     }
    @Override
    public String queue(String printer) throws RemoteException{
        
        return null;
    }
    @Override
    public void TopQueue(String printer, int job) throws RemoteException{
      

    }
    @Override
    public void start() throws RemoteException{

    }
    @Override
    public void stop() throws RemoteException{

    }
    @Override
    public void restart() throws RemoteException{

    }
    @Override
    public String status(String printer) throws RemoteException{

        return null;

    }
    @Override
    public String readConfig(String parameter) throws RemoteException{

        return null;

    }
    @Override
    public void setConfig(String parameter, String value) throws RemoteException{

    }

    
}