package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.indexing.PageData;
import searchengine.dto.indexing.SiteData;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageCrudService implements CrudService<PageData> {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    @Override
    public PageData getById(Integer id) {
        PageEntity entity = pageRepository.findById(id).orElseThrow();
        return mapToData(entity);
    }

    @Override
    public PageData create(PageData item) {
        SiteEntity site = siteRepository.findById(item.getSiteId()).orElseThrow();
        PageEntity page = pageRepository.save(mapToEntity(item, site));
        return mapToData(page);
    }

    @Override
    public void update(PageData item) {
        SiteEntity site = siteRepository.findById(item.getId()).orElseThrow();
        pageRepository.save(mapToEntity(item, site));
    }

    @Override
    public void delete(Integer id) {
        pageRepository.deleteById(id);
    }

    private static PageData mapToData(PageEntity entity) {
        PageData data = new PageData();
        data.setId(entity.getId());
        data.setSiteId(entity.getSite().getId());
        data.setPath(entity.getPath());
        data.setCode(entity.getCode());
        data.setContent(entity.getContent());
        return data;
    }

    private static PageEntity mapToEntity(PageData data, SiteEntity site) {
        PageEntity entity = new PageEntity();
        entity.setId(data.getId());
        entity.setSite(site);
        entity.setPath(data.getPath());
        entity.setCode(data.getCode());
        entity.setContent(data.getContent());
        return entity;
    }

    @Transactional
    public void deleteAllBySiteId(int siteId) {
        List<Integer> pageIds = pageRepository.findAllIdsBySiteId(siteId);
        if (!pageIds.isEmpty()) {
            pageIds.forEach(pageRepository::deleteById);
        }
    }
}
