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
@Table(name = "lemma")
public class LemmaModel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", columnDefinition = "INT NOT NULL")
    @OnDelete(action = OnDeleteAction.CASCADE)                //каскадное удаление в БД
    private SiteModel site;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "lemmaId", cascade = CascadeType.PERSIST)
    private List<IndexModel> indexModels = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LemmaModel that = (LemmaModel) o;
        return Objects.equals(site, that.site) && Objects.equals(lemma, that.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, lemma);
    }

    @Override
    public String toString() {
        return "LemmaModel{" +
                "lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                '}';
    }
}
