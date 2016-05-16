package focusedCrawler.target;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import focusedCrawler.target.model.Page;

public class TargetStorageMonitor {
    
    private PrintWriter fCrawledPages;
    private PrintWriter fRelevantPages;
    private PrintWriter fNonRelevantPages;
    private PrintWriter fHarvestInfo;
    private PrintWriter fStatistics;
    
    private List<String> crawledUrls = new ArrayList<String>(); 
    private List<String> relevantUrls = new ArrayList<String>();
    private List<String> nonRelevantUrls = new ArrayList<String>();
    private List<String> harvestRates = new ArrayList<String>();
    private List<String> statistics = new ArrayList<String>();
    private HashSet<String> domains = new HashSet<String>();
    private String onionRegex = "^[a-z0-9]{16}\\.onion";

    private TargetStorageConfig config;

    int totalOnTopicPages = 0;
    private int totalOfPages = 0;
    private int numDifferentDomains = 0;
    
    public TargetStorageMonitor(String dataPath, TargetStorageConfig config) {
        
        File file = new File(dataPath+"/data_monitor/");
        if(!file.exists()) file.mkdirs();
        
        this.config = config;
        String fileCrawledPages = dataPath + "/data_monitor/crawledpages.csv";
        String fileRelevantPages = dataPath + "/data_monitor/relevantpages.csv";
        String fileHarvestInfo = dataPath + "/data_monitor/harvestinfo.csv";
        String fileNonRelevantPages = dataPath + "/data_monitor/nonrelevantpages.csv";
        String fileStatistics = dataPath + "/data_monitor/statistics.csv";

        
        try {
            fCrawledPages = new PrintWriter(fileCrawledPages, "UTF-8");
            fRelevantPages = new PrintWriter(fileRelevantPages, "UTF-8");
            fHarvestInfo = new PrintWriter(fileHarvestInfo, "UTF-8");
            fNonRelevantPages = new PrintWriter(fileNonRelevantPages, "UTF-8");
            fStatistics = new PrintWriter(fileStatistics, "UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException("Problem while opening files to export target metrics", e);
        }
    }
    
    public void countPage(Page page, boolean isRelevant, double prob) {
        
        totalOfPages++;
        
        String domain = page.getURL().getHost();
        if(domain.matches(onionRegex)){
        	domain = domain.split(".onion")[0];
        	if(domains.add(domain))
        		numDifferentDomains++;
        }
        
        

        crawledUrls.add(page.getIdentifier() + "\t" +
                        String.valueOf(System.currentTimeMillis() / 1000L));
        
        harvestRates.add(Integer.toString(totalOnTopicPages) + "\t" + 
                         String.valueOf(totalOfPages) + "\t" +
                         String.valueOf(System.currentTimeMillis() / 1000L));
        
        if(isRelevant) {
            totalOnTopicPages++;
            relevantUrls.add(page.getIdentifier() + "\t" + String.valueOf(System.currentTimeMillis() / 1000L));
        } else {
            nonRelevantUrls.add(page.getIdentifier() + "\t" + String.valueOf(prob) + "\t" + String.valueOf(System.currentTimeMillis() / 1000L));
        }
        
        statistics.add(String.valueOf(System.currentTimeMillis() / 1000L)+"\t"+
 			   page.getIdentifier()+"\t"+
 			   String.valueOf(totalOfPages)+"\t"+
 			   String.valueOf(totalOnTopicPages)+"\t"+
 			   String.valueOf(numDifferentDomains));
 
        
        if (config.isRefreshSync()){
          if(totalOnTopicPages % config.getRefreshFreq() == 0) {
               exportHarvestInfo(harvestRates);
               harvestRates.clear();
               exportCrawledPages(crawledUrls);
               crawledUrls.clear();    
               exportRelevantPages(relevantUrls);
               relevantUrls.clear();
               exportNonRelevantPages(nonRelevantUrls);
               nonRelevantUrls.clear();
               exportStatistics(statistics);
               statistics.clear();
          }
        } else{
            if(totalOfPages % config.getHarvestInfoRefreshFrequency() == 0) {
                exportHarvestInfo(harvestRates);
                harvestRates.clear();
            }
            if(totalOfPages % config.getCrawledRefreshFrequency() == 0) {
                exportCrawledPages(crawledUrls);
                crawledUrls.clear();    
            }
            if(totalOnTopicPages % config.getRelevantRefreshFrequency() == 0) {
                exportRelevantPages(relevantUrls);
                relevantUrls.clear();

                exportNonRelevantPages(nonRelevantUrls);
                nonRelevantUrls.clear();
                
                exportStatistics(statistics);
                statistics.clear();
            }
        }
        
    }

    private void export(List<String> list, PrintWriter file) {
        for (String item : list) {
            file.println(item);
        }
        file.flush();
    }

    private void exportHarvestInfo(List<String> list) {
        export(list, this.fHarvestInfo);
    }

    private void exportCrawledPages(List<String> list) {
        export(list, fCrawledPages);
    }

    private void exportRelevantPages(List<String> list) {
        export(list, this.fRelevantPages);
    }

    private void exportNonRelevantPages(List<String> list) {
        export(list, this.fNonRelevantPages);
    }
    
    private void exportStatistics(List<String> list) {
        export(list, this.fStatistics);
    }

    public int getTotalOfPages() {
        return totalOfPages;
    }

}
