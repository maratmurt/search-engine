package searchengine.services;

import searchengine.dto.ApiResponse;

public interface IndexingService {
    ApiResponse startIndexing();

    ApiResponse stopIndexing();
}
