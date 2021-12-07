package lucene;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class GenerateDocs {
    public static void writeTxt(int fnm, String cont) {
        try {
            String base_path = "docsPath2\\";
            String fpth = base_path + "F" + fnm + ".txt";
            FileWriter writer = new FileWriter(fpth, false);
            writer.write(cont);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static void main(String[] args) {
        try {
            FileReader reader = new FileReader("dblp_title.txt");
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            int i=1;
            while ((line = bufferedReader.readLine()) != null) {
                writeTxt(i, line);
                System.out.println(line);
                i++;
            }
            reader.close();
            System.out.println("All Done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

