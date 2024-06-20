package searchengine.services;

import searchengine.dto.ApiResponse;

public interface SearchService {
    ApiResponse search(String query, String site, int offset, int limit);
}
