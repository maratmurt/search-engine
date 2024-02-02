package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private String error;
    private int count = 0;
    private List<SearchData> data;
}
