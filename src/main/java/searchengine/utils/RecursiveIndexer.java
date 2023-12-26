package searchengine.utils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.dto.indexing.IndexData;
import searchengine.dto.indexing.LemmaData;
import searchengine.dto.indexing.PageData;
import searchengine.dto.indexing.SiteData;
import searchengine.model.Status;
import searchengine.services.IndexCrudService;
import searchengine.services.LemmaCrudService;
import searchengine.services.PageCrudService;
import searchengine.services.SiteCrudService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

@Slf4j
@Setter
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class RecursiveIndexer extends RecursiveAction {
    private int siteId;
    private String sourcePath;
    private Set<String> paths;
    private final ApplicationContext context;
    private final SiteCrudService siteService;
    private final PageCrudService pageService;
    private final LemmaCrudService lemmaService;
    private final IndexCrudService indexService;

    @Override
    protected void compute() {
        SiteData site = siteService.getById(siteId);
        String url = site.getUrl() + sourcePath.substring(1);

        HtmlScraper scraper = new HtmlScraper();
        try {
            scraper.initialize(url);
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            return;
        }
        PageData page = new PageData();
        page.setSiteId(siteId);
        page.setPath(sourcePath);
        page.setCode(scraper.getStatusCode());
        page.setContent(scraper.getHtml());
        page = pageService.create(page);

        try {
            saveLemmasAndIndices(scraper.getText(), page.getId());
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        List<RecursiveIndexer> taskList = new ArrayList<>();
        List<String> links = scraper.getLinks();
        for (String link : links) {
            String path = convertToPath(link);
            if (paths.contains(path) || path == null) {
                continue;
            }
            paths.add(path);

            RecursiveIndexer task = context.getBean(RecursiveIndexer.class);
            task.setSiteId(siteId);
            task.setSourcePath(path);
            task.setPaths(paths);
            task.fork();
            taskList.add(task);
        }
        taskList.forEach(ForkJoinTask::join);

        log.info(url + " DONE");

        if (sourcePath.equals("/")) {
            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteService.update(site);
        }
    }

    private String convertToPath(String link) {
        String siteUrl = siteService.getById(siteId).getUrl();
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

    private void saveLemmasAndIndices(String text, int pageId) throws IOException {
        List<String> lemmaWords = LemmaCollector.extractLemmas(text);
        HashSet<String> uniqueWords = new HashSet<>(lemmaWords);
        Map<String, Integer> lemmaRanks = uniqueWords.stream().collect(Collectors.toMap(word -> word, rank -> 0));
        for (String lemmaWord : lemmaWords) {
            int rank = lemmaRanks.get(lemmaWord);
            lemmaRanks.put(lemmaWord, rank + 1);
        }

        for (Map.Entry<String, Integer> entry : lemmaRanks.entrySet()) {
            String lemmaKey = entry.getKey();
            double rank = entry.getValue();
            LemmaData lemma = updateOrCreateLemma(lemmaKey);
            IndexData index = new IndexData();
            index.setLemmaId(lemma.getId());
            index.setPageId(pageId);
            index.setRank(rank);
            index = indexService.create(index);
        }
    }

    private LemmaData updateOrCreateLemma(String lemmaWord) {
        LemmaData lemma;
        try {
            lemma = lemmaService.getByWordAndSiteId(lemmaWord, siteId);
            lemma.setFrequency(lemma.getFrequency() + 1);
        } catch (NullPointerException e) {
            lemma = new LemmaData();
            lemma.setLemma(lemmaWord);
            lemma.setSiteId(siteId);
            lemma.setFrequency(1);
            lemma = lemmaService.create(lemma);
        }
        return lemma;
    }
}

