package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.TaskManager;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final TaskManager taskManager;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(taskManager.isIndexing());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
            Site siteConfig = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteConfig.getName());
            item.setUrl(siteConfig.getUrl());

            SiteEntity siteEntity;
            try {
                String siteUrl = siteConfig.getUrl();
                siteUrl += siteUrl.endsWith("/") ? "" : "/";
                siteEntity = siteRepository.findByUrl(siteUrl).orElseThrow();
                int pages = pageRepository.countBySite(siteEntity);
                int lemmas = lemmaRepository.countBySite(siteEntity);
                item.setPages(pages);
                item.setLemmas(lemmas);
                item.setStatus(siteEntity.getStatus().toString());
                item.setError(siteEntity.getLastError());
                ZonedDateTime zdt = ZonedDateTime.of(siteEntity.getStatusTime(), ZoneId.systemDefault());
                item.setStatusTime(zdt.toEpochSecond());
                total.setPages(total.getPages() + pages);
                total.setLemmas(total.getLemmas() + lemmas);
            } catch (NoSuchElementException e) {
                log.error(e.getMessage());
            }
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
