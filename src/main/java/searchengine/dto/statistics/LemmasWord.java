package searchengine.dto.statistics;

import lombok.Data;
import searchengine.model.LemmaModel;

import java.util.List;

@Data
public class LemmasWord {
    private String lemma;
    private int frequency;
    private float rank;
    private List<LemmaModel> listLemmas;
}
