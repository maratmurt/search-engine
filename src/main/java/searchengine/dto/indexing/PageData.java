package searchengine.dto.indexing;

import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
public class PageData {
    int statusCode;
    String body;
    String text;
    Set<String> links;
    Map<String, Double> lemmaRanks;
}
