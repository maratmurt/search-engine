package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.indexing.IndexData;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexCrudService implements CrudService<IndexData> {

    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;

    @Override
    public IndexData getById(Integer id) {
        IndexEntity entity = indexRepository.findById(id).orElseThrow();
        return mapToData(entity);
    }

    @Override
    public IndexData create(IndexData item) {
        LemmaEntity lemma = lemmaRepository.findById(item.getLemmaId()).orElseThrow();
        PageEntity page = pageRepository.findById(item.getPageId()).orElseThrow();
        IndexEntity index = indexRepository.save(mapToEntity(item, lemma, page));
        log.info(index.getLemma().getLemma());
        return mapToData(index);
    }

    @Override
    public void update(IndexData item) {
        LemmaEntity lemma = lemmaRepository.findById(item.getLemmaId()).orElseThrow();
        PageEntity page = pageRepository.findById(item.getPageId()).orElseThrow();
        indexRepository.save(mapToEntity(item, lemma, page));
    }

    @Override
    public void delete(Integer id) {
        indexRepository.deleteById(id);
    }

    private static IndexData mapToData(IndexEntity entity) {
        IndexData data = new IndexData();
        data.setId(entity.getId());
        data.setLemmaId(entity.getLemma().getId());
        data.setPageId(entity.getPage().getId());
        data.setRank(entity.getRank());
        return data;
    }

    private static IndexEntity mapToEntity(IndexData data, LemmaEntity lemma, PageEntity page) {
        IndexEntity entity = new IndexEntity();
        entity.setId(data.getId());
        entity.setLemma(lemma);
        entity.setPage(page);
        entity.setRank(data.getRank());
        return entity;
    }

    public IndexData getByLemmaIdAndPageId(int lemmaId, int pageId) {
        LemmaEntity lemma = lemmaRepository.findById(lemmaId).orElseThrow();
        PageEntity page = pageRepository.findById(pageId).orElseThrow();
        IndexEntity index = indexRepository.findByLemmaAndPage(lemma, page);
        return mapToData(index);
    }

    @Transactional
    public void deleteAllBySiteId(int siteId) {
        indexRepository.deleteAllByPage_Site_Id(siteId);
    }
}
