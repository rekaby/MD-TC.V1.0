package parser;

import static utils.DictionarySet.DictionaryTypes.DEPLABEL;
import static utils.DictionarySet.DictionaryTypes.POS;
import static utils.DictionarySet.DictionaryTypes.WORD;
import static utils.DictionarySet.DictionaryTypes.WORDVEC;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.io.*;

import javax.management.relation.RelationType;

import parser.Options.PossibleLang;
import parser.visual.ContextConstances;
import parser.visual.ContextInstance;
import parser.visual.ContextRelation;
import parser.visual.ContextRelationType;
import parser.visual.LocationVerbs;
import parser.visual.io.ExcludedLemmasInGaps;
import parser.visual.io.PossibleGapsPoss;
import utils.Alphabet;
import utils.Dictionary;
import utils.DictionarySet;

public class DependencyInstance implements Serializable {
	
	public enum SpecialPos {
		C, P, PNX, V, AJ, N, OTHER,
	}

	private static final long serialVersionUID = 1L;
	
	public int length;
	public boolean train=true;
	// FORM: the forms - usually words, like "thought"
	public String[] forms;
	public String[] references;
	// LEMMA: the lemmas, or stems, e.g. "think"
	public String[] lemmas;
	
	// COARSE-POS: the coarse part-of-speech tags, e.g."V"
	public String[] cpostags;

	// FINE-POS: the fine-grained part-of-speech tags, e.g."VBD"
	public String[] postags;
	
	// MOST-COARSE-POS: the coarsest part-of-speech tags (about 11 in total)
	public SpecialPos[] specialPos;
	
	// FEATURES: some features associated with the elements separated by "|", e.g. "PAST|3P"
	public String[][] feats;

	// HEAD: the IDs of the heads for each element
	public int[] heads;

	// DEPREL: the dependency relations, e.g. "SUBJ"
	public String[] deprels;
	//these are list of IDS not the actuall words from the input, after he reads the input he looksup in the Dicts to return the IDs and hold them here in these lists
	public int[] formids;
	public int[] lemmaids;
	public int[] postagids;
	public int[] cpostagids;
	public int[] deprelids;
	public int[][] featids;//for each word in inst, there is array of features
	public int[] wordVecIds;

	public int[] deplbids;

	public ContextInstance contextInstance=new ContextInstance(); 
	public String[] contextReferences;
	public List<List<String>>  possibleContextReferences=new ArrayList<List<String>>();
	
	
    public DependencyInstance() {}
    
    public DependencyInstance(int length) { this.length = length; }
    
    public DependencyInstance(String[] forms) {
    	length = forms.length;
    	this.forms = forms;
    	this.feats = new String[length][];
    	this.deprels = new String[length];
    }
    
    public DependencyInstance(String[] forms, String[] postags, int[] heads) {
    	this.length = forms.length;
    	this.forms = forms;    	
    	this.heads = heads;
	    this.postags = postags;
    }
    
    public DependencyInstance(String[] forms, String[] postags, int[] heads, String[] deprels) {
    	this(forms, postags, heads);
    	this.deprels = deprels;    	
    }
    public DependencyInstance(String[] forms, String[] lemmas, String[] cpostags, String[] postags,
            String[][] feats, int[] heads, String[] deprels) {
    	this(forms, postags, heads, deprels);
    	this.lemmas = lemmas;    	
    	this.feats = feats;
    	this.cpostags = cpostags;
    }

    public DependencyInstance(String[] forms, String[] lemmas, String[] cpostags, String[] postags,
            String[][] feats, int[] heads, String[] deprels,String[]references) {
    	this(forms, lemmas,  cpostags, postags,feats, heads,  deprels);
    	this.references=references;
    }
    
