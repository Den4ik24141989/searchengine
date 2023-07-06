package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.parsers.LemmaAndIndexCreator;
import searchengine.parsers.NodeUrl;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Getter
@Service
public class WorkingWithDataService {
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public SiteModel createAndSaveSite(Site site) {
        SiteModel siteModel = new SiteModel();
        siteModel.setName(site.getName());
        siteModel.setStatus(StatusSiteModel.INDEXING);
        siteModel.setStatusTime(LocalDateTime.now());
        siteModel.setUrl(site.getUrl());
        siteRepository.save(siteModel);
        return siteModel;
    }

    public void updateTimeAndSaveSite(SiteModel siteModel) {
        siteModel.setStatusTime(LocalDateTime.now());
        this.siteRepository.save(siteModel);
    }

    public synchronized void savePageInterruptedException(NodeUrl node, SiteModel siteModel, IndexingProcessService indexingProcessService) {
        String pathPageNotNameSite = setPathPageNotNameSite(node);
        PageModel pageModel = new PageModel();
        pageModel.setSite(siteModel);
        pageModel.setCodeHTTPResponse(HttpStatus.NO_CONTENT.value());
        pageModel.setContentHTMLCode("индексация остановлена пользователем");
        pageModel.setPathPageNotNameSite(pathPageNotNameSite);
        if (!indexingProcessService.repeatPage(pageModel)) {
            pageRepository.save(pageModel);
        }
    }

    public synchronized void savePageException(NodeUrl node, SiteModel siteModel, Exception e) {
        PageModel pageModel = new PageModel();
        pageModel.setSite(siteModel);
        pageModel.setPathPageNotNameSite(setPathPageNotNameSite(node));
        pageModel.setCodeHTTPResponse(HttpStatus.NOT_FOUND.value());
        pageModel.setContentHTMLCode(e.getMessage());
        pageRepository.save(pageModel);
    }

    public void saveLemmasAndIndexes(PageModel pageModel) throws IOException {
        new LemmaAndIndexCreator(lemmaRepository, indexRepository, pageModel);
    }

    public PageModel createPageModel(Document document, String url, SiteModel siteModel) {
        String content = document.outerHtml();
        PageModel pageModel = new PageModel();
        pageModel.setPathPageNotNameSite(url);
        pageModel.setSite(siteModel);
        pageModel.setContentHTMLCode(content);
        pageModel.setCodeHTTPResponse(document.connection().response().statusCode());
        return pageModel;
    }

    public String setPathPageNotNameSite(NodeUrl node) {
        if (node.getUrl().equals(node.getRootElement().getUrl()) || node.getUrl().equals(node.getRootElement().getUrl() + "/")) {
            return "/";
        }
        if (node.getUrl().endsWith("/")) {
            String path = node.getUrl().substring(0, node.getUrl().length() - 1);
            return path.replaceAll(node.getRootElement().getUrl(), "/");
        }
        return node.getUrl().replaceAll(node.getRootElement().getUrl(), "/");
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

    public void clearDB() {
        siteRepository.deleteAll();
    }
}
