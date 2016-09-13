import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.tokensregex.types.Tags;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.international.arabic.ArabicHeadFinder;
import sun.misc.ASCIICaseInsensitiveComparator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.lang.System.out;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {

        String modelFile = "../resources/models/english-left3words-distsim.tagger";
        String fileToTag = "../resources/samples_for_tests/book_of_the_stranger_full.txt";
        MaxentTagger tagger = new MaxentTagger(modelFile);

//        List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new BufferedReader(new FileReader(fileToTag)));
//        List<List<TaggedWord>> taggedSentences = tagger.process(sentences);

        //ArrayList<String> lines = new ArrayList<>();
        FileReader reader = new FileReader(fileToTag);
        BufferedReader bufReader = new BufferedReader(reader);
        List<String> lines = bufReader.lines().filter(line -> !line.isEmpty()).collect(Collectors.toList());
        //List<String> lines = bufReader.lines().collect(Collectors.toList());

        List<List<HasWord>> sentences = new ArrayList<>();
        for(String line: lines){
            List<List<HasWord>> tokenizedSentence = MaxentTagger.tokenizeText(new StringReader(line));
            sentences.add(tokenizedSentence.get(0));
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

                //if(taggedWord.tag().equals("NNP") || taggedWord.tag().equals("NNPS")){
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
    }

    private static boolean isComplement(TaggedWord taggedWord) {
        List<String> allowed = Arrays.asList("of", "the", "'s", "with");
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

    // Fazia a comparação para ignorar a diferença entre, por exemplo, Starks e Stark. Mas isso não
    // faz muito sentido se considerarmos Starks como sendo o nome próprio da família e Stark um sobrenome.
    private static class PluralInsensitiveComparator implements Comparator<String> {

        ASCIICaseInsensitiveComparator defaultComparator = new ASCIICaseInsensitiveComparator();

        @Override
        public int compare(String o1, String o2) {
            if(o1.equals(o2.substring(0, o2.length()-1)) || o2.equals(o1.substring(0, o1.length()-1))){
                if(o2.endsWith("s") && o2.length() > o1.length()){
                    return 0;
                }
                if(o1.endsWith("s") && o1.length() > o2.length()){
                    return 0;
                }
            }

            return defaultComparator.compare(o1, o2);
        }
    }
}
