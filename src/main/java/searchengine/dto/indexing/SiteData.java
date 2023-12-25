package searchengine.dto.indexing;

import lombok.Data;
import searchengine.model.Status;

import java.time.LocalDateTime;

@Data
public class SiteData {
    private Integer id;
    private Status status;
    private LocalDateTime statusTime;
    private String lastError;
    private String url;
    private String name;
}
