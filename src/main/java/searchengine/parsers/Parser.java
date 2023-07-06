package searchengine.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jsoup.nodes.Document;
import searchengine.сonnection.Connection;
import searchengine.model.PageModel;
import searchengine.services.IndexingProcessService;
import searchengine.model.SiteModel;
import searchengine.model.StatusSiteModel;
import searchengine.services.WorkingWithDataService;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class Parser implements Runnable {
    private final SiteModel siteModel;
    private final WorkingWithDataService workingWithDataService;
    private final IndexingProcessService indexingProcessService;

    @Override
    public void run() {
        if (indexingProcessService.isFullIndexing()) {
            try {
                fullIndexing();
            } catch (Exception ex) {
                log.info(siteModel.getUrl() + " " + ex.getMessage());
            }
        } else {
            singlePage(indexingProcessService.getSinglePageUrl());
        }
    }

    private void fullIndexing() {
        NodeUrl nodeUrl = new NodeUrl(siteModel.getUrl());
        indexingProcessService.addSite(siteModel);
        log.info(siteModel.getUrl() + " " + "индексация началась");
        indexingProcessService.getForkJoinPool()
                .invoke(new ParserURL(nodeUrl, workingWithDataService, indexingProcessService));

        if (!indexingProcessService.isInterrupted() && !siteModel.getStatus().equals(StatusSiteModel.FAILED)) {
            siteModel.setStatus(StatusSiteModel.INDEXED);
            workingWithDataService.updateTimeAndSaveSite(siteModel);
            log.info(siteModel.getUrl() + " индексация успешно завершена");
        }

        if (indexingProcessService.isInterrupted()) {
            String message = "индексация остановлена пользователем";
            siteModel.setLastError(message);
            siteModel.setStatus(StatusSiteModel.FAILED);
            workingWithDataService.updateTimeAndSaveSite(siteModel);
            log.info(siteModel.getUrl() + " " + message);
        }

        if (indexingProcessService.getForkJoinPool().getActiveThreadCount() == 0) {
            indexingProcessService.defaultSet();
        }
    }

    private void singlePage(String url) {
        log.info(url + " индексация началась");
        try {
            Connection connection = new Connection(url);
            Document document = connection.getConnection().get();
            String pathPageNotNameSite = url.replaceAll(siteModel.getUrl(), "/");

            PageModel pageModel = workingWithDataService.createPageModel(document, pathPageNotNameSite, siteModel);
            workingWithDataService.getPageRepository().save(pageModel);
            workingWithDataService.saveLemmasAndIndexes(pageModel);
            if (siteModel.getStatus() == null) {
                siteModel.setStatus(StatusSiteModel.INDEXED);
            }
            workingWithDataService.getPageRepository().save(pageModel);
            workingWithDataService.updateTimeAndSaveSite(siteModel);
            indexingProcessService.defaultSet();
            log.info(indexingProcessService.getSinglePageUrl() + " страница добавлена/обновлена");
        } catch (IOException e) {
            log.info(siteModel.getUrl() + " " + e.getMessage());
        }
    }
}