    public DependencyInstance(DependencyInstance a) {
    	//this(a.forms, a.lemmas, a.cpostags, a.postags, a.feats, a.heads, a.deprels);
    	specialPos = a.specialPos;
    	length = a.length;
    	heads = a.heads;
    	formids = a.formids;
    	lemmaids = a.lemmaids;
    	postagids = a.postagids;
    	cpostagids = a.cpostagids;
    	deprelids = a.deprelids;
    	deplbids = a.deplbids;
    	featids = a.featids;
    	wordVecIds = a.wordVecIds;
    	contextInstance=a.contextInstance;
    	forms=a.forms;
    	lemmas=a.lemmas;
    	postags=a.postags;
    	cpostags=a.cpostags;
    	possibleContextReferences=a.possibleContextReferences;
    	contextReferences=a.contextReferences;
    	references=a.references;
    }
    public DependencyInstance  clone(DependencyInstance a) {
    	DependencyInstance b=new DependencyInstance();
    	//b.specialPos = a.specialPos;
    	b.length = a.length;
    	b.heads = new int[a.heads.length];
    	System.arraycopy(a.heads, 0, b.heads, 0, a.heads.length);
    	//b.heads = a.heads;
    	b.formids = new int[a.formids.length];
    	System.arraycopy(a.formids, 0, b.formids, 0, a.formids.length);
    	//b.formids = a.formids;
    	b.lemmaids = new int[a.lemmaids.length];
    	System.arraycopy(a.lemmaids, 0, b.lemmaids, 0, a.lemmaids.length);
    	//b.lemmaids = a.lemmaids;
    	b.postagids = new int[a.postagids.length];
    	System.arraycopy(a.postagids, 0, b.postagids, 0, a.postagids.length);
    	//b.postagids = a.postagids;
    	b.cpostagids = new int[a.cpostagids.length];
    	System.arraycopy(a.cpostagids, 0, b.cpostagids, 0, a.cpostagids.length);
    	//b.cpostagids = a.cpostagids;
    	//b.deprelids= new int[a.deprelids.length];
    	//System.arraycopy(a.deprelids, 0, b.deprelids, 0, a.deprelids.length);
    	b.deprelids = a.deprelids;
    	
    	b.deplbids  = new int[a.deplbids .length];
    	System.arraycopy(a.deplbids , 0, b.deplbids , 0, a.deplbids .length);
    	//b.deplbids = a.deplbids;
    	
    	b.featids = a.featids;
    	
    	b.wordVecIds = a.wordVecIds;
    	
    	b.contextInstance=a.contextInstance;
    	
    	b.forms = new String[a.forms.length];
    	System.arraycopy(a.forms, 0, b.forms, 0, a.forms.length);
    	//b.forms=a.forms;
    	if (a.deprels!=null) {
    		b.deprels= new String[a.deprels.length];
        	System.arraycopy(a.deprels, 0, b.deprels, 0, a.deprels.length);
        }
    	else
    	{
    		b.deprels= null;
    	}
    	
    	b.lemmas = new String[a.lemmas.length];
    	System.arraycopy(a.lemmas, 0, b.lemmas, 0, a.lemmas.length);
    	//b.lemmas=a.lemmas;
    	b.postags = new String[a.postags.length];
    	System.arraycopy(a.postags, 0, b.postags, 0, a.postags.length);
    	//b.postags=a.postags;
    	b.cpostags = new String[a.cpostags.length];
    	System.arraycopy(a.cpostags, 0, b.cpostags, 0, a.cpostags.length);
    	//b.cpostags=a.cpostags;
    	b.possibleContextReferences=a.possibleContextReferences;
    	
    	b.contextReferences = new String[a.contextReferences.length];
    	System.arraycopy(a.contextReferences, 0, b.contextReferences, 0, a.contextReferences.length);
    	//b.contextReferences=a.contextReferences;
    	
    	b.references = new String[a.references.length];
    	System.arraycopy(a.references, 0, b.references, 0, a.references.length);
    	
    	b.train=a.train;
    	//b.references=a.references;
    	return b;
    }
    //public void setDepIds(int[] depids) {
    //	this.depids = depids;
    //}
    
    
    public void setInstIds(DictionarySet dicts, 
    		HashMap<String, String> coarseMap, HashSet<String> conjWord, PossibleLang lang) {
    	    	
    	formids = new int[length];    	
		deplbids = new int[length];
		postagids = new int[length];
		cpostagids = new int[length];
		
    	for (int i = 0; i < length; ++i) {
    		formids[i] = dicts.lookupIndex(WORD, "form="+normalize(forms[i]));
			postagids[i] = dicts.lookupIndex(POS, "pos="+postags[i]);
			cpostagids[i] = dicts.lookupIndex(POS, "cpos="+cpostags[i]);
			deplbids[i] = dicts.lookupIndex(DEPLABEL, deprels[i]) - 1;	// zero-based
    	}
    	
    	if (lemmas != null) {
    		lemmaids = new int[length];
    		for (int i = 0; i < length; ++i)
    			lemmaids[i] = dicts.lookupIndex(WORD, "lemma="+normalize(lemmas[i]));
    	}

		featids = new int[length][];
		for (int i = 0; i < length; ++i) if (feats[i] != null) {
			featids[i] = new int[feats[i].length];
			for (int j = 0; j < feats[i].length; ++j)
				featids[i][j] = dicts.lookupIndex(POS, "feat="+feats[i][j]);
		}
		
		if (dicts.size(WORDVEC) > 0) {
			wordVecIds = new int[length];
			for (int i = 0; i < length; ++i) {
				int wvid = dicts.lookupIndex(WORDVEC, forms[i]);
				if (wvid <= 0) wvid = dicts.lookupIndex(WORDVEC, forms[i].toLowerCase());
				if (wvid > 0) wordVecIds[i] = wvid; else wordVecIds[i] = -1; 
			}
		}
		
		// set special pos
		specialPos = new SpecialPos[length];
		for (int i = 0; i < length; ++i) {
			if (coarseMap.containsKey(postags[i])) {
				String cpos = coarseMap.get(postags[i]);
				if ((cpos.equals("CONJ")
						|| PossibleLang.Japanese == lang) && conjWord.contains(forms[i])) {
					specialPos[i] = SpecialPos.C;
				}
				else if (cpos.equals("ADP"))
					specialPos[i] = SpecialPos.P;
				else if (cpos.equals("."))
					specialPos[i] = SpecialPos.PNX;
				else if (cpos.equals("VERB"))
					specialPos[i] = SpecialPos.V;
				else
					specialPos[i] = SpecialPos.OTHER;
			}
			else {
				//System.out.println("Can't find coarse map: " + postags[i]);
				//coarseMap.put(postags[i], "X");
				specialPos[i] = getSpecialPos(forms[i], postags[i]);
			}
		}
    }

