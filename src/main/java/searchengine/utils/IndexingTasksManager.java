package searchengine.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

@Slf4j
@Getter
@Setter
@Component
public class IndexingTasksManager implements Runnable {
    private boolean running = false;
    private List<ForkJoinTask<Void>> tasks = new CopyOnWriteArrayList<>();
    private ForkJoinPool pool;

    @Override
    public void run() {
        running = true;

        while (!tasks.isEmpty()) {
            tasks.removeIf(ForkJoinTask::isDone);
        }

        pool.shutdown();

        log.info("FINISHED");

        running = false;
    }

    public void cancelAllTasks() {
        running = false;

        tasks.forEach(task -> task.cancel(true));
        pool.shutdownNow();

        tasks.clear();
    }

    public synchronized ForkJoinTask<Void> addTask(SiteCrawler crawler) {
        ForkJoinTask<Void> task = pool.submit(crawler);
        tasks.add(task);
        log.info("Task " + crawler.getSite().getUrl() + crawler.getPath() + " submitted. Tasks count = " + tasks.size());
        return task;
    }

    public void initialize() {
        pool = new ForkJoinPool(8);
        log.info("Pool created");
    }
}
