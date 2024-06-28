package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dao.IndexDao;
import searchengine.dao.LemmaDao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dto.ApiResponse;
import searchengine.dto.ErrorResponse;
import searchengine.dto.indexing.IndexDto;
import searchengine.dto.indexing.LemmaDto;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SiteDto;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.utils.HtmlParser;
import searchengine.utils.Lemmatizer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final Lemmatizer lemmatizer;
    private final LemmaDao lemmaDao;
    private final IndexDao indexDao;
    private final PageDao pageDao;
    private final SiteDao siteDao;
    private final HtmlParser parser;
    private List<PageDto> relevantPages;
    private List<LemmaDto> existingLemmas;
    private List<IndexDto> allIndexes;
    private Map<Integer, Double> pageAbsRelevanceMap;
    private double maxRelevance;

    @Override
    public ApiResponse search(String query, String site, int offset, int limit) {
        if (query.isEmpty())
            return new ErrorResponse("Задан пустой поисковый запрос");

        List<String> queryLemmas = lemmatizer.buildLemmaRankMap(query).keySet().stream().toList();

        Optional<String> noMatchLemma = queryLemmas.stream()
                .filter(lemma -> lemmaDao.findAllByLemma(List.of(lemma)).isEmpty()).findAny();
        if (noMatchLemma.isPresent())
            return blankResponse();

        existingLemmas = lemmaDao.findAllByLemma(queryLemmas);

        if (site != null) {
            int requestSiteId = siteDao.findByUrl(site).orElseThrow().getId();
            existingLemmas.removeIf(lemma -> lemma.getSiteId() != requestSiteId);
            if (existingLemmas.isEmpty()) return blankResponse();
        }

        filterIrrelevantPages();

        if (relevantPages.isEmpty())
            return blankResponse();

        getAbsoluteAndMaxRelevance();

        List<SearchData> allData = collectSearchData(query);
        int bound = Math.min(offset + limit, relevantPages.size());
        List<SearchData> responseData = allData.subList(offset, bound);

        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setCount(relevantPages.size());
        response.setData(responseData);

        return response;
    }

    private void filterIrrelevantPages() {
        List<Integer> existingLemmaIds = existingLemmas.stream().map(LemmaDto::getId).toList();
        allIndexes = indexDao.findAllByLemmaIds(existingLemmaIds);
        List<Integer> allPageIds = allIndexes.stream().map(IndexDto::getPageId).toList();
        List<PageDto> allPages = pageDao.findAllById(allPageIds);
        relevantPages = new ArrayList<>(allPages);
        for (LemmaDto lemma : existingLemmas) {
            List<Integer> lemmaIds = lemmaDao.findAllByLemma(List.of(lemma.getLemma()))
                    .stream().map(LemmaDto::getId).toList();
            List<IndexDto> lemmaIndexes = allIndexes.stream()
                    .filter(index -> lemmaIds.contains(index.getLemmaId())).toList();
            List<Integer> lemmaPageIds = lemmaIndexes.stream()
                    .map(IndexDto::getPageId).toList();
            List<PageDto> lemmaPages = allPages.stream()
                    .filter(page -> lemmaPageIds.contains(page.getId())).toList();
            relevantPages.removeIf(page -> !lemmaPages.contains(page));
        }
    }

    private void getAbsoluteAndMaxRelevance() {
        List<Integer> relevantPageIds = relevantPages.stream().map(PageDto::getId).toList();
        List<IndexDto> relevantIndexes = allIndexes.stream()
                .filter(index -> relevantPageIds.contains(index.getPageId())).toList();
        pageAbsRelevanceMap = relevantIndexes.stream()
                .collect(Collectors.toMap(IndexDto::getPageId, IndexDto::getRank, Double::sum));

        maxRelevance = pageAbsRelevanceMap.values().stream()
                .max(Comparator.naturalOrder()).orElseThrow();
    }

    private List<SearchData> collectSearchData(String query) {
        List<Integer> siteIds = relevantPages.stream().map(PageDto::getSiteId).distinct().toList();
        Map<Integer, SiteDto> siteIdToSite = siteDao.findAllById(siteIds).stream()
                .collect(Collectors.toMap(SiteDto::getId, siteDto -> siteDto));
        List<SearchData> allData = new ArrayList<>();
        relevantPages.forEach(page -> {
            SearchData item = new SearchData();

            SiteDto pageSite = siteIdToSite.get(page.getSiteId());
            item.setSite(pageSite.getUrl());
            item.setSiteName(pageSite.getName());
            item.setUri(page.getPath());
            item.setTitle(parser.getTitle(page.getContent()));

            //set relative relevance
            double absRelevance = pageAbsRelevanceMap.get(page.getId());
            double relativeRelevance = absRelevance / maxRelevance;
            item.setRelevance(relativeRelevance);

            //set snippet
            String text = parser.getText(page.getContent());
            String snippet = generateSnippet(text, query);
            item.setSnippet(snippet);

            allData.add(item);
        });

        allData.sort(Comparator.comparing(SearchData::getRelevance).reversed());

        return allData;
    }

    private String generateSnippet(String text, String query) {
        StringBuilder snippet = new StringBuilder();
        List<String> matchingWords = findMatchingWords(text, query);
        String regex = "(?<=[^A-Za-z'А-Яа-яЁё])(" +
                String.join("|", matchingWords) +
                ")(?=[^A-Za-z'А-Яа-яЁё])";
        Matcher match = Pattern.compile(regex).matcher(text);
        int range = 80 / matchingWords.size();
        String bOpen = "<b>", bClose = "</b>", dots = "...";
        if (!match.find()) return "";
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

    private List<String> findMatchingWords(String text, String query) {
        List<String> queryLemmas = lemmatizer.buildLemmaRankMap(query).keySet().stream().toList();
        Map<String, List<String>> wordLemmasMap = lemmatizer.buildWordLemmasMap(text);
        Set<String> matchingWords = new HashSet<>();

        wordLemmasMap.forEach((word, lemmas) -> {
            Optional<String> match = lemmas.stream().filter(queryLemmas::contains).findAny();
            if (match.isPresent()) {
                matchingWords.add(word);
                log.info("{} - {}", word, match.get());
            }
        });
        return new ArrayList<>(matchingWords);
    }

    private SearchResponse blankResponse() {
        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setCount(0);
        response.setData(List.of());
        return response;
    }
}
