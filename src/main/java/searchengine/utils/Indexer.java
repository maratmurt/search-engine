package searchengine.utils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import searchengine.dto.indexing.PageData;
import searchengine.dto.indexing.SiteData;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

@Slf4j
@Setter
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class Indexer extends RecursiveAction {
    private SiteData siteData;
    private String sourcePath;
    private final ApplicationContext context;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final TaskManager taskManager;
    private final LemmaCollector lemmaCollector;
    private final HtmlScraper htmlScraper;

    @Override
    protected void compute() {
        SiteEntity site = siteRepository.findById(siteData.getSiteId()).orElseThrow();
        String url = site.getUrl() + sourcePath.substring(1);
        PageData pageData;
        try {
            pageData = fetchData(url);
        } catch (Exception e) {
            log.error(e.getMessage());
            if (e instanceof HttpStatusException) {
                int statusCode = ((org.jsoup.HttpStatusException) e).getStatusCode();
                PageEntity page = new PageEntity();
                page.setCode(statusCode);
                page.setContent(e.getMessage());
                page.setSite(site);
                page.setPath(sourcePath);
                pageRepository.save(page);
            }
            return;
        }

        PageEntity page = savePage(pageData);

        saveLemmasAndIndices(pageData.getText(), page);

        Set<String> links = pageData.getLinks();
        Set<String> sitePaths = siteData.getPaths();
        for (String link : links) {
            String path = convertLinkToPath(link);
            if (path == null || sitePaths.contains(path)) {
                continue;
            }
            sitePaths.add(path);
            Indexer task = context.getBean(Indexer.class);
            task.setSiteData(siteData);
            task.setSourcePath(path);
            taskManager.addTask(task, site.getId());
        }
        log.info(url + " DONE");
    }

    public PageEntity savePage(PageData data) {
        SiteEntity site = siteRepository.findById(siteData.getSiteId()).orElseThrow();
        PageEntity page = new PageEntity();
        page.setSite(site);
        page.setPath(sourcePath);
        page.setCode(data.getStatusCode());
        page.setContent(data.getBody());
        page = pageRepository.save(page);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        return page;
    }

    public PageData fetchData(String url) throws IOException, InterruptedException {
        ResponseEntity<String> response = htmlScraper.getResponse(url);
        PageData data = new PageData();
        data.setStatusCode(response.getStatusCodeValue());
        String body = response.getBody();
        data.setBody(body);
        data.setText(htmlScraper.getText(body));
        data.setLinks(htmlScraper.getLinks(body));
        return data;
    }

    private String convertLinkToPath(String link) {
        SiteEntity site = siteRepository.findById(siteData.getSiteId()).orElseThrow();
        String siteUrl = site.getUrl();
        String urlRegex = "(" + siteUrl + ")[\\w-/]+$";
        String pathRegex = "^/[\\w-]+[\\w-/]*/?$";

        String path;
        if (link.matches(urlRegex)) {
            path = link.substring(siteUrl.length() - 1);
        } else if (link.matches(pathRegex)) {
            path = link;
        } else {
            return null;
        }
        path += link.endsWith("/") ? "" : "/";

        return path;
    }

    public void saveLemmasAndIndices(String text, PageEntity page) {
        Map<String, Double> lemmaRanks = lemmaCollector.mapLemmasAndRanks(text);

        for (Map.Entry<String, Double> entry : lemmaRanks.entrySet()) {
            String word = entry.getKey();
            double rank = entry.getValue();

            LemmaEntity lemma;
            try {
                lemma = createOrUpdateLemma(word);
            } catch (NoSuchElementException e) {
                log.error(word + ": " + e.getMessage());
                continue;
            }
            IndexEntity index = new IndexEntity();
            index.setLemma(lemma);
            index.setPage(page);
            index.setRank(rank);
            indexRepository.save(index);
        }
        log.info("SAVED " + lemmaRanks.size() + " indices from " + page.getPath());
    }

    private LemmaEntity createOrUpdateLemma(String lemmaWord) {
        SiteEntity site = siteRepository.findById(siteData.getSiteId()).orElseThrow();
        Set<String> lemmas = siteData.getLemmas();
        int freq = 1;
        LemmaEntity lemma;
        if (lemmas.contains(lemmaWord)) {
            //TODO update lemmas properly
            lemma = lemmaRepository.findByLemmaAndSite_Id(lemmaWord, site.getId()).orElseThrow();
            freq += lemma.getFrequency();
        } else {
            lemmas.add(lemmaWord);
            lemma = new LemmaEntity();
            lemma.setLemma(lemmaWord);
            lemma.setSite(site);
        }
        lemma.setFrequency(freq);
        return lemmaRepository.saveAndFlush(lemma);
    }
}
