package searchengine.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Component
public class LemmaCollector {
    private LuceneMorphology morphology;

    public HashMap<String, Double> collect(String text) {
        HashMap<String, Double> lemmaRanks = new HashMap<>();

        String[] words = text.split("[^А-Яа-я]+");
        List<String> legalWords = new ArrayList<>();
        for (String word : words) {
            if (word.length() > 1) {
                legalWords.add(word.toLowerCase());
            }
        }

        List<String> lemmas = new ArrayList<>();
        for (String word : legalWords) {
            List<String> wordMorphInfo = morphology.getMorphInfo(word);
            String[] functionalTypes = {"СОЮЗ", "ПРЕДЛ", "МЕЖД", "ЧАСТ"};
            boolean isFunctional = false;
            for (String type : functionalTypes) {
                if (wordMorphInfo.get(0).contains(type)) {
                    isFunctional = true;
                    break;
                }
            }
            if (isFunctional) {
                continue;
            }

            List<String> wordBaseForms = morphology.getNormalForms(word);
            lemmas.addAll(wordBaseForms);
        }

        for (String lemma : lemmas) {
            if (lemmaRanks.containsKey(lemma)) {
                lemmaRanks.put(lemma, lemmaRanks.get(lemma) + 1);
            } else {
                lemmaRanks.put(lemma, 1D);
            }
        }

        return lemmaRanks;
    }

    @PostConstruct
    private void postConstruct() throws IOException {
        morphology = new RussianLuceneMorphology();
    }
}
