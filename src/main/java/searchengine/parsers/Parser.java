package searchengine.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jsoup.nodes.Document;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.connection.Connection;
import searchengine.model.PageModel;
import searchengine.services.IndexingProcessService;
import searchengine.model.SiteModel;
import searchengine.model.StatusSiteModel;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class Parser implements Runnable {
    private final SiteModel siteModel;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
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
                .invoke(new ParserURL(siteRepository, pageRepository, lemmaRepository, indexRepository, nodeUrl, indexingProcessService));

        if (!indexingProcessService.isInterrupted() && !siteModel.getStatus().equals(StatusSiteModel.FAILED)) {
            siteModel.setStatus(StatusSiteModel.INDEXED);
            updateTimeAndSaveSite(siteModel);
            log.info(siteModel.getUrl() + " индексация успешно завершена");
        }

        if (indexingProcessService.isInterrupted()) {
            String message = "индексация остановлена пользователем";
            siteModel.setLastError(message);
            siteModel.setStatus(StatusSiteModel.FAILED);
            updateTimeAndSaveSite(siteModel);
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
            PageModel pageModel = createPageModel(document, pathPageNotNameSite, siteModel);
            pageRepository.save(pageModel);
            new LemmaAndIndexCreator(lemmaRepository, indexRepository, pageModel);
            if (siteModel.getStatus() == null) {
                siteModel.setStatus(StatusSiteModel.INDEXED);
            }
            pageRepository.save(pageModel);
            updateTimeAndSaveSite(siteModel);
            indexingProcessService.defaultSet();
            log.info(indexingProcessService.getSinglePageUrl() + " страница добавлена/обновлена");
        } catch (IOException e) {
            log.info(siteModel.getUrl() + " " + e.getMessage());
        }
    }
    public void updateTimeAndSaveSite(SiteModel siteModel) {
        siteModel.setStatusTime(LocalDateTime.now());
        this.siteRepository.save(siteModel);
    }
    private static PageModel createPageModel(Document document, String url, SiteModel siteModel) {
        String content = document.outerHtml();
        PageModel pageModel = new PageModel();
        pageModel.setPathPageNotNameSite(url);
        pageModel.setSite(siteModel);
        pageModel.setContentHTMLCode(content);
        pageModel.setCodeHTTPResponse(document.connection().response().statusCode());
        return pageModel;
    }
}
