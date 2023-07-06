package searchengine.dto.statistics;

import lombok.*;

@Data
public class StatisticsSearch {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Float relevance;
}
