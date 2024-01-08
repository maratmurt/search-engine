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

        List<String> queryLemmas = lemmaCollector.collect(query).keySet().stream().toList();
        Map<Integer, List<String>> lemmaFreqs = findAllLemmaFreqs(queryLemmas);
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

//        List<Integer> pageIds = relevantPages.stream().map(PageEntity::getId).toList();
        List<SearchData> data = new ArrayList<>();
        for (PageEntity page : relevantPages) {
            SearchData item = new SearchData();
            item.setUri(page.getPath());
            item.setTitle(htmlScraper.getTitle(page.getContent()));
            String siteUrl = page.getSite().getUrl();
            siteUrl = siteUrl.substring(0, siteUrl.length() - 1);
            item.setSite(siteUrl);
            item.setSiteName(page.getSite().getName());
            //TODO SearchData snippet and relevance
            data.add(item);
        }
        response.setData(data);
        response.setResult(true);
        response.setCount(data.size());
        return response;
    }

    private Map<Integer, List<String>> findAllLemmaFreqs(List<String> lemmaWords) {
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
