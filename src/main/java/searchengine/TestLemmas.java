package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestLemmas {
    public static void main(String[] args) throws IOException {
        String text = Files.readString(Path.of("res/test.txt"));
        String[] words = text.split("[^А-Яа-я]+");
        List<String> legalWords = new ArrayList<>();
        for (String word : words) {
            if (word.length() > 1) {
                legalWords.add(word.toLowerCase());
            }
        }

        HashMap<String, Integer> lemmas = new HashMap<>();

        LuceneMorphology luceneMorph;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String word : legalWords) {
            List<String> wordMorphInfo = luceneMorph.getMorphInfo(word);
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

            List<String> wordBaseForms = luceneMorph.getNormalForms(word);
            System.out.println(wordBaseForms);
        }
    }
}
