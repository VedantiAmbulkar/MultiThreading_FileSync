import java.nio.file.*;

public class FileWatcher {

    private String folderPath = "";
    private Client client = null;

    public FileWatcher(String folderPath, Client client) {
        this.folderPath = folderPath;
        this.client = client;
    }

    public void start() throws Exception {
        System.out.println("File Watcher service active for " + this.folderPath);
        WatchService watchService = FileSystems.getDefault().newWatchService();


        Path folder = Paths.get(this.folderPath);
        folder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

        while (true) {
            WatchKey key;

            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                Path fileName = (Path) event.context();

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    System.out.println("File created: " + fileName);
                    client.setTrigger("C");
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    if (fileName.getFileName().toString().equals(".DS_Store")) {
                        continue;
                    }
                    System.out.println("File modified: " + fileName);
                    client.setTrigger("M", fileName.toString());
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    System.out.println("File deleted: " + fileName);
                    client.setTrigger("D");
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }
}
