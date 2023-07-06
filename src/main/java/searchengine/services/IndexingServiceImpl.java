package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.parsers.Parser;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final WorkingWithDataService workingWithDataService;
    private final IndexingProcessService indexingProcessService;
    private final int processorCoreCount = Runtime.getRuntime().availableProcessors();
    private Executor executor;

    @Override
    public boolean startFullIndexing() {
        if (!isIndexingProcessRunning()) {
            workingWithDataService.clearDB();
            indexingProcessService.enableFullIndexing();
            executeFullIndexing();
            return true;
        }
        log.info("Индексация уже запущена");
        return false;
    }

    @Override
    public boolean addOrUpdatePage(String url) {
        String[] splitUrl = url.split("/");
        String rootUrl = splitUrl[0] + "//" + splitUrl[1] + splitUrl[2] + "/";
        String pathPageNotNameSite = url.replaceAll(rootUrl, "/");
        Site site = new Site();
        site.setUrl(rootUrl);
        if (!workingWithDataService.getSitesList().getSites().contains(site)) {
            log.info(url + " - данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return false;
        }
        executor = Executors.newSingleThreadExecutor();
        indexingProcessService.enableSingleIndexing(url);
        PageModel pageModel = workingWithDataService.getPageRepository().findByPath(pathPageNotNameSite);
        if (pageModel != null) {
            SiteModel siteModel = pageModel.getSite();
            workingWithDataService.updateLemmaFrequency(pageModel);
            workingWithDataService.getPageRepository().delete(pageModel);
            executor.execute(new Parser(siteModel, workingWithDataService, indexingProcessService));
            return true;
        }
        SiteModel siteModel = workingWithDataService.getSiteRepository().findByUrl(rootUrl);
        if (siteModel != null) {
            executor.execute(new Parser(siteModel, workingWithDataService, indexingProcessService));
            return true;
        }
        site = workingWithDataService.getSitesList().getSite(rootUrl);
        siteModel = workingWithDataService.createAndSaveSite(site);
        executor.execute(new Parser(siteModel, workingWithDataService, indexingProcessService));
        return true;
    }

    @Override
    public boolean stopIndexing() {
        if (isIndexingProcessRunning()) {
            indexingProcessService.setInterrupted(true);
            while (!indexingProcessService.getForkJoinPool().isTerminated()) {
                indexingProcessService.getForkJoinPool().shutdownNow();
            }
            return true;
        }
        log.info("Индексация не запущена");
        return false;
    }

    @Override
    public boolean urlValidator (String url) {
        String URL_REGEX ="^((((https?|ftps?|gopher|telnet|nntp)://)|(mailto:|news:))" +
                "(%[0-9A-Fa-f]{2}|[-()_.!~*';/?:@&=+$,A-Za-z0-9])+)" +
                "([).!';/?:,][[:blank:]])?$";
        Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
        if (url == null) {
            return false;
        }
        Matcher matcher = URL_PATTERN.matcher(url);
        return matcher.matches();
    }

    @Override
    public boolean isIndexingProcessRunning() {
        return indexingProcessService.isIndexingProcessRunning();
    }

    private void executeFullIndexing () {
        executor = Executors.newFixedThreadPool(processorCoreCount);
        for (Site site : workingWithDataService.getSitesList().getSites()) {
            SiteModel siteModel = workingWithDataService.createAndSaveSite(site);
            executor.execute(new Parser(siteModel, workingWithDataService, indexingProcessService));
        }
    }
}
