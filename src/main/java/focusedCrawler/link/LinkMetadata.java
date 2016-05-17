package focusedCrawler.link;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(Include.NON_NULL)
public class LinkMetadata {
	
	private String url;
	
	
	// data from the page in itself
	@JsonProperty("is_page_info_set")
	private boolean isPageInfoSet;
	
	@JsonProperty("page_content")
	private String[] pageContent;
	
	@JsonProperty("page_relevance")
	private int pageRelevance;
	
	
	// data describing the page itself BUT obtained through a Search Engine results
	@JsonProperty("is_page_info_search_engine_set")
	private boolean isPageInfoSearchEngineSet;
	
	@JsonProperty("search_engine_title")
	private String searchEngineTitle;
	
	@JsonProperty("search_engine_snippet")
	private String searchEngineSnippet;
	
	
	// data concerning the BACKLINK page (not the same URL)
	@JsonProperty("is_target_of_backlink")
	private boolean isTargetOfBacklink;
	
	@JsonProperty("backlink_titles")
	private Vector<String> backlinkTitles = new Vector<String>();
	
	@JsonProperty("backlink_snippets")
	private Vector<String> backlinkSnippets = new Vector<String>();
	
	@JsonProperty("backlink_urls")
	private Vector<String> backlinkUrls = new Vector<String>();
	
	// 
	@JsonProperty("is_a_backlink")
	private boolean isABacklink;
	
	@JsonProperty("url_backlinked")
	private String urlBacklinked;
	
	
	// data concerning the outlinks on THIS page
	@JsonProperty("is_target_of_outlink")
	private boolean isTargetOfOutlink;
	
	private String[] anchor = new String[0];
	
	private String[] around = new String[0];
	
	@JsonProperty("image_src")
	private String imgSource;
	  
	@JsonProperty("image_alt")
	private String[] imgAlt;
	  
	@JsonProperty("around_position")
	private int aroundPosition;
	
	@JsonProperty("number_of_words")
	private int numOfWordsAnchor;
	  
	@JsonProperty("same_site")
	private boolean sameSite = false;
	
	
	// constructors
	public LinkMetadata() {
		// required for JSON serialization
	}
	
	public LinkMetadata(String url) {
		this.url = url;
	}
	
	
	// setters
	public void setIsTargetOfOutlink(boolean isTargetOfOutlink){
		this.isTargetOfOutlink = isTargetOfOutlink;
	}
	
	public void setIsTargetOfBacklink(boolean isTargetOfBacklink){
		this.isTargetOfBacklink = isTargetOfBacklink;
	}
	
	public void setIsPageInfoSet(boolean isPageInfoSet){
		this.isPageInfoSet = isPageInfoSet;
	}
	
	public void setIsPageInfoSearchEngineSet(boolean isPageInfoSearchEngineSet){
		this.isPageInfoSearchEngineSet = isPageInfoSearchEngineSet;
	}
	
	public void setBacklinkUrls(Vector<String> backlinkUrls){
		this.backlinkUrls=backlinkUrls;
	}
	
	public void setUrl(String url){
		this.url=url;
	}
	
	public void setPageRelevance(int pageRelevance){
		this.pageRelevance = pageRelevance;
	}

	public void setAnchor(String[] anchor){
	    this.anchor = anchor;
	}
	
	public void setAround(String[] around){
	    this.around = around;
	}

	public void setSearchEngineTitle(String searchEngineTitle) {
		this.searchEngineTitle = searchEngineTitle;
	}

	public void setSearchEngineSnippet(String searchEngineSnippet) {
		this.searchEngineSnippet = searchEngineSnippet;
	}
	
	public void setBacklinkTitles(Vector<String> backlinkTitles) {
		this.backlinkTitles = backlinkTitles;
	}

	public void setBacklinkSnippets(Vector<String> backlinkSnippets) {
		this.backlinkSnippets = backlinkSnippets;
	}
	
	public void setPageContent(String[] pageContent){
		this.pageContent = pageContent;
	}
	
	public void setAroundPosition(int pos){
	    this.aroundPosition = pos;
	}
	
	public void setNumberOfWordsAnchor(int num){
	    this.numOfWordsAnchor = num;
	}
	
	public void setImgSource(String source){
		this.imgSource = source;
	}
	  
	public void setImgAlt(String[] alt){
		this.imgAlt = alt;
	}
	
	public void setSameSite(boolean sameSite){
		this.sameSite = sameSite;
	}
	
	public void setIsABacklink(boolean isABacklink){
		this.isABacklink = isABacklink;
	}
	
	public void setUrlBacklinked(String urlBacklinked){
		this.urlBacklinked = urlBacklinked;
	}

	
	
	// getters
	public int getPageRelevance() {
		return pageRelevance;
	}

	public String getSearchEngineTitle() {
		return searchEngineTitle;
	}

