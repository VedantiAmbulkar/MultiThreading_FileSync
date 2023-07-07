import java.io.File;
import java.util.ArrayList;

public class Utils {
    public static ArrayList<String> getFileList(String folderPath) {
        ArrayList<String> fileList = new ArrayList<>();
        File folder = new File(folderPath);

        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    fileList.add(file.getName());
                }
            }
        }
        return fileList;
    }

    public static void showProgress(int current, int total) {
        int progressBarWidth = 50; // Width of the progress bar

        // Calculate the percentage of completion
        int percent = (int) ((double) current / total * 100);

        // Calculate the number of progress bar characters to display
        int progressChars = (int) ((double) current / total * progressBarWidth);

        // Create the progress bar string
        StringBuilder progressBar = new StringBuilder();
        progressBar.append("[");
        for (int i = 0; i < progressBarWidth; i++) {
            if (i < progressChars) {
                progressBar.append("=");
            } else {
                progressBar.append(" ");
            }
        }
        progressBar.append("]");

        // Print the progress bar and percentage
        System.out.printf("\rProgress: %3d%% %s", percent, progressBar.toString());

        // If the current progress is equal to the total, print a newline character
        if (current == total) {
            System.out.println();
        }
    }

}
