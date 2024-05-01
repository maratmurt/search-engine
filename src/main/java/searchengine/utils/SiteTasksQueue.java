package searchengine.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SitesRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;

@Slf4j
@Getter
@Setter
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class SiteTasksQueue implements Runnable {
    private Site site;
    private List<ForkJoinTask<Void>> tasks = new ArrayList<>();
    private final SitesRepository sitesRepository;
    private final IndexingTasksManager tasksManager;

    @Override
    public void run() {
        for (int i = 0; i < tasks.size(); i++) {
            try {
                tasks.get(i).join();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        if (tasksManager.isRunning()) {
            site.setStatus(Status.INDEXED);
            log.info(site.getName() + " INDEXED");
        } else {
            site.setStatus(Status.FAILED);
            log.info(site.getName() + " FAILED");
        }
        site.setStatusTime(LocalDateTime.now());
        site = sitesRepository.save(site);
    }

    public synchronized void add(ForkJoinTask<Void> task) {
        tasks.add(task);
    }

    public void cancelAll() {
        tasks.forEach(task -> task.cancel(true));
        tasks.clear();
    }
}
