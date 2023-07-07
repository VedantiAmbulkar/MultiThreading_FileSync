import java.nio.file.*;

public class FileWatcher {
    public static void start(String folderPath) throws Exception {
        System.out.println("File Watcher service started for " + folderPath);
        WatchService watchService = FileSystems.getDefault().newWatchService();

        Path folder = Paths.get(folderPath);
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
                    UserClient.setTrigger("C");
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    System.out.println("File modified: " + fileName);
                    UserClient.setTrigger("M");
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    System.out.println("File deleted: " + fileName);
                    UserClient.setTrigger("D");
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }
}