    public boolean isTrain() {
		return train;
	}

	public void setTrain(boolean train) {
		this.train = train;
	}

	// Heuristic rules to "guess" POS type based on the POS tag string 
    // This is an extended version of the rules in EGSTRA code
    // 	(http://groups.csail.mit.edu/nlp/egstra/).
    //
    private SpecialPos getSpecialPos(String form, String tag) {
    	if (tag.charAt(0) == 'v' || tag.charAt(0) == 'V')
    		return SpecialPos.V;
    	else if (tag.charAt(0) == 'n' || tag.charAt(0) == 'N')
    		return SpecialPos.N;
    	else if (tag.equalsIgnoreCase("cc") ||
    		tag.equalsIgnoreCase("conj") ||
    		tag.equalsIgnoreCase("kon") ||
    		tag.equalsIgnoreCase("conjunction"))
    		return SpecialPos.C;
    	else if (tag.equalsIgnoreCase("prep") ||
    			 tag.equalsIgnoreCase("preposition") ||
    			 tag.equals("IN"))
    		return SpecialPos.P;
    	else if (tag.equalsIgnoreCase("punc") ||
    			 tag.equals("$,") ||
    			 tag.equals("$.") ||
    			 tag.equals(",") ||
    			 tag.equals(";") ||
    			 Evaluator.puncRegex.matcher(form).matches())
    		return SpecialPos.PNX;
    	else
    		return SpecialPos.OTHER;
    }
	
