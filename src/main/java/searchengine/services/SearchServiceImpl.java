package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.utils.LemmaCollector;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final LemmaCollector lemmaCollector;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    @Override
    public Object search(String query, String site, int offset, int limit) {
        Set<String> lemmaWords = lemmaCollector.collect(query).keySet();
        Map<Integer, String> lemmaFreq = new TreeMap<>();
        if (site == null) {
            for (String lemmaWord : lemmaWords) {
                List<LemmaEntity> lemmas = lemmaRepository.findAllByLemma(lemmaWord);
                lemmas.removeIf(lemma -> lemma.getFrequency() > 100);
                int freqSum = lemmas.stream().map(LemmaEntity::getFrequency).reduce(Integer::sum).get();
                lemmaFreq.put(freqSum, lemmaWord);
            }
            List<PageEntity> relevantPages = new ArrayList<>();

        }

        return null;
    }
}
