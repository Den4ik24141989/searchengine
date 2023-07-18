package searchengine.parsers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LemmaAndIndexCreator {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageModel pageModel;

    public LemmaAndIndexCreator(LemmaRepository lemmaRepository, IndexRepository indexRepository, PageModel pageModel) throws IOException {
        this.lemmaRepository = lemmaRepository;
        this.pageModel = pageModel;
        this.indexRepository = indexRepository;
        createAndSaveLemmasAndIndexes();
    }

    private void createAndSaveLemmasAndIndexes() throws IOException {
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        List<IndexModel> indexes = new ArrayList<>();
        Document document = Jsoup.parse(pageModel.getContentHTMLCode());
        HashMap<String, Integer> collectLemmas = lemmaFinder.collectLemmas(document.text());
        for (String word : collectLemmas.keySet()) {
            LemmaModel lemmaModel = saveLemma(word);
            int rank = collectLemmas.get(word);
            indexes.add(createIndex(lemmaModel, pageModel, rank));
        }
        indexRepository.saveAll(indexes);
    }
    private LemmaModel saveLemma(String word) {
        LemmaModel lemmaModel = lemmaRepository.findAllByLemmaAndSiteId(word, pageModel.getSite().getId());
        if (lemmaModel == null) {
            lemmaModel = createLemma(word, pageModel);
            lemmaRepository.save(lemmaModel);
            return lemmaModel;
        }
        lemmaModel.setFrequency(lemmaModel.getFrequency() + 1);
        lemmaRepository.save(lemmaModel);
        return lemmaModel;
    }
    private static LemmaModel createLemma (String word, PageModel pageModel) {
        LemmaModel lemmaModel = new LemmaModel();
        lemmaModel.setLemma(word);
        lemmaModel.setFrequency(1);
        lemmaModel.setSite(pageModel.getSite());
        return lemmaModel;
    }

    private static IndexModel createIndex(LemmaModel lemmaModel, PageModel pageModel, int rank) {
        IndexModel indexModel = new IndexModel();
        indexModel.setRank(rank);
        indexModel.setLemmaId(lemmaModel);
        indexModel.setPageId(pageModel);
        return indexModel;
    }
}
