package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dao.LemmaDao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dto.ApiResponse;
import searchengine.dto.indexing.SiteDto;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.utils.IndexingTasksManager;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final PageDao pageDao;
    private final LemmaDao lemmaDao;
    private final IndexingTasksManager tasksManager;
    private final SiteDao siteDao;

    @Override
    public ApiResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setPages(pageDao.getAllPagesCount());
        total.setLemmas(lemmaDao.getAllLemmasCount());
        total.setIndexing(tasksManager.isRunning());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteConfig> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
            SiteConfig siteConfig = sitesList.get(i);
            SiteDto site = siteDao.findByUrl(siteConfig.getUrl()).orElseThrow();
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteConfig.getName());
            item.setUrl(siteConfig.getUrl());
            int siteId = site.getId();
            int pages = pageDao.getSitePagesCount(siteId);
            int lemmas = lemmaDao.getSiteLemmasCount(siteId);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(site.getStatus());
            item.setError(site.getLastError());
            item.setStatusTime(site.getStatusTime().getTime());
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
