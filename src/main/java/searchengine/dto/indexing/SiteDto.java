package searchengine.dto.indexing;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class SiteDto {
    private int id;
    private String status;
    private String url;
    private String name;
    private Timestamp statusTime;
    private String lastError;
}
