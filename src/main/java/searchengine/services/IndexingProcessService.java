package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;

@Service
@Getter
@Setter
public class IndexingProcessService {
    private final CopyOnWriteArraySet<PageModel> listPages;
    private final ConcurrentHashMap<String, SiteModel> listSites;
    private ForkJoinPool forkJoinPool;
    private boolean isIndexingProcessRunning;
    private boolean interrupted;
    private boolean fullIndexing;
    private String singlePageUrl;

    public IndexingProcessService() {
        listPages = new CopyOnWriteArraySet<>();
        listSites = new ConcurrentHashMap<>();
        fullIndexing = true;
    }

    public void enableFullIndexing() {
        isIndexingProcessRunning = true;
        fullIndexing = true;
        forkJoinPool = new ForkJoinPool();
    }
    public void enableSingleIndexing (String singleUrl) {
        isIndexingProcessRunning = true;
        fullIndexing = false;
        this.singlePageUrl = singleUrl;
    }

    public void defaultSet() {
        listPages.clear();
        listSites.clear();
        isIndexingProcessRunning = false;
        interrupted = false;
        fullIndexing = true;
    }

    public void addSite(SiteModel siteModel) {
        listSites.put(siteModel.getUrl(), siteModel);
    }

    public SiteModel getSite(String rootSiteURL) {
        return listSites.get(rootSiteURL);
    }

    public boolean repeatPage(PageModel pageModel) {
        if (listPages.contains(pageModel)) {
            return true;
        }
        listPages.add(pageModel);
        return false;
    }
}
