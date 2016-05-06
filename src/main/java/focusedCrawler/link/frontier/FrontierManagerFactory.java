package focusedCrawler.link.frontier;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import focusedCrawler.link.LinkStorageConfig;
import focusedCrawler.link.frontier.selector.LinkSelector;
import focusedCrawler.link.frontier.selector.MaximizeWebsitesLinkSelector;
import focusedCrawler.link.frontier.selector.MultiLevelLinkSelector;
import focusedCrawler.link.frontier.selector.NonRandomLinkSelector;
import focusedCrawler.link.frontier.selector.PoliteTopkLinkSelector;
import focusedCrawler.link.frontier.selector.RandomLinkSelector;
import focusedCrawler.link.frontier.selector.SiteLinkSelector;
import focusedCrawler.link.frontier.selector.TopicLinkSelector;
import focusedCrawler.link.frontier.selector.TopkLinkSelector;
import focusedCrawler.util.LinkFilter;
import focusedCrawler.util.ParameterFile;

public class FrontierManagerFactory {

	private static final Logger logger = LoggerFactory
			.getLogger(FrontierManagerFactory.class);

	public static FrontierManager create(LinkStorageConfig config,
			String configPath, String dataPath, String seedFile,
			String stoplistFile) {

		String[] seedUrls = ParameterFile.getSeeds(seedFile);

		String directory = Paths.get(dataPath, config.getLinkDirectory())
				.toString();
		String backdirectory = Paths.get(dataPath,
				config.getBacklinkDirectory()).toString();

		Frontier linkFrontier = null;
		Frontier backlinkFrontier = null;
		if (config.isUseScope()) {
			Map<String, Integer> scope = extractDomains(seedUrls);
			linkFrontier = new Frontier(directory,
					config.getMaxCacheUrlsSize(), scope);
			backlinkFrontier = new Frontier(backdirectory,
					config.getMaxCacheUrlsSize(), scope);
		} else {
			linkFrontier = new Frontier(directory, config.getMaxCacheUrlsSize());
			backlinkFrontier = new Frontier(backdirectory,config.getMaxCacheUrlsSize());
		}

		LinkFilter linkFilter = new LinkFilter(configPath);

		LinkSelector outlinkSelector = createLinkSelector(config, 1);
		LinkSelector backlinkForwardSelector = createLinkSelector(config, 2);
		LinkSelector backlinkBackwardSelector = createLinkSelector(config, 3);

		logger.info("OUTLINK_SELECTOR: " + outlinkSelector.getClass().getName());
		logger.info("BACKLINK_FORWARD_SELECTOR: "
				+ backlinkForwardSelector.getClass().getName());
		logger.info("BACKLINK_BACKWADS_SELECTOR: "
				+ backlinkBackwardSelector.getClass().getName());

		return new FrontierManager(linkFrontier, backlinkFrontier,
				config.getMaxSizeLinkQueue(), config.getMaxSizeLinkQueue(),
				outlinkSelector, backlinkForwardSelector,
				backlinkBackwardSelector, linkFilter);
	}

	private static LinkSelector createLinkSelector(LinkStorageConfig config,
			int type) {
		String linkSelectorConfig = null;
		switch (type) {
		case 1:
			linkSelectorConfig = config.getOutlinkSelector();
		case 2:
			linkSelectorConfig = config.getBacklinkForwardSelector();
		case 3:
			linkSelectorConfig = config.getBacklinkBackwardSelector();
		}
		if (linkSelectorConfig != null) {
			if (linkSelectorConfig.equals("TopkLinkSelector")) {
				return new TopkLinkSelector();
			}
			if (linkSelectorConfig.equals("PoliteTopkLinkSelector")) {
				return new PoliteTopkLinkSelector(4, 10000);
			} else if (linkSelectorConfig.equals("SiteLinkSelector")) {
				return new SiteLinkSelector();
			} else if (linkSelectorConfig.equals("RandomLinkSelector")) {
				return new RandomLinkSelector();
			} else if (linkSelectorConfig.equals("NonRandomLinkSelector")) {
				return new NonRandomLinkSelector();
			} else if (linkSelectorConfig.equals("MultiLevelLinkSelector")) {
				return new MultiLevelLinkSelector();
			} else if (linkSelectorConfig.equals("TopicLinkSelector")) {
				return new TopicLinkSelector();
			} else if (linkSelectorConfig
					.equals("MaximizeWebsitesLinkSelector")) {
				return new MaximizeWebsitesLinkSelector();
			}
		}

		// Maintain old defaults to keep compatibility
		if (config.isUseScope()) {
			if (config.getOutlinkSelector().contains("Baseline")) {
				return new SiteLinkSelector();
			} else {
				return new MultiLevelLinkSelector();
			}
		} else {
			if (config.getOutlinkSelector().contains("Baseline")) {
				return new NonRandomLinkSelector();
			} else {
				return new MultiLevelLinkSelector();
			}
		}
	}

	private static HashMap<String, Integer> extractDomains(String[] urls) {
		HashMap<String, Integer> scope = new HashMap<String, Integer>();
		for (int i = 0; i < urls.length; i++) {
			try {
				URL url = new URL(urls[i]);
				String host = url.getHost();
				scope.put(host, new Integer(1));
			} catch (MalformedURLException e) {
				logger.warn("Invalid URL in seeds file. Ignoring URL: "
						+ urls[i]);
			}
		}
		logger.info("Using scope of following domains:");
		for (String host : scope.keySet()) {
			logger.info(host);
		}
		return scope;
	}

}
