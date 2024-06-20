package searchengine.dto.search;

import lombok.Getter;
import lombok.Setter;
import searchengine.dto.ApiResponse;

import java.util.List;

@Getter
@Setter
public class SearchResponse extends ApiResponse {
    private boolean result;
    private String error;
    private int count = 0;
    private List<SearchData> data;
}
