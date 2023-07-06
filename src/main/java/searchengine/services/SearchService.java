package searchengine.services;

import searchengine.dto.statistics.SearchResults;
import searchengine.model.PageModel;

import java.io.IOException;
import java.util.List;

public interface SearchService {
    SearchResults getStatistics(String query, String site, int offset, int limit) throws IOException;
}
