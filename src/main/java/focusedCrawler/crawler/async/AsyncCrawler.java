package focusedCrawler.crawler.async;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import focusedCrawler.config.ConfigService;
import focusedCrawler.link.DownloadScheduler;
import focusedCrawler.link.LinkStorage;
import focusedCrawler.link.frontier.LinkRelevance;
import focusedCrawler.util.DataNotFoundException;
import focusedCrawler.util.storage.Storage;
import focusedCrawler.util.storage.StorageConfig;
import focusedCrawler.util.storage.StorageException;
import focusedCrawler.util.storage.StorageFactoryException;
import focusedCrawler.util.storage.distribution.StorageCreator;

public class AsyncCrawler {
	
    private static final Logger logger = LoggerFactory.getLogger(AsyncCrawler.class);

    private final LinkStorage linkStorage;
    private final HttpDownloader downloader;
    private final FetchedResultHandler resultHandler;
    private final DownloadScheduler downloadScheduler;
    
    private boolean shouldStop = false;
    
    public AsyncCrawler(Storage targetStorage, LinkStorage linkStorage, AsyncCrawlerConfig crawlerConfig) {
        this.linkStorage = linkStorage;
		this.downloader = new HttpDownloader(crawlerConfig.getDownloaderConfig());
        this.resultHandler = new FetchedResultHandler(targetStorage);
        this.downloadScheduler = new DownloadScheduler(
                crawlerConfig.getHostMinAccessInterval(),
                crawlerConfig.getMaxLinksInScheduler());
    }
    
    private class DownloadDispatcher extends Thread {
        public DownloadDispatcher() {
            setName("download-dispatcher");
        }
        @Override
        public void run() {
            while(!shouldStop) {
                LinkRelevance linkRelevance = downloadScheduler.nextLink();
                if(linkRelevance != null) {
                    downloader.dipatchDownload(linkRelevance, resultHandler);
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        logger.error("LinkDispatcher was interrupted.", e);
                    }
                }
            }
        }
    }

    public void run() {
        try {
            DownloadDispatcher linkDispatcher = new DownloadDispatcher();
            linkDispatcher.start();
            while(!this.shouldStop) {
                try {
                    LinkRelevance link = (LinkRelevance) linkStorage.select(null);
                    if(link != null) {
                        downloadScheduler.addLink(link);
                    }
                }
                catch (DataNotFoundException e) {
                    // There are no more links available in the frontier right now
                    if(downloader.hasPendingDownloads() || downloadScheduler.hasPendingLinks()) {
                        // If there are still pending downloads, new links 
                        // may be found in these pages, so we should wait some
                        // time until more links are available and try again
                        try {
                            logger.info("Waiting for links from pages being downloaded...");
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) { }
                        continue;
                    }
                    // There are no more pending downloads and there are no
                    // more links available in the frontier, so stop crawler
                    logger.info("LinkStorage ran out of links, stopping crawler.");
                    this.shouldStop = true;
                    break;
                } catch (StorageException e) {
                    logger.error("Problem when selecting link from LinkStorage.", e);
                } catch (Exception e) {
                    logger.error("An unexpected error happened.", e);
                }
            }
            downloader.await();
        } finally {
            logger.info("Shutting down crawler...");
            downloader.close();
            logger.info("Done.");
        }
    }

    public void stop() {
        this.shouldStop = true;
    }

    public static void run(ConfigService config) throws IOException, NumberFormatException {
        logger.info("Starting CrawlerManager...");
        try {
            StorageConfig linkStorageServerConfig = config.getLinkStorageConfig().getStorageServerConfig();
            Storage linkStorage = new StorageCreator(linkStorageServerConfig).produce();
            
            StorageConfig targetServerConfig = config.getTargetStorageConfig().getStorageServerConfig();
            Storage targetStorage = new StorageCreator(targetServerConfig).produce();
            
            AsyncCrawlerConfig crawlerConfig = config.getCrawlerConfig();

            AsyncCrawler crawler = new AsyncCrawler(targetStorage, (LinkStorage) linkStorage, crawlerConfig);
            crawler.run();

        } catch (StorageFactoryException ex) {
            logger.error("An error occurred while starting CrawlerManager. ", ex);
        }
    }

}
