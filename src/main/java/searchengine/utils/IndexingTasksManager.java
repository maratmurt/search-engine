package searchengine.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinTask;

@Getter
@Setter
@Component
public class IndexingTasksManager implements Runnable {
    private boolean running = false;
    private List<SiteCrawler> tasks = new CopyOnWriteArrayList<>();

    @Override
    public void run() {
        running = true;

        tasks.forEach(ForkJoinTask::fork);
        tasks.forEach(ForkJoinTask::join);
        tasks.clear();

        running = false;
    }

    public void cancelAllTasks() {
        running = false;

        // TODO tasks cancellation

        tasks.clear();
    }

    public void addTask(SiteCrawler task) {
        tasks.add(task);
    }
}
