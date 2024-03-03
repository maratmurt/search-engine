package searchengine.dto.indexing;

import lombok.Data;
import searchengine.model.SiteEntity;

import java.util.Set;

@Data
public class SiteData {
    SiteEntity site;
    Set<String> paths;
    Set<String> lemmas;
}