	public String getSearchEngineSnippet() {
		return searchEngineSnippet;
	}

	public String getUrl() {
		return url;
	}
	
	public String[] getPageContent(){
		return pageContent;
	}
	
	@JsonIgnore
	public String getDomainName(){
		try {
			URL link = new URL(url);
			String domain = link.getHost();
			return domain.startsWith("www.") ? domain.substring(4) : domain;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@JsonIgnore
	public URL getLink(){
		try {
			URL link = new URL(url);
			return link;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public int getAroundPosition(){
		return this.aroundPosition;
	}
	
	public int getNumWordsAnchor(){
		return this.numOfWordsAnchor;
	}
	
	public String[] getAnchor(){
		return this.anchor;
	}
	
	public String getAltString(){
		StringBuffer buffer = new StringBuffer();
		String[] alts = getImgAlt();
		for (int j = 0; alts != null && j < alts.length; j++) {
			buffer.append(alts[j]);
	        buffer.append(" ");
		}
		return buffer.toString();
	}
	
	public String getAnchorString(){
		StringBuffer buffer = new StringBuffer();
		String[] anchors = getAnchor();
		for (int j = 0; j < anchors.length; j++) {
			buffer.append(anchors[j]);
			buffer.append(" ");
		}
		return buffer.toString();
	}
	
	public String getAroundString(){
		StringBuffer buffer = new StringBuffer();
		String[] arounds = getAround();
		for (int j = 0; j < arounds.length; j++) {
			buffer.append(arounds[j]);
	        buffer.append(" ");
		}
		return buffer.toString();
	}
	  
	public String[] getAround(){
		return this.around;
	}
	public String[] getImgAlt(){
		return this.imgAlt;
	}
	
	public String getImgSrc(){
		return this.imgSource;
	}
	
	public boolean getSameSite(){
		return this.sameSite;
	}
	
	public boolean getIsTargetOfOutlink(){
		return isTargetOfOutlink;
	}
	
	public boolean getIsTargetOfBacklink(){
		return isTargetOfBacklink;
	}
	
	public boolean getIsPageInfoSet(){
		return isPageInfoSet;
	}
	
	public boolean getIsPageInfoSearchEngineSet(){
		return isPageInfoSearchEngineSet;
	}
	
	public Vector<String> getBacklinkUrls(){
		return backlinkUrls;
	}
	
	public Vector<String> getBacklinkTitles() {
		return backlinkTitles;
	}

	public Vector<String> getBacklinkSnippets() {
		return backlinkSnippets;
	}
	
	public boolean getIsABacklink(){
		return isABacklink;
	}
	
	public String getUrlBacklinked(){
		return urlBacklinked;
	}
	
	
	public String[] getSearchEngineTitleAsArray(){
		// TITLE TOKENISING MUST BE DONE SOMEWHERE
		if(searchEngineTitle != null){
			StringTokenizer tokenizer = new StringTokenizer(searchEngineTitle," ");
			ArrayList<String> titleTemp = new ArrayList<String>();
			while(tokenizer.hasMoreTokens()){
				titleTemp.add(tokenizer.nextToken());
   		  	}
   		  	String[] titleArray = new String[titleTemp.size()];
   		  	titleTemp.toArray(titleArray);
   		  	return titleArray;
		}
		return null;
	}
	
	public String[] getSearchEngineSnippetAsArray(){
		if(searchEngineSnippet != null){
			StringTokenizer tokenizer = new StringTokenizer(searchEngineSnippet," ");
			ArrayList<String> snippetTemp = new ArrayList<String>();
			while(tokenizer.hasMoreTokens()){
				snippetTemp.add(tokenizer.nextToken());
   		  	}
   		  	String[] snippetArray = new String[snippetTemp.size()];
   		  	snippetTemp.toArray(snippetArray);
   		  	return snippetArray;
		}
		return null;
	}
	
	// other functions
	public LinkMetadata clone(){
		LinkMetadata lm = new LinkMetadata();
		lm.setUrl(url);
		lm.setPageRelevance(pageRelevance);
		lm.setIsTargetOfBacklink(isTargetOfBacklink);
		lm.setIsTargetOfOutlink(isTargetOfOutlink);
		lm.setBacklinkUrls(backlinkUrls);
		lm.setAnchor(anchor);
		lm.setAround(around);
		lm.setNumberOfWordsAnchor(numOfWordsAnchor);
		lm.setAroundPosition(aroundPosition);
		lm.setImgAlt(imgAlt);
		lm.setImgSource(imgSource);
		lm.setPageContent(pageContent);
		lm.setSameSite(sameSite);
		lm.setSearchEngineTitle(searchEngineTitle);
		lm.setSearchEngineSnippet(searchEngineSnippet);
		lm.setIsPageInfoSearchEngineSet(isPageInfoSearchEngineSet);
		lm.setIsPageInfoSet(isPageInfoSet);
		lm.setBacklinkSnippets(backlinkSnippets);
		lm.setBacklinkTitles(backlinkTitles);
		lm.setIsABacklink(isABacklink);
		lm.setUrlBacklinked(urlBacklinked);
		return lm;
	}
	
	@Override
	public String toString(){
		String result = "";
		result += "\turl: "+url+"\n";
		result += "\tisPageInfoSet: "+isPageInfoSet+"\n";
		result += "\tpageContent: ";
		if(pageContent!=null){
			for(int i=0; i<pageContent.length;i++){
				result += pageContent[i]+" ";
			}
		}
		result += "\n\tpageRelevance: "+pageRelevance+"\n";
		result += "\tisTargetOfBacklink: "+isTargetOfBacklink+"\n";
		result += "\tbacklinkUrls: ";
		for(int i=0; i<backlinkUrls.size();i++){
			result += backlinkUrls.elementAt(i)+" ";
		}
		result += "\n\tbacklinkSnippets: ";
		for(int i=0; i<backlinkSnippets.size();i++){
			result += backlinkSnippets.elementAt(i)+" ";
		}
		result += "\n\tbacklinkTitles: ";
		for(int i=0; i<backlinkTitles.size();i++){
			result += backlinkTitles.elementAt(i)+" ";
		}
		result += "\n\tisTargetOfOutlink: "+isTargetOfOutlink+"\n";
		result += "\tanchor: ";
		for(int i=0; i<anchor.length;i++){
			result += anchor[i]+" ";
		}
		result += "\n\taround: ";
		for(int i=0; i<around.length;i++){
			result += around[i]+" ";
		}
		result += "\n\tnumOfWordsAnchor: "+numOfWordsAnchor+"\n";
		result += "\taroundPosition: "+aroundPosition+"\n";
		result += "\timgAlt: "+imgAlt+"\n";
		result += "\timgSource: "+imgSource+"\n";
		result += "\tsameSite: "+sameSite+"\n";
		result += "\tisPageInfoSearchEngineSet: "+isPageInfoSearchEngineSet+"\n";
		result += "\tsearchEngineTitle: "+searchEngineTitle+"\n";
		result += "\tsearchEngineSnippet: "+searchEngineSnippet+"\n";
		result += "\tisABacklink: "+isABacklink+"\n";
		result += "\turlBacklinked: "+urlBacklinked+"\n";
		
		return result;
	}
	
	public void addBacklinkTitles(Vector<String> backlinkTitles){
		this.backlinkTitles.addAll(backlinkTitles);
	}
	
	public void addBacklinkSnippets(Vector<String> backlinkSnippets){
		this.backlinkSnippets.addAll(backlinkSnippets);
	}
	
	public void addBacklinkUrls(Vector<String> backlinkUrls){
		this.backlinkUrls.addAll(backlinkUrls);
	}
	
	public void addBacklinkTitle(String backlinkTitle){
		this.backlinkTitles.add(backlinkTitle);
	}
	
	public void addBacklinkSnippet(String backlinkSnippet){
		this.backlinkSnippets.add(backlinkSnippet);
	}
	
	public void addBacklinkUrl(String backlinkUrl){
		this.backlinkUrls.add(backlinkUrl);
	}
	
	public void updateOutlinkMetadata(LinkMetadata externalLM){
		this.setAnchor(externalLM.anchor);
		this.setAround(externalLM.around);
		this.setAroundPosition(externalLM.aroundPosition);
		this.setNumberOfWordsAnchor(externalLM.numOfWordsAnchor);
		this.setImgAlt(externalLM.imgAlt);
		this.setImgSource(externalLM.imgSource);
		this.setSameSite(externalLM.sameSite);
		
		this.setIsTargetOfOutlink(true);
	}
	
	public void updateBacklinkMetadata(LinkMetadata externalLM){
		this.addBacklinkTitles(externalLM.backlinkTitles);
		this.addBacklinkSnippets(externalLM.backlinkSnippets);
		this.addBacklinkUrls(externalLM.backlinkUrls);
		
		this.setIsTargetOfBacklink(true);
	}
	
	public void updatePageMetadata(LinkMetadata externalLM){
		this.setPageContent(externalLM.pageContent);
		this.setPageRelevance(externalLM.pageRelevance);
		
		this.setIsPageInfoSet(true);
	}
	
	public void updatePageSearchEngineMetadata(LinkMetadata externalLM){
		this.setSearchEngineTitle(externalLM.searchEngineTitle);
		this.setSearchEngineSnippet(externalLM.searchEngineSnippet);
		
		this.setIsPageInfoSearchEngineSet(true);
	}
	
	public void updateOriginOfBacklink(String urlBacklinked){
		this.setUrlBacklinked(urlBacklinked);
		
		this.setIsABacklink(true);
	}
	
	
	
}
