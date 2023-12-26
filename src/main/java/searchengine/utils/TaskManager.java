package searchengine.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

@Slf4j
@Component
@Setter @Getter
@RequiredArgsConstructor
public class TaskManager implements Runnable {

//    private volatile boolean indexing;
    private volatile Map<Integer, List<ForkJoinTask<Void>>> taskMap = new ConcurrentHashMap<>();
    private ForkJoinPool pool;
    private final SiteRepository siteRepository;

    @Override
    public void run() {
        log.info("STARTED");
        while (!taskMap.isEmpty()) {
            List<Integer> siteIds = taskMap.keySet().stream().toList();
            for (int i = 0; i < siteIds.size(); i++) {
                int siteId = siteIds.get(i);
                List<ForkJoinTask<Void>> siteTasks = taskMap.get(siteId);
                for (int j = 0; j < siteTasks.size(); j++) {
                    ForkJoinTask<Void> task = siteTasks.get(j);
                    if (task.isDone()) {
                        siteTasks.remove(task);
                        log.info("Removed one from " + siteId + ". " + siteTasks.size() + " tasks left.");
                    }
                }
                if (siteTasks.isEmpty()) {
                    setFinalStatus(siteId);
                    taskMap.remove(siteId);
                }
            }
        }
        pool.shutdown();
        log.info("FINISHED");
    }

    public void initialize() {
        pool = new ForkJoinPool(8);
    }

    public void addTask(ForkJoinTask<Void> task, int siteId) {
        if (!pool.isShutdown()) {
            List<ForkJoinTask<Void>> siteTasks;
            if (taskMap.containsKey(siteId)) {
                siteTasks = taskMap.get(siteId);
            } else {
                siteTasks = new ArrayList<>();
                taskMap.put(siteId, siteTasks);
            }
            siteTasks.add(pool.submit(task));
            log.info("Added a task to " + siteId + ". Tasks count = " + siteTasks.size());
        }
    }

    public void stopProcess() {
        pool.shutdownNow();
        List<Integer> siteIds = taskMap.keySet().stream().toList();
        for (int i = 0; i < siteIds.size(); i++) {
            int siteId = siteIds.get(i);
            List<ForkJoinTask<Void>> siteTasks = taskMap.get(siteId);
            for (int j = 0; j < siteTasks.size(); j++) {
                siteTasks.get(j).cancel(true);
            }
        }
    }

    private void setFinalStatus(int siteId) {
        SiteEntity site = siteRepository.findById(siteId).orElseThrow();
        if (pool.isShutdown()) {
            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена пользователем");
        } else {
            site.setStatus(Status.INDEXED);
        }
        site.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(site);
    }

    public boolean isIndexing() {
        if (pool == null) {
            return false;
        } else {
            return !pool.isShutdown();
        }
    }
}
