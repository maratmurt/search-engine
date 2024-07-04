package searchengine.dto.indexing;

import lombok.Data;

@Data
public class LemmaDto {
    private Integer id;
    private String lemma;
    private Integer siteId;
    private Integer frequency;
}
