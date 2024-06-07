package searchengine.dto.indexing;

import lombok.Data;

@Data
public class PageDto {
    private int id;
    private int siteId;
    private String path;
    private int code;
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
