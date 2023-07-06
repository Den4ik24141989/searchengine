package searchengine.services;

import org.springframework.stereotype.Service;

@Service
public interface IndexingService {
    boolean startFullIndexing();
    boolean stopIndexing();
    boolean addOrUpdatePage(String url);
    boolean isIndexingProcessRunning();
    boolean urlValidator(String url);
}
