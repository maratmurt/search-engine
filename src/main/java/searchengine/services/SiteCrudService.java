package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.SiteData;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteCrudService implements CrudService<SiteData> {

    private final SiteRepository siteRepository;

    @Override
    public SiteData getById(Integer id) {
        SiteEntity entity = siteRepository.findById(id).orElseThrow();
        return mapToData(entity);
    }

    @Override
    public SiteData create(SiteData item) {
        SiteEntity site = siteRepository.save(mapToEntity(item));
        log.info(item.getName() + " CREATED");
        return mapToData(site);
    }

    @Override
    public void update(SiteData item) {
        siteRepository.save(mapToEntity(item));
        log.info(item.getName() + " UPDATED");
    }

    @Override
    public void delete(Integer id) {
        siteRepository.deleteById(id);
    }

    private static SiteData mapToData(SiteEntity entity) {
        SiteData data = new SiteData();
        data.setId(entity.getId());
        data.setName(entity.getName());
        data.setUrl(entity.getUrl());
        data.setStatus(entity.getStatus());
        data.setStatusTime(entity.getStatusTime());
        data.setLastError(entity.getLastError());
        return data;
    }

    private static SiteEntity mapToEntity(SiteData data) {
        SiteEntity entity = new SiteEntity();
        entity.setId(data.getId());
        entity.setName(data.getName());
        entity.setUrl(data.getUrl());
        entity.setStatus(data.getStatus());
        entity.setStatusTime(data.getStatusTime());
        entity.setLastError(data.getLastError());
        return entity;
    }

    public SiteData getByUrl(String url) {
        SiteEntity entity;
        try {
            entity = siteRepository.findByUrl(url);
        } catch (NoSuchElementException e) {
            throw new RuntimeException(e);
        }
        return mapToData(entity);
    }

    public List<SiteData> getAllByStatus(Status status) {
        List<SiteData> dataList = new ArrayList<>();
        List<SiteEntity> entityList = siteRepository.findAllByStatus(status);
        for (SiteEntity entity : entityList) {
            SiteData data = mapToData(entity);
            dataList.add(data);
        }
        return dataList;
    }
}
