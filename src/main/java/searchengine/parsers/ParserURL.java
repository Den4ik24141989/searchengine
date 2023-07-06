package searchengine.parsers;

import lombok.extern.slf4j.Slf4j;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import searchengine.сonnection.Connection;
import searchengine.model.StatusSiteModel;
import searchengine.services.IndexingProcessService;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.services.WorkingWithDataService;

import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

@Slf4j
public class ParserURL extends RecursiveAction {
    private final NodeUrl node;
    private final WorkingWithDataService workingWithDataService;
    private final IndexingProcessService indexingProcessService;
    private final SiteModel siteModel;

    public ParserURL(NodeUrl node, WorkingWithDataService workingWithDataService, IndexingProcessService indexingProcessService) {
        siteModel = indexingProcessService.getSite(node.getRootElement().getUrl());
        this.node = node;
        this.workingWithDataService = workingWithDataService;
        this.indexingProcessService = indexingProcessService;
    }

    @Override
    protected void compute() {
        int random = (int) ((Math.random() * 1000) + 500);
        try {
            sleep(random);
            Connection connection = new Connection(node.getUrl());
            Document document = connection.getConnection().get();
            String pathPageNotNameSite = workingWithDataService.setPathPageNotNameSite(node);
            PageModel pageModel = workingWithDataService.createPageModel(document, pathPageNotNameSite, siteModel);

            if (!indexingProcessService.repeatPage(pageModel)) {
                workingWithDataService.getPageRepository().save(pageModel);
                workingWithDataService.updateTimeAndSaveSite(siteModel);
                workingWithDataService.saveLemmasAndIndexes(pageModel);
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
            workingWithDataService.savePageInterruptedException(node, siteModel, indexingProcessService);
        } catch (Exception e) {
            workingWithDataService.savePageException(node, siteModel, e);
            siteModel.setLastError(e.getMessage());
            if (node.getUrl().equals(node.getRootElement().getUrl())) {
                siteModel.setStatus(StatusSiteModel.FAILED);
            }
            workingWithDataService.updateTimeAndSaveSite(siteModel);
            log.info(siteModel.getUrl() + " " + e.getMessage());
        }
        addTasks(node);
    }

    private void addTasks(NodeUrl node) {
        if (!indexingProcessService.isInterrupted() && indexingProcessService.isFullIndexing()) {
            for (NodeUrl child : node.getChildren()) {
                ParserURL task = new ParserURL(child, workingWithDataService, indexingProcessService);
                task.compute();
            }
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
}
