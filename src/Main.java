import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import sun.misc.ASCIICaseInsensitiveComparator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import static java.lang.System.out;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {

        String modelFile = "../resources/models/english-left3words-distsim.tagger";
        String fileToTag = "../resources/samples_for_tests/book_of_the_stranger_full.txt";
        MaxentTagger tagger = new MaxentTagger(modelFile);

        List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new BufferedReader(new FileReader(fileToTag)));
        List<List<TaggedWord>> taggedSentences = tagger.process(sentences);

        SortedSet<String> foundNamedEntities = new TreeSet<>();
        for (List<TaggedWord> taggedSentence : taggedSentences) {
            String compoundNNP = "";
            for(TaggedWord taggedWord: taggedSentence){
                String word = taggedWord.word();

                if(word.endsWith("Edit")){
                    word = word.substring(0, word.length()-4);
                }

                if(word.length() == 0) continue;

                if(taggedWord.tag().equals("NNP") || taggedWord.tag().equals("NNPS")){
                //if(word.substring(0,1).matches("[A-Z]")) {
                    if(compoundNNP.isEmpty()){
                        compoundNNP = word;
                    } else {
                        compoundNNP += " " + word;
                    }
                } else {
                    if(!compoundNNP.isEmpty()) {
                        debug(taggedSentence, compoundNNP);

                        foundNamedEntities.add(compoundNNP);
                        compoundNNP = "";
                    }
                }
            }
            if(!compoundNNP.isEmpty()){
                debug(taggedSentence, compoundNNP);

                foundNamedEntities.add(compoundNNP);
            }
        }

        foundNamedEntities.forEach(System.out::println);
    }

    // Printa a entidade com a frase onde ela foi achada.
    private static void debug(List<TaggedWord> taggedSentence, String compoundNNP) {
        out.print(compoundNNP);
        out.print(" (");
        out.print(Arrays.toString(taggedSentence.stream().map(TaggedWord::toString).toArray()));
        out.print(") ");
        out.println();
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
