package hust.tools.csc.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import hust.tools.csc.ngram.NGramModel;
import hust.tools.csc.util.ConfusionSet;
import hust.tools.csc.util.Dictionary;
import hust.tools.csc.util.Sentence;
import hust.tools.csc.wordseg.AbstractWordSegment;

/**
 *<ul>
 *<li>Description: 在DoubleStage噪音通道模型的基础上，引入字的概率 
 *<li>Company: HUST
 *<li>@author Sonly
 *<li>Date: 2017年11月15日
 *</ul>
 */
public class DoubleStageNoisyChannelModelBasedCharacter extends AbstractNoisyChannelModel {
	
	private Dictionary dictionary;
	private AbstractWordSegment wordSegment;
 
	public DoubleStageNoisyChannelModelBasedCharacter(Dictionary dictionary, NGramModel nGramModel, ConfusionSet confusionSet,
			AbstractWordSegment wordSegment) throws IOException {
		super(confusionSet, nGramModel);
		
		this.dictionary = dictionary;
		this.wordSegment = wordSegment;
	}
	
	public DoubleStageNoisyChannelModelBasedCharacter(Dictionary dictionary, NGramModel nGramModel, ConfusionSet confusionSet,
			AbstractWordSegment wordSegment, double magicNumber) throws IOException {
		super(confusionSet, nGramModel, magicNumber);
		
		this.dictionary = dictionary;
		this.wordSegment = wordSegment;
	}
	
	@Override
	public Sentence getBestSentence(Sentence sentence) {
		return getBestKSentence(sentence, 1).get(0);
	}
	
	@Override
	public ArrayList<Sentence> getBestKSentence(Sentence sentence, int k) {
		if(k < 1)
			throw new IllegalArgumentException("返回候选句子数目不能小于1");
		beamSize = k;
		ArrayList<Sentence> candSens = new ArrayList<>();
		ArrayList<Integer> locations = new ArrayList<>();
		
		////////////////////////////////基于bigram匹配的检错
		locations = getErrorLocationsBySIMD(dictionary, sentence);
		//连续单字词的最大个数小于2，不作处理直接返回原句
		if(locations.size() > 1) {
			candSens = beamSearch(confusionSet, beamSize, sentence, locations);
			return candSens;
		}else {
			candSens.add(sentence);
		}
		
		////////////////////////////////基于分词的检错
		sentence = candSens.get(0);
		ArrayList<String> words = wordSegment.segment(sentence);
		if(words.size() < 2) {//分词后，词的个数小于2的不作处理，不作处理直接返回原句
			return candSens;
		}
		locations = locationsOfSingleWords(words);
		//连续单字词的最大个数小于2，不作处理直接返回原句
		if(locations.size() > 1) {
			candSens = new ArrayList<>();
			candSens = beamSearch(confusionSet, beamSize, sentence, locations);
			return candSens;
		}	

		return candSens;
	}

	@Override
	public double getSourceModelLogScore(Sentence candidate) {
		return nGramModel.getSentenceLogProb(candidate, order);
	}

	@Override
	public double getChannelModelLogScore(Sentence sentence, int location, String candidate, HashSet<String> cands) {
		double total = getTotalCharcterCount(cands, dictionary);
		int count = dictionary.getCount(candidate);
		
		return count / total;
	}
}

