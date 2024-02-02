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

        if (query.isEmpty()) {
            response.setResult(false);
            response.setError("Задан пустой поисковый запрос");
            return response;
        }

        List<String> queryLemmas = lemmaCollector.mapLemmasAndRanks(query).keySet().stream().toList();
        Map<Integer, List<String>> lemmasFreqs = findAllLemmaFrequencies(queryLemmas);
        if (lemmasFreqs.isEmpty()) {
            response.setResult(true);
            return response;
        }

        List<Integer> freqs = lemmasFreqs.keySet().stream().toList();
        String rarestWord = lemmasFreqs.get(freqs.get(0)).get(0);
        relevantPages = indexRepository.findAllPagesByLemmaWord(rarestWord);

        Integer siteId = null;
        if (site != null && !site.isBlank()) {
            site += site.endsWith("/") ? "" : "/";
            siteId = siteRepository.findByUrl(site).orElseThrow().getId();
        }

        for (int i = 0; i < freqs.size(); i++) {
            List<String> lemmaWords = lemmasFreqs.get(freqs.get(i));
            for (String word : lemmaWords) {
                filterRelevantPages(word, siteId);
            }
        }
        log.info("relevant pages size = " + relevantPages.size());

        Map<Integer, Double> pageAbsRelevanceMap = getPageAbsoluteRelevanceMap(queryLemmas);
        double maxRelevance = pageAbsRelevanceMap.values().stream().reduce(Double::max).orElse(0D);

        List<SearchData> data = getData(pageAbsRelevanceMap, maxRelevance, query);
        response.setData(data);
        response.setResult(true);
        response.setCount(data.size());
        return response;
    }

    private String generateSnippet(String text, String query) {
        StringBuilder snippet = new StringBuilder();
        List<String> matchingWords = findMatchingWords(text, query);
        String regex = "(?<=[^А-Яа-я])(" + String.join("|", matchingWords) + ")(?=[^А-Яа-я])";
        log.info("regex: " + regex);
        Matcher match = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        int range = 80 / matchingWords.size();
        String bOpen = "<b>", bClose = "</b>", dots = "...";
        match.find();
        int start = Math.max(0, match.start() - range);
        snippet.append(dots)
                .append(text, start, match.start())
                .append(bOpen)
                .append(text, match.start(), match.end())
                .append(bClose);
        int lastMatchEnd = match.end();

        while (match.find() && !matchingWords.isEmpty()) {
            String matchingWord = match.group();
            if (!matchingWords.contains(matchingWord)) {
                continue;
            }
            matchingWords.remove(matchingWord);
            if (match.start() - lastMatchEnd > range * 2) {
                snippet.append(text, lastMatchEnd, lastMatchEnd + range)
                        .append(dots)
                        .append(text, match.start() - range, match.start())
                        .append(bOpen)
                        .append(text, match.start(), match.end())
                        .append(bClose);
            } else {
                snippet.append(text, lastMatchEnd, match.start())
                        .append(bOpen)
                        .append(text, match.start(), match.end())
                        .append(bClose);
            }
            lastMatchEnd = match.end();
        }
        if (lastMatchEnd + range < text.length()) {
            snippet.append(text, lastMatchEnd, lastMatchEnd + range).append(dots);
        } else {
            snippet.append(text, lastMatchEnd, text.length());
        }
        return snippet.toString();
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

    private List<SearchData> getData(Map<Integer, Double> pageAbsRelevanceMap, double maxRelevance, String query) {
        List<SearchData> data = new ArrayList<>();
        for (PageEntity page : relevantPages) {
            SearchData item = new SearchData();
            item.setUri(page.getPath());
            item.setTitle(htmlScraper.getTitle(page.getContent()));
            String siteUrl = page.getSite().getUrl();
            siteUrl = siteUrl.substring(0, siteUrl.length() - 1);
            item.setSite(siteUrl);
            item.setSiteName(page.getSite().getName());
            double absRelevance = pageAbsRelevanceMap.get(page.getId());
            double relevance = absRelevance / maxRelevance;
            item.setRelevance(relevance);
            String text = htmlScraper.getText(page.getContent());
            String snippet = generateSnippet(text, query);
            log.info("snippet: " + snippet);
            item.setSnippet(snippet);
            data.add(item);
        }
        return data;
    }

    private Map<Integer, Double> getPageAbsoluteRelevanceMap(List<String> queryLemmas) {
        Map<Integer, Double> pageAbsRelevanceMap = new HashMap<>();
        for (PageEntity page: relevantPages) {
            double absRelevance = 0;
            for (String lemmaWord : queryLemmas) {
                double rank = indexRepository.getRankByPageIdAndLemmaWord(page.getId(), lemmaWord);
                absRelevance += rank;
            }
            pageAbsRelevanceMap.put(page.getId(), absRelevance);
        }
        return pageAbsRelevanceMap;
    }

    private List<String> findMatchingWords(String text, String query) {
        List<String> queryLemmas = lemmaCollector.mapLemmasAndRanks(query).keySet().stream().toList();
        Map<String, List<String>> wordsLemmasMap = lemmaCollector.mapWordsAndLemmas(text);
        Set<String> matchingWords = new HashSet<>();
        Set<String> allWords = wordsLemmasMap.keySet();
        for (String word : allWords) {
            List<String> lemmas = wordsLemmasMap.get(word);
            for (String lemma : lemmas) {
                if (queryLemmas.contains(lemma)) {
                    log.info(word + " - " + lemma);
                    matchingWords.add(word);
                }
            }
        }
        return new ArrayList<>(matchingWords);
    }
}
