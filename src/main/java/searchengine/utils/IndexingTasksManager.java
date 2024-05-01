package searchengine.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import searchengine.model.Site;
import searchengine.repositories.SitesRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Getter
@Setter
@Component
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "indexing-settings")
public class IndexingTasksManager implements Runnable{
    private volatile boolean running = false;
    private List<SiteTasksQueue> queueList = new ArrayList<>();
    private ForkJoinPool forkJoinPool;
    private ExecutorService fixedThreadPool;
    private int parallelism;
    private final SitesRepository sitesRepository;
    private final ApplicationContext context;

    @Override
    public void run() {
        running = true;

        long start = System.currentTimeMillis();

        List<Future<?>> tasks = new ArrayList<>();
        queueList.forEach(queue -> tasks.add(fixedThreadPool.submit(queue)));

        for (int i = 0; i < tasks.size(); i++) {
            try {
                tasks.get(i).get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        forkJoinPool.shutdown();
        fixedThreadPool.shutdown();

        if (running) {
            Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
            int hours = duration.toHoursPart();
            int minutes = duration.toMinutesPart();
            int seconds = duration.toSecondsPart();

            log.info("FINISHED in " + hours + ":" + minutes + ":" + seconds);

            running = false;

        } else {
            log.info("STOPPED");
        }
    }

    public void abort() {
        queueList.forEach(SiteTasksQueue::cancelAll);
        queueList.clear();



        forkJoinPool.shutdownNow();
        fixedThreadPool.shutdownNow();

        running = false;
    }

    public synchronized void queueTask(SiteCrawler crawler) {
        Site site = crawler.getSite();

        ForkJoinTask<Void> task = forkJoinPool.submit(crawler);

        SiteTasksQueue queue = queueList.stream().filter(q -> q.getSite() == site).findAny().orElseGet(()->{
            SiteTasksQueue newQueue = context.getBean(SiteTasksQueue.class);
            newQueue.setSite(site);
            queueList.add(newQueue);
            return newQueue;
        });

        queue.add(task);

        log.info(
                "Task " +
                crawler.getSite().getUrl() +
                crawler.getPath() +
                " submitted. Tasks count = " +
                queueList.stream()
                        .map(SiteTasksQueue::getTasks)
                        .map(List::size)
                        .reduce(Integer::sum).orElse(0)
        );
    }

    public void initialize(int threadsCount) {
        fixedThreadPool = Executors.newFixedThreadPool(threadsCount);
        forkJoinPool = new ForkJoinPool(parallelism);
    }
}
