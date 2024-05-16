package searchengine.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SitesRepository;
import searchengine.dao.PageDao;

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

    private Site site;
    private String sourcePath;
    private Set<String> visited;
    private final HtmlParser parser;
    private final ApplicationContext context;
    private final SitesRepository sitesRepository;
    private final PageDao pageDao;
    private final IndexingTasksManager tasksManager;

    @Override
    protected void compute() {
        String url = site.getUrl() + sourcePath;
        ResponseEntity<String> response;
        try {
            response = parser.getResponse(url);
        } catch (Exception e) {
            log.error(e.getMessage());
            if (sourcePath.equals("/")) {
                site.setStatus(Status.FAILED);
                site.setLastError("Главная страница не доступна");
                site.setStatusTime(LocalDateTime.now());
                site = sitesRepository.save(site);
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
            pageDao.flush();
            setFinalStatus();
        }
    }

    private void setFinalStatus() {
        if (tasksManager.isRunning()) {
            site.setStatus(Status.INDEXED);
            log.info(site.getName() + " INDEXED");
        } else {
            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена пользователем");
            log.info(site.getName() + " FAILED");
        }
        site.setStatusTime(LocalDateTime.now());
        sitesRepository.save(site);
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
        Page page = new Page();

        page.setSite(site);
        page.setPath(sourcePath);
        page.setCode(response.getStatusCodeValue());
        page.setContent(response.getBody());

        pageDao.batch(page);
    }

    private String convertToPath(String link) {
        String rootUrl = site.getUrl();
        String urlRegex = "^(" + rootUrl + ")[\\w-/]+(\\.html)?$";
        String pathRegex = "^/[\\w-]+[\\w-/]*(\\.html)?$";

        String path;
        if (link.matches(urlRegex)) {
            path = link.substring(rootUrl.length());
        } else if (link.matches(pathRegex)) {
            path = link;
        } else {
            return null;
        }
        return path;
    }
}
