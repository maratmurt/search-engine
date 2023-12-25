package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.indexing.IndexData;
import searchengine.dto.indexing.LemmaData;
import searchengine.dto.indexing.PageData;
import searchengine.dto.indexing.SiteData;
import searchengine.model.Status;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Slf4j
@Setter @Getter
public class RecursiveIndexer extends RecursiveAction {
    private int siteId;
    private String sourcePath;
    private HashSet<String> paths;
    private SiteCrudService siteService;
    private PageCrudService pageService;
    private LemmaCrudService lemmaService;
    private IndexCrudService indexService;

    @Override
    protected void compute() {
        SiteData site = siteService.getById(siteId);

        PageData page;
        try {
            page = getPage();
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            return;
        }

        Document document = Jsoup.parse(page.getContent());
        String[] words = document.text().split("[^А-Яа-я]+");
        List<String> legalWords = new ArrayList<>();
        for (String word : words) {
            if (word.length() > 1) {
                legalWords.add(word.toLowerCase());
            }
        }

        LuceneMorphology luceneMorph;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (String word : legalWords) {
            List<String> wordMorphInfo = luceneMorph.getMorphInfo(word);
            String[] functionalTypes = {"СОЮЗ", "ПРЕДЛ", "МЕЖД", "ЧАСТ"};

            boolean isFunctional = false;
            for (String type : functionalTypes) {
                if (wordMorphInfo.get(0).contains(type)) {
                    isFunctional = true;
                    break;
                }
            }
            if (isFunctional) {
                continue;
            }

            List<String> wordBaseForms = luceneMorph.getNormalForms(word);
            for (String baseForm : wordBaseForms) {
                saveLemma(baseForm, page.getId());
            }
        }

        Elements elements = document.getElementsByAttribute("href");

        List<RecursiveIndexer> taskList = new ArrayList<>();

        for (Element element : elements) {
            String href = element.attr("href");
            String path = normalizePath(href);
            if (paths.contains(path)) {
                continue;
            }
            paths.add(path);

//            log.info("NEW PATH: " + path);

            RecursiveIndexer task = new RecursiveIndexer();
            task.setSiteId(siteId);
            task.setSourcePath(path);
            task.setPaths(paths);
            task.setSiteService(siteService);
            task.setPageService(pageService);
            task.fork();
            taskList.add(task);
        }
        taskList.forEach(ForkJoinTask::join);

        log.info(site.getUrl() + sourcePath.substring(1) + " DONE");

        if (sourcePath.equals("/")) {
            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteService.update(site);
        }
    }

    private PageData getPage() {
        SiteData site = siteService.getById(siteId);
        String url = site.getUrl() + sourcePath.substring(1);

        log.info("CONNECTING TO: " + url);

        Connection.Response connectionResponse;
        try {
            Thread.sleep(1000);
            connectionResponse = Jsoup.connect(url)
                    .userAgent("MySearchEngine")
                    .referrer("http://www.google.com")
                    .execute();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        PageData pageData = new PageData();
        pageData.setSiteId(siteId);
        pageData.setPath(sourcePath);
        pageData.setCode(connectionResponse.statusCode());
        pageData.setContent(connectionResponse.body());
        pageData = pageService.create(pageData);

        return pageData;
    }

    private String normalizePath(String path) {
        String siteUrl = siteService.getById(siteId).getUrl();
        String urlRegex = "(" + siteUrl + ")[\\w-/]+$";
        String pathRegex = "^/[\\w-]+[\\w-/]*/?$";

        String normalPath = "";

        if (path.matches(urlRegex)) {
            normalPath = path.substring(siteUrl.length() - 1);
//            log.info("TRIMMED to path: " + path);
        } else if (path.matches(pathRegex)) {
            normalPath = path;
//            log.info("PATH match: " + path);
        }

        if (normalPath.isEmpty()) {
            return null;
        } else if (!path.endsWith("/")) {
            normalPath += "/";
        }
        return normalPath;
    }

    private void saveLemma(String lemmaWord, int pageId) {
        LemmaData lemma;
        try {
            lemma = lemmaService.getByWordAndSiteId(lemmaWord, siteId);
            lemma.setFrequency(lemma.getFrequency() + 1);
            lemmaService.update(lemma);
        } catch (NullPointerException e) {
//            log.info(e.getMessage());
            lemma = new LemmaData();
            lemma.setLemma(lemmaWord);
            lemma.setSiteId(siteId);
            lemma.setFrequency(1);
            lemma = lemmaService.create(lemma);
        }

        IndexData index;
        try {
            index = indexService.getByLemmaIdAndPageId(lemma.getId(), pageId);
            index.setRank(index.getRank() + 1);
            indexService.update(index);
        } catch (NullPointerException e) {
//            log.info(e.getMessage());
            index = new IndexData();
            index.setLemmaId(lemma.getId());
            index.setPageId(pageId);
            index.setRank(1D);
            index = indexService.create(index);
        }
    }
}
