import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Hello World!");

        String modelFile = "resources/models/wsj-0-18-bidirectional-distsim.tagger";
        String fileToTag = "resources/files/book_of_the_stranger.txt";
        MaxentTagger tagger = new MaxentTagger(modelFile);

        List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new BufferedReader(new FileReader(fileToTag)));
        Set<String> foundNamedEntities = new HashSet<>();
        for (List<HasWord> sentence : sentences) {
            List<TaggedWord> tSentence = tagger.tagSentence(sentence);

            for(TaggedWord taggedWord: tSentence){
                String word = taggedWord.word();

                if(taggedWord.tag().equals("NNP")){
                    foundNamedEntities.add(word);
                }
                if(taggedWord.tag().equals("NNPS")){
                    foundNamedEntities.add(word);
                }
            }

            //System.out.println(Sentence.listToString(tSentence, false));
        }

        foundNamedEntities.forEach(System.out::println);
    }
}
