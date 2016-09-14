import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.tokensregex.types.Tags;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.international.arabic.ArabicHeadFinder;
import sun.misc.ASCIICaseInsensitiveComparator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.lang.System.out;

public class Main {

    public static void main(String[] args) throws IOException {

        String modelFile = "../resources/models/english-left3words-distsim.tagger";
        String fileToTag = "../resources/samples_for_tests/book_of_the_stranger_full.txt";
        MaxentTagger tagger = new MaxentTagger(modelFile);

        List<File> files;
        try {
            files = Files.walk(Paths.get("../resources/episodes")).filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            out.println("O caminho para a pasta com os textos está errado.");
            return;
        }

        List<List<HasWord>> sentences = new ArrayList<>();
        for(File file: files) {
            out.println("Lendo arquivo " + file.getName());
            FileReader reader = new FileReader(file);
            BufferedReader bufReader = new BufferedReader(reader);
            List<String> lines = bufReader
                    .lines()
                    .filter(line -> !line.trim().isEmpty())
                    .collect(Collectors.toList());

            for (String line : lines) {
                List<List<HasWord>> tokenizedSentence = MaxentTagger.tokenizeText(new StringReader(line));
                sentences.add(tokenizedSentence.get(0));
            }
        }

        List<List<TaggedWord>> taggedSentences = tagger.process(sentences);

        ArrayList<String> foundNamedEntities = new ArrayList<>();
        for (List<TaggedWord> taggedSentence : taggedSentences) {
            ArrayList<TaggedWord> namedEntityWords = new ArrayList<>();
            for (int i = 0; i < taggedSentence.size(); i++) {
                TaggedWord taggedWord = taggedSentence.get(i);
                String word = taggedWord.word();

                if (word.endsWith("Edit")) {
                    word = word.substring(0, word.length() - 4);
                    if (word.length() == 0) continue;
                    taggedWord.setWord(word);
                }

                if (word.substring(0, 1).matches("^[A-Z]")) {
                    if (i == 0 && !(taggedWord.tag().equals("NNP") || taggedWord.tag().equals("NNPS"))) {
                        continue;
                    }
                    namedEntityWords.add(taggedWord);
                } else if(!namedEntityWords.isEmpty() && isComplement(taggedWord)){
                    namedEntityWords.add(taggedWord);
                } else {
                    if (!namedEntityWords.isEmpty()) {
                        if (hasNNP(namedEntityWords)) {

                            if(isComplement(namedEntityWords.get(namedEntityWords.size() - 1))){
                                namedEntityWords.remove(namedEntityWords.size() - 1);
                            }

                            debug(taggedSentence, namedEntityString(namedEntityWords));
                            foundNamedEntities.add(namedEntityString(namedEntityWords));
                        }

                        namedEntityWords.clear();
                    }
                }
            }
            if(!namedEntityWords.isEmpty()){
                debug(taggedSentence, namedEntityString(namedEntityWords));

                foundNamedEntities.add(namedEntityString(namedEntityWords));
            }
        }

        SortedSet<String> sortedNamedEntities = new TreeSet<>(new ASCIICaseInsensitiveComparator());
        sortedNamedEntities.addAll(foundNamedEntities);
        sortedNamedEntities.forEach(System.out::println);
        out.println(sortedNamedEntities.size());

        Path outputCSVPath = Paths.get("../resources/named_entities.csv");
        Files.deleteIfExists(outputCSVPath);
        FileWriter f = new FileWriter(outputCSVPath.toString());
        for(String namedEntity: sortedNamedEntities){
            f.write(namedEntity);
            f.write(System.lineSeparator());
        }
    }

    private static boolean isComplement(TaggedWord taggedWord) {
        List<String> allowed = Arrays.asList("of", "the", "'s");
        return allowed.contains(taggedWord.word());
    }

    private static boolean hasNNP(List<TaggedWord> namedEntityWords) {
        for(TaggedWord taggedWord : namedEntityWords){
            if(taggedWord.tag().equals("NNP") || taggedWord.tag().equals("NNPS")) return true;
        }

        return false;
    }

    private static String namedEntityString(List<TaggedWord> namedEntityWords){
        String result = "";
        for(TaggedWord taggedWord : namedEntityWords){
            result += " " + taggedWord.word();
        }
        return result.substring(1); // remove beggining whitespace
    }

    // Printa a entidade com a frase onde ela foi achada.
    private static void debug(List<TaggedWord> taggedSentence, String compoundNNP) {
//        out.print(compoundNNP);
//        out.print(" (");
//        out.print(Arrays.toString(taggedSentence.stream().map(TaggedWord::toString).toArray()));
//        out.print(") ");
//        out.println();
    }
}
