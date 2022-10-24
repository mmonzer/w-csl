package com.csl.web.database;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.json.Json;

public class DirWatcher implements Runnable {

    private final Path dir;
    private final WatchService watcher;
    private final WatchKey key;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    public DirWatcher(Path dir) throws IOException {
        this.dir = dir;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.key = dir.register(watcher,
        		
        		 StandardWatchEventKinds.ENTRY_CREATE, 
                 StandardWatchEventKinds.ENTRY_DELETE, 
                 StandardWatchEventKinds.ENTRY_MODIFY);
        
    }

    public void run() {
        try {
            for (;;) {
                // wait for key to be signalled
                WatchKey key = watcher.take();

                if (this.key != key) {
                    System.err.println("WatchKey not recognized!");
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent<Path> ev = cast(event);
                    Path p=dir.resolve(ev.context());
                    Path pfilename = p.getFileName();
                    String filename=pfilename.toFile().getName();
                    if (filename.indexOf(".") > 0) {
                        filename = filename.substring(0, filename.lastIndexOf("."));
                    }
                    
                    System.out.format("%s: %s\n", ev.kind(), dir.resolve(ev.context()));
                    // TODO: handle event. E.g. call listeners
                    
                    Json j=Json.object();
    		        j.set("action","modified");
    		        j.set("name",filename);
    		        
//    		        CSLWebSocketForDatabase.broadcastMessageJson("database",j );
    		        CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_DATABASE,j );
    		        
                }

                // reset key
                if (!key.reset()) {
                    break;
                }
            }
        } catch (InterruptedException x) {
            return;
        }
    }

    public static void startWatcher(String dirpath) throws IOException, InterruptedException, ExecutionException,
            TimeoutException {

        Path dir = Paths.get(dirpath);
        DirWatcher watcher = new DirWatcher(dir);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(watcher);
        executor.shutdown();

        // Now, the watcher runs in parallel
        // Do other stuff here

        // Shutdown after 10 seconds
        //executor.awaitTermination(10, TimeUnit.SECONDS);
        // abort watcher
        //future.cancel(true);

        //executor.awaitTermination(1, TimeUnit.SECONDS);
        //executor.shutdownNow();
    }
}