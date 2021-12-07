package lucene;

import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;

public class Spell_Checker
{
    public static void check_txt_dictionary(String input_word) throws IOException
    {
        // Reading Index
        String spellIndexPath = "spellIdxPath2";
        Directory sdir = FSDirectory.open(Paths.get(spellIndexPath));
        SpellChecker checker = new SpellChecker(sdir);
        System.out.print("Loading SpellCheckerIndex ");
        checker.setSpellIndex(sdir);
        sdir.close();
        // Searching and presenting the suggested words by selecting a string distance
        checker.setStringDistance(new JaroWinklerDistance());
//        checker.setStringDistance(new LevenshteinDistance());
//        checker.setStringDistance(new LuceneLevenshteinDistance());
//        checker.setStringDistance(new NGramDistance());
        String[] suggestions = checker.suggestSimilar(input_word, 5);
        System.out.println("By '" + input_word + "' did you mean:");
        for(String suggestion : suggestions)
            System.out.println("\t" + suggestion);
    }


    public static void main(String[] args) throws IOException, Throwable
    {
        Scanner scan = new Scanner(System.in);
        Spell_Checker spell_checker = new Spell_Checker();

        System.out.print("\nType a word to spell check: ");
        String input_word = scan.next();

        spell_checker.check_txt_dictionary(input_word);
    }
}