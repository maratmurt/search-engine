package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;

@Service
public class RepositoryService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Autowired
    public RepositoryService(SiteRepository siteRepository, PageRepository pageRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    public void save(SiteEntity site) {
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    public void save(Page page) {
        SiteEntity site = page.getSite();
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        pageRepository.save(page);
    }

    public SiteEntity findSiteByUrl(String url) {
        return siteRepository.findByURL(url);
    }

    public Iterable<Page> findPagesBySite(SiteEntity site) {
        return pageRepository.findBySite(site);
    }

    public void deleteAll(Iterable<Page> pages) {
        pageRepository.deleteAll(pages);
    }

    public void delete(SiteEntity site) {
        siteRepository.delete(site);
    }

    public boolean pageExists(SiteEntity site, String path) {
        return pageRepository.existsBySiteAndPath(site, path);
    }
}
