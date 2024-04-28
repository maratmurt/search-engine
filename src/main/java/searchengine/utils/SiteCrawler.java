package searchengine.utils;

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
import searchengine.repositories.PagesRepository;
import searchengine.repositories.SitesRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Slf4j
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
    private final PagesRepository pagesRepository;
    private final SitesRepository sitesRepository;

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

        Page page = new Page();

        page.setSite(site);
        page.setPath(path);
        page.setCode(response.getStatusCodeValue());
        page.setContent(response.getBody());

        pagesRepository.save(page);

        List<String> links = parser.getLinks(response.getBody());

        List<String> newPaths = new ArrayList<>();

        for (String link : links) {
            String path = convertToPath(link);
            if (path != null && !visited.contains(path)) {
                newPaths.add(path);
                visited.add(path);
            }
        }

        List<ForkJoinTask<Void>> tasks = new ArrayList<>();

        for (String path : newPaths) {
            log.info(site.getName() + " " + path);
            SiteCrawler crawler = context.getBean(SiteCrawler.class);
            crawler.setSite(site);
            crawler.setPath(path);
            crawler.setVisited(visited);
            crawler.fork();
            tasks.add(crawler);
        }

        for (ForkJoinTask<Void> task : tasks) {
            try {
                task.join();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        if (path.equals("/")) {
            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            sitesRepository.save(site);
            log.info(site.getName() + " INDEXED");
        }
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
