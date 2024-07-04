package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dto.ApiResponse;
import searchengine.dto.ErrorResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.Status;
import searchengine.utils.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{

    private final SitesList sitesList;
    private final SiteDao siteDao;
    private final ApplicationContext context;
    private final IndexingTasksManager tasksManager;
    private final HtmlParser parser;
    private final PageDao pageDao;
    private final BatchProcessor batch;

    @Override
    public ApiResponse startIndexing() {
        if (tasksManager.isRunning())
            return new ErrorResponse("Индексация уже запущена");

        tasksManager.initialize();
        batch.setPagesOffset(0);

        for (SiteConfig siteConfig : sitesList.getSites()) {
            siteDao.findByUrl(siteConfig.getUrl()).ifPresent(siteDao::delete);

            SiteDto site = new SiteDto();

            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl());
            site.setStatus(Status.INDEXING.toString());
            site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
            site = siteDao.save(site);

            SiteCrawler crawler = context.getBean(SiteCrawler.class);
            String path = "/";
            Set<String> visited = new HashSet<>();
            visited.add(path);
            crawler.setSourcePath(path);
            crawler.setVisited(visited);
            crawler.setSite(site);
            tasksManager.submitTask(crawler);
        }
        new Thread(tasksManager).start();

        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }

    @Override
    public ApiResponse stopIndexing() {
        if (!tasksManager.isRunning())
            return new ErrorResponse("Индексация не запущена");

        tasksManager.abort();

        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }

    @Override
    public ApiResponse indexPage(String url) {
        url = URLDecoder.decode(url, StandardCharsets.UTF_8);

        Matcher rootMatch = Pattern.compile("http(s?)://[\\w-.]+").matcher(url);
        String rootUrl;
        if (rootMatch.find())
            rootUrl = rootMatch.group();
        else
            return new ErrorResponse("Введён некорректный адрес страницы");

        SiteConfig matchSiteConfig = findMatchingConfig(url);
        if (matchSiteConfig == null)
            return new ErrorResponse("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");

        SiteDto site = findOrCreateSite(matchSiteConfig.getName(), matchSiteConfig.getUrl());

        int siteId = site.getId();
        String path = url.substring(rootUrl.length());
        pageDao.findBySiteIdAndPath(siteId, path).ifPresent(pageDao::delete);

        ResponseEntity<String> pageResponse;
        try {
            pageResponse = parser.getResponse(url);
        } catch (Exception e) {
            return new ErrorResponse(e.getLocalizedMessage());
        }
        PageDto page = new PageDto();
        page.setSiteId(siteId);
        page.setPath(path);
        page.setCode(pageResponse.getStatusCodeValue());
        page.setContent(pageResponse.getBody());
        page = pageDao.save(page);

        IndexProcessor indexProcessor = context.getBean(IndexProcessor.class);
        indexProcessor.setPages(List.of(page));
        indexProcessor.start();

        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }

    private SiteConfig findMatchingConfig(String url) {
        SiteConfig matchSiteConfig = null;
        for (SiteConfig siteConfig : sitesList.getSites()) {
            if (url.contains(siteConfig.getUrl())) {
                matchSiteConfig = siteConfig;
                break;
            }
        }
        return matchSiteConfig;
    }

    private SiteDto findOrCreateSite(String name, String url) {
        SiteDto site;
        Optional<SiteDto> existingSite = siteDao.findByUrl(url);
        if (existingSite.isPresent()) {
            site = existingSite.get();
        } else {
            site = new SiteDto();
            site.setUrl(url);
            site.setName(name);
            site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
            site = siteDao.save(site);
        }
        return site;
    }
}
