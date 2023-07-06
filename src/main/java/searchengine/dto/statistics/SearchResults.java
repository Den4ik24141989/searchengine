package searchengine.dto.statistics;

import lombok.*;

import java.util.List;

@Data
public class SearchResults {
    private boolean result;
    private int count;
    private List<StatisticsSearch> data;
}