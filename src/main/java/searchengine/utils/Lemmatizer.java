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

        String[] words = text.split("[^A-Za-z'А-Яа-яЁё]+");

        for (String word : words) {
            if (word.contains("'")) continue;

            word = word.toLowerCase();

            if (word.matches("[А-Яа-яЁё]+")) {
                if (isRussianStopword(word)) continue;

                List<String> normalForms = russianMorph.getNormalForms(word);
                lemmas.addAll(normalForms);
            }

            if (word.matches("[A-Za-z]+")) {
                if (isEnglishStopword(word)) continue;

                List<String> normalForms = englishMorph.getNormalForms(word);
                lemmas.addAll(normalForms);
            }
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
}
