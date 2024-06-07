package searchengine.dto.indexing;

import lombok.Data;

@Data
public class LemmaDto {
    private int id;
    private String lemma;
    private int siteId;
    private int frequency;
}
