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
import java.util.Map;

@Slf4j
@Component
public class LemmaCollector {
    private LuceneMorphology morphology;

    public Map<String, Double> mapLemmasAndRanks(String text) {
        Map<String, Double> lemmaRanks = new HashMap<>();
        List<String> lemmas = new ArrayList<>();
        List<String> words = splitToWords(text);
        for (String word : words) {
            List<String> normalForms = morphology.getNormalForms(word);
            lemmas.addAll(normalForms);
        }
        for (String lemma : lemmas) {
            double rank = 1;
            if (lemmaRanks.containsKey(lemma)) {
                rank += lemmaRanks.get(lemma);
            }
            lemmaRanks.put(lemma, rank);
        }
        return lemmaRanks;
    }

    public Map<String, List<String>> mapWordsAndLemmas(String text) {
        Map<String, List<String>> wordLemmas = new HashMap<>();
        List<String> words = splitToWords(text);
        for (String word : words) {
            List<String> normalForms = morphology.getNormalForms(word);
            wordLemmas.put(word, normalForms);
        }
        return wordLemmas;
    }

    private List<String> splitToWords(String text) {
        String[] words = text.split("[^А-Яа-я]+");
        List<String> legalWords = new ArrayList<>();
        for (String word : words) {
            word = word.toLowerCase();
            if (word.length() > 1 && !isFunctional(word)) {
                legalWords.add(word);
            }
        }
        return legalWords;
    }

    private boolean isFunctional(String word) {
        List<String> morphInfo = morphology.getMorphInfo(word);
        String[] functionalTypes = {"СОЮЗ", "ПРЕДЛ", "МЕЖД", "ЧАСТ"};
        for (String type : functionalTypes) {
            if (morphInfo.get(0).contains(type)) {
                return true;
            }
        }
        return false;
    }

    @PostConstruct
    private void postConstruct() throws IOException {
        morphology = new RussianLuceneMorphology();
    }
}
