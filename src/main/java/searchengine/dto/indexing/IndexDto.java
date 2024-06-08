package searchengine.dto.indexing;

import lombok.Data;

@Data
public class IndexDto {
    private int id;
    private double rank;
    private int lemmaId;
    private int pageId;
}
