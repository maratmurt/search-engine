package searchengine.utils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.dao.IndexDao;
import searchengine.dao.LemmaDao;
import searchengine.dto.indexing.IndexDto;
import searchengine.dto.indexing.LemmaDto;
import searchengine.dto.indexing.PageDto;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Setter
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class IndexProcessor extends Thread {
    private List<PageDto> pages;
    private Map<String, Integer> lemmaFrequencyMap = new HashMap<>();

    private final Lemmatizer lemmatizer;
    private final HtmlParser parser;
    private final LemmaDao lemmaDao;
    private final IndexDao indexDao;
    private final IndexingTasksManager tasksManager;

    @Override
    public void run() {
        Map<Integer, Map<String, Double>> pageIdToLemmaRankMap = new HashMap<>();
        pages.forEach(page -> {
            String text = parser.getText(page.getContent());
            Map<String, Double> lemmaRankMap = lemmatizer.buildLemmaRankMap(text);
            pageIdToLemmaRankMap.put(page.getId(), lemmaRankMap);
            List<String> pageLemmas = lemmaRankMap.keySet().stream().toList();
            pageLemmas.forEach(lemma -> {
                int frequency = 1;
                if (lemmaFrequencyMap.containsKey(lemma)) {
                    frequency += lemmaFrequencyMap.get(lemma);
                }
                lemmaFrequencyMap.put(lemma, frequency);
            });
        });
        int siteId = pages.get(0).getSiteId();

        updateAndCreateLemmas(siteId);

        int i = 0;
        while (tasksManager.isRunning() && i < pages.size()) {
            PageDto page = pages.get(i);
            Map<String, Double> lemmaRankMap = pageIdToLemmaRankMap.get(page.getId());
            List<String> pageLemmas = lemmaRankMap.keySet().stream().toList();
            List<IndexDto> indexes = lemmaDao.findAllByLemmaAndSiteId(pageLemmas, siteId).stream().map(lemma -> {
                IndexDto index = new IndexDto();
                index.setPageId(page.getId());
                index.setLemmaId(lemma.getId());
                index.setRank(lemmaRankMap.get(lemma.getLemma()));
                return index;
            }).toList();
            indexDao.saveAll(indexes);
            i++;

            log.info("{} - {} INDEXED {}/{}", siteId, page.getPath(), i, pages.size());
        }
    }

    private void updateAndCreateLemmas(int siteId) {
        List<String> lemmas = lemmaFrequencyMap.keySet().stream().toList();

        synchronized (lemmaDao) {
            if (!tasksManager.isRunning()) {
                return;
            }

            long start = System.currentTimeMillis();

            List<LemmaDto> existingLemmas = lemmaDao.findAllByLemmaAndSiteId(lemmas, siteId);
            existingLemmas.forEach(lemma -> {
                int frequency = lemma.getFrequency() + lemmaFrequencyMap.get(lemma.getLemma());
                lemma.setFrequency(frequency);
            });
            lemmaDao.updateAll(existingLemmas);

            List<String> existingLemmaWords = existingLemmas.stream().map(LemmaDto::getLemma).toList();
            List<LemmaDto> newLemmas = lemmas.stream()
                    .filter(lemma -> !existingLemmaWords.contains(lemma))
                    .map(lemma -> {
                        LemmaDto lemmaDto = new LemmaDto();
                        lemmaDto.setLemma(lemma);
                        lemmaDto.setSiteId(siteId);
                        lemmaDto.setFrequency(1);
                        return lemmaDto;
                    }).toList();
            lemmaDao.saveAll(newLemmas);

            Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
            int minutes = duration.toMinutesPart();
            int seconds = duration.toSecondsPart();
            log.info("LEMMAS update time {}:{}", minutes, seconds);
        }
    }
}
