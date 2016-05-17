package focusedCrawler.link.classifier.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import focusedCrawler.util.string.PorterStemmer;
import focusedCrawler.util.string.StopList;

public class FrequencyMap {

	private Map<String, WordFrequency> frequencyMap;
	private String field;
	private StopList stoplist;
	private PorterStemmer stemmer;
	FilterData filterData;
	ArrayList<String> finalWords;
	String[] fieldWords;
	
	public FrequencyMap(String field, StopList stoplist, PorterStemmer stemmer, FilterData filterData){
		this.frequencyMap = new HashMap<>();
		this.finalWords = new ArrayList<>();
		this.field = field;
		this.stoplist = stoplist;
		this.stemmer = stemmer;
		this.filterData = filterData;
	}
	
	public void addWords(String[] words){
		for (int j = 0; j < words.length; j++) {
			String word = stemmer.stem(words[j]);
			if(word == null || stoplist.eIrrelevante(word)){
				continue;
			}
			WordFrequency wf = (WordFrequency) frequencyMap.get(word);
			if (wf != null) {
				frequencyMap.put(word, new WordFrequency(word, wf.getFrequency()+1));
			}
			else {
				frequencyMap.put(word, new WordFrequency(word, 1));
			}
		}
	}
	
	public void addWords(String[] words, String[] features){
		List<String> listFeatures = Arrays.asList(features);
		for (int j = 0; j < words.length; j++) {
			String word = stemmer.stem(words[j]);
			if(word == null || stoplist.eIrrelevante(word) || !listFeatures.contains(field+"_"+word)){
				continue;
			}
			WordFrequency wf = (WordFrequency) frequencyMap.get(word);
			if (wf != null) {
				frequencyMap.put(word, new WordFrequency(field+"_"+word, wf.getFrequency()+1));
			}
			else {
				frequencyMap.put(word, new WordFrequency(field+"_"+word, 1));
			}
		}
	}
	
	public Map<String, WordFrequency> getMap(){
		return frequencyMap;
	}
	
	public ArrayList<WordFrequency> filter(ArrayList<WordFrequency> aroundWords){
		ArrayList<WordFrequency> list = new ArrayList<>(frequencyMap.values());
		int size = list.size();
		Collections.sort(list,new WordFrequencyComparator());
		if(list.size() != size){
			System.out.println("SORTED LIST IS SHORTER...");
		}
		ArrayList<WordFrequency> fieldFinal = filterData.filter(list,aroundWords);
		fieldWords = new String[fieldFinal.size()];
		for (int i = 0; i < fieldFinal.size(); i++) {
			WordFrequency wf = fieldFinal.get(i);
			finalWords.add(field+"_"+wf.getWord());
			fieldWords[i] = wf.getWord();
		}
		return fieldFinal;
	}
	
	public String[] getFieldWords(){
		return fieldWords;
	}
	
	public ArrayList<String> getFinalWords(){
		return finalWords;
	}
	
	@Override
	public String toString(){
		String result = "";
		for(WordFrequency wf : frequencyMap.values()){
			result += wf.toString()+"\n";
		}
		return result;
	}

}