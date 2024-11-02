



import java.util.LinkedList;
import java.util.Queue;

public final class Printer  implements Runnable{
    public String name;
    private  Queue<PrintJob> queue;
    public Status myStatus;
    private PrintApplication printServer;
    enum Status{
        IDLE,
        PRINTING
    }

    public Printer(String name, PrintApplication server){
        this.name = name;
        this.myStatus = Status.IDLE;
        this.queue = new LinkedList<>();
        this.printServer=server;
        new Thread(this).start();
    }

    public String getName(){
        return this.name;
    }
    public void print(String filename){
        PrintJob newjob = new PrintJob(filename,queue.size()+1);
        this.queue.add(newjob);
    }
    public Queue<PrintJob> queue(){
        return this.queue;
    }
    public void topQueue(int job) throws Exception {
        PrintJob jobEntry = null;
        
        for (PrintJob entry : queue) {
            if (entry.getJobNumber() == job) {
                jobEntry = entry;
                break;
            }
        }
        
        if (jobEntry == null) {
            throw new Exception("Job: " + job + " is not in the queue");
        } else {
            
            queue.remove(jobEntry);
            ((LinkedList<PrintJob>) queue).addFirst(jobEntry);
        }
    }   
    public String status(){
        switch(myStatus) {
                
            case IDLE ->{ return "Printer: "+this.name+" is ready to print!";}
            
            case PRINTING -> {
                return "Printer: "+this.name+" is currently printing...";
            }
        }
        return null;
       
    }
    public void startPrint(){
        myStatus = Status.PRINTING;

    }

    public void stop(){
        myStatus = Status.IDLE;
    }

    @Override
    public void run(){
        processQueue();
    }

    public void sendMessageToServer(String message){
        try {
            printServer.receiveMessage(message);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processQueue() {
        while (true) {
            if (myStatus == Status.PRINTING) {
                
                PrintJob job = queue.poll();
                if (job != null) {
                    sendMessageToServer("Printing job: " + job.getFilename());
                }

               
                myStatus = queue.isEmpty() ? Status.IDLE : Status.PRINTING;
            }

            
            try {
                Thread.sleep(1000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

    }
   


}