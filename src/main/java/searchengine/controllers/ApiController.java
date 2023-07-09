package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.BadRequest;
import searchengine.dto.statistics.Response;
import searchengine.dto.statistics.SearchResults;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;
    private final String[] errors = {
            "Индексация уже запущена",
            "Некорректная ссылка",
            "Данная страница находится за пределами сайтов, указанных в конфигурационном файле",
            "Индексация не запущена",
            "Задан пустой поисковый запрос",
            "Указанная страница не найдена"
    };
    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        if (indexingService.startFullIndexing()) {
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        }
        return new ResponseEntity<>(new BadRequest(false, errors[0]),
                HttpStatus.BAD_REQUEST);
    }
    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(String url) {
        if (indexingService.isIndexingProcessRunning()) {
            log.info(errors[0]);
            return new ResponseEntity<>(new BadRequest(false, errors[0]),
                    HttpStatus.BAD_REQUEST);
        }
        if (!indexingService.urlValidator(url)) {
            log.info(errors[1]);
            return new ResponseEntity<>(new BadRequest(false, errors[1]),
                    HttpStatus.BAD_REQUEST);
        }
        if (indexingService.addOrUpdatePage(url)) {
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        }
        else return new ResponseEntity<>(new BadRequest(false, errors[2]),
                HttpStatus.BAD_REQUEST);
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        if (indexingService.stopIndexing()) {
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        }
        else return new ResponseEntity<>(new BadRequest(false, errors[3]),
                HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/search")
    public ResponseEntity <Object> search(@RequestParam(name = "query", required = false, defaultValue = "")
                                          String query, @RequestParam(name = "site", required = false, defaultValue = "") String site,
                                          @RequestParam(name = "offset", required = false, defaultValue = "0") int offset,
                                          @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) throws IOException {
        if (query.isEmpty()) {
            log.info(errors[4]);
            return new ResponseEntity<>(new BadRequest(false, errors[4]),
                    HttpStatus.BAD_REQUEST);
        }
        SearchResults searchResults = searchService.getStatistics(query, site, offset, limit);
        if (searchResults == null) {
            log.info(errors[5] + " - " + query);
            return new ResponseEntity<>(new BadRequest(false, errors[5]),
                    HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(searchResults);
    }
}
