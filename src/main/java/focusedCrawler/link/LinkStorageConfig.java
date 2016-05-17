package focusedCrawler.link;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import focusedCrawler.util.storage.StorageConfig;

public class LinkStorageConfig {

    public static class BackSurferConfig {
        
        @JsonProperty("link_storage.backsurfer.moz.access_id")
        private String mozAccessId = null;
        
        @JsonProperty("link_storage.backsurfer.moz.secret_key")
        private String mozKey = null;
        
        public BackSurferConfig() { }
        
        public String getMozAccessId() {
            return mozAccessId;
        }
        
        public String getMozKey() {
            return mozKey;
        }
        
    }
    
    public static class BiparitieGraphRepConfig {
        
        private String parentsGraphDirectory = "data_backlinks/parents_graph";
        private String urlIdDirectory = "data_backlinks/url";
        private String childrenGraphDirectory = "data_backlinks/children_graph";
        private String nodeIdDirectory = "data_backlinks/node_id";
        
        public BiparitieGraphRepConfig() { }
        
        public String getParentsGraphDirectory() {
            return parentsGraphDirectory;
        }
        
        public String getUrlIdDirectory() {
            return urlIdDirectory;
        }
        
        public String getChildrenGraphDirectory() {
            return childrenGraphDirectory;
        }
        
        public String getNodeIdDirectory() {
            return nodeIdDirectory;
        }
        
    }
    
    @JsonProperty("link_storage.max_pages_per_domain")
    private int maxPagesPerDomain = 100;
    
    @JsonProperty("link_storage.link_classifier.outlinks.type")
    private String outlinkClassifier = "LinkClassifierBaseline";
    
    @JsonProperty("link_storage.link_classifier.backlinks.forward.type")
    private String backlinkForwardClassifier = "LinkClassifierBaseline";
    
    @JsonProperty("link_storage.link_classifier.backlinks.backward.type")
    private String backlinkBackwardClassifier = "LinkClassifierBaseline";
    
    
    @JsonProperty("link_storage.link_strategy.outlinks")
    private boolean getOutlinks = true;
    
    @JsonProperty("link_storage.link_strategy.use_scope")
    private boolean useScope = false;
    
    
    @JsonProperty("link_storage.directory")
    private String linkDirectory = "data_url/dir";
    
    @JsonProperty("link_storage.backlink_directory")
    private String backlinkDirectory = "data_url/backdir";
    
    @JsonProperty("link_storage.max_size_cache_urls")
    private int maxCacheUrlsSize = 200000;
    
    @JsonProperty("link_storage.max_size_link_queue")
    private int maxSizeLinkQueue = 100000;
    
    @JsonProperty("link_storage.link_strategy.backlinks")
    private boolean getBacklinks = false;
  
    @JsonProperty("link_storage.link_strategy.forward_search_engine_links")
    private boolean getForwardSElinks = false;
    
    
    @JsonProperty("link_storage.online_learning.enabled")
    private boolean useOnlineLearning = false;
    
    @JsonProperty("link_storage.online_learning.type")
    private String onlineMethod = "FORWARD_CLASSIFIER_BINARY";
    
    @JsonProperty("link_storage.online_learning.learning_limit")
    private int learningLimit = 500;
    
    
    @JsonProperty("link_storage.link_selector.outlink")
    private String outlinkSelector = "TopkLinkSelector";
    
    @JsonProperty("link_storage.link_selector.backlinks.backward")
    private String backlinkBackwardSelector = "TopkLinkSelector";
    
    @JsonProperty("link_storage.link_selector.backlinks.forward")
    private String backlinkForwardSelector = "TopkLinkSelector";
    
    @JsonProperty("link_storage.link_selector.weights")
    private int[] selectionWeights = {50,1,2};
    
    
    // TODO Remove target storage folder dependency from link storage
    private String targetStorageDirectory = "data_target/";
    
    @JsonUnwrapped
    private BackSurferConfig backSurferConfig = new BackSurferConfig();
    
    private BiparitieGraphRepConfig biparitieGraphRepConfig = new BiparitieGraphRepConfig();
    
    private final StorageConfig serverConfig;
    
    public LinkStorageConfig(JsonNode config, ObjectMapper objectMapper) throws IOException {
        objectMapper.readerForUpdating(this).readValue(config);
        this.serverConfig = StorageConfig.create(config, "link_storage.server.");
    }

    public int getMaxPagesPerDomain() {
        return maxPagesPerDomain;
    }
    
    public String getTypeOfOutlinkClassifier() {
        return outlinkClassifier;
    }
    
    public String getTypeOfBacklinkForwardClassifier() {
        return backlinkForwardClassifier;
    }
    
    public String getTypeOfBacklinkBackwardClassifier() {
        return backlinkBackwardClassifier;
    }
    
    public boolean getOutlinks() {
        return getOutlinks;
    }
    
    public boolean getForwardSElinks(){
    	return getForwardSElinks;
    }
    
    public boolean isUseScope() {
        return useScope;
    }
    
    public String getLinkDirectory() {
        return linkDirectory;
    }
    
    public String getBacklinkDirectory() {
        return backlinkDirectory;
    }
    
    public int getMaxCacheUrlsSize() {
        return maxCacheUrlsSize;
    }
    
    public int getMaxSizeLinkQueue() {
        return maxSizeLinkQueue;
    }
    
    public boolean getBacklinks() {
        return getBacklinks;
    }
    
    public boolean isUseOnlineLearning() {
        return useOnlineLearning;
    }
    
    public String getOnlineMethod() {
        return onlineMethod;
    }
    
    public int getLearningLimit() {
        return learningLimit;
    }
    
    public String getTargetStorageDirectory() {
        return targetStorageDirectory;
    }
    
    public BiparitieGraphRepConfig getBiparitieGraphRepConfig() {
        return biparitieGraphRepConfig;
    }
    
    public BackSurferConfig getBackSurferConfig() {
        return backSurferConfig;
    }

    public String getOutlinkSelector() {
        return outlinkSelector;
    }
    
    public String getBacklinkBackwardSelector() {
        return backlinkBackwardSelector;
    }
    
    public String getBacklinkForwardSelector() {
        return backlinkForwardSelector;
    }

    public StorageConfig getStorageServerConfig() {
        return serverConfig;
    }
    
    public int[] getSelectionWeights(){
    	return selectionWeights;
    }

}
