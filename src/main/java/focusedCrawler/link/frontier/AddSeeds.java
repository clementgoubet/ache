package focusedCrawler.link.frontier;

import java.net.URL;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import focusedCrawler.config.ConfigService;
import focusedCrawler.util.ParameterFile;

public class AddSeeds {

	private static final Logger logger = LoggerFactory
			.getLogger(AddSeeds.class);

	public static void main(ConfigService config, String seedFile,
			String dataOutputPath) {
		try {
			String linkDirectory = config.getLinkStorageConfig()
					.getLinkDirectory();
			String backlinkDirectory = config.getLinkStorageConfig()
					.getBacklinkDirectory();
			String dir = Paths.get(dataOutputPath, linkDirectory).toString();
			String backdir = Paths.get(dataOutputPath, backlinkDirectory)
					.toString();
			Frontier linkFrontier = new Frontier(dir, 1000);
			Frontier backlinkFrontier = new Frontier(backdir, 1000);
			int count = 0;

			logger.info("Adding seeds from file: " + seedFile);

			boolean getOutlinks = config.getLinkStorageConfig().getOutlinks();
			boolean getBacklinks = config.getLinkStorageConfig().getBacklinks();
			boolean getForwardSEqueries = config.getLinkStorageConfig()
					.getForwardSElinks();

			String[] seeds = ParameterFile.getSeeds(seedFile);
			if (seeds != null && seeds.length > 0) {
				for (String seed : seeds) {
					boolean inserted = false;
					// seed can potentially be crawled as a type 1 or 2
					if (getOutlinks) {
						LinkRelevance linkRel = new LinkRelevance(
								new URL(seed), LinkRelevance.TYPE_FORWARD,
								LinkRelevance.DEFAULT_RELEVANCE);
						Integer exist = linkFrontier.exist(linkRel);
						if (exist == null || exist == -1) {
							System.out.println("Adding seed URL: " + seed
									+ " with TYPE_FORWARD");
							linkFrontier.insert(linkRel);
							inserted = true;
						}
					}
					if (getBacklinks) {
						LinkRelevance linkRel = new LinkRelevance(
								new URL(seed), LinkRelevance.TYPE_BACKLINK_BACKWARD,
								LinkRelevance.DEFAULT_RELEVANCE);
						Integer exist = backlinkFrontier.exist(linkRel);
						if (exist == null || exist == -1) {
							System.out.println("Adding seed URL: " + seed
									+ " with TYPE_BACKLINK_BACKWARD");
							backlinkFrontier.insert(linkRel);
							inserted = true;
						}
					}
					if (inserted) {
						count++;
					}
				}
			}
			logger.info("Number of seeds added:" + count);
			linkFrontier.close();
			backlinkFrontier.close();

		} catch (Exception e) {
			logger.error("Problem while adding seeds. ", e);
		}
	}

}
