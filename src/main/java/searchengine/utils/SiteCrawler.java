package searchengine.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import searchengine.dao.SiteDao;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.Status;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Slf4j
@Getter
@Setter
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class SiteCrawler extends RecursiveAction {

    private SiteDto site;
    private String sourcePath;
    private Set<String> visited;
    private final HtmlParser parser;
    private final ApplicationContext context;
    private final SiteDao siteDao;
    private final IndexingTasksManager tasksManager;
    private final BatchProcessor batch;

    @Override
    protected void compute() {
        String url = site.getUrl() + sourcePath;
        ResponseEntity<String> response;
        try {
            response = parser.getResponse(url);
        } catch (Exception e) {
            log.error(e.getMessage());
            if (sourcePath.equals("/")) {
                site.setStatus(Status.FAILED.toString());
                site.setLastError("Главная страница не доступна");
                site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
                site = siteDao.save(site);
            }
            return;
        }

        storePage(response);

        List<String> newPaths = extractNewPaths(response.getBody());

        List<ForkJoinTask<Void>> tasks = new ArrayList<>();

        for (String path : newPaths) {
            log.info(site.getName() + " " + path);
            SiteCrawler crawler = context.getBean(SiteCrawler.class);
            crawler.setSite(site);
            crawler.setSourcePath(path);
            crawler.setVisited(visited);
            ForkJoinTask<Void> task = tasksManager.submitTask(crawler);
            tasks.add(task);
        }

        for (ForkJoinTask<Void> task : tasks) {
            try {
                task.join();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        if (sourcePath.equals("/")) {
            batch.flush();
            setFinalStatus();
        }
    }

    private void setFinalStatus() {
        if (tasksManager.isRunning()) {
            site.setStatus(Status.INDEXED.toString());
            log.info(site.getName() + " INDEXED");
        } else {
            site.setStatus(Status.FAILED.toString());
            site.setLastError("Индексация остановлена пользователем");
            log.info(site.getName() + " FAILED");
        }
        site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
        siteDao.save(site);
    }

    private List<String> extractNewPaths(String body) {
        List<String> links = parser.getLinks(body);

        List<String> newPaths = new ArrayList<>();

        for (String link : links) {
            String path = convertToPath(link);

            synchronized (visited) {
                if (path != null && visited.add(path))
                    newPaths.add(path);
            }
        }

        return newPaths;
    }

    private void storePage(ResponseEntity<String> response) {
        PageDto page = new PageDto();

        page.setSiteId(site.getId());
        page.setPath(sourcePath);
        page.setCode(response.getStatusCodeValue());
        page.setContent(response.getBody());

        batch.add(page);
    }

    private String convertToPath(String link) {
        String rootUrl = site.getUrl();
        String urlRegex = "^(" + rootUrl + ")[\\w-/]+(\\.html)?$";
        String pathRegex = "^/[\\w-]+[\\w-/]*(\\.html)?$";

        String path;
        if (link.matches(urlRegex)) {
            path = link.substring(rootUrl.length()).toLowerCase();
        } else if (link.matches(pathRegex)) {
            path = link.toLowerCase();
        } else {
            return null;
        }
        return path;
    }
}
