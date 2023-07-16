package searchengine.parsers;

import lombok.extern.slf4j.Slf4j;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.springframework.http.HttpStatus;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.connection.Connection;
import searchengine.model.StatusSiteModel;
import searchengine.services.IndexingProcessService;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

import java.time.LocalDateTime;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

@Slf4j
public class ParserURL extends RecursiveAction {
    private final NodeUrl node;
    private final IndexingProcessService indexingProcessService;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteModel siteModel;

    public ParserURL(SiteRepository siteRepository, PageRepository pageRepository,
                     LemmaRepository lemmaRepository, IndexRepository indexRepository,
                     NodeUrl node, IndexingProcessService indexingProcessService) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.node = node;
        this.indexingProcessService = indexingProcessService;
        siteModel = indexingProcessService.getSite(node.getRootElement().getUrl());
    }

    @Override
    protected void compute() {
        int random = (int) ((Math.random() * 1000) + 500);
        try {
            sleep(random);
            Connection connection = new Connection(node.getUrl());
            Document document = connection.getConnection().get();
            String pathAddressWithoutSiteRoot = setPathAddressWithoutSiteRoot(node);
            PageModel pageModel = createPageModel(document, pathAddressWithoutSiteRoot, siteModel);

            if (!indexingProcessService.repeatPage(pageModel)) {
                pageRepository.save(pageModel);
                updateTimeAndSaveSite(siteModel);
                new LemmaAndIndexCreator(lemmaRepository, indexRepository, pageModel);
            }

            Elements elements = document.select("a[href]");

            for (Element element : elements) {
                String childUrl = element.absUrl("href")
                        .replaceAll("\\?.+", "");
                if (isCorrectUrl(childUrl)) {
                    node.addChild(new NodeUrl(childUrl));
                }
            }
        } catch (InterruptedException ignored) {
            savePageInterruptedException(node, siteModel, indexingProcessService);
        } catch (Exception e) {
            savePageException(node, siteModel, e);
            siteModel.setLastError(e.getMessage());
            if (node.getUrl().equals(node.getRootElement().getUrl())) {
                siteModel.setStatus(StatusSiteModel.FAILED);
            }
            updateTimeAndSaveSite(siteModel);
            log.info(siteModel.getUrl() + " " + e.getMessage());
        }
        if (!indexingProcessService.isInterrupted()) {
            addTasks(node);
        }
    }

    private void addTasks(NodeUrl node) {
        for (NodeUrl child : node.getChildren()) {
            ParserURL task = new ParserURL( siteRepository, pageRepository,lemmaRepository, indexRepository, child, indexingProcessService);
            task.compute();
        }
    }

    private boolean isCorrectUrl(String url) {
        Pattern patternRoot = Pattern.compile("^" + node.getUrl());
        Pattern patternNotFile = Pattern.compile("(\\S+(\\.(?i)(jpg|png|gif|bmp|pdf))$)");
        Pattern patternNotAnchor = Pattern.compile("#([\\w\\-]+)?$");

        return patternRoot.matcher(url).lookingAt()
                && !patternNotFile.matcher(url).find()
                && !patternNotAnchor.matcher(url).find();
    }

    private void updateTimeAndSaveSite(SiteModel siteModel) {
        siteModel.setStatusTime(LocalDateTime.now());
        this.siteRepository.save(siteModel);
    }

    private synchronized void savePageInterruptedException(NodeUrl node, SiteModel siteModel, IndexingProcessService indexingProcessService) {
        String pathAddressWithoutSiteRoot = setPathAddressWithoutSiteRoot(node);
        PageModel pageModel = new PageModel();
        pageModel.setSite(siteModel);
        pageModel.setCodeHTTPResponse(HttpStatus.NO_CONTENT.value());
        pageModel.setContentHTMLCode("индексация остановлена пользователем");
        pageModel.setPathAddressWithoutSiteRoot(pathAddressWithoutSiteRoot);
        if (!indexingProcessService.repeatPage(pageModel)) {
            pageRepository.save(pageModel);
        }
    }

    private synchronized void savePageException(NodeUrl node, SiteModel siteModel, Exception e) {
        PageModel pageModel = new PageModel();
        pageModel.setSite(siteModel);
        pageModel.setPathAddressWithoutSiteRoot(setPathAddressWithoutSiteRoot(node));
        pageModel.setCodeHTTPResponse(HttpStatus.NOT_FOUND.value());
        pageModel.setContentHTMLCode(e.getMessage());
        pageRepository.save(pageModel);
    }

    private static String setPathAddressWithoutSiteRoot(NodeUrl node) {
        if (node.getUrl().equals(node.getRootElement().getUrl()) || node.getUrl().equals(node.getRootElement().getUrl() + "/")) {
            return "/";
        }
        if (node.getUrl().endsWith("/")) {
            String path = node.getUrl().substring(0, node.getUrl().length() - 1);
            return path.replaceAll(node.getRootElement().getUrl(), "/");
        }
        return node.getUrl().replaceAll(node.getRootElement().getUrl(), "/");
    }

    private static PageModel createPageModel(Document document, String url, SiteModel siteModel) {
        String content = document.outerHtml();
        PageModel pageModel = new PageModel();
        pageModel.setPathAddressWithoutSiteRoot(url);
        pageModel.setSite(siteModel);
        pageModel.setContentHTMLCode(content);
        pageModel.setCodeHTTPResponse(document.connection().response().statusCode());
        return pageModel;
    }
}
