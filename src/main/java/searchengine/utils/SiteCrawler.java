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
import searchengine.repositories.SitesRepository;
import searchengine.services.PagesService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

@Slf4j
@Getter
@Setter
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class SiteCrawler extends RecursiveAction {

    private Site site;
    private String path;
    private List<String> visited;
    private final HtmlParser parser;
    private final ApplicationContext context;
    private final SitesRepository sitesRepository;
    private final PagesService pagesService;
    private final IndexingTasksManager tasksManager;

    @Override
    protected void compute() {
        String url = site.getUrl() + path;
        ResponseEntity<String> response;
        try {
            response = parser.getResponse(url);
        } catch (Exception e) {
            log.error(e.getMessage());
            return;
        }

        storePage(response);

        List<String> newPaths = extractNewPaths(response.getBody());

        for (String path : newPaths) {
            log.info(site.getName() + " " + path);
            SiteCrawler crawler = context.getBean(SiteCrawler.class);
            crawler.setSite(site);
            crawler.setPath(path);
            crawler.setVisited(visited);
            tasksManager.queueTask(crawler);
        }

        log.info(url + " saved");
    }

    private List<String> extractNewPaths(String body) {
        List<String> links = parser.getLinks(body);

        List<String> newPaths = new ArrayList<>();

        for (String link : links) {
            String path = convertToPath(link);

            synchronized (visited) {
                if (path != null && !visited.contains(path)) {
                    newPaths.add(path);
                    visited.add(path);
                }
            }
        }

        return newPaths;
    }

    private void storePage(ResponseEntity<String> response) {
        Page page = new Page();

        page.setSite(site);
        page.setPath(path);
        page.setCode(response.getStatusCodeValue());
        page.setContent(response.getBody());

        pagesService.batch(page);
    }

    private String convertToPath(String link) {
        String siteUrl = site.getUrl();
        String urlRegex = "(" + siteUrl + ")[\\w-/.]+$";
        String pathRegex = "^/[\\w-]+[\\w-/]*(/|(\\.html))?$";

        String path;
        if (link.matches(urlRegex)) {
            path = link.substring(siteUrl.length() - 1);
        } else if (link.matches(pathRegex)) {
            path = link;
        } else {
            return null;
        }
        return path;
    }
}
