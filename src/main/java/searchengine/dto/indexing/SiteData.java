package searchengine.dto.indexing;

import lombok.Data;

import java.util.Set;

@Data
public class SiteData {
    int siteId;
    Set<String> paths;
    Set<String> lemmas;
}
