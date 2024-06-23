package searchengine.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
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
public class Lemmatizer {
    private RussianLuceneMorphology russianMorph;
    private EnglishLuceneMorphology englishMorph;

    public Map<String, Double> buildLemmaRankMap(String text) {
        List<String> lemmas = new ArrayList<>();

        List<String> words = splitToWords(text);
        for (String word : words) {
            List<String> normalForms = filterAndGetNormalForms(word);
            if (normalForms != null) lemmas.addAll(normalForms);
        }

        Map <String, Double> lemmaRankMap = new HashMap<>();

        for (String lemma : lemmas) {
            double rank = 1;
            if (lemmaRankMap.containsKey(lemma))
                rank += lemmaRankMap.get(lemma);

            lemmaRankMap.put(lemma, rank);
        }

        return lemmaRankMap;
    }

    private boolean isRussianStopword(String word) {
        boolean isStopword = false;

        List<String> stopwordTypes = List.of("СОЮЗ", "ПРЕДЛ", "ЧАСТ", "МС");
        List<String> morphInfo = russianMorph.getMorphInfo(word);

        for (String item : morphInfo) {
            for (String type : stopwordTypes) {
                if (item.contains(type)) {
                    isStopword = true;
                    break;
                }
            }
        }

        return isStopword;
    }

    private boolean isEnglishStopword(String word) {
        boolean isStopword = false;

        List<String> stopwordTypes = List.of("ARTICLE", "PREP", "PRON", "PN", "VBE");
        List<String> morphInfo = englishMorph.getMorphInfo(word);

        for (String item : morphInfo) {
            for (String type : stopwordTypes) {
                if (item.contains(type)) {
                    isStopword = true;
                    break;
                }
            }
        }

        return isStopword;
    }

    @PostConstruct
    private void postConstruct() throws IOException {
        russianMorph = new RussianLuceneMorphology();
        englishMorph = new EnglishLuceneMorphology();
    }

    public Map<String, List<String>> buildWordLemmasMap(String text) {
        Map<String, List<String>> wordLemmasMap = new HashMap<>();
        List<String> words = splitToWords(text);
        for (String word : words) {
            List<String> normalForms = filterAndGetNormalForms(word);
            if (normalForms != null) wordLemmasMap.put(word, normalForms);
        }
        return wordLemmasMap;
    }

    private List<String> splitToWords(String text) {
        String[] words = text.split("[^A-Za-z'А-Яа-яЁё]+");
        List<String> legalWords = new ArrayList<>();
        for (String word : words) {
            if (word.length() < 2 || word.contains("'")) continue;
            legalWords.add(word);
        }
        return legalWords;
    }

    private List<String> filterAndGetNormalForms(String word) {
        List<String> normalForms = null;
        word = word.toLowerCase();

        if (word.matches("[А-Яа-яЁё]+")) {
            if (isRussianStopword(word)) return null;

            normalForms = russianMorph.getNormalForms(word);
            normalForms = normalForms.stream().map(eWord -> eWord.replace("ё", "е")).toList();
        }

        if (word.matches("[A-Za-z]+")) {
            if (isEnglishStopword(word)) return null;

            normalForms = englishMorph.getNormalForms(word);
        }
        return normalForms;
    }
}
