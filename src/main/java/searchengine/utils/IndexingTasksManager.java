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

    @Value("${indexing-settings.thread_multiplier}")
    private int threadMultiplier;

    @Override
    public void run() {
        long start = System.currentTimeMillis();

        while (!tasks.isEmpty() && running)
            checkAndRemoveCompletedTasks();

        pool.shutdown();

        Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
        int hours = duration.toHoursPart();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();

        log.info("FINISHED in {}:{}:{}", hours, minutes, seconds);

        running = false;
    }

    public void abort() {
        running = false;

        tasks.forEach(task -> task.cancel(true));
        tasks.clear();

        pool.shutdownNow();
    }

    public synchronized ForkJoinTask<Void> submitTask(SiteCrawler crawler) {
        ForkJoinTask<Void> task = pool.submit(crawler);
        tasks.add(task);
        log.info("Task {}{} submitted. Tasks count = {}", crawler.getSite().getUrl(), crawler.getSourcePath(), tasks.size());
        return task;
    }

    public void initialize() {
        running = true;
        int parallelism = Runtime.getRuntime().availableProcessors() * threadMultiplier;
        log.info("ForkJoinPool parallelism = {}", parallelism);
        pool = new ForkJoinPool(parallelism);
    }

    private void checkAndRemoveCompletedTasks() {
        for (int i = 0; i < tasks.size(); i++) {
            ForkJoinTask<Void> task = tasks.get(i);
            if (task != null && task.isDone()) {
                tasks.remove(task);
                log.info("Removed completed task. Tasks count = {}", tasks.size());
            }
        }
    }
}
