



import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public final class Printer  implements Runnable{
    public String name;
    private  Queue<PrintJob> queue;
    public Status myStatus;

    @Override
    public void run() {
        processQueue();
    }

    enum Status{
        IDLE,
        PRINTING
    }

    public Printer(String name){
        this.name = name;
        this.myStatus = Status.IDLE;
        this.queue = new LinkedList<>();
        new Thread(this).start();
    }

    public String getName(){
        return this.name;
    }
    public void print(String filename){
        PrintJob newJob = new PrintJob(filename,queue.size()+1);
        this.queue.add(newJob);
        if (myStatus == Status.IDLE) {
            myStatus = Status.PRINTING;
        }
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

    public void processQueue() {
        String filepath = "prints/print.txt";
        new java.io.File("prints").mkdirs(); // Ensure directory exists

        while (true) {
            if (myStatus == Status.PRINTING) {
                try {
                    Thread.sleep(10000); // Simulate printing delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                PrintJob job = queue.poll();
                if (job != null) {
                    // Use try-with-resources to open, write, and close the file each time
                    try (FileWriter writer = new FileWriter(filepath, true)) { // Open in append mode
                        writer.write("Job id: " + job.getJobNumber() +
                                " Filename: " + job.getFilename() +
                                " Printed by: " + name + System.lineSeparator());
                        writer.flush(); // Ensure data is written to the file
                    } catch (IOException e) {
                        throw new RuntimeException("Error writing job to file", e);
                    }
                }
            }
            // Update status based on queue state
            myStatus = queue.isEmpty() ? Status.IDLE : Status.PRINTING;
        }
    }
}