package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final WorkingWithDataService workingWithDataService;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();

        for (Site site : sitesList) {
            SiteModel siteModel = workingWithDataService.getSiteRepository().findByUrl(site.getUrl());
            if (siteModel == null) {
                continue;
            }
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            int pages = getCountPageSite(siteModel.getId());
            int lemmas = getCountLemmasSite(siteModel.getId());
            item.setName(siteModel.getName());
            item.setUrl(siteModel.getUrl());
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteModel.getStatus().toString());
            item.setError(siteModel.getLastError());
            item.setStatusTime(siteModel.getStatusTime().toEpochSecond(ZoneOffset.UTC) * 1000);
            total.setSites(total.getSites() + 1);
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private int getCountPageSite (int siteId) {
        List<PageModel> listPages = workingWithDataService.getPageRepository().findAllBySiteId(siteId);
        return listPages.size();
    }
    private int getCountLemmasSite (int siteId) {
        List<LemmaModel> listLemmas = workingWithDataService.getLemmaRepository().findAllBySiteId(siteId);
        return listLemmas.size();
    }
}
