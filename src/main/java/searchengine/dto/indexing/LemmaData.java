package searchengine.dto.indexing;

import lombok.Data;

@Data
public class LemmaData {
    private Integer id;
    private Integer siteId;
    private String lemma;
    private Integer frequency;
}
