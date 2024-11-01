import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Queue;


public class PrintRMI extends UnicastRemoteObject implements PrinterInterface {

    Queue<Printer> printQueue;
    Queue<String> receivedMessages;

    public PrintRMI() throws RemoteException{
        printQueue = new LinkedList<>();
        receivedMessages = new LinkedList<>();
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
    @Override
    public void readConfig(String parameter) throws RemoteException{

    }
    @Override
    public void setConfig(String parameter, String value) throws RemoteException{

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