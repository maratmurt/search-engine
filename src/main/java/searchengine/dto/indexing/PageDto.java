package searchengine.dto.indexing;

import lombok.Data;

@Data
public class PageDto {
    private Integer id;
    private Integer siteId;
    private String path;
    private Integer code;
    private String content;

    @Override
    public String toString() {
        return "PageDto{" +
                "id=" + id +
                ", siteId=" + siteId +
                ", path='" + path + '\'' +
                '}';
    }
}
