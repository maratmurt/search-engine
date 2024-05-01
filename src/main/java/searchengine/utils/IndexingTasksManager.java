package searchengine.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

@Slf4j
@Getter
@Setter
@Component
public class IndexingTasksManager implements Runnable {
    private boolean running = false;
    private List<ForkJoinTask<Void>> tasks = new ArrayList<>();
    private ForkJoinPool pool;

    @Value("${indexing-settings.parallelism}")
    private int parallelism;

    @Override
    public void run() {
        running = true;

        long start = System.currentTimeMillis();

        while (!tasks.isEmpty()) {
            tasks.removeIf(ForkJoinTask::isDone);
        }

        pool.shutdown();

        Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
        int hours = duration.toHoursPart();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();

        log.info("FINISHED in " + hours + ":" + minutes + ":" + seconds);

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
        pool = new ForkJoinPool(parallelism);
    }
}
