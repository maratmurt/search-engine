package searchengine.dto.indexing;

import lombok.Data;

@Data
public class IndexData {
    private Integer id;
    private Integer pageId;
    private Integer lemmaId;
    private Double rank;
}
