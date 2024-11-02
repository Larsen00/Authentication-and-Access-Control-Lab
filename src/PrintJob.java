import java.io.Serializable;

public class PrintJob implements Serializable {
    private static final long serialVersionUID = 1L; // Versioning for serialization
    private String filename;
    private int jobNumber;

    public PrintJob(String filename, int jobNumber) {
        this.filename = filename;
        this.jobNumber = jobNumber;
    }

    public String getFilename() {
        return filename;
    }

    public int getJobNumber() {
        return jobNumber;
    }

    @Override
    public String toString() {
        return "PrintJob{" +
                "filename='" + filename + '\'' +
                ", jobNumber=" + jobNumber +
                '}';
    }
}
