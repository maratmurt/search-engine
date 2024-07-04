package searchengine.dto.indexing;

import lombok.Data;

@Data
public class IndexDto {
    private Integer id;
    private Double rank;
    private Integer lemmaId;
    private Integer pageId;
}