    private String normalize(String s) {
		if(s!=null && s.matches("[0-9]+|[0-9]+\\.[0-9]+|[0-9]+[0-9,]+"))
		    return "<num>";
		return s;
    }
    public void definePossibleContextReferences()
    {
    	possibleContextReferences = new ArrayList<List<String>>();
    	 contextReferences = new String[length ];
    	for (int i = 0; i < postags.length; i++) {
    		contextReferences[i]="";
    		Set<String>possibleReferences=new HashSet<String>();
    		if (forms[i].equalsIgnoreCase(ContextConstances.GAP_SIGN)) {
    			possibleReferences=new HashSet<String>();
    			 for (ContextRelation relation : contextInstance.getAllRelations()) {
    				 if (!(relation.getType().name().equalsIgnoreCase(ContextRelationType.$InferAgent$.name())||
    						 relation.getType().name().equalsIgnoreCase(ContextRelationType.$InferTheme$.name()))) 
    				{
    					 possibleReferences.add(relation.getVerb());	
    				}
    				 possibleReferences.add(relation.getEntity());	 
    				  
    				 
    			}
    			 possibleContextReferences.add(new ArrayList<String>(possibleReferences));
    			 continue;
			}
    		
    		try {//yes
    				PossibleGapsPoss pos;
    		        pos= PossibleGapsPoss.valueOf(postags[i]);
    		        possibleReferences=new HashSet<String>();
    		        	try {
    		        		ExcludedLemmasInGaps excluded;
    		        		excluded=ExcludedLemmasInGaps.valueOf(lemmas[i]);
    		        		//here means no exception then we should ignore this word
    		        		possibleReferences.add("");
    		        		possibleContextReferences.add(new ArrayList<String>(possibleReferences));
    		        		continue;
    		        	} catch (Exception e) {
    		        		//Do nothing...just continue
    		        	}
    		        for (ContextRelation relation : contextInstance.getAllRelations()) {//exact match loop
    		        	if (relation.getVerb().substring(0,relation.getVerb().indexOf(ContextConstances.REFERENCE_SEPARATOR)!=-1?
    		        			relation.getVerb().indexOf(ContextConstances.REFERENCE_SEPARATOR):relation.getVerb().length()).equalsIgnoreCase(lemmas[i])) {
    		        		possibleReferences.add(relation.getVerb());
    		        	}	
    		        	if (relation.getEntity().substring(0,relation.getEntity().indexOf(ContextConstances.REFERENCE_SEPARATOR)!=-1?
    		        			relation.getEntity().indexOf(ContextConstances.REFERENCE_SEPARATOR):relation.getEntity().length()).equalsIgnoreCase(lemmas[i])) {
    		        		possibleReferences.add(relation.getEntity());
    		        	}	
					}
    		        if (possibleReferences.size()==0) {//TODO, deal with inexact match or just respond with empty  
    		        	possibleReferences.add("");
					}
    		        possibleContextReferences.add(new ArrayList<String>(possibleReferences));
    		        continue;
    		    } catch (IllegalArgumentException ex) {  
    		        //this pos is not included in our possible ambigious poss
    		    	possibleReferences=new HashSet<String>();
    		    	possibleReferences.add("");
    		    	possibleContextReferences.add(new ArrayList<String>(possibleReferences));
    		    	continue;
    		  }
		}
    }
    public int getGapId()
    {
    	for (int i = 1; i <= length; i++) {
			if (forms[i].equals(ContextConstances.GAP_SIGN)) {
				return i;
			}
		}
    	return -1;
    }
    public DependencyInstance enrichDependencyInstanceContext()
    {
    	if (hasLocationVerb(this)) {
			List<ContextRelation>[] results=
					this.contextInstance.inferLocationsToAgentsAndThemes(this.contextInstance.getLocationRelations());
					//this.contextInstance.inferLocationsToAgentsAndThemes(this.contextInstance.getAgentRelations(), this.contextInstance.getThemeRelations(), this.contextInstance.getLocationRelations());
			//this.contextInstance.setAgentRelations(results[0]);
			//this.contextInstance.setThemeRelations(results[1]);
			this.contextInstance.setInferedAgentRelations(results[0]);
			this.contextInstance.setInferedThemeRelations(results[1]);
			
		    
		}
    	return this;
    }
    
    private boolean hasLocationVerb(DependencyInstance inst)
    { 
    	for (LocationVerbs verb : LocationVerbs.values()) {
    		for (String lemma : inst.lemmas) {
        		if (lemma.equalsIgnoreCase(verb.name())) {
					return true;
				}
        	}	
    	}
    	return false;
    	
    }
}
