package searchengine.dto.statistics;

import lombok.Data;
import searchengine.model.PageModel;

@Data
public class StatisticsPage {
    private PageModel pageModel;
    private int rankPage;
}
