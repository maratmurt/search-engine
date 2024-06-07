package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.ApiResponse;
import searchengine.dto.ErrorResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PagesRepository;
import searchengine.repositories.SitesRepository;
import searchengine.utils.HtmlParser;
import searchengine.utils.IndexingTasksManager;
import searchengine.utils.Lemmatizer;
import searchengine.utils.SiteCrawler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{

    private final SitesList sitesList;
    private final SitesRepository sitesRepository;
    private final ApplicationContext context;
    private final IndexingTasksManager tasksManager;
    private final HtmlParser parser;
    private final PagesRepository pagesRepository;
    private final Lemmatizer lemmatizer;

    @Override
    public ApiResponse startIndexing() {
        if (tasksManager.isRunning())
            return new ErrorResponse("Индексация уже запущена");

        tasksManager.initialize();

        for (SiteConfig siteConfig : sitesList.getSites()) {
            sitesRepository.findByUrl(siteConfig.getUrl()).ifPresent(sitesRepository::delete);

            Site site = new Site();

            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl());
            site.setStatus(Status.INDEXING);
            site.setStatusTime(LocalDateTime.now());

            site = sitesRepository.save(site);

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
        if (rootMatch.find()) {
            rootUrl = rootMatch.group();
        } else {
            return new ErrorResponse("Введён некорректный адрес страницы");
        }

        SiteConfig matchSiteConfig = null;
        for (SiteConfig siteConfig : sitesList.getSites()) {
            if (url.contains(siteConfig.getUrl())) {
                matchSiteConfig = siteConfig;
                break;
            }
        }
        if (matchSiteConfig == null) {
            return new ErrorResponse("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        Site site;
        Optional<Site> existingSite = sitesRepository.findByUrl(rootUrl);
        if (existingSite.isPresent()) {
            site = existingSite.get();
        } else {
            site = new Site();
            site.setUrl(matchSiteConfig.getUrl());
            site.setName(matchSiteConfig.getName());
            site.setStatusTime(LocalDateTime.now());
            site = sitesRepository.save(site);
        }

        String path = url.substring(rootUrl.length());
        pagesRepository.findBySiteAndPath(site, path).ifPresent(pagesRepository::delete);

        ResponseEntity<String> pageResponse;
        try {
            pageResponse = parser.getResponse(url);
        } catch (Exception e) {
            return new ErrorResponse(e.getLocalizedMessage());
        }
        Page page = new Page();
        page.setSite(site);
        page.setPath(path);
        page.setCode(pageResponse.getStatusCodeValue());
        page.setContent(pageResponse.getBody());
        page = pagesRepository.save(page);

        String text = parser.getText(page.getContent());
        Map<String, Integer> lemmaRankMap = lemmatizer.buildLemmaRankMap(text);

        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }
}
