package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LemmaCollector {
    public static List<String> extractLemmas(String text) throws IOException {
        List<String> lemmas = new ArrayList<>();

        String[] words = text.split("[^А-Яа-я]+");
        List<String> legalWords = new ArrayList<>();
        for (String word : words) {
            if (word.length() > 1) {
                legalWords.add(word.toLowerCase());
            }
        }

        LuceneMorphology morphology = new RussianLuceneMorphology();
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
        return lemmas;
    }
}
