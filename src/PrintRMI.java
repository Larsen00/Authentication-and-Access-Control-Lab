import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;


public class PrintRMI extends UnicastRemoteObject implements PrinterInterface {

    Queue<Printer> printQueue;
    Queue<String> receivedMessages;
    private Map<String, String> config;
    public PrintRMI() throws RemoteException{
        printQueue = new LinkedList<>();
        receivedMessages = new LinkedList<>();
        config = new HashMap<>();
    }

    @Override
    public void print(String filename, String printer) throws RemoteException {

            if (!printQueue.isEmpty()){
                for(Printer p :printQueue){
                    if(p.getName().equals(printer)){
                        p.print(filename);
                    }
             }
            }
            else{
                
                Printer p = new Printer(printer,this);
                p.print(filename);
             
                printQueue.add(p);
            }
        
            
     }
    @Override
    public Queue<PrintJob> queue(String printer) throws RemoteException{
        
        for (Printer job : printQueue) {
            if (job.getName().equals(printer)) {
                return job.queue();
            }
        }
        return null;

    }
    @Override
    public void TopQueue(String printer, int job) throws RemoteException{
        for (Printer p : printQueue) {
            if (p.getName().equals(printer)) {
                try {
                    p.topQueue(job);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }
    @Override
    public void setConfig(String parameter, String value) throws RemoteException{
         config.put(parameter, value);
    }

    @Override
    public String readConfig(String parameter) throws RemoteException{
         return config.get(parameter);
    }

    @Override
    public void start() throws RemoteException{
            printQueue.forEach(Printer::startPrint);
    }
    @Override
    public void stop() throws RemoteException{
        printQueue.forEach(Printer::stop);
    }
    @Override
    public void restart() throws RemoteException{
        printQueue.clear();
    }
    @Override
    public String status(String printer) throws RemoteException{
        for (Printer job : printQueue) {
            if (job.getName().equals(printer)) {
                return job.status();
            }
        }
        return "Printer: "+printer+" does not exist!";
    }
  

    public void receiveMessage(String message){
        receivedMessages.add(message);
        printMessages();
    }
    public void printMessages(){
        while(!receivedMessages.isEmpty()){
            System.err.println(receivedMessages.poll());
        }
    }
    
}