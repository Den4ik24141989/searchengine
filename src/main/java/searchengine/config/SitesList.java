package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesList {
    private List<Site> sites;

    public List<Site> getSites() {
        sites.forEach(site -> site.setUrl(addEndSlash(site.getUrl())));
        return sites;
    }
    public Site getSite(String rootUrl) {
        Site site = null;
        for (Site site1 : sites) {
            if (site1.getUrl().equals(rootUrl)) {
                site = site1;
            }
        }
        return site;
    }
    private String addEndSlash(String url) {
        if (url.endsWith("/")) {
            return url;
        }
        return url + "/";
    }
}
