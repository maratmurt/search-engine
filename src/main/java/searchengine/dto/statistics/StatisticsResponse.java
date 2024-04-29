package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;
import searchengine.dto.ApiResponse;

@Setter
@Getter
public class StatisticsResponse extends ApiResponse {
    private StatisticsData statistics;
}
