package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.SiteEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageCollector extends RecursiveAction {
    private final SiteEntity site;
    private final String srcPath;
    private final RepositoryService repositoryService;

    public PageCollector(SiteEntity site, String srcPath, RepositoryService repositoryService) {
        this.site = site;
        this.srcPath = srcPath;
        this.repositoryService = repositoryService;
    }

    @Override
    protected void compute() {
        Logger logger = Logger.getLogger("PGS-log");
        String rootUrl = site.getUrl();

        Connection.Response response;
        try {
            Thread.sleep(500);
            response = Jsoup.connect(rootUrl + srcPath)
                    .userAgent("MySearchEngine")
                    .referrer("http://www.google.com")
                    .execute();
        } catch (IOException | InterruptedException e) {
            site.setLastError(e.getMessage());
            repositoryService.save(site);
            throw new RuntimeException(e);
        }

        Page page = new Page();
        page.setSite(site);
        page.setPath(srcPath);
        page.setCode(response.statusCode());
        page.setContent(response.body());
        repositoryService.save(page);

        Document document;

        try {
            document = response.parse();

            Pattern urlPattern = Pattern.compile("(" + rootUrl + ")/[\\w\\-/]+$");

            Elements elements = document.getElementsByAttributeValueMatching("abs:href", urlPattern);

            List<PageCollector> taskList = new ArrayList<>();

            for (Element element : elements) {
                String path = element.attr("href");
                Matcher match = Pattern.compile(rootUrl).matcher(path);
                if (match.find()) {
                    path = path.substring(match.end() - 1);
                }

                if (!path.startsWith("/") || repositoryService.pageExists(site, path)) {
                    continue;
                }
                logger.info(path);

                PageCollector task = new PageCollector(site, path, repositoryService);
                task.fork();
                taskList.add(task);
            }

            taskList.forEach(ForkJoinTask::join);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
