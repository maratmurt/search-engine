package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.HtmlScraper;
import searchengine.utils.LemmaCollector;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final LemmaCollector lemmaCollector;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final HtmlScraper htmlScraper;
    private List<PageEntity> relevantPages;

    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        SearchResponse response = new SearchResponse();

        List<String> queryLemmas = lemmaCollector.mapLemmasAndRanks(query).keySet().stream().toList();
        //TODO empty query lemmas list exception
        log.info(queryLemmas.size() + " query lemmas");
        Map<Integer, List<String>> lemmaFreqs = findAllLemmaFrequencies(queryLemmas);
        List<Integer> freqs = lemmaFreqs.keySet().stream().toList();
        String rarestWord = lemmaFreqs.get(freqs.get(0)).get(0);
        relevantPages = indexRepository.findAllPagesByLemmaWord(rarestWord);

        Integer siteId = null;
        if (!site.isEmpty()) {
            site += site.endsWith("/") ? "" : "/";
            siteId = siteRepository.findByUrl(site).orElseThrow().getId();
        }

        for (int i = 0; i < freqs.size(); i++) {
            List<String> lemmaWords = lemmaFreqs.get(freqs.get(i));
            for (String word : lemmaWords) {
                filterRelevantPages(word, siteId);
            }
        }
        log.info("relevant pages size = " + relevantPages.size());

        double maxRelevance = 0;
        Map<Integer, Double> pageAbsRelevances = new HashMap<>();
        for (PageEntity page: relevantPages) {
            double absRelevance = 0;
            for (String lemmaWord : queryLemmas) {
                double rank = indexRepository.getRankByPageIdAndLemmaWord(page.getId(), lemmaWord);
                absRelevance += rank;
            }
            pageAbsRelevances.put(page.getId(), absRelevance);
            maxRelevance = Math.max(absRelevance, maxRelevance);
        }

        List<SearchData> data = new ArrayList<>();
        for (PageEntity page : relevantPages) {
            SearchData item = new SearchData();
            item.setUri(page.getPath());
            item.setTitle(htmlScraper.getTitle(page.getContent()));
            String siteUrl = page.getSite().getUrl();
            siteUrl = siteUrl.substring(0, siteUrl.length() - 1);
            item.setSite(siteUrl);
            item.setSiteName(page.getSite().getName());
            double absRelevance = pageAbsRelevances.get(page.getId());
            double relevance = absRelevance / maxRelevance;
            item.setRelevance(relevance);
            String text = htmlScraper.getText(page.getContent());
            String snippet = generateSnippet(text, query);
            log.info(snippet);
            item.setSnippet(snippet);
            data.add(item);
        }
        response.setData(data);
        response.setResult(true);
        response.setCount(data.size());
        return response;
    }

    private String generateSnippet(String text, String query) {
        String snippet = "";
        List<String> queryLemmas = lemmaCollector.mapLemmasAndRanks(query).keySet().stream().toList();
        Map<String, List<String>> wordsLemmas = lemmaCollector.mapWordsAndLemmas(text);
        Set<String> matchingWords = new HashSet<>();
        Set<String> allWords = wordsLemmas.keySet();
        for (String word : allWords) {
            String lemma = wordsLemmas.get(word).get(0);
            if (queryLemmas.contains(lemma)) {
                log.info(word + " - " + lemma);
                matchingWords.add(word);
            }
        }
        String regex = "(" + String.join("|", matchingWords) + ")";
        log.info(regex);
        Matcher match = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);

        int range = 100 / matchingWords.size();
        int start = 0, end = 0;

        while (match.find() && !matchingWords.isEmpty()) {
            String matchingWord = match.group();
            if (!matchingWords.contains(matchingWord)) {
                continue;
            }
            matchingWords.remove(matchingWord);
            if (match.start() - end > range) {
                start = Math.max(0, match.start() - range);
                end = Math.min(text.length(), match.end() + range);
                snippet = snippet + "..." + text.substring(start, end);
            } else {
                snippet = snippet + text.substring(end, match.end() + range);
                end = match.end() + range;
            }
        }
        snippet += "...";

        return snippet;
    }

    private Map<Integer, List<String>> findAllLemmaFrequencies(List<String> lemmaWords) {
        Map<Integer, List<String>> lemmaFreqs = new TreeMap<>();
        for (String word : lemmaWords) {
            List<LemmaEntity> lemmas = lemmaRepository.findAllByLemma(word).orElseThrow();
            int freq = lemmas.stream().map(LemmaEntity::getFrequency).reduce(Integer::sum).orElse(0);
            if (freq < 100) {
                List<String> words = lemmaFreqs.getOrDefault(freq, new ArrayList<>());
                words.add(word);
                lemmaFreqs.put(freq, words);
            }
        }
        return lemmaFreqs;
    }

    private void filterRelevantPages(String word, Integer siteId) {
        List<PageEntity> nextPages = indexRepository.findAllPagesByLemmaWord(word);
        if (siteId != null) {
            for (Iterator<PageEntity> iterator = nextPages.iterator(); iterator.hasNext();) {
                PageEntity page = iterator.next();
                if (!page.getSite().getId().equals(siteId)) {
                    iterator.remove();
                    log.info("Removed other sites page " + page.getId());
                }
            }
        }

        for (Iterator<PageEntity> iterator = relevantPages.iterator(); iterator.hasNext();) {
            PageEntity relevantPage = iterator.next();
            if (!nextPages.contains(relevantPage)) {
                iterator.remove();
                log.info("Removed relevant page " + relevantPage.getId());
            }
        }
    }
}
