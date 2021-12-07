package lucene;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

//import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * This is a simple text extraction example to get started. For more advance usage, see the
 * ExtractTextByArea and the DrawPrintTextLocations examples in this subproject, as well as the
 * ExtractText tool in the tools subproject.
 *
 * @author Tilman Hausherr
 */
public class ExtractTextSimple
{
//    private ExtractTextSimple()
//    {
//        // example class should not be instantiated
//    }

//    public static void main(String[] args) throws IOException
    public static String[] fetch_extractPdf(String url_s, String uuid) throws IOException
    {
//        String url_s = "https://meltdownattack.com/meltdown.pdf";
//        String uuid = "617ba72a96049b0248570a67";
        URL url = new URL(url_s);
        String path = String.format("pdf_dumps/%s.pdf", uuid);
        StringBuilder content = new StringBuilder();
        try (InputStream in = url.openStream()) {
            Files.copy(in, Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.printf("Error fetching pdf from %s", url_s);
        }

//        try (PDDocument document = Loader.loadPDF(new File("D:\\Mayank\\FinalUI\\mayank.pdf")))
        try (PDDocument document = PDDocument.load(new File(path)))
        {
            AccessPermission ap = document.getCurrentAccessPermission();
            if (!ap.canExtractContent())
            {
                throw new IOException("You do not have permission to extract text");
            }

            PDFTextStripper stripper = new PDFTextStripper();

            // This example uses sorting, but in some cases it is more useful to switch it off,
            // e.g. in some files with columns where the PDF content stream respects the
            // column order.
            stripper.setSortByPosition(false);

            for (int p = 1; p <= document.getNumberOfPages(); ++p)
            {
                // Set the page interval to extract. If you don't, then all pages would be extracted.
                stripper.setStartPage(p);
                stripper.setEndPage(p);

                // let the magic happen
                String text = stripper.getText(document);
                content.append(text.trim());
                // do some nice output with a header
//                String pageStr = String.format("page %d:", p);
//                System.out.println(pageStr);
//                for (int i = 0; i < pageStr.length(); ++i)
//                {
//                    System.out.print("-");
//                }
//                System.out.println();
//                System.out.println(text.trim());
//                System.out.println();

                // If the extracted text is empty or gibberish, please try extracting text
                // with Adobe Reader first before asking for help. Also read the FAQ
                // on the website:
                // https://pdfbox.apache.org/2.0/faq.html#text-extraction
            }
        }
        String[] resp = {path, content.toString()};
        return resp;
    }
}
