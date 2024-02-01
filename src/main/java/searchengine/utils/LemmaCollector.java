package searchengine.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class LemmaCollector {
    private RussianLuceneMorphology russianMorph;
    private EnglishLuceneMorphology englishMorph;

    public Map<String, Double> mapLemmasAndRanks(String text) {
        Map<String, Double> lemmasRanks = new HashMap<>();
        List<String> lemmas = new ArrayList<>();
        List<String> words = splitToWords(text);
        for (String word : words) {
            try {
                List<String> normalForms = detectAndGetNormalForms(word);
                lemmas.addAll(normalForms);
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage());
            }
        }
        for (String lemma : lemmas) {
            double rank = 1;
            if (lemmasRanks.containsKey(lemma)) {
                rank += lemmasRanks.get(lemma);
            }
            lemmasRanks.put(lemma, rank);
        }
        return lemmasRanks;
    }

    public Map<String, List<String>> mapWordsAndLemmas(String text) {
        Map<String, List<String>> wordsLemmas = new HashMap<>();
        List<String> words = splitToWords(text);
        for (String word : words) {
            try {
                List<String> normalForms = detectAndGetNormalForms(word);
                wordsLemmas.put(word, normalForms);
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage());
            }
        }
        return wordsLemmas;
    }

    private List<String> splitToWords(String text) {
        String[] words = text.split("[^А-Яа-яA-Za-z]+");
        List<String> legalWords = new ArrayList<>();
        for (String word : words) {
            if (word.length() > 1 && !isFunctional(word)) {
                legalWords.add(word);
            }
        }
        return legalWords;
    }

    private boolean isFunctional(String word) {
        if (word.matches("[А-Яа-я]+")) {
            List<String> morphInfo = russianMorph.getMorphInfo(word.toLowerCase());
            String[] functionalTypes = {"СОЮЗ", "ПРЕДЛ", "МЕЖД", "ЧАСТ"};
            for (String type : functionalTypes) {
                if (morphInfo.get(0).contains(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> detectAndGetNormalForms(String word) {
        List<String> normalForms;
        if (word.matches("[А-Яа-я]+")) {
            normalForms = russianMorph.getNormalForms(word.toLowerCase());
        } else if (word.matches("[A-Za-z]+")) {
            normalForms = englishMorph.getNormalForms(word.toLowerCase());
        } else {
            throw new IllegalArgumentException("\'" + word + "\' is not correct english or russian word!");
        }
        return normalForms;
    }

    @PostConstruct
    private void postConstruct() throws IOException {
        russianMorph = new RussianLuceneMorphology();
        englishMorph = new EnglishLuceneMorphology();
    }
}
