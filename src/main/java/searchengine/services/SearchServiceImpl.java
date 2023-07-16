package searchengine.services;

import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.LemmasWord;
import searchengine.dto.statistics.StatisticsPage;
import searchengine.dto.statistics.SearchResults;
import searchengine.dto.statistics.StatisticsSearch;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.parsers.LemmaFinder;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;

@Data
@Service
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final int percentageOfPagesForLemmaElimination = 50;

    private int countPagesInDB;

    @Override
    public SearchResults getStatistics(String query, String site, int offset, int limit) throws IOException {
        List<StatisticsSearch> statisticsSearches = search(query, site, limit);
        SearchResults searchResults = new SearchResults();
        searchResults.setData(statisticsSearches);
        searchResults.setResult(true);
        if (statisticsSearches == null) {
            searchResults.setCount(0);
        } else {
            searchResults.setCount(statisticsSearches.size());
        }
        return searchResults;
    }

    private List<StatisticsSearch> search(String query, String site, int limit) throws IOException {
        SiteModel siteModel = null;
        countPagesInDB = pageRepository.getCountRecords();
        if (!site.isEmpty()) {
            siteModel = siteRepository.findByUrl(site);
        }
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        Set<String> normalFormWordsQuery = lemmaFinder.getNormalFormWords(query);
        if (normalFormWordsQuery.isEmpty()) {
            return null;
        }
        List<LemmasWord> listSortedByFrequencyLemmasByWordFromDB = getSortedByFrequencyLemmasByWordFromDB(normalFormWordsQuery, siteModel);
        List<PageModel> listPages = getPagesSortedRelevance(listSortedByFrequencyLemmasByWordFromDB);
        if (listPages.isEmpty()) {
            return null;
        }
        List<StatisticsPage> list = getSortedStatisticsPageByRankPage(listSortedByFrequencyLemmasByWordFromDB, listPages);
        return getStatisticsSearch(list, query, limit);
    }

    private List<LemmasWord> getSortedByFrequencyLemmasByWordFromDB(Set<String> setLemmas, SiteModel siteModel) {
        List<LemmasWord> lemmas = new ArrayList<>();
        for (String word : setLemmas) {
            LemmasWord lemmasWord = new LemmasWord();
            List<LemmaModel> lemmaModels;
            if (siteModel == null) {
                lemmaModels = lemmaRepository.findAllByLemma(word);
            } else lemmaModels = lemmaRepository.findAllByLemmaAndSite(word, siteModel.getId());
            int countPagesLemma = 0;
            int frequency = 0;
            float rank = 0;
            for (LemmaModel lemma : lemmaModels) {
                frequency += lemma.getFrequency();
                List<IndexModel> indexModelList = lemma.getIndexModels();
                for (IndexModel index : indexModelList) {
                    if (index.getPageId().getCodeHTTPResponse() == 200) {
                        rank += index.getRank();
                        countPagesLemma++;
                    }
                }
            }
            double percentage = ((double) countPagesLemma / countPagesInDB) * 100;
            if (percentage < percentageOfPagesForLemmaElimination) {
                lemmasWord.setLemma(word);
                lemmasWord.setFrequency(frequency);
                lemmasWord.setListLemmas(lemmaModels);
                lemmasWord.setRank(rank);
                lemmas.add(lemmasWord);
            }
        }
        lemmas.sort(Comparator.comparingInt(LemmasWord::getFrequency));
        return lemmas;
    }

    private static List<PageModel> getPagesSortedRelevance(List<LemmasWord> listLemmas) {
        List<PageModel> listPages = new ArrayList<>();
        for (int i = 0; i < listLemmas.size(); i++) {
            if (i == 0) {
                listPages = getPagesByLemma(listLemmas, i);
                continue;
            }
            List<PageModel> pages = getPagesByLemma(listLemmas, i);
            listPages.removeIf(page -> !pages.contains(page));
        }
        return listPages;
    }

    private static List<StatisticsPage> getSortedStatisticsPageByRankPage(List<LemmasWord> listLemmas, List<PageModel> listPages) {
        List<StatisticsPage> pages = new ArrayList<>();
        for (PageModel page : listPages) {
            StatisticsPage statisticsPage = getPageStatistics(listLemmas, page);
            pages.add(statisticsPage);
        }
        pages.sort(Comparator.comparingInt(StatisticsPage::getRankPage).reversed());
        return pages;
    }

    private static List<StatisticsSearch> getStatisticsSearch(List<StatisticsPage> statisticsPage, String query, int limit) throws IOException {
        int maxRelevance = statisticsPage.get(0).getRankPage();
        List<StatisticsSearch> statisticsSearches = new ArrayList<>();
        int countResult = 0;
        for (StatisticsPage page : statisticsPage) {
            Document document = Jsoup.parse(page.getPageModel().getContentHTMLCode());
            StatisticsSearch statisticsSearch = new StatisticsSearch();
            statisticsSearch.setRelevance((float) page.getRankPage() / maxRelevance);
            statisticsSearch.setUri(page.getPageModel().getPathAddressWithoutSiteRoot().substring(1));
            statisticsSearch.setSite(page.getPageModel().getSite().getUrl());
            statisticsSearch.setSiteName(page.getPageModel().getSite().getName());
            statisticsSearch.setTitle(document.title());
            statisticsSearch.setSnippet(getSnippet(document.text(), query));
            statisticsSearches.add(statisticsSearch);
            countResult++;
            if (countResult == limit) {
                break;
            }
        }
        return statisticsSearches;
    }

    private static StatisticsPage getPageStatistics(List<LemmasWord> listLemmas, PageModel page) {
        StatisticsPage statisticsPage = new StatisticsPage();
        int rankPage = 0;
        for (LemmasWord list : listLemmas) {
            for (LemmaModel lemma : list.getListLemmas()) {
                for (IndexModel index : lemma.getIndexModels()) {
                    if (page.equals(index.getPageId())) {
                        rankPage += index.getRank();
                    }
                }
            }
        }
        statisticsPage.setRankPage(rankPage);
        statisticsPage.setPageModel(page);
        return statisticsPage;
    }

    private static List<PageModel> getPagesByLemma(List<LemmasWord> listLemmas, int lemma) {
        List<PageModel> listPages = new ArrayList<>();
        listLemmas.get(lemma).getListLemmas().forEach(lemmas -> {
            List<IndexModel> indexList = lemmas.getIndexModels();
            indexList.forEach(index -> listPages.add(index.getPageId()));
        });
        return listPages;
    }

    private static String getSnippet(String content, String query) throws IOException {
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        Set<String> lemmaList = lemmaFinder.getNormalFormWords(query);

        List<Integer> lemmaIndex = new ArrayList<>();
        StringBuilder result = new StringBuilder();

        for (String lemma : lemmaList) {
            lemmaIndex.addAll(findLemmaIndexInText(content, lemma));
        }
        Collections.sort(lemmaIndex);
        List<String> wordsList = getWordsFromContent(content, lemmaIndex);
        for (int i = 0; i < wordsList.size(); i++) {
            result.append(wordsList.get(i)).append("... ");
            if (i > 3) {
                break;
            }
        }
        return result.toString();
    }

    private static List<String> getWordsFromContent(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int nextPoint = i + 1;
            while (nextPoint < lemmaIndex.size() && lemmaIndex.get(nextPoint) - end > 0 && lemmaIndex.get(nextPoint) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(nextPoint));
                nextPoint += 1;
            }
            i = nextPoint - 1;
            String text = getWordsFromIndex(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private static String getWordsFromIndex(int start, int end, String content) {
        String word = content.substring(start, end);
        int prevPoint;
        int lastPoint;
        if (content.lastIndexOf(" ", start) != -1) {
            prevPoint = content.lastIndexOf(" ", start);
        } else prevPoint = start;
        if (content.indexOf(" ", end + 30) != -1) {
            lastPoint = content.indexOf(" ", end + 30);
        } else lastPoint = content.indexOf(" ", end);
        String text = content.substring(prevPoint, lastPoint);
        text = text.replaceAll(word, "<b>" + word + "</b>");
        return text;
    }

    private static List<Integer> findLemmaIndexInText(String content, String lemma) throws IOException {
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        List<Integer> lemmaIndexList = new ArrayList<>();
        String regex = "([^а-я\\s])";
        String[] elements = content.toLowerCase(Locale.ROOT).split("\\p{Punct}|\\s");
        int index = 0;
        for (String el : elements) {
            String replaceEl = el.replaceAll(regex, "");
            if (replaceEl.length() < 3) {
                index += el.length() + 1;
                continue;
            }
            Set<String> lemmas = lemmaFinder.getNormalFormWords(replaceEl);
            for (String lem : lemmas) {
                if (lem.equals(lemma)) {
                    lemmaIndexList.add(index);
                }
            }
            index += el.length() + 1;
        }
        return lemmaIndexList;
    }
}
