package searchengine.model;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "page")
public class PageModel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", columnDefinition = "INT NOT NULL")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SiteModel site;

    @Column(name = "path", columnDefinition = "TEXT NOT NULL, Index (path(512))")
    private String pathAddressWithoutSiteRoot;

    @Column(name = "code",nullable = false)
    private int codeHTTPResponse;

    @Column(name = "content",columnDefinition = "MEDIUMTEXT NOT NULL")
    private String contentHTMLCode;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "lemma", cascade = CascadeType.PERSIST)
    private List<LemmaModel> lemmaModels = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "pageId", cascade = CascadeType.PERSIST)
    private List<IndexModel> indexModels = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageModel pageModel = (PageModel) o;
        return Objects.equals(site, pageModel.site) && Objects.equals(pathAddressWithoutSiteRoot, pageModel.pathAddressWithoutSiteRoot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, pathAddressWithoutSiteRoot);
    }

    @Override
    public String toString() {
        return "PageModel{" +
                "site=" + site +
                ", pathPageNotNameSite='" + pathAddressWithoutSiteRoot + '\'' +
                ", codeHTTPResponse=" + codeHTTPResponse +
                '}';
    }
}
