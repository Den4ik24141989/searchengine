package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.parsers.Parser;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final IndexingProcessService indexingProcessService;
    private final int processorCoreCount = Runtime.getRuntime().availableProcessors();
    private Executor executor;

    @Override
    public boolean startFullIndexing() {
        if (!isIndexingProcessRunning()) {
            indexingProcessService.enableFullIndexing();
            siteRepository.deleteAll();
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
        String pathAddressWithoutSiteRoot = url.replaceAll(rootUrl, "/");
        Site site = new Site();
        site.setUrl(rootUrl);
        if (!sitesList.getSites().contains(site)) {
            log.info(url + " - данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return false;
        }
        executor = Executors.newSingleThreadExecutor();
        indexingProcessService.enableSingleIndexing(url);
        PageModel pageModel = pageRepository.findByPath(pathAddressWithoutSiteRoot);
        if (pageModel != null) {
            SiteModel siteModel = pageModel.getSite();
            updateLemmaFrequency(pageModel);
            pageRepository.delete(pageModel);
            executeParser(siteModel);
            return true;
        }
        SiteModel siteModel = siteRepository.findByUrl(rootUrl);
        if (siteModel != null) {
            executeParser(siteModel);
            return true;
        }
        site = sitesList.getSite(rootUrl);
        siteModel = createAndSaveSite(site);
        executeParser(siteModel);
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
        for (Site site : sitesList.getSites()) {
            SiteModel siteModel = createAndSaveSite(site);
            executeParser(siteModel);
        }
    }

    private void executeParser (SiteModel siteModel) {
        executor.execute(new Parser(siteModel, siteRepository, pageRepository,
                lemmaRepository, indexRepository, indexingProcessService));
    }

    public void updateLemmaFrequency (PageModel pageModel) {
        List<IndexModel> listIndex = indexRepository.findAllByPageId(pageModel.getId());
        List <LemmaModel> listLemmaUpdate = new ArrayList<>(listIndex.size());
        for (IndexModel index : listIndex) {
            LemmaModel lemmaModel = lemmaRepository.findById(index.getLemmaId().getId());
            lemmaModel.setFrequency(lemmaModel.getFrequency() - 1);
            listLemmaUpdate.add(lemmaModel);
        }
        lemmaRepository.saveAll(listLemmaUpdate);
    }

    public SiteModel createAndSaveSite(Site site) {
        SiteModel siteModel = new SiteModel();
        siteModel.setName(site.getName());
        siteModel.setStatus(StatusSiteModel.INDEXING);
        siteModel.setStatusTime(LocalDateTime.now());
        siteModel.setUrl(site.getUrl());
        siteRepository.save(siteModel);
        return siteModel;
    }
}
