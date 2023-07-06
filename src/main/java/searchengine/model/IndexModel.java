package searchengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@EqualsAndHashCode
@Table(name = "`index_search`")
public class IndexModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false, foreignKey = @ForeignKey(name = "LEMMA_ID_FK"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LemmaModel lemmaId;

    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false, foreignKey = @ForeignKey(name = "PAGE_ID_FK"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PageModel pageId;

    @Column(name = "`rank`", nullable = false)
    private int rank;

    @Override
    public String toString() {
        return "IndexModel{" +
                ", lemmaId=" + lemmaId.getLemma() +
                ", rank=" + rank +
                "page" + pageId.getSite().getUrl() + pageId.getPathPageNotNameSite() +
                '}';
    }
}
