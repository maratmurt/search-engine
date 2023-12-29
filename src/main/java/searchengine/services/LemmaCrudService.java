package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.indexing.LemmaData;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaCrudService implements CrudService<LemmaData> {

    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    @Override
    public LemmaData getById(Integer id) {
        LemmaEntity entity = lemmaRepository.findById(id).orElseThrow();
        return mapToData(entity);
    }

    @Override
    public LemmaData create(LemmaData item) {
        SiteEntity site = siteRepository.findById(item.getSiteId()).orElseThrow();
        LemmaEntity lemma = lemmaRepository.saveAndFlush(mapToEntity(item, site));
        return mapToData(lemma);
    }

    @Override
    public void update(LemmaData item) {
        SiteEntity site = siteRepository.findById(item.getSiteId()).orElseThrow();
        lemmaRepository.saveAndFlush(mapToEntity(item, site));
    }

    @Override
    public void delete(Integer id) {
        lemmaRepository.deleteById(id);
    }

    private static LemmaData mapToData(LemmaEntity entity) {
        LemmaData data = new LemmaData();
        data.setId(entity.getId());
        data.setLemma(entity.getLemma());
        data.setSiteId(entity.getSite().getId());
        data.setFrequency(entity.getFrequency());
        return data;
    }

    private static LemmaEntity mapToEntity(LemmaData data, SiteEntity site) {
        LemmaEntity entity = new LemmaEntity();
        entity.setId(data.getId());
        entity.setLemma(data.getLemma());
        entity.setSite(site);
        entity.setFrequency(data.getFrequency());
        return entity;
    }

    public LemmaData getByWordAndSiteId(String word, int siteId) {
        LemmaEntity entity = lemmaRepository.findByLemmaAndSite_Id(word, siteId).orElseThrow();
        return mapToData(entity);
    }

    @Transactional
    public void deleteAllBySiteId(int siteId) {
        SiteEntity site = siteRepository.findById(siteId).orElseThrow();
        lemmaRepository.deleteAllBySite(site);
    }
}
