package focusedCrawler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import focusedCrawler.target.classifier.TargetClassifier;
import focusedCrawler.target.classifier.TargetClassifierFactory;
import focusedCrawler.target.model.Page;

public class MainTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void wekaFeaturesFileShouldBeGeneratedInTheProperFormat() throws Exception {
        
        String stopList     = "config/sample_config/stoplist.txt";
        String trainingPath = MainTest.class.getResource("sample_training_data").getFile();
        String modelPath    = tempFolder.newFolder().toString();
        
        // Train a page classifier model
        String[] args = { "buildModel", "-c", stopList, "-t", trainingPath, "-o", modelPath };
        Main.main(args);

        // Load model trained
        TargetClassifier tc = TargetClassifierFactory.create(modelPath, 0.9, stopList);
        
        // Classify one example from training data just for sanity check
        Page samplePositivePage = readOnePageFromFolder(trainingPath+"/positive");
        Page sampleNegativePage = readOnePageFromFolder(trainingPath+"/negative");
        assertThat(tc.classify(samplePositivePage).isRelevant(), is(true));
        assertThat(tc.classify(sampleNegativePage).isRelevant(), is(false));
    }

    private Page readOnePageFromFolder(String positiveFolder) throws Exception {
        File[] allPositivePages = (new File(positiveFolder)).listFiles();
        assertThat(allPositivePages.length, is(6));
        String positiveFileName = allPositivePages[0].getName();
        String fileContent = new String(Files.readAllBytes(Paths.get(allPositivePages[0].getAbsolutePath())));
        Page samplePositivePage = new Page(new URL(URLDecoder.decode(positiveFileName, "UTF-8")), fileContent);
        return samplePositivePage;
    }

}
