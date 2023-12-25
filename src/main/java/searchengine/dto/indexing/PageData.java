package searchengine.dto.indexing;

import lombok.Data;

@Data
public class PageData {
    private Integer id;
    private Integer siteId;
    private String path;
    private Integer code;
    private String content;
}
