package parser.feature;


import static parser.feature.FeatureTemplate.Arc.*;
import static parser.feature.FeatureTemplate.Word.*;
import gnu.trove.set.hash.TLongHashSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import parser.DependencyArcList;
import parser.DependencyInstance;
import parser.DependencyPipe;
import parser.GlobalFeatureData;
import parser.LowRankParam;
import parser.Options;
import parser.Parameters;
import parser.DependencyInstance.SpecialPos;
import parser.Options.LearningMode;
import parser.feature.FeatureTemplate.Arc;
import parser.feature.FeatureTemplate.Word;
import parser.visual.ContextConstances;
import parser.visual.ContextRelation;
import utils.Alphabet;
import utils.FeatureVector;
import utils.Utils;

public class SyntacticFeatureFactory implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public int TOKEN_START = 1;
	public int TOKEN_END = 2;
	public int TOKEN_MID = 3;

	// for punc
	public int TOKEN_QUOTE = 4;
	public int TOKEN_RRB = 5;
	public int TOKEN_LRB = 6;
	
	public Options options;
	
	public double[][] wordVectors = null;
	public double[] unknownWv = null;
	
	public int tagNumBits, wordNumBits, depNumBits, disNumBits = 4;//wordNumBits is word number in the dictionary which is bigger than the words in sentences coz have others and start, end tokens
	//Tagnumbits is the count of dictionary in POS
	//depNumBits is Deprel in dictionary Dep
	public int labelNumBits, flagBits;
	
	public int ccDepType;
	
	public final int numArcFeats = 115911564;	// number of arc structure features
	public final int numLabeledArcFeats = 115911564;
	public int numWordFeats;			// number of word features
	
	private boolean stoppedGrowth;
	private transient TLongHashSet featureHashSet;
	private Alphabet wordAlphabet;		// the alphabet of word features (e.g. \phi_h, \phi_m)
	//private Alphabet arcAlphabet;		// the alphabet of 1st order arc features (e.g. \phi_{h->m})
	
	public SyntacticFeatureFactory(Options options)
	{
		this.options = options;
		
		wordAlphabet = new Alphabet();
		//arcAlphabet = new Alphabet();
		
		stoppedGrowth = false;
		featureHashSet = new TLongHashSet(100000);
		
		//numArcFeats = 0;
		numWordFeats = 0;
	}
	
	public void closeAlphabets()
	{
		wordAlphabet.stopGrowth();
		//arcAlphabet.stopGrowth();
		
		stoppedGrowth = true;
	}
    public void initFeatureAlphabets(DependencyInstance inst) 
    {
    	List<ContextRelation> usedRelations=new ArrayList<ContextRelation>();
        int n = inst.length;
        
    	// word 
        for (int i = 0; i < n; ++i)
            createWordFeatures(inst, i);
    	
        int[] heads = inst.heads;
        int[] deplbids = inst.deplbids;
    	
        DependencyArcList arcLis = new DependencyArcList(heads, options.useHO);
        
        // 1st order arc
        for (int i = 0; i < n; ++i) {
    		
    		if (heads[i] == -1) continue;
    	     
    		int parent = heads[i];
    		createArcFeatures(inst, parent, i, usedRelations);	// arc features    		
    		if (options.learnLabel) {
    			int type = deplbids[i]; 
    			boolean toRight = parent < i;
    			//createLabelFeatures(inst, parent, type, toRight, false);
    			//createLabelFeatures(inst, i, type, toRight, true);
    			//createLabelFeatures(inst, heads[i], i, type);
    			createLabelFeatures(inst, arcLis, heads, i, type, usedRelations);
    		}
    	}
    	
        if (options.learningMode != LearningMode.Basic) {
        	
        	//DependencyArcList arcLis = new DependencyArcList(heads);
    		
    		// 2nd order (h,m,s) & (m,s)
    		for (int h = 0; h < n; ++h) {
    			
    			int st = arcLis.startIndex(h);
    			int ed = arcLis.endIndex(h);
    			
    			for (int p = st; p+1 < ed; ++p) {
    				// mod and sib
    				int m = arcLis.get(p);
    				int s = arcLis.get(p+1);
    				
    				if (options.useCS) {
    					createTripsFeatureVector(inst, h, m, s);
    					createSibFeatureVector(inst, m, s/*, false*/);
    				}
					
					// gp-sibling
					int gp = heads[h];
					if (options.useGS && gp >= 0) {
						createGPSibFeatureVector(inst, gp, h, m, s);
					}
					
					// tri-sibling
					if (options.useTS && p + 2 < ed) {
						int s2 = arcLis.get(p + 2);
						createTriSibFeatureVector(inst, h, m, s, s2);
					}
					
					// parent, sibling and child
					if (options.usePSC) {
						// mod's child
						int mst = arcLis.startIndex(m);
						int med = arcLis.endIndex(m);
						
						for (int mp = mst; mp < med; ++mp) {
							int c = arcLis.get(mp);
							createPSCFeatureVector(inst, h, m, c, s);
						}
						
						// sib's child
						int sst = arcLis.startIndex(s);
						int sed = arcLis.endIndex(s);
						
						for (int sp = sst; sp < sed; ++sp) {
							int c = arcLis.get(sp);
							createPSCFeatureVector(inst, h, s, c, m);
						}
					}
    			}
    		}
			
			for (int m = 1; m < n; ++m) {
				int h = heads[m];
				
				Utils.Assert(h >= 0);
				
				// grandparent
				int gp = heads[h];
				if (options.useGP && gp != -1) {
					createGPCFeatureVector(inst, gp, h, m);
				}
				
				// head bigram
				if (options.useHB && m + 1 < n) {
					int h2 = heads[m + 1];
					Utils.Assert(h2 >= 0);
					
					createHeadBiFeatureVector(inst, m, h, h2);
				}
				
				// great-grandparent
				if (options.useGGP && gp != -1 && heads[gp] != -1) {
					int ggp = heads[gp];
					createGGPCFeatureVector(inst, ggp, gp, h, m);
				}
			}
			
			// global feature
			if (options.useHO) {
				FeatureVector fv = new FeatureVector(numArcFeats);
				
				// non-proj
				//for (int i = 0; i < n; ++i) {
				//	if (heads[i] == -1)
				//		continue;
				//	int num = getBinnedDistance(arcLis.nonproj[i]);
				//	createNonprojFeatureVector(inst, num, heads[i], i);
				//}

				int[] toks = inst.formids;
				int[] pos = inst.postagids;
				int[] posA = inst.cpostagids;
				SpecialPos[] specialPos = inst.specialPos;
				int[] spanLeft = arcLis.left;
				int[] spanRight = arcLis.right;

				long code = 0;

				for (int i = 0; i < n; ++i) {
					// pp attachment
					if (SpecialPos.P == specialPos[i]) {
						int par = heads[i];
						int[] c = findPPArg(inst.heads, inst.specialPos, arcLis, i);
						for (int z = 0; z < c.length; ++z) {
							if (par != -1 && c[z] != -1) {
								createPPFeatureVector(inst, par, i, c[z]);
							}
						}
					}

					// conjunction pos
					if (SpecialPos.C == specialPos[i]) {
						int[] arg = findConjArg(arcLis, heads, i);
						int head = arg[0];
						int left = arg[1];
						int right = arg[2];
						if (left != -1 && right != -1 && left < right) {
							createCC1FeatureVector(inst, left, i, right);
							if (head != -1) {
								createCC2FeatureVector(inst, i, head, left);
								createCC2FeatureVector(inst, i, head, right);
							}
						}
					}

					// punc head
					if (SpecialPos.PNX == specialPos[i]) {
						int j = findPuncCounterpart(toks, i);
						if (j != -1 && heads[i] == heads[j])
							createPNXFeatureVector(inst, heads[i], i, j);
					}
				}

				int rb = getMSTRightBranch(specialPos, arcLis, 0, 0);
				
				code = createArcCodeP(Arc.RB, 0x0);
				addArcFeature(code, (double)rb / n, fv);
				
				for (int m = 1; m < n; ++m) {

					// child num
					int leftNum = 0;
					int rightNum = 0;
					
					// **CHECK**
					int maxDigit = 64 - Arc.numArcFeatBits - flagBits;
					//int maxDigit = 64 - Arc.numArcFeatBits - 4;
					
					int maxChildStrNum = (maxDigit / tagNumBits) - 1;
					int childStrNum = 0;
					code = pos[m];
					
					int st = arcLis.startIndex(m);
					int ed = arcLis.endIndex(m);
					
					for (int j = st; j < ed; ++j) {
						int cid = arcLis.get(j);
						if (SpecialPos.PNX != specialPos[cid]) {
							if (cid < m && leftNum < GlobalFeatureData.MAX_CHILD_NUM)
								leftNum++;
							else if (cid > m && rightNum < GlobalFeatureData.MAX_CHILD_NUM)
								rightNum++;
							if (childStrNum < maxChildStrNum) {
								code = ((code << tagNumBits) | pos[cid]);
								childStrNum++;
							}
						}
					}
					
					// **CHECK**
					code = ((code << Arc.numArcFeatBits) | Arc.CN_STR.ordinal()) << flagBits;
					//code = ((code << Arc.numArcFeatBits) | Arc.CN_STR.ordinal()) << 4;
					addArcFeature(code, fv);

					createChildNumFeatureVector(inst, m, leftNum, rightNum);

					// span
					int end = spanRight[m] == n ? 1 : 0;
					int punc = (spanRight[m] < n && SpecialPos.PNX == specialPos[spanRight[m]]) ? 1 : 0;
					int bin = Math.min(GlobalFeatureData.MAX_SPAN_LENGTH, (spanRight[m] - spanLeft[m]));
					createSpanFeatureVector(inst, m, end, punc, bin);

					if (heads[m] != -1) {
						// neighbors
						int leftID = spanLeft[m] > 0 ? posA[spanLeft[m] - 1] : TOKEN_START;
						int rightID = spanRight[m] < n ? posA[spanRight[m]] : TOKEN_END;
						if (leftID > 0 && rightID > 0) {
							createNeighborFeatureVector(inst, heads[m], m, leftID, rightID);
						}
					}
				}
			}
        }		
    }
    
    
	/************************************************************************
	 * Region start #
	 * 
	 *  Functions that create feature vectors for arc structures in the
	 *  sentence. Arc structures could be 1-order arcs (i.e. parent-child),
	 *  2-order arcs (e.g. parent-siblings or grandparent-parent-child) and
	 *  so on.
	 *  
	 ************************************************************************/
    
    /**
     * Create 1st order feature vector of an dependency arc. 
     * 
     * This is an re-implementation of MST parser 1st order feature construction. 
     * There is slightly difference on constructions of morphology features and 
     * bigram features, in order to reduce redundancy.
     * 
     * @param inst 	the current sentence
     * @param h		index of the head
     * @param c		index of the modifier
     * @return
     */
    public FeatureVector createArcFeatures(DependencyInstance inst, int h, int c,List<ContextRelation> usedRelations) 
    {
    	
    	int attDist = getBinnedDistance(h-c);//this is from 1-14, 1-7 if head>M, and from 8-17 if M>H, this will be ORed with code of features, 
    										//so always the features code are shifter 4 bits left
    	
    	FeatureVector fv = new FeatureVector(numArcFeats);
    	
    	addBasic1OFeatures(fv, inst, h, c, attDist);
    	//System.out.println("1--------------C="+c+ " H="+h+"="+fv.size());
    	addCore1OPosFeatures(fv, inst, h, c, attDist);
    	//System.out.println("2--------------C="+c+ " H="+h+"="+fv.size());	    		
    	addCore1OBigramFeatures(fv, inst.formids[h], inst.postagids[h], 
    			inst.formids[c], inst.postagids[c], attDist);
    	//System.out.println("3--------------C="+c+ " H="+h+"="+fv.size());    		
		if (inst.lemmaids != null)
			addCore1OBigramFeatures(fv, inst.lemmaids[h], inst.postagids[h], 
					inst.lemmaids[c], inst.postagids[c], attDist);
		//System.out.println("4--------------C="+c+ " H="+h+"="+fv.size());
		addCore1OBigramFeatures(fv, inst.formids[h], inst.cpostagids[h], 
    			inst.formids[c], inst.cpostagids[c], attDist);
		//System.out.println("5--------------C="+c+ " H="+h+"="+fv.size());
		if (inst.lemmaids != null)
			addCore1OBigramFeatures(fv, inst.lemmaids[h], inst.cpostagids[h], 
					inst.lemmaids[c], inst.cpostagids[c], attDist);
		//System.out.println("6--------------C="+c+ " H="+h+"="+fv.size());
    	if (inst.featids[h] != null && inst.featids[c] != null) {
    		for (int i = 0, N = inst.featids[h].length; i < N; ++i)
    			for (int j = 0, M = inst.featids[c].length; j < M; ++j) {
    				
    				addCore1OBigramFeatures(fv, inst.formids[h], inst.featids[h][i], 
    						inst.formids[c], inst.featids[c][j], attDist);
    				
    				if (inst.lemmas != null)
    					addCore1OBigramFeatures(fv, inst.lemmaids[h], inst.featids[h][i], 
    							inst.lemmaids[c], inst.featids[c][j], attDist);
    			}
    	}
    	//System.out.println("7--------------C="+c+ " H="+h+"="+fv.size());
    	if(options.context && ContextConstances.useContextFeatures)//&1==2
    	{
    		int old=fv.size();
    		//System.out.println("Before c="+c+ " h="+h+"="+fv.size());
    		addArcContextFeatures(fv, inst, h, c, attDist,-1,usedRelations);// -1 as default value which means no type
    		if(fv.size()>old)
    			{
    			//System.out.println("---------------C="+c+ " H="+h+"="+fv.size()+ " - "+old);
    			}
    		else
    			{
    			//System.out.println("Context---------C="+c+ " H="+h+"="+fv.size()+ " - "+old);
    			}
    	}
    			
    	return fv;
    }
    
    public void addArcContextFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int m, int attDist, int type,List<ContextRelation> usedRelations) 
    {
    	
    	addArcContextGenericFeatures(fv, inst, h, m, attDist,type,"AG",usedRelations);
    	addArcContextGenericFeatures(fv, inst, h, m, attDist,type,"TH",usedRelations);
    	addArcContextGenericFeatures(fv, inst, h, m, attDist,type,"IAG",usedRelations);
    	addArcContextGenericFeatures(fv, inst, h, m, attDist,type,"ITH",usedRelations);
   // 	addArcContextInstrumentFeatures(fv, inst, h, m, attDist,type);
    	addArcContextGenericFeatures(fv, inst, h, m, attDist,type,"OWN",usedRelations);
    	addArcContextGenericFeatures(fv, inst, h, m, attDist,type,"LOC",usedRelations);
    	addArcContextGenericFeatures(fv, inst, h, m, attDist,type,"NXT",usedRelations);
    	addArcContextGenericFeatures(fv, inst, h, m, attDist,type,"PRO",usedRelations);
    }
    public void addArcContextGenericFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int m, int attDist, int type,String prefix,List<ContextRelation> usedRelations) 
    {
    	List<ContextRelation> relations=new ArrayList<ContextRelation>();
    	int weight=0;
    	switch (prefix) {
		case "AG":
			relations=inst.contextInstance.getFilteredAgentRelations();//getFilteredAgentRelations();//AgentRelations();
			weight=ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL;
			break;
		case "TH":
			relations=inst.contextInstance.getFilteredThemeRelations();//getFilteredThemeRelations();//ThemeRelations
			weight=ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL;
			break;
		case "IAG":
			relations=inst.contextInstance.getFilteredInferedAgentRelations();//getFilteredInferedAgentRelations();//Infered
			weight=ContextConstances.ARC_HIGH_WIEGHT_INFERED;
			break;
		case "ITH":
			relations=inst.contextInstance.getFilteredInferedThemeRelations();//getFilteredInferedThemeRelations();//Infered
			weight=ContextConstances.ARC_HIGH_WIEGHT_INFERED;
			break;
		case "OWN":
			relations=inst.contextInstance.getOwnershipRelations();
			weight=ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL;
			break;
		case "LOC":
			relations=inst.contextInstance.getLocationRelations();
			weight=ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL;
			break;
		case "NXT":
			relations=inst.contextInstance.getNextToRelations();
			weight=ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL;
			break;
		case "PRO":
			relations=inst.contextInstance.getPropertyRelations();
			weight=ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL;
			break;

		default:
			break;
		}
    	for (int j = 0; j < relations.size(); j++) {//this word feature will not be added if the word is not agent in any relation
        	ContextRelation relation =relations.get(j);
        	if (usedRelations.contains(relation)) {
				//continue;
			}
        	/*if(relation.hasEntity(inst.forms,m))//m is the object
        	{
        		if(relation.hasAction(inst.forms,h))//h is the verb
        		{
        			addCore1OPosAgentFeatures(fv, inst, h, m, attDist,type);
        			addCore1OBigramAgentFeatures(fv,inst, inst.formids[h], inst.postagids[h], inst.formids[m], inst.postagids[m], attDist,type);
        			//return;
        		}
        	}*/
        	if (inst.isTrain()) {
        		if ((!inst.references[m].equals("")&&relation.getVerb().equalsIgnoreCase(inst.references[m]) )
        				||(inst.references[m].equals("")&&inst.possibleContextReferences.get(m).contains(relation.getVerb()))) 
        		{
        			if ((!inst.references[h].equals("")&&relation.getEntity().equalsIgnoreCase(inst.references[h]) )
        					||(inst.references[h].equals("")&&inst.possibleContextReferences.get(h).contains(relation.getEntity()))) {
        				addCore1OPosContextGenericFeaturesSimple(fv, inst, h, m, attDist,type,prefix,weight);
        				addCore1OBigramContextGenericFeaturesSimple(fv,inst,h,m, inst.formids[h], inst.cpostagids[h], inst.formids[m], inst.cpostagids[m], attDist,type,prefix,weight);
        				if (ContextConstances.DEBUG) {
        	    			System.out.println("Train h:"+h+" m:"+m+relation.getVerb()+ " "+ relation.getEntity()+ " "+relation.getType().name());
        				}
        				usedRelations.add(relation);
        				break;
        			}
        		}
        	
        		if ((!inst.references[m].equals("")&&relation.getEntity().equalsIgnoreCase(inst.references[m]) )
        				||(inst.references[m].equals("")&&inst.possibleContextReferences.get(m).contains(relation.getEntity()))) 
              		{
              			if ((!inst.references[h].equals("")&&relation.getVerb().equalsIgnoreCase(inst.references[h]) )
              					||(inst.references[h].equals("")&&inst.possibleContextReferences.get(h).contains(relation.getVerb()))) {
              				addCore1OPosContextGenericFeaturesSimple(fv, inst, h, m, attDist,type,prefix,weight);
              				addCore1OBigramContextGenericFeaturesSimple(fv,inst,h,m, inst.formids[h], inst.cpostagids[h], inst.formids[m], inst.cpostagids[m], attDist,type,prefix,weight);
              				if (ContextConstances.DEBUG) {
            	    			System.out.println("Train h:"+h+" m:"+m+relation.getVerb()+ " "+ relation.getEntity()+ " "+relation.getType().name());
              				}
              				usedRelations.add(relation);
              				break;
              			}
              		}
        	}
    		
    	
        	if (!inst.isTrain()) {
        		if ((!inst.contextReferences[m].equals("")&&relation.getVerb().equalsIgnoreCase(inst.contextReferences[m]) )
        				||(inst.contextReferences[m].equals("")&&inst.possibleContextReferences.get(m).contains(relation.getVerb()))) 
        		{
        			if ((!inst.contextReferences[h].equals("")&&relation.getEntity().equalsIgnoreCase(inst.contextReferences[h]) )
        					||(inst.contextReferences[h].equals("")&&inst.possibleContextReferences.get(h).contains(relation.getEntity()))) {
        				addCore1OPosContextGenericFeaturesSimple(fv, inst, h, m, attDist,type,prefix,weight);
        				addCore1OBigramContextGenericFeaturesSimple(fv,inst,h,m, inst.formids[h], inst.cpostagids[h], inst.formids[m], inst.cpostagids[m], attDist,type,prefix,weight);
        				if (ContextConstances.DEBUG) {
        	    			System.out.println("Test h:"+h+" m:"+m+relation.getVerb()+ " "+ relation.getEntity()+ " "+relation.getType().name());
        				}
        				usedRelations.add(relation);
        				break;
        			}
        		}
        	
        		if ((!inst.contextReferences[m].equals("")&&relation.getEntity().equalsIgnoreCase(inst.contextReferences[m]) )
        				||(inst.contextReferences[m].equals("")&&inst.possibleContextReferences.get(m).contains(relation.getEntity()))) 
              		{
              			if ((!inst.contextReferences[h].equals("")&&relation.getVerb().equalsIgnoreCase(inst.contextReferences[h]) )
              					||(inst.contextReferences[h].equals("")&&inst.possibleContextReferences.get(h).contains(relation.getVerb()))) {
              				addCore1OPosContextGenericFeaturesSimple(fv, inst, h, m, attDist,type,prefix,weight);
              				addCore1OBigramContextGenericFeaturesSimple(fv,inst,h,m, inst.formids[h], inst.cpostagids[h], inst.formids[m], inst.cpostagids[m], attDist,type,prefix,weight);
              				if (ContextConstances.DEBUG) {
            	    			System.out.println("Test h:"+h+" m:"+m+relation.getVerb()+ " "+ relation.getEntity()+ " "+relation.getType().name());
              				}
              				usedRelations.add(relation);
              				break;
              			}
              		}
        		}
    		}
    }
    /*
    
    public void addArcContextAgentFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int m, int attDist, int type) 
    {
    	for (int j = 0; j < inst.contextInstance.getAgentRelations().size(); j++) {//this word feature will not be added if the word is not agent in any relation
        	ContextRelation relation =inst.contextInstance.getAgentRelations().get(j);
        	//if(relation.hasEntity(inst.forms,m))//m is the object
        	//{
        	//	if(relation.hasAction(inst.forms,h))//h is the verb
        	//	{
        	//		addCore1OPosAgentFeatures(fv, inst, h, m, attDist,type);
        	//		addCore1OBigramAgentFeatures(fv,inst, inst.formids[h], inst.postagids[h], inst.formids[m], inst.postagids[m], attDist,type);
        			//return;
        	//	}
        	//}
        	if ((!inst.contextReferences[m].equals("")&&relation.getVerb().equalsIgnoreCase(inst.contextReferences[m]) )
        	  ||(inst.contextReferences[m].equals("")&&inst.possibleContextReferences.get(m).contains(relation.getVerb()))) 
        	{
        		if ((!inst.contextReferences[h].equals("")&&relation.getEntity().equalsIgnoreCase(inst.contextReferences[h]) )
        	        	  ||(inst.contextReferences[h].equals("")&&inst.possibleContextReferences.get(h).contains(relation.getEntity()))) {
        			addCore1OPosAgentFeatures(fv, inst, h, m, attDist,type);
        			addCore1OBigramAgentFeatures(fv,inst, inst.formids[h], inst.postagids[h], inst.formids[m], inst.postagids[m], attDist,type);
        			continue;
        		}
			}
        	if ((!inst.contextReferences[m].equals("")&&relation.getEntity().equalsIgnoreCase(inst.contextReferences[m]) )
              	  ||(inst.contextReferences[m].equals("")&&inst.possibleContextReferences.get(m).contains(relation.getEntity()))) 
              	{
              		if ((!inst.contextReferences[h].equals("")&&relation.getVerb().equalsIgnoreCase(inst.contextReferences[h]) )
              	        	  ||(inst.contextReferences[h].equals("")&&inst.possibleContextReferences.get(h).contains(relation.getVerb()))) {
              			addCore1OPosAgentFeatures(fv, inst, h, m, attDist,type);
              			addCore1OBigramAgentFeatures(fv,inst, inst.formids[h], inst.postagids[h], inst.formids[m], inst.postagids[m], attDist,type);
              			continue;
              		}
      			}
        	
    	}
    }
    public void addArcContextThemeFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int m, int attDist, int type) 
    {
    	for (int j = 0; j < inst.contextInstance.getThemeRelations().size(); j++) {//this word feature will not be added if the word is not agent in any relation
        	ContextRelation relation =inst.contextInstance.getThemeRelations().get(j);
        	//if(relation.hasEntity(inst.forms,m))//m is the object
        	//{
        	//	if(relation.hasAction(inst.forms,h))//h is the verb
        	//	{
        	//		addCore1OPosThemeFeatures(fv, inst, h, m, attDist,type);
        	//		addCore1OBigramThemeFeatures(fv,inst, inst.formids[h], inst.postagids[h], inst.formids[m], inst.postagids[m], attDist,type);
        	//		//return;
        	//	}
        	//}
        	if ((!inst.contextReferences[m].equals("")&&relation.getVerb().equalsIgnoreCase(inst.contextReferences[m]) )
              	  ||(inst.contextReferences[m].equals("")&&inst.possibleContextReferences.get(m).contains(relation.getVerb()))) 
              	{
              		if ((!inst.contextReferences[h].equals("")&&relation.getEntity().equalsIgnoreCase(inst.contextReferences[h]) )
              	        	  ||(inst.contextReferences[h].equals("")&&inst.possibleContextReferences.get(h).contains(relation.getEntity()))) {
              			addCore1OPosThemeFeatures(fv, inst, h, m, attDist,type);
              			addCore1OBigramThemeFeatures(fv,inst, inst.formids[h], inst.postagids[h], inst.formids[m], inst.postagids[m], attDist,type);
              			continue;
              		}
      			}
              	if ((!inst.contextReferences[m].equals("")&&relation.getEntity().equalsIgnoreCase(inst.contextReferences[m]) )
                    	  ||(inst.contextReferences[m].equals("")&&inst.possibleContextReferences.get(m).contains(relation.getEntity()))) 
                    	{
                    		if ((!inst.contextReferences[h].equals("")&&relation.getVerb().equalsIgnoreCase(inst.contextReferences[h]) )
                    	        	  ||(inst.contextReferences[h].equals("")&&inst.possibleContextReferences.get(h).contains(relation.getVerb()))) {
                    			addCore1OPosThemeFeatures(fv, inst, h, m, attDist,type);
                    			addCore1OBigramThemeFeatures(fv,inst, inst.formids[h], inst.postagids[h], inst.formids[m], inst.postagids[m], attDist,type);
                    			continue;
                    		}
            	}
    	}

    }
    */
 /*   public void addArcContextInstrumentFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int m, int attDist, int type) 
    {
    	for (int j = 0; j < inst.contextInstance.getInstrumentRelations().size(); j++) {//this word feature will not be added if the word is not agent in any relation
        	ContextRelation relation =inst.contextInstance.getInstrumentRelations().get(j);
        	if(relation.hasEntity(inst.forms,m))//m is the object
        	{
        		if(relation.hasAction(inst.forms,h))//h is the verb
        		{
        			addCore1OPosInstrumentFeatures(fv, inst, h, m, attDist,type);
        			addCore1OBigramInstrumentFeatures(fv,inst, inst.formids[h], inst.postagids[h], inst.formids[m], inst.postagids[m], attDist,type);
        			//return;
        		}
        	}
    	}

    }
*/
    /*
    public void addArcContextOwnerFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int m, int attDist, int type) 
    {
    	for (int j = 0; j < inst.contextInstance.getOwnershipRelations().size(); j++) {//this word feature will not be added if the word is not agent in any relation
        	ContextRelation relation =inst.contextInstance.getOwnershipRelations().get(j);
        	
        	// if(relation.hasOwned(inst.forms,m))//m is the object
        	//{
        	//	if(relation.hasOwner(inst.forms,h))//h is the verb
        	//	{
        	//		addCore1OPosOwnerFeatures(fv, inst, h, m, attDist,type);
        	//		addCore1OBigramOwnerFeatures(fv,inst, inst.formids[h], inst.postagids[h], inst.formids[m], inst.postagids[m], attDist,type);
        	//		//return;
        	//	}
        	//}
        	if ((!inst.contextReferences[m].equals("")&&relation.getVerb().equalsIgnoreCase(inst.contextReferences[m]) )
              	  ||(inst.contextReferences[m].equals("")&&inst.possibleContextReferences.get(m).contains(relation.getVerb()))) 
              	{
              		if ((!inst.contextReferences[h].equals("")&&relation.getEntity().equalsIgnoreCase(inst.contextReferences[h]) )
              	        	  ||(inst.contextReferences[h].equals("")&&inst.possibleContextReferences.get(h).contains(relation.getEntity()))) {
              			addCore1OPosOwnerFeatures(fv, inst, h, m, attDist,type);
              			addCore1OBigramOwnerFeatures(fv,inst, inst.formids[h], inst.postagids[h], inst.formids[m], inst.postagids[m], attDist,type);
              			continue;
              		}
      			}
              	if ((!inst.contextReferences[m].equals("")&&relation.getEntity().equalsIgnoreCase(inst.contextReferences[m]) )
                    	  ||(inst.contextReferences[m].equals("")&&inst.possibleContextReferences.get(m).contains(relation.getEntity()))) 
                    	{
                    		if ((!inst.contextReferences[h].equals("")&&relation.getVerb().equalsIgnoreCase(inst.contextReferences[h]) )
                    	        	  ||(inst.contextReferences[h].equals("")&&inst.possibleContextReferences.get(h).contains(relation.getVerb()))) {
                    			addCore1OPosOwnerFeatures(fv, inst, h, m, attDist,type);
                    			addCore1OBigramOwnerFeatures(fv,inst, inst.formids[h], inst.postagids[h], inst.formids[m], inst.postagids[m], attDist,type);
                    			continue;
                    		}
            			}
    	}

    }
    */
    public void addBasic1OFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int m, int attDist) 
    {
    	
    	long code = 0; 			// feature code
    	
    	int[] forms = inst.formids, lemmas = inst.lemmaids, postags = inst.postagids;
    	int[] cpostags = inst.cpostagids;
    	int[][] feats = inst.featids;
    	

    	code = createArcCodeW(CORE_HEAD_WORD, forms[h]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	    	    	
    	code = createArcCodeW(CORE_MOD_WORD, forms[m]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodeWW(HW_MW, forms[h], forms[m]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	int pHF = h == 0 ? TOKEN_START : (h == m+1 ? TOKEN_MID : forms[h-1]);
    	int nHF = h == inst.length - 1 ? TOKEN_END : (h+1 == m ? TOKEN_MID : forms[h+1]);
    	int pMF = m == 0 ? TOKEN_START : (m == h+1 ? TOKEN_MID : forms[m-1]);
    	int nMF = m == inst.length - 1 ? TOKEN_END : (m+1 == h ? TOKEN_MID : forms[m+1]);
    	
    	code = createArcCodeW(CORE_HEAD_pWORD, pHF);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodeW(CORE_HEAD_nWORD, nHF);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodeW(CORE_MOD_pWORD, pMF);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodeW(CORE_MOD_nWORD, nMF);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
	
		
    	code = createArcCodeP(CORE_HEAD_POS, postags[h]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodeP(CORE_HEAD_POS, cpostags[h]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodeP(CORE_MOD_POS, postags[m]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodeP(CORE_MOD_POS, cpostags[m]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodePP(HP_MP, postags[h], postags[m]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodePP(HP_MP, cpostags[h], cpostags[m]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	     	
    	if (lemmas != null) {
    		code = createArcCodeW(CORE_HEAD_WORD, lemmas[h]);
        	addArcFeature(code, fv);
        	addArcFeature(code | attDist, fv);
        	
    		code = createArcCodeW(CORE_MOD_WORD, lemmas[m]);
        	addArcFeature(code, fv);
        	addArcFeature(code | attDist, fv);
        	
        	code = createArcCodeWW(HW_MW, lemmas[h], lemmas[m]);
        	addArcFeature(code, fv);
        	addArcFeature(code | attDist, fv);
        	
	    	int pHL = h == 0 ? TOKEN_START : (h == m+1 ? TOKEN_MID : lemmas[h-1]);
	    	int nHL = h == inst.length - 1 ? TOKEN_END : (h+1 == m ? TOKEN_MID : lemmas[h+1]);
	    	int pML = m == 0 ? TOKEN_START : (m == h+1 ? TOKEN_MID : lemmas[m-1]);
	    	int nML = m == inst.length - 1 ? TOKEN_END : (m+1 == h ? TOKEN_MID : lemmas[m+1]);
	    	
	    	code = createArcCodeW(CORE_HEAD_pWORD, pHL);
	    	addArcFeature(code, fv);
	    	addArcFeature(code | attDist, fv);
	    	
	    	code = createArcCodeW(CORE_HEAD_nWORD, nHL);
	    	addArcFeature(code, fv);
	    	addArcFeature(code | attDist, fv);
	    	
	    	code = createArcCodeW(CORE_MOD_pWORD, pML);
	    	addArcFeature(code, fv);
	    	addArcFeature(code | attDist, fv);
	    	
	    	code = createArcCodeW(CORE_MOD_nWORD, nML);
	    	addArcFeature(code, fv);
	    	addArcFeature(code | attDist, fv);
    	}
    	
		if (feats[h] != null)
			for (int i = 0, N = feats[h].length; i < N; ++i) {
				code = createArcCodeP(CORE_HEAD_POS, feats[h][i]);
	        	addArcFeature(code, fv);
	        	addArcFeature(code | attDist, fv);
			}
		
		if (feats[m] != null)
			for (int i = 0, N = feats[m].length; i < N; ++i) {
				code = createArcCodeP(CORE_MOD_POS, feats[m][i]);
	        	addArcFeature(code, fv);
	        	addArcFeature(code | attDist, fv);
			}
		
		if (feats[h] != null && feats[m] != null) {
			for (int i = 0, N = feats[h].length; i < N; ++i)
				for (int j = 0, M = feats[m].length; j < M; ++j) {
			    	code = createArcCodePP(HP_MP, feats[h][i], feats[m][j]);
			    	addArcFeature(code, fv);
			    	addArcFeature(code | attDist, fv);
				}
		}
		
		if (wordVectors != null) {
			
			int wvid = inst.wordVecIds[h];
			double [] v = wvid > 0 ? wordVectors[wvid] : unknownWv;
			if (v != null) {
				for (int i = 0; i < v.length; ++i) {
					code = createArcCodeW(HEAD_EMB, i);
					addArcFeature(code, v[i], fv);
					addArcFeature(code | attDist, v[i], fv);
				}
			}
			
			wvid = inst.wordVecIds[m];
			v = wvid > 0 ? wordVectors[wvid] : unknownWv;
			if (v != null) {
				for (int i = 0; i < v.length; ++i) {
					code = createArcCodeW(MOD_EMB, i);
					addArcFeature(code, v[i], fv);
					addArcFeature(code | attDist, v[i], fv);
				}
			}
		}
    }
    
    
    
    
    
    
    public void addCore1OPosContextGenericFeaturesSimple(FeatureVector fv, DependencyInstance inst, 
    		int h, int c, int attDist, int type,String prefix, int weight) 
    {  	
    	
    	int[] pos = inst.postagids;
    	int[] posA = inst.cpostagids;
	
    	int pHead = pos[h], pHeadA = posA[h];
    	int pMod = pos[c], pModA = posA[c];
    	int pHeadLeft = h > 0 ? (h-1 == c ? TOKEN_MID : pos[h-1]) : TOKEN_START;    	
    	int pModRight = c < pos.length-1 ? (c+1 == h ? TOKEN_MID : pos[c+1]) : TOKEN_END;
    	int pHeadRight = h < pos.length-1 ? (h+1 == c ? TOKEN_MID: pos[h+1]) : TOKEN_END;
    	int pModLeft = c > 0 ? (c-1 == h ? TOKEN_MID : pos[c-1]) : TOKEN_START;
    	
    	int pHeadLeftA = h > 0 ? (h-1 == c ? TOKEN_MID : posA[h-1]) : TOKEN_START;    	
    	int pModRightA = c < posA.length-1 ? (c+1 == h ? TOKEN_MID : posA[c+1]) : TOKEN_END;
    	int pHeadRightA = h < posA.length-1 ? (h+1 == c ? TOKEN_MID: posA[h+1]) : TOKEN_END;
    	int pModLeftA = c > 0 ? (c-1 == h ? TOKEN_MID : posA[c-1]) : TOKEN_START;
    	
    	int wordHead=inst.lemmaids[h];
    	int wordMod=inst.lemmaids[c];
    	int wordHeadLeft = h > 0 ? (h-1 == c ? TOKEN_MID : inst.lemmaids[h-1]) : TOKEN_START;    	
    	int wordModRight = c < inst.length-1 ? (c+1 == h ? TOKEN_MID : inst.lemmaids[c+1]) : TOKEN_END;
    	int wordHeadRight = h < inst.length-1 ? (h+1 == c ? TOKEN_MID: inst.lemmaids[h+1]) : TOKEN_END;
    	int wordModLeft = c > 0 ? (c-1 == h ? TOKEN_MID : inst.lemmaids[c-1]) : TOKEN_START;
    	
    	int tid = type==-1 ? 0:type << 4;

    	long code = 0;
    	
    	// feature posR posMid posL
    	int small = h < c ? h : c;
    	int large = h > c ? h : c;
    	for(int i = small+1; i < large; i++) {    		
    		code = createArcCodePPP(Arc.valueOf("HP_BP_M"+prefix+"P"), pHead, pos[i], pMod)| tid;
    		addArcFeature(code,weight, fv);
    		addArcFeature(code | attDist,weight, fv);
    		
    		code = createArcCodePPP(Arc.valueOf("HP_BP_M"+prefix+"P"), pHeadA, posA[i], pModA)| tid;
    		addArcFeature(code,weight, fv);
    		addArcFeature(code | attDist,weight, fv);
    	}
    	
    	// feature posL-1 posL posR posR+1
    	
    	code = createArcCodePP(Arc.valueOf("HP_HPn"+prefix), pHead, pHeadRight)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		code = createArcCodePP(Arc.valueOf("M"+prefix+"P_M"+prefix+"Pn"), pMod, pModRight)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		code = createArcCodePP(Arc.valueOf("HPp_HP"+prefix), pHeadLeft, pHead)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		code = createArcCodePP(Arc.valueOf("M"+prefix+"Pp_M"+prefix+"P"), pModLeft,pMod)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		
    	
		code = createArcCodeWW(Arc.valueOf("HW_HWn"+prefix), wordHead, wordHeadRight)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		code = createArcCodeWW(Arc.valueOf("M"+prefix+"W_M"+prefix+"Wn"), wordMod, wordModRight)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		code = createArcCodeWW(Arc.valueOf("HWp_HW"+prefix), wordHeadLeft, wordHead)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		code = createArcCodeWW(Arc.valueOf("M"+prefix+"Wp_M"+prefix+"W"), wordModLeft,wordMod)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		
		/* Start of comment */
    	code = createArcCodePPP(Arc.valueOf("HP_M"+prefix+"P_M"+prefix+"Pn"), pHead, pMod, pModRight)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HPp_HP_M"+prefix+"P"), pHeadLeft, pHead, pMod)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HPp_M"+prefix+"P_M"+prefix+"Pn"), pHeadLeft, pMod, pModRight)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HPp_HP_M"+prefix+"Pn"), pHeadLeft, pHead, pModRight)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HP_M"+prefix+"P_M"+prefix+"Pn"), pHeadA, pModA, pModRightA)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HPp_HP_M"+prefix+"P"), pHeadLeftA, pHeadA, pModA)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HPp_M"+prefix+"P_M"+prefix+"Pn"), pHeadLeftA, pModA, pModRightA)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HPp_HP_M"+prefix+"Pn"), pHeadLeftA, pHeadA, pModRightA)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		/*End of comment */
    	
    	// feature posL posL+1 posR-1 posR
		
		code = createArcCodePPP(Arc.valueOf("HP_M"+prefix+"Pp_M"+prefix+"P"), pHead, pModLeft, pMod)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);
		
		code = createArcCodePPP(Arc.valueOf("HP_HPn_M"+prefix+"P"), pHead, pHeadRight, pMod)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);
		
		code = createArcCodePPP(Arc.valueOf("HP_M"+prefix+"Pp_M"+prefix+"P"), pHeadA, pModLeftA, pModA)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist, weight,fv);
		
		code = createArcCodePPP(Arc.valueOf("HP_HPn_M"+prefix+"P"), pHeadA, pHeadRightA, pModA)| tid;
		addArcFeature(code,weight, fv);
		addArcFeature(code | attDist,weight, fv);
		
		
		//newly introduced features
		code = createArcCodeP(Arc.valueOf("HP"+prefix), pHeadA)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);
		
		code = createArcCodeP(Arc.valueOf("HPn"+prefix), pHeadRightA)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);
		
		code = createArcCodeP(Arc.valueOf("HPp"+prefix), pHeadLeftA)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);

		code = createArcCodeP(Arc.valueOf("MP"+prefix), pModA)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);
		
		code = createArcCodeP(Arc.valueOf("MPn"+prefix), pModRightA)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);
		
		code = createArcCodeP(Arc.valueOf("MPp"+prefix), pModLeftA)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);
///
		code = createArcCodeP(Arc.valueOf("HW"+prefix), wordHead)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);
		
		code = createArcCodeP(Arc.valueOf("HWn"+prefix), wordHeadRight)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);
		
		code = createArcCodeP(Arc.valueOf("HWp"+prefix), wordHeadLeft)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);

		code = createArcCodeP(Arc.valueOf("MW"+prefix), wordMod)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);
		
		code = createArcCodeP(Arc.valueOf("MWn"+prefix), wordModRight)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);
		
		code = createArcCodeP(Arc.valueOf("MWp"+prefix), wordModLeft)| tid;
		addArcFeature(code, weight,fv);
		addArcFeature(code | attDist,weight, fv);

		
	}
    
    
    
    
    
    
    public void addCore1OPosContextGenericFeaturesOld(FeatureVector fv, DependencyInstance inst, 
    		int h, int c, int attDist, int type,String prefix) 
    {  	
    	
    	int[] pos = inst.postagids;
    	int[] posA = inst.cpostagids;
	
    	int pHead = pos[h], pHeadA = posA[h];
    	int pMod = pos[c], pModA = posA[c];
    	int pHeadLeft = h > 0 ? (h-1 == c ? TOKEN_MID : pos[h-1]) : TOKEN_START;    	
    	int pModRight = c < pos.length-1 ? (c+1 == h ? TOKEN_MID : pos[c+1]) : TOKEN_END;
    	int pHeadRight = h < pos.length-1 ? (h+1 == c ? TOKEN_MID: pos[h+1]) : TOKEN_END;
    	int pModLeft = c > 0 ? (c-1 == h ? TOKEN_MID : pos[c-1]) : TOKEN_START;
    	int pHeadLeftA = h > 0 ? (h-1 == c ? TOKEN_MID : posA[h-1]) : TOKEN_START;    	
    	int pModRightA = c < posA.length-1 ? (c+1 == h ? TOKEN_MID : posA[c+1]) : TOKEN_END;
    	int pHeadRightA = h < posA.length-1 ? (h+1 == c ? TOKEN_MID: posA[h+1]) : TOKEN_END;
    	int pModLeftA = c > 0 ? (c-1 == h ? TOKEN_MID : posA[c-1]) : TOKEN_START;
    	
    	int tid = type==-1 ? 0:type << 4;

    	long code = 0;
    	
    	// feature posR posMid posL
    	int small = h < c ? h : c;
    	int large = h > c ? h : c;
    	for(int i = small+1; i < large; i++) {    		
    		code = createArcCodePPP(Arc.valueOf("HP_BP_M"+prefix+"P"), pHead, pos[i], pMod)| tid;
    		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    		
    		code = createArcCodePPP(Arc.valueOf("HP_BP_M"+prefix+"P"), pHeadA, posA[i], pModA)| tid;
    		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	}
    	
    	// feature posL-1 posL posR posR+1
    	code = createArcCodePPPP(Arc.valueOf("HPp_HP_M"+prefix+"P_M"+prefix+"Pn"), pHeadLeft, pHead, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HP_M"+prefix+"P_M"+prefix+"Pn"), pHead, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HPp_HP_M"+prefix+"P"), pHeadLeft, pHead, pMod)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HPp_M"+prefix+"P_M"+prefix+"Pn"), pHeadLeft, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HPp_HP_M"+prefix+"Pn"), pHeadLeft, pHead, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);

    	code = createArcCodePPPP(Arc.valueOf("HPp_HP_M"+prefix+"P_M"+prefix+"Pn"), pHeadLeftA, pHeadA, pModA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HP_M"+prefix+"P_M"+prefix+"Pn"), pHeadA, pModA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HPp_HP_M"+prefix+"P"), pHeadLeftA, pHeadA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HPp_M"+prefix+"P_M"+prefix+"Pn"), pHeadLeftA, pModA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
    	code = createArcCodePPP(Arc.valueOf("HPp_HP_M"+prefix+"Pn"), pHeadLeftA, pHeadA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
    	
    	// feature posL posL+1 posR-1 posR
		code = createArcCodePPPP(Arc.valueOf("HP_HPn_M"+prefix+"Pp_M"+prefix+"P"), pHead, pHeadRight, pModLeft, pMod)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
		code = createArcCodePPP(Arc.valueOf("HP_M"+prefix+"Pp_M"+prefix+"P"), pHead, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
		code = createArcCodePPP(Arc.valueOf("HP_HPn_M"+prefix+"P"), pHead, pHeadRight, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
		code = createArcCodePPP(Arc.valueOf("HPn_M"+prefix+"Pp_M"+prefix+"P"), pHeadRight, pModLeft, pMod)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
		
		code = createArcCodePPP(Arc.valueOf("HP_HPn_M"+prefix+"Pp"), pHead, pHeadRight, pModLeft)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
		
		code = createArcCodePPPP(Arc.valueOf("HP_HPn_M"+prefix+"Pp_M"+prefix+"P"), pHeadA, pHeadRightA, pModLeftA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
		code = createArcCodePPP(Arc.valueOf("HP_M"+prefix+"Pp_M"+prefix+"P"), pHeadA, pModLeftA, pModA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
		
		code = createArcCodePPP(Arc.valueOf("HP_HPn_M"+prefix+"P"), pHeadA, pHeadRightA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
		code = createArcCodePPP(Arc.valueOf("HPn_M"+prefix+"Pp_M"+prefix+"P"), pHeadRightA, pModLeftA, pModA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
		code = createArcCodePPP(Arc.valueOf("HP_HPn_M"+prefix+"Pp"), pHeadA, pHeadRightA, pModLeftA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
	
    	
		// feature posL-1 posL posR-1 posR
		// feature posL posL+1 posR posR+1
		code = createArcCodePPPP(Arc.valueOf("HPp_HP_M"+prefix+"Pp_M"+prefix+"P"), pHeadLeft, pHead, pModLeft, pMod)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
		code = createArcCodePPPP(Arc.valueOf("HP_HPn_M"+prefix+"P_M"+prefix+"Pn"), pHead, pHeadRight, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
		code = createArcCodePPPP(Arc.valueOf("HPp_HP_M"+prefix+"Pp_M"+prefix+"P"), pHeadLeftA, pHeadA, pModLeftA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
		code = createArcCodePPPP(Arc.valueOf("HP_HPn_M"+prefix+"P_M"+prefix+"Pn"), pHeadA, pHeadRightA, pModA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
		
    }
    /*
    public void addCore1OPosAgentFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int c, int attDist, int type) 
    {  	
    	
    	int[] pos = inst.postagids;
    	int[] posA = inst.cpostagids;
	
    	int pHead = pos[h], pHeadA = posA[h];
    	int pMod = pos[c], pModA = posA[c];
    	int pHeadLeft = h > 0 ? (h-1 == c ? TOKEN_MID : pos[h-1]) : TOKEN_START;    	
    	int pModRight = c < pos.length-1 ? (c+1 == h ? TOKEN_MID : pos[c+1]) : TOKEN_END;
    	int pHeadRight = h < pos.length-1 ? (h+1 == c ? TOKEN_MID: pos[h+1]) : TOKEN_END;
    	int pModLeft = c > 0 ? (c-1 == h ? TOKEN_MID : pos[c-1]) : TOKEN_START;
    	int pHeadLeftA = h > 0 ? (h-1 == c ? TOKEN_MID : posA[h-1]) : TOKEN_START;    	
    	int pModRightA = c < posA.length-1 ? (c+1 == h ? TOKEN_MID : posA[c+1]) : TOKEN_END;
    	int pHeadRightA = h < posA.length-1 ? (h+1 == c ? TOKEN_MID: posA[h+1]) : TOKEN_END;
    	int pModLeftA = c > 0 ? (c-1 == h ? TOKEN_MID : posA[c-1]) : TOKEN_START;
    	
    	int tid = type==-1 ? 0:type << 4;

    	long code = 0;
    	
    	// feature posR posMid posL
    	int small = h < c ? h : c;
    	int large = h > c ? h : c;
    	for(int i = small+1; i < large; i++) {    		
    		code = createArcCodePPP(HP_BP_MAGP, pHead, pos[i], pMod)| tid;
    		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
    		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
    		
    		code = createArcCodePPP(HP_BP_MAGP, pHeadA, posA[i], pModA)| tid;
    		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
    		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
    	}
    	
    	// feature posL-1 posL posR posR+1
    	code = createArcCodePPPP(HPp_HP_MAGP_MAGPn, pHeadLeft, pHead, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HP_MAGP_MAGPn, pHead, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HPp_HP_MAGP, pHeadLeft, pHead, pMod)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HPp_MAGP_MAGPn, pHeadLeft, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HPp_HP_MAGPn, pHeadLeft, pHead, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);

    	code = createArcCodePPPP(HPp_HP_MAGP_MAGPn, pHeadLeftA, pHeadA, pModA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HP_MAGP_MAGPn, pHeadA, pModA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HPp_HP_MAGP, pHeadLeftA, pHeadA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HPp_MAGP_MAGPn, pHeadLeftA, pModA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HPp_HP_MAGPn, pHeadLeftA, pHeadA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	
    	// feature posL posL+1 posR-1 posR
		code = createArcCodePPPP(HP_HPn_MAGPp_MAGP, pHead, pHeadRight, pModLeft, pMod)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_MAGPp_MAGP, pHead, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MAGP, pHead, pHeadRight, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HPn_MAGPp_MAGP, pHeadRight, pModLeft, pMod)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPP(HP_HPn_MAGPp, pHead, pHeadRight, pModLeft)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPPP(HP_HPn_MAGPp_MAGP, pHeadA, pHeadRightA, pModLeftA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_MAGPp_MAGP, pHeadA, pModLeftA, pModA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPP(HP_HPn_MAGP, pHeadA, pHeadRightA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HPn_MAGPp_MAGP, pHeadRightA, pModLeftA, pModA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MAGPp, pHeadA, pHeadRightA, pModLeftA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
	
    	
		// feature posL-1 posL posR-1 posR
		// feature posL posL+1 posR posR+1
		code = createArcCodePPPP(HPp_HP_MAGPp_MAGP, pHeadLeft, pHead, pModLeft, pMod)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPPP(HP_HPn_MAGP_MAGPn, pHead, pHeadRight, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPPP(HPp_HP_MAGPp_MAGP, pHeadLeftA, pHeadA, pModLeftA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPPP(HP_HPn_MAGP_MAGPn, pHeadA, pHeadRightA, pModA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    }

    public void addCore1OPosThemeFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int c, int attDist, int type) 
    {  	
    	
    	int[] pos = inst.postagids;
    	int[] posA = inst.cpostagids;
	
    	int pHead = pos[h], pHeadA = posA[h];
    	int pMod = pos[c], pModA = posA[c];
    	int pHeadLeft = h > 0 ? (h-1 == c ? TOKEN_MID : pos[h-1]) : TOKEN_START;    	
    	int pModRight = c < pos.length-1 ? (c+1 == h ? TOKEN_MID : pos[c+1]) : TOKEN_END;
    	int pHeadRight = h < pos.length-1 ? (h+1 == c ? TOKEN_MID: pos[h+1]) : TOKEN_END;
    	int pModLeft = c > 0 ? (c-1 == h ? TOKEN_MID : pos[c-1]) : TOKEN_START;
    	int pHeadLeftA = h > 0 ? (h-1 == c ? TOKEN_MID : posA[h-1]) : TOKEN_START;    	
    	int pModRightA = c < posA.length-1 ? (c+1 == h ? TOKEN_MID : posA[c+1]) : TOKEN_END;
    	int pHeadRightA = h < posA.length-1 ? (h+1 == c ? TOKEN_MID: posA[h+1]) : TOKEN_END;
    	int pModLeftA = c > 0 ? (c-1 == h ? TOKEN_MID : posA[c-1]) : TOKEN_START;
    	
    	int tid = type==-1 ? 0:type << 4;
    	
    	long code = 0;
    	
    	// feature posR posMid posL
    	int small = h < c ? h : c;
    	int large = h > c ? h : c;
    	for(int i = small+1; i < large; i++) {    		
    		code = createArcCodePPP(HP_BP_MTHP, pHead, pos[i], pMod)| tid;
    		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
    		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
    		
    		code = createArcCodePPP(HP_BP_MTHP, pHeadA, posA[i], pModA)| tid;
    		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
    		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
    	}
    	
    	// feature posL-1 posL posR posR+1
    	code = createArcCodePPPP(HPp_HP_MTHP_MTHPn, pHeadLeft, pHead, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HP_MTHP_MTHPn, pHead, pMod, pModRight)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_HP_MTHP, pHeadLeft, pHead, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_MTHP_MTHPn, pHeadLeft, pMod, pModRight)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HPp_HP_MTHPn, pHeadLeft, pHead, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);

    	code = createArcCodePPPP(HPp_HP_MTHP_MTHPn, pHeadLeftA, pHeadA, pModA, pModRightA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HP_MTHP_MTHPn, pHeadA, pModA, pModRightA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_HP_MTHP, pHeadLeftA, pHeadA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_MTHP_MTHPn, pHeadLeftA, pModA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_HP_MTHPn, pHeadLeftA, pHeadA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	
    	// feature posL posL+1 posR-1 posR
		code = createArcCodePPPP(HP_HPn_MTHPp_MTHP, pHead, pHeadRight, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_MTHPp_MTHP, pHead, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MTHP, pHead, pHeadRight, pMod)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HPn_MTHPp_MTHP, pHeadRight, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MTHPp, pHead, pHeadRight, pModLeft)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPPP(HP_HPn_MTHPp_MTHP, pHeadA, pHeadRightA, pModLeftA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_MTHPp_MTHP, pHeadA, pModLeftA, pModA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MTHP, pHeadA, pHeadRightA, pModA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPP(HPn_MTHPp_MTHP, pHeadRightA, pModLeftA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MTHPp, pHeadA, pHeadRightA, pModLeftA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
	
    	
		// feature posL-1 posL posR-1 posR
		// feature posL posL+1 posR posR+1
		code = createArcCodePPPP(HPp_HP_MTHPp_MTHP, pHeadLeft, pHead, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPPP(HP_HPn_MTHP_MTHPn, pHead, pHeadRight, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPPP(HPp_HP_MTHPp_MTHP, pHeadLeftA, pHeadA, pModLeftA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPPP(HP_HPn_MTHP_MTHPn, pHeadA, pHeadRightA, pModA, pModRightA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    }

    public void addCore1OPosInstrumentFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int c, int attDist, int type) 
    {  	
    	
    	int[] pos = inst.postagids;
    	int[] posA = inst.cpostagids;
	
    	int pHead = pos[h], pHeadA = posA[h];
    	int pMod = pos[c], pModA = posA[c];
    	int pHeadLeft = h > 0 ? (h-1 == c ? TOKEN_MID : pos[h-1]) : TOKEN_START;    	
    	int pModRight = c < pos.length-1 ? (c+1 == h ? TOKEN_MID : pos[c+1]) : TOKEN_END;
    	int pHeadRight = h < pos.length-1 ? (h+1 == c ? TOKEN_MID: pos[h+1]) : TOKEN_END;
    	int pModLeft = c > 0 ? (c-1 == h ? TOKEN_MID : pos[c-1]) : TOKEN_START;
    	int pHeadLeftA = h > 0 ? (h-1 == c ? TOKEN_MID : posA[h-1]) : TOKEN_START;    	
    	int pModRightA = c < posA.length-1 ? (c+1 == h ? TOKEN_MID : posA[c+1]) : TOKEN_END;
    	int pHeadRightA = h < posA.length-1 ? (h+1 == c ? TOKEN_MID: posA[h+1]) : TOKEN_END;
    	int pModLeftA = c > 0 ? (c-1 == h ? TOKEN_MID : posA[c-1]) : TOKEN_START;
    	
    	int tid = type==-1 ? 0:type << 4;
    	
    	long code = 0;
    	
    	// feature posR posMid posL
    	int small = h < c ? h : c;
    	int large = h > c ? h : c;
    	for(int i = small+1; i < large; i++) {    		
    		code = createArcCodePPP(HP_BP_MINSP, pHead, pos[i], pMod)| tid;
    		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
    		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
    		
    		code = createArcCodePPP(HP_BP_MINSP, pHeadA, posA[i], pModA)| tid;
    		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
    		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
    	}
    	
    	// feature posL-1 posL posR posR+1
    	code = createArcCodePPPP(HPp_HP_MINSP_MINSPn, pHeadLeft, pHead, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HP_MINSP_MINSPn, pHead, pMod, pModRight)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_HP_MINSP, pHeadLeft, pHead, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_MINSP_MINSPn, pHeadLeft, pMod, pModRight)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HPp_HP_MINSPn, pHeadLeft, pHead, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);

    	code = createArcCodePPPP(HPp_HP_MINSP_MINSPn, pHeadLeftA, pHeadA, pModA, pModRightA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HP_MINSP_MINSPn, pHeadA, pModA, pModRightA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_HP_MINSP, pHeadLeftA, pHeadA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_MINSP_MINSPn, pHeadLeftA, pModA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_HP_MINSPn, pHeadLeftA, pHeadA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	
    	// feature posL posL+1 posR-1 posR
		code = createArcCodePPPP(HP_HPn_MINSPp_MINSP, pHead, pHeadRight, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_MINSPp_MINSP, pHead, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MINSP, pHead, pHeadRight, pMod)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HPn_MINSPp_MINSP, pHeadRight, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MINSPp, pHead, pHeadRight, pModLeft)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPPP(HP_HPn_MINSPp_MINSP, pHeadA, pHeadRightA, pModLeftA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_MINSPp_MINSP, pHeadA, pModLeftA, pModA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MINSP, pHeadA, pHeadRightA, pModA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPP(HPn_MINSPp_MINSP, pHeadRightA, pModLeftA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MINSPp, pHeadA, pHeadRightA, pModLeftA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
	
    	
		// feature posL-1 posL posR-1 posR
		// feature posL posL+1 posR posR+1
		code = createArcCodePPPP(HPp_HP_MINSPp_MINSP, pHeadLeft, pHead, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPPP(HP_HPn_MINSP_MINSPn, pHead, pHeadRight, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPPP(HPp_HP_MINSPp_MINSP, pHeadLeftA, pHeadA, pModLeftA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPPP(HP_HPn_MINSP_MINSPn, pHeadA, pHeadRightA, pModA, pModRightA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    }
    public void addCore1OPosOwnerFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int c, int attDist, int type) 
    {  	
    	
    	int[] pos = inst.postagids;
    	int[] posA = inst.cpostagids;
	
    	int pHead = pos[h], pHeadA = posA[h];
    	int pMod = pos[c], pModA = posA[c];
    	int pHeadLeft = h > 0 ? (h-1 == c ? TOKEN_MID : pos[h-1]) : TOKEN_START;    	
    	int pModRight = c < pos.length-1 ? (c+1 == h ? TOKEN_MID : pos[c+1]) : TOKEN_END;
    	int pHeadRight = h < pos.length-1 ? (h+1 == c ? TOKEN_MID: pos[h+1]) : TOKEN_END;
    	int pModLeft = c > 0 ? (c-1 == h ? TOKEN_MID : pos[c-1]) : TOKEN_START;
    	int pHeadLeftA = h > 0 ? (h-1 == c ? TOKEN_MID : posA[h-1]) : TOKEN_START;    	
    	int pModRightA = c < posA.length-1 ? (c+1 == h ? TOKEN_MID : posA[c+1]) : TOKEN_END;
    	int pHeadRightA = h < posA.length-1 ? (h+1 == c ? TOKEN_MID: posA[h+1]) : TOKEN_END;
    	int pModLeftA = c > 0 ? (c-1 == h ? TOKEN_MID : posA[c-1]) : TOKEN_START;
    	
    	int tid = type==-1 ? 0:type << 4;
    	
    	long code = 0;
    	
    	// feature posR posMid posL
    	int small = h < c ? h : c;
    	int large = h > c ? h : c;
    	for(int i = small+1; i < large; i++) {    		
    		code = createArcCodePPP(HP_BP_MOWNP, pHead, pos[i], pMod)| tid;
    		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
    		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
    		
    		code = createArcCodePPP(HP_BP_MOWNP, pHeadA, posA[i], pModA)| tid;
    		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
    		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
    	}
    	
    	// feature posL-1 posL posR posR+1
    	code = createArcCodePPPP(HPp_HP_MOWNP_MOWNPn, pHeadLeft, pHead, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HP_MOWNP_MOWNPn, pHead, pMod, pModRight)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_HP_MOWNP, pHeadLeft, pHead, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_MOWNP_MOWNPn, pHeadLeft, pMod, pModRight)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HPp_HP_MOWNPn, pHeadLeft, pHead, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);

    	code = createArcCodePPPP(HPp_HP_MOWNP_MOWNPn, pHeadLeftA, pHeadA, pModA, pModRightA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    	code = createArcCodePPP(HP_MOWNP_MOWNPn, pHeadA, pModA, pModRightA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_HP_MOWNP, pHeadLeftA, pHeadA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_MOWNP_MOWNPn, pHeadLeftA, pModA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	code = createArcCodePPP(HPp_HP_MOWNPn, pHeadLeftA, pHeadA, pModRightA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
    	
    	// feature posL posL+1 posR-1 posR
		code = createArcCodePPPP(HP_HPn_MOWNPp_MOWNP, pHead, pHeadRight, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_MOWNPp_MOWNP, pHead, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MOWNP, pHead, pHeadRight, pMod)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HPn_MOWNPp_MOWNP, pHeadRight, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MOWNPp, pHead, pHeadRight, pModLeft)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPPP(HP_HPn_MOWNPp_MOWNP, pHeadA, pHeadRightA, pModLeftA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_MOWNPp_MOWNP, pHeadA, pModLeftA, pModA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MOWNP, pHeadA, pHeadRightA, pModA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPP(HPn_MOWNPp_MOWNP, pHeadRightA, pModLeftA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
		code = createArcCodePPP(HP_HPn_MOWNPp, pHeadA, pHeadRightA, pModLeftA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
	
    	
		// feature posL-1 posL posR-1 posR
		// feature posL posL+1 posR posR+1
		code = createArcCodePPPP(HPp_HP_MOWNPp_MOWNP, pHeadLeft, pHead, pModLeft, pMod)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPPP(HP_HPn_MOWNP_MOWNPn, pHead, pHeadRight, pMod, pModRight)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPPP(HPp_HP_MOWNPp_MOWNP, pHeadLeftA, pHeadA, pModLeftA, pModA)| tid;
		addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
		addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
		
		code = createArcCodePPPP(HP_HPn_MOWNP_MOWNPn, pHeadA, pHeadRightA, pModA, pModRightA)| tid;
		addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
		addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
		
    }
    */
    public void addCore1OPosFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int c, int attDist) 
    {  	
    	
    	int[] pos = inst.postagids;
    	int[] posA = inst.cpostagids;
	
    	int pHead = pos[h], pHeadA = posA[h];
    	int pMod = pos[c], pModA = posA[c];
    	int pHeadLeft = h > 0 ? (h-1 == c ? TOKEN_MID : pos[h-1]) : TOKEN_START;    	
    	int pModRight = c < pos.length-1 ? (c+1 == h ? TOKEN_MID : pos[c+1]) : TOKEN_END;
    	int pHeadRight = h < pos.length-1 ? (h+1 == c ? TOKEN_MID: pos[h+1]) : TOKEN_END;
    	int pModLeft = c > 0 ? (c-1 == h ? TOKEN_MID : pos[c-1]) : TOKEN_START;
    	int pHeadLeftA = h > 0 ? (h-1 == c ? TOKEN_MID : posA[h-1]) : TOKEN_START;    	
    	int pModRightA = c < posA.length-1 ? (c+1 == h ? TOKEN_MID : posA[c+1]) : TOKEN_END;
    	int pHeadRightA = h < posA.length-1 ? (h+1 == c ? TOKEN_MID: posA[h+1]) : TOKEN_END;
    	int pModLeftA = c > 0 ? (c-1 == h ? TOKEN_MID : posA[c-1]) : TOKEN_START;
    	
    	    	
    	long code = 0;
    	
    	// feature posR posMid posL
    	int small = h < c ? h : c;
    	int large = h > c ? h : c;
    	for(int i = small+1; i < large; i++) {    		
    		code = createArcCodePPP(HP_BP_MP, pHead, pos[i], pMod);
    		addArcFeature(code, fv);
    		addArcFeature(code | attDist, fv);
    		
    		code = createArcCodePPP(HP_BP_MP, pHeadA, posA[i], pModA);
    		addArcFeature(code, fv);
    		addArcFeature(code | attDist, fv);
    	}
    	
    	// feature posL-1 posL posR posR+1
    	code = createArcCodePPPP(HPp_HP_MP_MPn, pHeadLeft, pHead, pMod, pModRight);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HP_MP_MPn, pHead, pMod, pModRight);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HPp_HP_MP, pHeadLeft, pHead, pMod);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HPp_MP_MPn, pHeadLeft, pMod, pModRight);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HPp_HP_MPn, pHeadLeft, pHead, pModRight);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);

    	code = createArcCodePPPP(HPp_HP_MP_MPn, pHeadLeftA, pHeadA, pModA, pModRightA);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HP_MP_MPn, pHeadA, pModA, pModRightA);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HPp_HP_MP, pHeadLeftA, pHeadA, pModA);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HPp_MP_MPn, pHeadLeftA, pModA, pModRightA);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HPp_HP_MPn, pHeadLeftA, pHeadA, pModRightA);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
    	
    	// feature posL posL+1 posR-1 posR
		code = createArcCodePPPP(HP_HPn_MPp_MP, pHead, pHeadRight, pModLeft, pMod);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HP_MPp_MP, pHead, pModLeft, pMod);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HP_HPn_MP, pHead, pHeadRight, pMod);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HPn_MPp_MP, pHeadRight, pModLeft, pMod);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HP_HPn_MPp, pHead, pHeadRight, pModLeft);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
		code = createArcCodePPPP(HP_HPn_MPp_MP, pHeadA, pHeadRightA, pModLeftA, pModA);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HP_MPp_MP, pHeadA, pModLeftA, pModA);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HP_HPn_MP, pHeadA, pHeadRightA, pModA);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HPn_MPp_MP, pHeadRightA, pModLeftA, pModA);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HP_HPn_MPp, pHeadA, pHeadRightA, pModLeftA);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
	
    	
		// feature posL-1 posL posR-1 posR
		// feature posL posL+1 posR posR+1
		code = createArcCodePPPP(HPp_HP_MPp_MP, pHeadLeft, pHead, pModLeft, pMod);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
		code = createArcCodePPPP(HP_HPn_MP_MPn, pHead, pHeadRight, pMod, pModRight);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
		code = createArcCodePPPP(HPp_HP_MPp_MP, pHeadLeftA, pHeadA, pModLeftA, pModA);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
		code = createArcCodePPPP(HP_HPn_MP_MPn, pHeadA, pHeadRightA, pModA, pModRightA);
		addArcFeature(code, fv);
		addArcFeature(code | attDist, fv);
		
    }

    public void addCore1OBigramThemeFeatures(FeatureVector fv, DependencyInstance inst, int head, int headP, 
    		int mod, int modP, int attDist, int type) 
    {
    	
    	long code = 0;
    	int[] forms = inst.formids, lemmas = inst.lemmaids, postags = inst.postagids;
    	int[] cpostags = inst.cpostagids;
    	int tid = type==-1 ? 0:type << 4;
    	
    	code = createArcCodeWWPP(HW_MTHW_HP_MTHP, head, mod, headP, modP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	
    	code = createArcCodeWPP(MTHW_HP_MTHP, mod, headP, modP)| tid;
    	addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	
    	code = createArcCodeWPP(HW_HP_MTHP, head, headP, modP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	
    	code = createArcCodeWP(MTHW_HP, mod, headP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	
    	code = createArcCodeWP(HW_MTHP, head, modP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	
   /* 	code = createArcCodeWW(HW_MAGW, forms[head], forms[mod]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);

    	code = createArcCodeWW(HW_MAGW, lemmas[head], lemmas[mod]);
        	addArcFeature(code, fv);
        	addArcFeature(code | attDist, fv);

        code = createArcCodePP(HP_MAGP, postags[head], postags[mod]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodePP(HP_MAGP, cpostags[head], cpostags[mod]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);*/
    }

    public void addCore1OBigramContextGenericFeaturesSimple(FeatureVector fv, DependencyInstance inst, int h, int c, int head, int headP, 
    		int mod, int modP, int attDist, int type, String prefix, int weight) 
    {
    	
    	long code = 0;
    	//int[] forms = inst.formids, lemmas = inst.lemmaids, postags = inst.postagids;
    	//int[] cpostags = inst.cpostagids;
    	int tid = type==-1 ? 0:type << 4;
    	
    	
    	code = createArcCodeWPP(Arc.valueOf("HW_HP_M"+prefix+"P"), inst.lemmaids[h], headP, modP)| tid;
    	addArcFeature(code, weight, fv);
    	addArcFeature(code | attDist,  weight,fv);
    	
    	code = createArcCodeWP(Arc.valueOf("M"+prefix+"W_HP"), inst.lemmaids[c], headP)| tid;
    	addArcFeature(code, weight,fv);
    	addArcFeature(code | attDist, weight, fv);
    	
    	code = createArcCodeWP(Arc.valueOf("HW_M"+prefix+"P"), inst.lemmaids[h], modP)| tid;
    	addArcFeature(code, weight,fv);
    	addArcFeature(code | attDist,  weight,fv);
    	
    	code = createArcCodeWW(Arc.valueOf("HW_M"+prefix+"W"), head, mod);
    	addArcFeature(code, weight, fv);
    	addArcFeature(code | attDist, weight, fv);

    	code = createArcCodeWW(Arc.valueOf("HW_M"+prefix+"W"), inst.lemmaids[h], inst.lemmaids[c]);
        	addArcFeature(code, weight, fv);
        	addArcFeature(code | attDist, weight, fv);

        code = createArcCodePP(Arc.valueOf("HP_M"+prefix+"P"), headP, modP);
    	addArcFeature(code, weight,fv);
    	addArcFeature(code | attDist, weight,fv);
    	
    	code = createArcCodePP(Arc.valueOf("HP_M"+prefix+"P"), inst.cpostagids[h], inst.cpostagids[c]);
    	addArcFeature(code, weight,fv);
    	addArcFeature(code | attDist, weight,fv);
    }
    
    
    public void addCore1OBigramContextGenericFeaturesOld(FeatureVector fv, DependencyInstance inst, int h, int c, int head, int headP, 
    		int mod, int modP, int attDist, int type, String prefix) 
    {
    	
    	long code = 0;
    	int[] forms = inst.formids, lemmas = inst.lemmaids, postags = inst.postagids;
    	int[] cpostags = inst.cpostagids;
    	int tid = type==-1 ? 0:type << 4;
    	
    	code = createArcCodeWWPP(Arc.valueOf("HW_M"+prefix+"W_HP_M"+prefix+"P"), head, mod, headP, modP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	
    	code = createArcCodeWPP(Arc.valueOf("M"+prefix+"W_HP_M"+prefix+"P"), mod, headP, modP)| tid;
    	addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	
    	code = createArcCodeWPP(Arc.valueOf("HW_HP_M"+prefix+"P"), head, headP, modP)| tid;
    	addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	
    	code = createArcCodeWP(Arc.valueOf("M"+prefix+"W_HP"), mod, headP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	
    	code = createArcCodeWP(Arc.valueOf("HW_M"+prefix+"P"), head, modP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	
    	code = createArcCodeWW(Arc.valueOf("HW_M"+prefix+"W"), head, mod);
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);

    	code = createArcCodeWW(Arc.valueOf("HW_M"+prefix+"W"), inst.lemmaids[h], inst.lemmaids[c]);
        	addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
        	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);

        code = createArcCodePP(Arc.valueOf("HP_M"+prefix+"P"), headP, modP);
    	addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	
    	code = createArcCodePP(Arc.valueOf("HP_M"+prefix+"P"), inst.cpostagids[h], inst.cpostagids[c]);
    	addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    }
/*
    public void addCore1OBigramAgentFeatures(FeatureVector fv, DependencyInstance inst, int head, int headP, 
    		int mod, int modP, int attDist, int type) 
    {
    	
    	long code = 0;
    	int[] forms = inst.formids, lemmas = inst.lemmaids, postags = inst.postagids;
    	int[] cpostags = inst.cpostagids;
    	int tid = type==-1 ? 0:type << 4;
    	
    	code = createArcCodeWWPP(HW_MAGW_HP_MAGP, head, mod, headP, modP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
    	
    	code = createArcCodeWPP(MAGW_HP_MAGP, mod, headP, modP)| tid;
    	addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
    	
    	code = createArcCodeWPP(HW_HP_MAGP, head, headP, modP)| tid;
    	addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT, fv);
    	addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
    	
    	code = createArcCodeWP(MAGW_HP, mod, headP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT, fv);
    	
    	code = createArcCodeWP(HW_MAGP, head, modP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT,fv);
    	addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT,fv);
    	
   // 	code = createArcCodeWW(HW_MAGW, forms[head], forms[mod]);
   // 	addArcFeature(code, fv);
   // 	addArcFeature(code | attDist, fv);

 //   	code = createArcCodeWW(HW_MAGW, lemmas[head], lemmas[mod]);
  //      	addArcFeature(code, fv);
   //     	addArcFeature(code | attDist, fv);

//        code = createArcCodePP(HP_MAGP, postags[head], postags[mod]);
 //   	addArcFeature(code, fv);
  //  	addArcFeature(code | attDist, fv);
    	
   // 	code = createArcCodePP(HP_MAGP, cpostags[head], cpostags[mod]);
   // 	addArcFeature(code, fv);
   // 	addArcFeature(code | attDist, fv);
    }*/
    public void addCore1OBigramInstrumentFeaturesNotUsed(FeatureVector fv, DependencyInstance inst, int head, int headP, 
    		int mod, int modP, int attDist, int type) 
    {
    	
    	long code = 0;
    	int[] forms = inst.formids, lemmas = inst.lemmaids, postags = inst.postagids;
    	int[] cpostags = inst.cpostagids;
    	int tid = type==-1 ? 0:type << 4;
    	
    	code = createArcCodeWWPP(HW_MINSW_HP_MINSP, head, mod, headP, modP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	
    	code = createArcCodeWPP(MINSW_HP_MINSP, mod, headP, modP)| tid;
    	addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	
    	code = createArcCodeWPP(HW_HP_MINSP, head, headP, modP)| tid;
    	addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	
    	code = createArcCodeWP(MINSW_HP, mod, headP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	
    	code = createArcCodeWP(HW_MINSP, head, modP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	
   /* 	code = createArcCodeWW(HW_MAGW, forms[head], forms[mod]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);

    	code = createArcCodeWW(HW_MAGW, lemmas[head], lemmas[mod]);
        	addArcFeature(code, fv);
        	addArcFeature(code | attDist, fv);

        code = createArcCodePP(HP_MAGP, postags[head], postags[mod]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodePP(HP_MAGP, cpostags[head], cpostags[mod]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);*/
    }
    public void addCore1OBigramOwnerFeaturesNotUsed(FeatureVector fv, DependencyInstance inst, int head, int headP, 
    		int mod, int modP, int attDist, int type) 
    {
    	
    	long code = 0;
    	int[] forms = inst.formids, lemmas = inst.lemmaids, postags = inst.postagids;
    	int[] cpostags = inst.cpostagids;
    	int tid = type==-1 ? 0:type << 4;
    	
    	code = createArcCodeWWPP(HW_MOWNW_HP_MOWNP, head, mod, headP, modP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	
    	code = createArcCodeWPP(MOWNW_HP_MOWNP, mod, headP, modP)| tid;
    	addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	
    	code = createArcCodeWPP(HW_HP_MOWNP, head, headP, modP)| tid;
    	addArcFeature(code,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	
    	code = createArcCodeWP(MOWNW_HP, mod, headP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	addArcFeature(code | attDist,ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL, fv);
    	
    	code = createArcCodeWP(HW_MOWNP, head, modP)| tid;
    	addArcFeature(code, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	addArcFeature(code | attDist, ContextConstances.ARC_HIGH_WIEGHT_ORIGINAL,fv);
    	
   /* 	code = createArcCodeWW(HW_MAGW, forms[head], forms[mod]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);

    	code = createArcCodeWW(HW_MAGW, lemmas[head], lemmas[mod]);
        	addArcFeature(code, fv);
        	addArcFeature(code | attDist, fv);

        code = createArcCodePP(HP_MAGP, postags[head], postags[mod]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodePP(HP_MAGP, cpostags[head], cpostags[mod]);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);*/
    }
    public void addCore1OBigramFeatures(FeatureVector fv, int head, int headP, 
    		int mod, int modP, int attDist) 
    {
    	
    	long code = 0;
    	
    	code = createArcCodeWWPP(HW_MW_HP_MP, head, mod, headP, modP);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodeWPP(MW_HP_MP, mod, headP, modP);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodeWPP(HW_HP_MP, head, headP, modP);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodeWP(MW_HP, mod, headP);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodeWP(HW_MP, head, modP);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	    	
    	code = createArcCodeWP(HW_HP, head, headP);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
    	
    	code = createArcCodeWP(MW_MP, mod, modP);
    	addArcFeature(code, fv);
    	addArcFeature(code | attDist, fv);
      
    }

    
    /************************************************************************
     *  Region end #
     ************************************************************************/
    
    
    
    /************************************************************************
     * Region start #
     * 
     *  Functions that create feature vectors of a specific word in the 
     *  sentence
     *  
     ************************************************************************/
    
    public FeatureVector createWordFeatures(DependencyInstance inst, int i) 
    {
    	
    	int[] pos = inst.postagids;
        int[] toks = inst.formids;
        int[][] feats = inst.featids;
        
        int w0 = toks[i];
        int l0 = inst.lemmaids == null ? 0 : inst.lemmaids[i];
        
        FeatureVector fv = new FeatureVector(wordAlphabet.size());
    	
    	long code = 0;
        
    	code = createWordCodeP(WORDFV_BIAS, 0);
    	addWordFeature(code, fv);

    	code = createWordCodeW(WORDFV_W0, w0);
    	addWordFeature(code, fv);
    	
    	int Wp = i == 0 ? TOKEN_START : toks[i-1];
    	int Wn = i == inst.length - 1 ? TOKEN_END : toks[i+1];
    		    	
    	code = createWordCodeW(WORDFV_Wp, Wp);
    	addWordFeature(code, fv);
    	
    	code = createWordCodeW(WORDFV_Wn, Wn);
    	addWordFeature(code, fv);

    	
		if (l0 != 0) {
    		code = createWordCodeW(WORDFV_W0, l0);
    		addWordFeature(code, fv);
    		
	    	int Lp = i == 0 ? TOKEN_START : inst.lemmaids[i-1];
	    	int Ln = i == inst.length - 1 ? TOKEN_END : inst.lemmaids[i+1];
	    		    	
	    	code = createWordCodeW(WORDFV_Wp, Lp);
	    	addWordFeature(code, fv);
	    	
	    	code = createWordCodeW(WORDFV_Wn, Ln);
	    	addWordFeature(code, fv);
		}
		
		if (feats[i] != null) {
    		for (int u = 0; u < feats[i].length; ++u) {
    			int f = feats[i][u];
    			
    			code = createWordCodeP(WORDFV_P0, f);
    			addWordFeature(code, fv);
    			
                if (l0 != 0) {
                	code = createWordCodeWP(WORDFV_W0P0, l0, f);
                	addWordFeature(code, fv);
                }
                
            }
		}
			
        int p0 = pos[i];
    	int pLeft = i > 0 ? pos[i-1] : TOKEN_START;
    	int pRight = i < pos.length-1 ? pos[i+1] : TOKEN_END;
    	
    	code = createWordCodeP(WORDFV_P0, p0);
    	addWordFeature(code, fv);
    	code = createWordCodeP(WORDFV_Pp, pLeft);
    	addWordFeature(code, fv);
    	code = createWordCodeP(WORDFV_Pn, pRight);
    	addWordFeature(code, fv);
    	code = createWordCodePP(WORDFV_PpP0, pLeft, p0);
    	addWordFeature(code, fv);
    	code = createWordCodePP(WORDFV_P0Pn, p0, pRight);
    	addWordFeature(code, fv);
    	code = createWordCodePPP(WORDFV_PpP0Pn, pLeft, p0, pRight);
    	addWordFeature(code, fv);
    		    	
		if (l0 != 0) {
    		code = createWordCodeWP(WORDFV_W0P0, l0, p0);
    		addWordFeature(code, fv);
		}
    	    	
    	if (wordVectors != null) {
    		addWordVectorFeatures(inst, i, 0, fv);
    		addWordVectorFeatures(inst, i, -1, fv);
    		addWordVectorFeatures(inst, i, 1, fv);	
    	}
    	if(options.context && ContextConstances.useContextFeatures)
    	{
    		int old=fv.size();
    		addContextWordFeatures(inst, i, fv);
    		addContextWordFeaturesBigram(inst, i, fv);
    		if (fv.size()>old) {
    			System.out.println("After Context:"+old+"----->"+fv.size());	
			}
    		
    	}
    	
    	return fv;
    }
    
    public void addContextWordFeatures(DependencyInstance inst, int i, FeatureVector fv)
    {
    	
    	long code = 0;
    	int[] toks = inst.formids;
        int w0 = toks[i];
    //	System.out.println("Debug:"+i + " "+inst.forms.length +" "+inst.forms[2]+ " "+ inst+ " "+inst.contextInstance);
    			//System.out.println(inst.contextInstance.getAgentRelations().size()+ " " +inst.contextInstance.getThemeRelations().size()
    	//		+ " "+ inst.contextInstance.getInstrumentRelations().size());
        for (int j = 0; j < inst.contextInstance.getAgentRelations().size(); j++) {//this word feature will not be added if the word is not agent in any relation
        	ContextRelation relation =inst.contextInstance.getAgentRelations().get(j);
        	if(relation.hasEntity(inst.forms,i))
        	{
        		code = createWordVisualCodeW(WORDFV_AG0, w0);
            	addWordFeature(code, fv);
           // 	System.out.println("new Context feature is added:"+WORDFV_AG0);
            	break;
        	}
        	if(relation.hasAction(inst.forms,i))
        	{
        		code = createWordVisualCodeW(WORDFV_VB0, w0);
            	addWordFeature(code, fv);
            //	System.out.println("new Context feature is added:"+WORDFV_VB0);
            	return;
        	}
		}
        for (int j = 0; j < inst.contextInstance.getThemeRelations().size(); j++) {//this word feature will not be added if the word is not theme in any relation
        	ContextRelation relation =inst.contextInstance.getThemeRelations().get(j);
        	if(relation.hasEntity(inst.forms,i))
        	{
        		code = createWordVisualCodeW(WORDFV_TH0, w0);
            	addWordFeature(code, fv);
            //	System.out.println("new Context feature is added:"+WORDFV_TH0);
            	break;
        	}
        	if(relation.hasAction(inst.forms,i))
        	{
        		code = createWordVisualCodeW(WORDFV_VB0, w0);
            	addWordFeature(code, fv);
            //	System.out.println("new Context feature is added:"+WORDFV_VB0);
            	return;
        	}
		}
        for (int j = 0; j < inst.contextInstance.getInstrumentRelations().size(); j++) {//this word feature will not be added if the word is not agent in any relation
        	ContextRelation relation =inst.contextInstance.getInstrumentRelations().get(j);
        	if(relation.hasEntity(inst.forms,i))
        	{
        		code = createWordVisualCodeW(WORDFV_INS0, w0);
            	addWordFeature(code, fv);
            //	System.out.println("new Context feature is added:"+WORDFV_INS0);
            	break;
        	}
        	if(relation.hasAction(inst.forms,i))
        	{
        		code = createWordVisualCodeW(WORDFV_VB0, w0);
            	addWordFeature(code, fv);
            //	System.out.println("new Context feature is added:"+WORDFV_VB0);
            	return;
        	}
		}
        for (int j = 0; j < inst.contextInstance.getOwnershipRelations().size(); j++) {//this word feature will not be added if the word is not agent in any relation
        	ContextRelation relation =inst.contextInstance.getOwnershipRelations().get(j);
        	if(relation.hasOwner(inst.forms,i))
        	{
        		code = createWordVisualCodeW(Word.WORDFV_OWNER0, w0);
            	addWordFeature(code, fv);
           // 	System.out.println("new Context feature is added:"+WORDFV_AG0);
            	break;
        	}
        	if(relation.hasOwned(inst.forms,i))
        	{
        		code = createWordVisualCodeW(WORDFV_OWNED0, w0);
            	addWordFeature(code, fv);
            //	System.out.println("new Context feature is added:"+WORDFV_VB0);
            	return;
        	}
		}
        
    }
    
    public void addContextWordFeaturesBigram(DependencyInstance inst, int i, FeatureVector fv)
    {
    	//TODO
    	long code = 0;
    	int[] toks = inst.formids;
        int w0 = toks[i];
        int wLeft = i > 0 ? toks[i-1] : TOKEN_START;
    	int wRight = i < toks.length-1 ? toks[i+1] : TOKEN_END;
    	int iLeft= i > 0 ? i-1 : TOKEN_START;
    	int iRight= i < toks.length-1 ? i+1 : TOKEN_END;
    	
    	boolean ag0=false, agLeft=false,agRight=false, theme0=false,themeLeft=false, themeRight=false;
    	boolean vb0=false, vbLeft=false,vbRight=false;
    	
    	
        for (int j = 0; j < inst.contextInstance.getAgentRelations().size(); j++) {//this word feature will not be added if the word is not agent in any relation
        	ContextRelation relation =inst.contextInstance.getAgentRelations().get(j);
        	if(relation.hasEntity(inst.forms,i))
        	{
        		ag0=true;
        	}
        	if(i==1 || relation.hasEntity(inst.forms,iLeft))//real first word in sentence, 0 is for root element
        	{
        		agLeft=true;
        	}
        	if(i<toks.length-1 && relation.hasEntity(inst.forms,iRight))
        	{
        		agRight=true;
        	}
        	if(relation.hasAction(inst.forms,i))
        	{
        		vb0=true;
        	}
        	if(i==1 || relation.hasAction(inst.forms,iLeft))//real first word in sentence, 0 is for root element
        	{
        		vbLeft=true;
        	}
        	if(i<toks.length-1 && relation.hasAction(inst.forms,iRight))
        	{
        		vbRight=true;
        	}
        }
        
    	
    	for (int j = 0; j < inst.contextInstance.getThemeRelations().size(); j++) {//this word feature will not be added if the word is not theme in any relation
    		ContextRelation relation =inst.contextInstance.getThemeRelations().get(j);
        	if(relation.hasEntity(inst.forms,i))
        	{
        		theme0=true;
        	}
        	if(i==1 || relation.hasEntity(inst.forms,iLeft))//real first word in sentence, 0 is for root element
        	{
        		themeLeft=true;
        	}
        	if(i<toks.length-1 && relation.hasEntity(inst.forms,iRight))
        	{
        		themeRight=true;
        	}
        	if(relation.hasAction(inst.forms,i))
        	{
        		vb0=true;
        	}
        	if(i==1 || relation.hasAction(inst.forms,iLeft))//real first word in sentence, 0 is for root element
        	{
        		vbLeft=true;
        	}
        	if(i<toks.length-1 && relation.hasAction(inst.forms,iRight))
        	{
        		vbRight=true;
        	}
		}
        if(ag0 && agLeft)//TODO handle if still word has not come yet
        {
        	code = createWordVisualCodeBigram(WORDFV_AGpAG0, wLeft,w0);
        	addWordFeature(code, fv);
        //	System.out.println("new Context feature is added:"+WORDFV_AGpAG0);
        }
        if(ag0 && agRight)
        {
        	code = createWordVisualCodeBigram(WORDFV_AG0AGn, w0,wRight);
        	addWordFeature(code, fv);
        //	System.out.println("new Context feature is added:"+WORDFV_AG0AGn);
        }
        if(ag0 && themeLeft)
        {
        	code = createWordVisualCodeBigram(WORDFV_THpAG0, wLeft,w0);
        	addWordFeature(code, fv);
        //	System.out.println("new Context feature is added:"+WORDFV_THpAG0);
        }
        if(ag0 && themeRight)
        {
        	code = createWordVisualCodeBigram(WORDFV_AG0THn, w0,wRight);
        	addWordFeature(code, fv);
        //	System.out.println("new Context feature is added:"+WORDFV_AG0THn);
        }
        if(ag0 && vbLeft)
        {
        	code = createWordVisualCodeBigram(WORDFV_VBpAG0, wLeft,w0);
        	addWordFeature(code, fv);
        //	System.out.println("new Context feature is added:"+WORDFV_VBpAG0);
        }
        if(ag0 && vbRight)
        {
        	code = createWordVisualCodeBigram(WORDFV_AG0VBn, w0,wRight);
        	addWordFeature(code, fv);
        //	System.out.println("new Context feature is added:"+WORDFV_AG0VBn);
        }
        if(theme0 && agLeft)
        {
        	code = createWordVisualCodeBigram(WORDFV_AGpTH0, wLeft,w0);
        	addWordFeature(code, fv);
       // 	System.out.println("new Context feature is added:"+WORDFV_AGpTH0);
        }
        if(theme0 && agRight)
        {
        	code = createWordVisualCodeBigram(WORDFV_TH0AGn, w0,wRight);
        	addWordFeature(code, fv);
        //	System.out.println("new Context feature is added:"+WORDFV_TH0AGn);
        }
        if(theme0 && themeLeft)
        {
        	code = createWordVisualCodeBigram(WORDFV_THpTH0, wLeft,w0);
        	addWordFeature(code, fv);
        //	System.out.println("new Context feature is added:"+WORDFV_THpTH0);
        }
        if(theme0 && themeRight)
        {
        	code = createWordVisualCodeBigram(WORDFV_TH0THn, w0,wRight);
        	addWordFeature(code, fv);
        //	System.out.println("new Context feature is added:"+WORDFV_TH0THn);
        }
        if(theme0 && vbLeft)
        {
        	code = createWordVisualCodeBigram(WORDFV_VBpTH0, wLeft,w0);
        	addWordFeature(code, fv);
        //	System.out.println("new Context feature is added:"+WORDFV_VBpTH0);
        }
        if(theme0 && themeRight)
        {
        	code = createWordVisualCodeBigram(WORDFV_TH0VBn, w0,wRight);
        	addWordFeature(code, fv);
        //	System.out.println("new Context feature is added:"+WORDFV_TH0VBn);
        }
    }
    
    public void addWordVectorFeatures(DependencyInstance inst, int i, int dis, FeatureVector fv) {
    	
    	int d = getBinnedDistance(dis);
    	double [] v = unknownWv;
    	int pos = i + dis;
    	
    	if (pos >= 0 && pos < inst.length) {
    		int wvid = inst.wordVecIds[pos];
    		if (wvid > 0) v = wordVectors[wvid];
    	}
    	
		//if (v == unknownWv) ++wvMiss; else ++wvHit;
		
		if (v != null) {
			for (int j = 0; j < v.length; ++j) {
				long code = createWordCodeW(WORDFV_EMB, j);
				addWordFeature(code | d, v[j], fv);
			}
		}
    }

    /************************************************************************
     *  Region end #
     ************************************************************************/
    
    
    
    /************************************************************************
     * Region start #
     * 
     *  Functions that create feature vectors for labeled arcs
     *  
     ************************************************************************/
    
    public FeatureVector createLabelFeatures(DependencyInstance inst,
    		DependencyArcList arcLis, int[] heads, int mod, int type,List<ContextRelation> usedRelations)
    {
    	FeatureVector fv = new FeatureVector(numLabeledArcFeats);
    	if (!options.learnLabel) return fv;
    	
    	// label type to start from 1 in hashcode
    	type = type+1;
    	
    	//int[] heads = inst.heads;
    	int head = heads[mod];
    	    	
    	fv.addEntries(createLabeledArcFeatures(inst, head, mod, type+1, usedRelations));
    	
    	//int ghead = heads[head];
    	//if (ghead != -1) {
    	//	fv.addEntries(createGPCFeatureVector(inst, ghead, head, mod, type));
    	//}
    	
    	return fv;
    }
    
    public FeatureVector createLabeledArcFeatures(DependencyInstance inst, int h, int c, int type,List<ContextRelation> usedRelations) 
    {
    	
    	int attDist = getBinnedDistance(h-c);
    	
    	FeatureVector fv = new FeatureVector(numLabeledArcFeats);
    	
    	addBasic1OFeatures(fv, inst, h, c, attDist, type);
    	
    	addCore1OPosFeatures(fv, inst, h, c, attDist, type);
    		    		
    	addCore1OBigramFeatures(fv, inst.formids[h], inst.postagids[h], 
    			inst.formids[c], inst.postagids[c], attDist, type);
    	    		
		if (inst.lemmaids != null)
			addCore1OBigramFeatures(fv, inst.lemmaids[h], inst.postagids[h], 
					inst.lemmaids[c], inst.postagids[c], attDist, type);
		
		addCore1OBigramFeatures(fv, inst.formids[h], inst.cpostagids[h], 
    			inst.formids[c], inst.cpostagids[c], attDist, type);
		
		if (inst.lemmaids != null)
			addCore1OBigramFeatures(fv, inst.lemmaids[h], inst.cpostagids[h], 
					inst.lemmaids[c], inst.cpostagids[c], attDist, type);
    	
    	if (inst.featids[h] != null && inst.featids[c] != null) {
    		for (int i = 0, N = inst.featids[h].length; i < N; ++i)
    			for (int j = 0, M = inst.featids[c].length; j < M; ++j) {
    				
    				addCore1OBigramFeatures(fv, inst.formids[h], inst.featids[h][i], 
    						inst.formids[c], inst.featids[c][j], attDist, type);
    				
    				if (inst.lemmas != null)
    					addCore1OBigramFeatures(fv, inst.lemmaids[h], inst.featids[h][i], 
    							inst.lemmaids[c], inst.featids[c][j], attDist, type);
    			}
    	}
    	if(options.context && ContextConstances.useContextFeatures)
    	{
    		int old=fv.size();
    		//System.out.println("========ARC Label==========");
    		addArcContextFeatures(fv, inst, h, c, attDist,type,usedRelations);
    		if(fv.size()>old)
    			{
    				//System.out.println("==========C="+c+ " H="+h+"="+fv.size()+ " - "+old);
    			}
    	}
    			
    	return fv;
    }
    
    public void addBasic1OFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int m, int attDist, int type) 
    {
    	
    	long code = 0; 			// feature code
    	
    	int[] forms = inst.formids, lemmas = inst.lemmaids, postags = inst.postagids;
    	int[] cpostags = inst.cpostagids;
    	int[][] feats = inst.featids;
    	
    	int tid = type << 4;

    	code = createArcCodeW(CORE_HEAD_WORD, forms[h]) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	    	    	
    	code = createArcCodeW(CORE_MOD_WORD, forms[m]) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodeWW(HW_MW, forms[h], forms[m]) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	int pHF = h == 0 ? TOKEN_START : (h == m+1 ? TOKEN_MID : forms[h-1]);
    	int nHF = h == inst.length - 1 ? TOKEN_END : (h+1 == m ? TOKEN_MID : forms[h+1]);
    	int pMF = m == 0 ? TOKEN_START : (m == h+1 ? TOKEN_MID : forms[m-1]);
    	int nMF = m == inst.length - 1 ? TOKEN_END : (m+1 == h ? TOKEN_MID : forms[m+1]);
    	
    	code = createArcCodeW(CORE_HEAD_pWORD, pHF) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodeW(CORE_HEAD_nWORD, nHF) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodeW(CORE_MOD_pWORD, pMF) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodeW(CORE_MOD_nWORD, nMF) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
	
		
    	code = createArcCodeP(CORE_HEAD_POS, postags[h]) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodeP(CORE_HEAD_POS, cpostags[h]) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodeP(CORE_MOD_POS, postags[m]) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodeP(CORE_MOD_POS, cpostags[m]) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodePP(HP_MP, postags[h], postags[m]) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodePP(HP_MP, cpostags[h], cpostags[m]) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	     	
    	if (lemmas != null) {
    		code = createArcCodeW(CORE_HEAD_WORD, lemmas[h]) | tid;
        	addLabeledArcFeature(code, fv);
        	addLabeledArcFeature(code | attDist, fv);
        	
    		code = createArcCodeW(CORE_MOD_WORD, lemmas[m]) | tid;
        	addLabeledArcFeature(code, fv);
        	addLabeledArcFeature(code | attDist, fv);
        	
        	code = createArcCodeWW(HW_MW, lemmas[h], lemmas[m]) | tid;
        	addLabeledArcFeature(code, fv);
        	addLabeledArcFeature(code | attDist, fv);
        	
	    	int pHL = h == 0 ? TOKEN_START : (h == m+1 ? TOKEN_MID : lemmas[h-1]);
	    	int nHL = h == inst.length - 1 ? TOKEN_END : (h+1 == m ? TOKEN_MID : lemmas[h+1]);
	    	int pML = m == 0 ? TOKEN_START : (m == h+1 ? TOKEN_MID : lemmas[m-1]);
	    	int nML = m == inst.length - 1 ? TOKEN_END : (m+1 == h ? TOKEN_MID : lemmas[m+1]);
	    	
	    	code = createArcCodeW(CORE_HEAD_pWORD, pHL) | tid;
	    	addLabeledArcFeature(code, fv);
	    	addLabeledArcFeature(code | attDist, fv);
	    	
	    	code = createArcCodeW(CORE_HEAD_nWORD, nHL) | tid;
	    	addLabeledArcFeature(code, fv);
	    	addLabeledArcFeature(code | attDist, fv);
	    	
	    	code = createArcCodeW(CORE_MOD_pWORD, pML) | tid;
	    	addLabeledArcFeature(code, fv);
	    	addLabeledArcFeature(code | attDist, fv);
	    	
	    	code = createArcCodeW(CORE_MOD_nWORD, nML) | tid;
	    	addLabeledArcFeature(code, fv);
	    	addLabeledArcFeature(code | attDist, fv);
    	}
    	
		if (feats[h] != null)
			for (int i = 0, N = feats[h].length; i < N; ++i) {
				code = createArcCodeP(CORE_HEAD_POS, feats[h][i]) | tid;
	        	addLabeledArcFeature(code, fv);
	        	addLabeledArcFeature(code | attDist, fv);
			}
		
		if (feats[m] != null)
			for (int i = 0, N = feats[m].length; i < N; ++i) {
				code = createArcCodeP(CORE_MOD_POS, feats[m][i]) | tid;
	        	addLabeledArcFeature(code, fv);
	        	addLabeledArcFeature(code | attDist, fv);
			}
		
		if (feats[h] != null && feats[m] != null) {
			for (int i = 0, N = feats[h].length; i < N; ++i)
				for (int j = 0, M = feats[m].length; j < M; ++j) {
			    	code = createArcCodePP(HP_MP, feats[h][i], feats[m][j]) | tid;
			    	addLabeledArcFeature(code, fv);
			    	addLabeledArcFeature(code | attDist, fv);
				}
		}
		
		if (wordVectors != null) {
			
			int wvid = inst.wordVecIds[h];
			double [] v = wvid > 0 ? wordVectors[wvid] : unknownWv;
			if (v != null) {
				for (int i = 0; i < v.length; ++i) {
					code = createArcCodeW(HEAD_EMB, i) | tid;
					addLabeledArcFeature(code, v[i], fv);
					addLabeledArcFeature(code | attDist, v[i], fv);
				}
			}
			
			wvid = inst.wordVecIds[m];
			v = wvid > 0 ? wordVectors[wvid] : unknownWv;
			if (v != null) {
				for (int i = 0; i < v.length; ++i) {
					code = createArcCodeW(MOD_EMB, i) | tid;
					addLabeledArcFeature(code, v[i], fv);
					addLabeledArcFeature(code | attDist, v[i], fv);
				}
			}
		}
    }
    
       
    public void addCore1OPosFeatures(FeatureVector fv, DependencyInstance inst, 
    		int h, int c, int attDist, int type) 
    {  	
    	
    	int[] pos = inst.postagids;
    	int[] posA = inst.cpostagids;
	
    	int tid = type << 4;
    	
    	int pHead = pos[h], pHeadA = posA[h];
    	int pMod = pos[c], pModA = posA[c];
    	int pHeadLeft = h > 0 ? (h-1 == c ? TOKEN_MID : pos[h-1]) : TOKEN_START;    	
    	int pModRight = c < pos.length-1 ? (c+1 == h ? TOKEN_MID : pos[c+1]) : TOKEN_END;
    	int pHeadRight = h < pos.length-1 ? (h+1 == c ? TOKEN_MID: pos[h+1]) : TOKEN_END;
    	int pModLeft = c > 0 ? (c-1 == h ? TOKEN_MID : pos[c-1]) : TOKEN_START;
    	int pHeadLeftA = h > 0 ? (h-1 == c ? TOKEN_MID : posA[h-1]) : TOKEN_START;    	
    	int pModRightA = c < posA.length-1 ? (c+1 == h ? TOKEN_MID : posA[c+1]) : TOKEN_END;
    	int pHeadRightA = h < posA.length-1 ? (h+1 == c ? TOKEN_MID: posA[h+1]) : TOKEN_END;
    	int pModLeftA = c > 0 ? (c-1 == h ? TOKEN_MID : posA[c-1]) : TOKEN_START;
    	
    	    	
    	long code = 0;
    	
    	// feature posR posMid posL
    	int small = h < c ? h : c;
    	int large = h > c ? h : c;
    	for(int i = small+1; i < large; i++) {    		
    		code = createArcCodePPP(HP_BP_MP, pHead, pos[i], pMod) | tid;
    		addLabeledArcFeature(code, fv);
    		addLabeledArcFeature(code | attDist, fv);
    		
    		code = createArcCodePPP(HP_BP_MP, pHeadA, posA[i], pModA) | tid;
    		addLabeledArcFeature(code, fv);
    		addLabeledArcFeature(code | attDist, fv);
    	}
    	
    	// feature posL-1 posL posR posR+1
    	code = createArcCodePPPP(HPp_HP_MP_MPn, pHeadLeft, pHead, pMod, pModRight) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HP_MP_MPn, pHead, pMod, pModRight) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HPp_HP_MP, pHeadLeft, pHead, pMod) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HPp_MP_MPn, pHeadLeft, pMod, pModRight) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HPp_HP_MPn, pHeadLeft, pHead, pModRight) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);

    	code = createArcCodePPPP(HPp_HP_MP_MPn, pHeadLeftA, pHeadA, pModA, pModRightA) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HP_MP_MPn, pHeadA, pModA, pModRightA) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HPp_HP_MP, pHeadLeftA, pHeadA, pModA) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HPp_MP_MPn, pHeadLeftA, pModA, pModRightA) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
    	code = createArcCodePPP(HPp_HP_MPn, pHeadLeftA, pHeadA, pModRightA) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
    	
    	// feature posL posL+1 posR-1 posR
		code = createArcCodePPPP(HP_HPn_MPp_MP, pHead, pHeadRight, pModLeft, pMod) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HP_MPp_MP, pHead, pModLeft, pMod) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HP_HPn_MP, pHead, pHeadRight, pMod) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HPn_MPp_MP, pHeadRight, pModLeft, pMod) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HP_HPn_MPp, pHead, pHeadRight, pModLeft) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
		code = createArcCodePPPP(HP_HPn_MPp_MP, pHeadA, pHeadRightA, pModLeftA, pModA) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HP_MPp_MP, pHeadA, pModLeftA, pModA) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HP_HPn_MP, pHeadA, pHeadRightA, pModA) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HPn_MPp_MP, pHeadRightA, pModLeftA, pModA) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
		code = createArcCodePPP(HP_HPn_MPp, pHeadA, pHeadRightA, pModLeftA) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
	
    	
		// feature posL-1 posL posR-1 posR
		// feature posL posL+1 posR posR+1
		code = createArcCodePPPP(HPp_HP_MPp_MP, pHeadLeft, pHead, pModLeft, pMod) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
		code = createArcCodePPPP(HP_HPn_MP_MPn, pHead, pHeadRight, pMod, pModRight) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
		code = createArcCodePPPP(HPp_HP_MPp_MP, pHeadLeftA, pHeadA, pModLeftA, pModA) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
		code = createArcCodePPPP(HP_HPn_MP_MPn, pHeadA, pHeadRightA, pModA, pModRightA) | tid;
		addLabeledArcFeature(code, fv);
		addLabeledArcFeature(code | attDist, fv);
		
    }

    public void addCore1OBigramFeatures(FeatureVector fv, int head, int headP, 
    		int mod, int modP, int attDist, int type) 
    {
    	
    	long code = 0;
    	
    	int tid = type << 4;
    	
    	code = createArcCodeWWPP(HW_MW_HP_MP, head, mod, headP, modP) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodeWPP(MW_HP_MP, mod, headP, modP) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodeWPP(HW_HP_MP, head, headP, modP) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodeWP(MW_HP, mod, headP) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodeWP(HW_MP, head, modP) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	    	
    	code = createArcCodeWP(HW_HP, head, headP) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
    	
    	code = createArcCodeWP(MW_MP, mod, modP) | tid;
    	addLabeledArcFeature(code, fv);
    	addLabeledArcFeature(code | attDist, fv);
      
    }
    
//    public FeatureVector createLabelFeatures(DependencyInstance inst,
//    		int head, int mod, int type)
//    {
//    	FeatureVector fv = new FeatureVector(arcAlphabet.size());
//    	if (!options.learnLabel) return fv;
//    	
//    	int[] forms = inst.formids;
//    	int[] pos = inst.postagids;
//    	int[] cpos = inst.cpostagids;
//    	int[][] feats = inst.featids;
//    	
//    	int hw = forms[head], hp = pos[head], hcp = cpos[head];
//    	int mw = forms[mod], mp = pos[mod], mcp = cpos[mod];
//    			
//    	long code = 0;
//  
//    	int dist = getBinnedDistance(head-mod);
//    	
//		code = createArcCodeWP(HW_LABEL, hw, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		code = createArcCodePP(HP_LABEL, hp, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		code = createArcCodePP(HP_LABEL, hcp, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		if (feats[head] != null) {
//			for (int f : feats[head]) {
//				code = createArcCodePP(HP_LABEL, f, type);
//				addArcFeature(code, fv);
//				addArcFeature(code | dist, fv);	
//			}
//		}
//		
//		code = createArcCodeWP(MW_LABEL, mw, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		code = createArcCodePP(MP_LABEL, mp, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		code = createArcCodePP(MP_LABEL, mcp, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		if (feats[mod] != null) {
//			for (int f : feats[mod]) {
//				code = createArcCodePP(MP_LABEL, f, type);
//				addArcFeature(code, fv);
//				addArcFeature(code | dist, fv);	
//			}
//		}
//		
//		code = createArcCodeWPP(HW_MP_LABEL, hw, mp, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		code = createArcCodeWPP(HW_MP_LABEL, hw, mcp, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		code = createArcCodePPP(HP_MP_LABEL, hp, mp, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		code = createArcCodePPP(HP_MP_LABEL, hp, mcp, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		code = createArcCodePPP(HP_MP_LABEL, hcp, mp, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		code = createArcCodePPP(HP_MP_LABEL, hcp, mcp, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		code = createArcCodeWPP(MW_HP_LABEL, mw, hp, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		code = createArcCodeWPP(MW_HP_LABEL, mw, hcp, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | dist, fv);	
//		
//		if (feats[head] != null) {
//			for (int f : feats[head]) {
//				code = createArcCodeWPP(MW_HP_LABEL, mw, f, type);
//				addArcFeature(code, fv);
//				addArcFeature(code | dist, fv);	
//				
//				code = createArcCodePPP(HP_MP_LABEL, f, mp, type);
//				addArcFeature(code, fv);
//				addArcFeature(code | dist, fv);
//				
//				code = createArcCodePPP(HP_MP_LABEL, f, mcp, type);
//				addArcFeature(code, fv);
//				addArcFeature(code | dist, fv);	
//			}
//		}
//		
//		if (feats[mod] != null) {
//			for (int f : feats[mod]) {
//				code = createArcCodeWPP(HW_MP_LABEL, hw, f, type);
//				addArcFeature(code, fv);
//				addArcFeature(code | dist, fv);
//				
//				code = createArcCodePPP(HP_MP_LABEL, hp, f, type);
//				addArcFeature(code, fv);
//				addArcFeature(code | dist, fv);
//				
//				code = createArcCodePPP(HP_MP_LABEL, hcp, f, type);
//				addArcFeature(code, fv);
//				addArcFeature(code | dist, fv);	
//			}
//		}
//		
//		{
//			int pmw = mod > 0 ? forms[mod-1] : TOKEN_START;
//			int pmp = mod > 0 ? pos[mod-1] : TOKEN_START;
//			
//			code = createArcCodePPP(HP_pMP_LABEL, hp, pmp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodePPP(HP_pMP_LABEL, hcp, pmp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodeWPP(HP_pMW_LABEL, pmw, hp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodeWPP(HP_pMW_LABEL, pmw, hcp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodePPP(MP_pMP_LABEL, mp, pmp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodePPP(MP_pMP_LABEL, mcp, pmp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodeWPP(MP_pMW_LABEL, pmw, mp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodeWPP(MP_pMW_LABEL, pmw, mcp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//		}
//    	
//		{
//			int nmw = mod < inst.length-1 ? forms[mod+1] : TOKEN_END;
//			int nmp = mod < inst.length-1 ? pos[mod+1] : TOKEN_END;
//			
//			code = createArcCodePPP(HP_nMP_LABEL, hp, nmp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodePPP(HP_nMP_LABEL, hcp, nmp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodeWPP(HP_nMW_LABEL, nmw, hp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodeWPP(HP_nMW_LABEL, nmw, hcp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodePPP(MP_nMP_LABEL, mp, nmp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodePPP(MP_nMP_LABEL, mcp, nmp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodeWPP(MP_nMW_LABEL, nmw, mp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//			
//			code = createArcCodeWPP(MP_nMW_LABEL, nmw, mcp, type);
//			addArcFeature(code, fv);
//			addArcFeature(code | dist, fv);
//		}
//		
//    	return fv;
//    }
    
//    public FeatureVector createLabelFeatures(DependencyInstance inst, int word,
//    		int type, boolean toRight, boolean isChild) 
//    {
//    	
//    	FeatureVector fv = new FeatureVector(arcAlphabet.size());
//    	if (!options.learnLabel) return fv;
//    	    	
//    	int att = 1;
//    	if (toRight) att |= 2;
//    	if (isChild) att |= 4;
//    	
//    	int[] toks = inst.formids;
//    	int[] pos = inst.postagids;
//    	
//    	int w = toks[word];
//    	
//    	long code = 0;
//    	
//    	code = createArcCodeP(CORE_LABEL_NTS1, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | att, fv);		
//				
//    	int wP = pos[word];
//    	int wPm1 = word > 0 ? pos[word-1] : TOKEN_START;
//    	int wPp1 = word < pos.length-1 ? pos[word+1] : TOKEN_END;
//    	
//		code = createArcCodeWPP(CORE_LABEL_NTH, w, wP, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | att, fv);	
//		
//		code = createArcCodePP(CORE_LABEL_NTI, wP, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | att, fv);	
//		
//		code = createArcCodePPP(CORE_LABEL_NTIA, wPm1, wP, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | att, fv);	
//		
//		code = createArcCodePPP(CORE_LABEL_NTIB, wP, wPp1, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | att, fv);	
//		
//		code = createArcCodePPPP(CORE_LABEL_NTIC, wPm1, wP, wPp1, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | att, fv);	
//		
//		code = createArcCodeWP(CORE_LABEL_NTJ, w, type);
//		addArcFeature(code, fv);
//		addArcFeature(code | att, fv);	
//    	
//    	return fv;
//    }
    
    public FeatureVector createTripsFeatureVector(DependencyInstance inst, int par,
    		int ch1, int ch2) {

    	FeatureVector fv = new FeatureVector(numArcFeats);
    	
    	int[] pos = inst.postagids;
    	int[] posA = inst.cpostagids;

    	// ch1 is always the closes to par
    	int dirFlag = ((((par < ch1 ? 0 : 1) << 1) | (par < ch2 ? 0 : 1)) << 1) | 1;

    	int HP = pos[par];
    	int SP = ch1 == par ? TOKEN_START : pos[ch1];
    	int MP = pos[ch2];
    	int HC = posA[par];
    	int SC = ch1 == par ? TOKEN_START : posA[ch1];
    	int MC  = posA[ch2];

    	long code = 0;

    	code = createArcCodePPP(HP_SP_MP, HP, SP, MP);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPP(HC_SC_MC, HC, SC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	addTurboSib(inst, par, ch1, ch2, dirFlag, fv);
    	
    	return fv;
    }

    void addTurboSib(DependencyInstance inst, int par, int ch1, int ch2, int dirFlag, FeatureVector fv) 
    {
    	int[] posA = inst.cpostagids;
    	//int[] lemma = inst.lemmaids;
    	int[] lemma = inst.lemmaids != null ? inst.lemmaids : inst.formids;
    	int len = inst.length;

    	int HC = posA[par];
    	int SC = ch1 == par ? TOKEN_START : posA[ch1];
    	int MC = posA[ch2];
    	
    	int pHC = par > 0 ? posA[par - 1] : TOKEN_START;
    	int nHC = par < len - 1 ? posA[par + 1] : TOKEN_END;
    	int pSC = ch1 > 0 ? posA[ch1 - 1] : TOKEN_START;
    	int nSC = ch1 < len - 1 ? posA[ch1 + 1] : TOKEN_END;
    	int pMC = ch2 > 0 ? posA[ch2 - 1] : TOKEN_START;
    	int nMC = ch2 < len - 1 ? posA[ch2 + 1] : TOKEN_END;

    	long code = 0;

    	// CCC
    	code = createArcCodePPPP(pHC_HC_SC_MC, pHC, HC, SC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPP(HC_nHC_SC_MC, HC, nHC, SC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPP(HC_pSC_SC_MC, HC, pSC, SC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPP(HC_SC_nSC_MC, HC, SC, nSC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPP(HC_SC_pMC_MC, HC, SC, pMC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPP(HC_SC_MC_nMC, HC, SC, MC, nMC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	int HL = lemma[par];
    	int SL = ch1 == par ? TOKEN_START : lemma[ch1];
    	int ML = lemma[ch2];
    	
    	// LCC
    	code = createArcCodeWPPP(pHC_HL_SC_MC, HL, pHC, SC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HL_nHC_SC_MC, HL, nHC, SC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HL_pSC_SC_MC, HL, pSC, SC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HL_SC_nSC_MC, HL, SC, nSC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HL_SC_pMC_MC, HL, SC, pMC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HL_SC_MC_nMC, HL, SC, MC, nMC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	// CLC
    	code = createArcCodeWPPP(pHC_HC_SL_MC, SL, pHC, HC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HC_nHC_SL_MC, SL, HC, nHC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HC_pSC_SL_MC, SL, HC, pSC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HC_SL_nSC_MC, SL, HC, nSC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HC_SL_pMC_MC, SL, HC, pMC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HC_SL_MC_nMC, SL, HC, MC, nMC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	// CCL
    	code = createArcCodeWPPP(pHC_HC_SC_ML, ML, pHC, HC, SC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HC_nHC_SC_ML, ML, HC, nHC, SC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HC_pSC_SC_ML, ML, HC, pSC, SC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HC_SC_nSC_ML, ML, HC, SC, nSC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HC_SC_pMC_ML, ML, HC, SC, pMC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodeWPPP(HC_SC_ML_nMC, ML, HC, SC, nMC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);
    	
    	code = createArcCodePPPPP(HC_MC_SC_pHC_pMC, HC, MC, SC, pHC, pMC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(HC_MC_SC_pHC_pSC, HC, MC, SC, pHC, pSC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(HC_MC_SC_pMC_pSC, HC, MC, SC, pMC, pSC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(HC_MC_SC_nHC_nMC, HC, MC, SC, nHC, nMC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(HC_MC_SC_nHC_nSC, HC, MC, SC, nHC, nSC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(HC_MC_SC_nMC_nSC, HC, MC, SC, nMC, nSC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(HC_MC_SC_pHC_nMC, HC, MC, SC, pHC, nMC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(HC_MC_SC_pHC_nSC, HC, MC, SC, pHC, nSC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(HC_MC_SC_pMC_nSC, HC, MC, SC, pMC, nSC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(HC_MC_SC_nHC_pMC, HC, MC, SC, nHC, pMC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(HC_MC_SC_nHC_pSC, HC, MC, SC, nHC, pSC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(HC_MC_SC_nMC_pSC, HC, MC, SC, nMC, pSC);
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);
    }

    public FeatureVector createSibFeatureVector(DependencyInstance inst, int ch1, int ch2/*, boolean isST*/) {
    	
    	FeatureVector fv = new FeatureVector(numArcFeats);

    	int[] pos = inst.postagids;
    	int[] posA = inst.cpostagids;
    	int[] toks = inst.formids;
    	//int[] lemma = inst.lemmaids;
    	int[] lemma = inst.lemmaids != null ? inst.lemmaids : inst.formids;
    	
    	// ch1 is always the closes to par

    	int SP = /*isST ? TOKEN_START :*/ pos[ch1];
    	int MP = pos[ch2];
    	int SW = /*isST ? TOKEN_START :*/ toks[ch1];
    	int MW = toks[ch2];
    	int SC = /*isST ? TOKEN_START :*/ posA[ch1];
    	int MC = posA[ch2];


    	//Utils.Assert(ch1 < ch2);
    	int flag = getBinnedDistance(ch1 - ch2);

    	long code = 0;

    	code = createArcCodePP(SP_MP, SP, MP);
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);

    	code = createArcCodeWW(SW_MW, SW, MW);
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);

    	code = createArcCodeWP(SW_MP, SW, MP);
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);

    	code = createArcCodeWP(SP_MW, MW, SP);
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);

    	code = createArcCodePP(SC_MC, SC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);
    	
    		
    	int SL = /*isST ? TOKEN_START :*/ lemma[ch1];
    	int ML = lemma[ch2];
    	
    	code = createArcCodeWW(SL_ML, SL, ML);
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);

    	code = createArcCodeWP(SL_MC, SL, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);

    	code = createArcCodeWP(SC_ML, ML, SC);
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);
	    	
    	
    	return fv;
    }

    public FeatureVector createHeadBiFeatureVector(DependencyInstance inst, int ch, int par1, int par2) {
    	FeatureVector fv = new FeatureVector(numArcFeats);
    	
    	int[] pos = inst.postagids;
    	int[] posA = inst.cpostagids;

    	// exactly 16 combination, so need one more template
    	int flag = 0;
    	if (par1 == par2)
    		flag = 1;
    	else if (par1 == ch + 1)
    		flag = 2;
    	else if (par2 == ch)
    		flag = 3;

    	int dirFlag = flag;
    	dirFlag = (dirFlag << 1) | (par1 < ch ? 1 : 0);
    	dirFlag = (dirFlag << 1) | (par2 < ch + 1 ? 1 : 0);

    	long code = 0;

    	int H1P = pos[par1];
    	int H2P = pos[par2];
    	int M1P = pos[ch];
    	int M2P = pos[ch + 1];
    	int H1C = posA[par1];
    	int H2C = posA[par2];
    	int M1C = posA[ch];
    	int M2C = posA[ch + 1];

    	code = createArcCodePPPP(H1P_H2P_M1P_M2P, H1P, H2P, M1P, M2P);
    	addArcFeature(code | flag, fv);
    	code = createArcCodePPPP(H1P_H2P_M1P_M2P_DIR, H1P, H2P, M1P, M2P);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPP(H1C_H2C_M1C_M2C, H1C, H2C, M1C, M2C);
    	addArcFeature(code | flag, fv);
    	code = createArcCodePPPP(H1C_H2C_M1C_M2C_DIR, H1C, H2C, M1C, M2C);
    	addArcFeature(code | dirFlag, fv);
    	
    	return fv;
    }

    
    public FeatureVector createGPCFeatureVector(DependencyInstance inst, 
    		int gp, int par, int c)
    {
    	return createGPCFeatureVector(inst, gp, par, c, 0);
    }
    
    public FeatureVector createGPCFeatureVector(DependencyInstance inst, 
    		int gp, int par, int c, int type) 
    {

    	FeatureVector fv = new FeatureVector(numArcFeats);
    	
    	int tid = type << 4;
    	
    	int[] pos = inst.postagids;
    	int[] posA = inst.cpostagids;
    	//int[] lemma = inst.lemmaids;
    	int[] lemma = inst.lemmaids != null ? inst.lemmaids : inst.formids;
    	
    	int flag = (((((gp < par ? 0 : 1) << 1) | (par < c ? 0 : 1)) << 1) | 1);

    	int GP = pos[gp];
    	int HP = pos[par];
    	int MP = pos[c];
    	int GC = posA[gp];
    	int HC = posA[par];
    	int MC = posA[c];
    	long code = 0;

    	code = createArcCodePPP(GP_HP_MP, GP, HP, MP) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);

    	code = createArcCodePPP(GC_HC_MC, GC, HC, MC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);
    
        int GL = lemma[gp];
        int HL = lemma[par];
        int ML = lemma[c];

        code = createArcCodeWPP(GL_HC_MC, GL, HC, MC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPP(GC_HL_MC, HL, GC, MC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPP(GC_HC_ML, ML, GC, HC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	addTurboGPC(inst, gp, par, c, flag, tid, fv);
    	
    	return fv;
    }

    void addTurboGPC(DependencyInstance inst, int gp, int par, int c, 
    		int dirFlag, int tid, FeatureVector fv) {
    	int[] posA = inst.cpostagids;
    	//int[] lemma = inst.lemmaids;
    	int[] lemma = inst.lemmaids != null ? inst.lemmaids : inst.formids;
    	int len = posA.length;

    	int GC = posA[gp];
    	int HC = posA[par];
    	int MC = posA[c];

    	int pGC = gp > 0 ? posA[gp - 1] : TOKEN_START;
    	int nGC = gp < len - 1 ? posA[gp + 1] : TOKEN_END;
    	int pHC = par > 0 ? posA[par - 1] : TOKEN_START;
    	int nHC = par < len - 1 ? posA[par + 1] : TOKEN_END;
    	int pMC = c > 0 ? posA[c - 1] : TOKEN_START;
    	int nMC = c < len - 1 ? posA[c + 1] : TOKEN_END;

    	long code = 0;

    	// CCC
    	code = createArcCodePPPP(pGC_GC_HC_MC, pGC, GC, HC, MC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPP(GC_nGC_HC_MC, GC, nGC, HC, MC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPP(GC_pHC_HC_MC, GC, pHC, HC, MC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPP(GC_HC_nHC_MC, GC, HC, nHC, MC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPP(GC_HC_pMC_MC, GC, HC, pMC, MC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPP(GC_HC_MC_nMC, GC, HC, MC, nMC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(GC_HC_MC_pGC_pHC, GC, HC, MC, pGC, pHC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(GC_HC_MC_pGC_pMC, GC, HC, MC , pGC, pMC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(GC_HC_MC_pHC_pMC, GC, HC, MC, pHC, pMC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(GC_HC_MC_nGC_nHC, GC, HC, MC, nGC, nHC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(GC_HC_MC_nGC_nMC, GC, HC, MC, nGC, nMC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(GC_HC_MC_nHC_nMC, GC, HC, MC, nHC, nMC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(GC_HC_MC_pGC_nHC, GC, HC, MC, pGC, nHC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(GC_HC_MC_pGC_nMC, GC, HC, MC, pGC, nMC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(GC_HC_MC_pHC_nMC, GC, HC, MC, pHC, nMC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(GC_HC_MC_nGC_pHC, GC, HC, MC, nGC, pHC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(GC_HC_MC_nGC_pMC, GC, HC, MC, nGC, pMC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePPPPP(GC_HC_MC_nHC_pMC, GC, HC, MC, nHC, pMC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePP(GC_HC, GC, HC) | tid;
    	addArcFeature(code, fv);
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePP(GC_MC, GC, MC) | tid;
    	addArcFeature(code | dirFlag, fv);

    	code = createArcCodePP(HC_MC, HC, MC) | tid;
    	addArcFeature(code | dirFlag, fv);
        
        int GL = lemma[gp];
        int HL = lemma[par];
        int ML = lemma[c];

        // LCC
        code = createArcCodeWPPP(pGC_GL_HC_MC, GL, pGC, HC, MC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GL_nGC_HC_MC, GL, nGC, HC, MC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GL_pHC_HC_MC, GL, pHC, HC, MC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GL_HC_nHC_MC, GL, HC, nHC, MC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GL_HC_pMC_MC, GL, HC, pMC, MC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GL_HC_MC_nMC, GL, HC, MC, nMC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        // CLC
        code = createArcCodeWPPP(pGC_GC_HL_MC, HL, pGC, GC, MC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GC_nGC_HL_MC, HL, GC, nGC, MC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GC_pHC_HL_MC, HL, GC, pHC, MC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GC_HL_nHC_MC, HL, GC, nHC, MC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GC_HL_pMC_MC, HL, GC, pMC, MC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GC_HL_MC_nMC, HL, GC, MC, nMC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        // CCL
        code = createArcCodeWPPP(pGC_GC_HC_ML, ML, pGC, GC, HC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GC_nGC_HC_ML, ML, GC, nGC, HC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GC_pHC_HC_ML, ML, GC, pHC, HC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GC_HC_nHC_ML, ML, GC, HC, nHC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GC_HC_pMC_ML, ML, GC, HC, pMC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWPPP(GC_HC_ML_nMC, ML, GC, HC, nMC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWWP(GL_HL_MC, GL, HL, MC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWWP(GL_HC_ML, GL, ML, HC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWWP(GC_HL_ML, HL, ML, GC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        //code = createArcCodeWWW(GL_HL_ML, GL, HL, ML);
        //addCode(TemplateType::TThirdOrder, code, fv);

        code = createArcCodeWP(GL_HC, GL, HC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWP(GC_HL, HL, GC) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWW(GL_HL, GL, HL) | tid;
        addArcFeature(code, fv);
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWP(GL_MC, GL, MC) | tid;
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWP(GC_ML, ML, GC) | tid;
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWW(GL_ML, GL, ML) | tid;
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWP(HL_MC, HL, MC) | tid;
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWP(HC_ML, ML, HC) | tid;
        addArcFeature(code | dirFlag, fv);

        code = createArcCodeWW(HL_ML, HL, ML) | tid;
        addArcFeature(code | dirFlag, fv);
    }

    /************************************************************************
     *  Region end #
     ************************************************************************/
    
    
    /************************************************************************
     * Region start #
     * 
     *  Functions that create 3rd order feature vectors
     *  
     ************************************************************************/

    public FeatureVector createGPSibFeatureVector(DependencyInstance inst, int par, int arg, int prev, int curr) {
    	FeatureVector fv = new FeatureVector(numArcFeats);

    	int[] posA = inst.cpostagids;
        //int[] lemma = inst.lemmaids;
        int[] lemma = inst.lemmaids != null ? inst.lemmaids : inst.formids;
    	
        int flag = par < arg ? 0 : 1;					// bit 1
    	flag = (flag << 1) | (arg < prev ? 0 : 1);		// bit 2
    	flag = (flag << 1) | (arg < curr ? 0 : 1);		// bit 3
    	flag = (flag << 1) | 1;							// bit 4

    	int GC = posA[par];
    	int HC = posA[arg];
    	int SC = posA[prev];
    	int MC = posA[curr];
    	long code = 0;

    	code = createArcCodePPPP(GC_HC_MC_SC, GC, HC, SC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);
    
        int GL = lemma[par];
        int HL = lemma[arg];
        int SL = lemma[prev];
        int ML = lemma[curr];

        code = createArcCodeWPPP(GL_HC_MC_SC, GL, HC, SC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPPP(GC_HL_MC_SC, HL, GC, SC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPPP(GC_HC_ML_SC, ML, GC, HC, SC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPPP(GC_HC_MC_SL, SL, GC, HC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	return fv;
    }

    public FeatureVector createTriSibFeatureVector(DependencyInstance inst, int arg, int prev, int curr, int next) {
    	FeatureVector fv = new FeatureVector(numArcFeats);

    	int[] posA = inst.cpostagids;
    	//int[] lemma = inst.lemmaids;
    	int[] lemma = inst.lemmaids != null ? inst.lemmaids : inst.formids;
    	
    	int flag = arg < prev ? 0 : 1;					// bit 1
    	flag = (flag << 1) | (arg < curr ? 0 : 1);		// bit 2
    	flag = (flag << 1) | (arg < next ? 0 : 1);		// bit 3
    	flag = (flag << 1) | 1;							// bit 4

    	int HC = posA[arg];
    	int PC = posA[prev];
    	int MC = posA[curr];
    	int NC = posA[next];
    	long code = 0;

    	code = createArcCodePPPP(HC_PC_MC_NC, HC, PC, MC, NC);
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);

    	code = createArcCodePPP(HC_PC_NC, HC, PC, NC);
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);

    	code = createArcCodePPP(PC_MC_NC, PC, MC, NC);
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);

    	code = createArcCodePP(PC_NC, PC, NC);
    	addArcFeature(code, fv);
    	addArcFeature(code | flag, fv);
        
        int HL = lemma[arg];
        int PL = lemma[prev];
        int ML = lemma[curr];
        int NL = lemma[next];

        code = createArcCodeWPPP(HL_PC_MC_NC, HL, PC, MC, NC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPPP(HC_PL_MC_NC, PL, HC, MC, NC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPPP(HC_PC_ML_NC, ML, HC, PC, NC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPPP(HC_PC_MC_NL, NL, HC, PC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPP(HL_PC_NC, HL, PC, NC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPP(HC_PL_NC, PL, HC, NC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPP(HC_PC_NL, NL, HC, PC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPP(PL_MC_NC, PL, MC, NC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPP(PC_ML_NC, ML, PC, NC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWPP(PC_MC_NL, NL, PC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWP(PL_NC, PL, NC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

        code = createArcCodeWP(PC_NL, NL, PC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);
            
    	return fv;
    }

    public FeatureVector createGGPCFeatureVector(DependencyInstance inst, int ggp, int gp, int par, int c) {
    	FeatureVector fv = new FeatureVector(numArcFeats);
    	
    	int[] posA = inst.cpostagids;
    	int[] lemma = inst.lemmaids != null ? inst.lemmaids : inst.formids;

    	int flag = ggp < gp ? 0 : 1;				// bit 4
    	flag = (flag << 1) | (gp < par ? 0 : 1);	// bit 3
    	flag = (flag << 1) | (par < c ? 0 : 1);		// bit 2
    	flag = (flag << 1) | 1;						// bit 1

    	int GGC = posA[ggp];
    	int GC = posA[gp];
    	int HC = posA[par];
    	int MC = posA[c];
    	int GGL = lemma[ggp];
    	int GL = lemma[gp];
    	int HL = lemma[par];
    	int ML = lemma[c];

    	long code = 0;

    	code = createArcCodePPPP(GGC_GC_HC_MC, GGC, GC, HC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodeWPPP(GGL_GC_HC_MC, GGL, GC, HC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodeWPPP(GGC_GL_HC_MC, GL, GGC, HC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodeWPPP(GGC_GC_HL_MC, HL, GGC, GC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodeWPPP(GGC_GC_HC_ML, ML, GGC, GC, HC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodePPP(GGC_HC_MC, GGC, HC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodeWPP(GGL_HC_MC, GGL, HC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodeWPP(GGC_HL_MC, HL, GGC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodeWPP(GGC_HC_ML, ML, GGC, HC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodePPP(GGC_GC_MC, GGC, GC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodeWPP(GGL_GC_MC, GGL, GC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodeWPP(GGC_GL_MC, GL, GGC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodeWPP(GGC_GC_ML, ML, GGC, GC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodePP(GGC_MC, GGC, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodeWP(GGL_MC, GGL, MC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodeWP(GGC_ML, ML, GGC);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);

    	code = createArcCodeWW(GGL_ML, GGL, ML);
        addArcFeature(code, fv);
        addArcFeature(code | flag, fv);
        
        return fv;
    }

    public FeatureVector createPSCFeatureVector(DependencyInstance inst, int par, int mod, int ch, int sib) {
    	FeatureVector fv = new FeatureVector(numArcFeats);
    	
    	int[] posA = inst.cpostagids;
    	int[] lemma = inst.lemmaids != null ? inst.lemmaids : inst.formids;

    	int type = (mod - par) * (mod - sib) > 0 ? 0x0 : 0x1;	// 0-1

    	int dir = 0x0;						// 0-2
    	if (mod < par && sib < par)
    		dir = 0x0;
    	else if (mod > par && sib > par)
    		dir = 0x1;
    	else
    		dir = 0x2;
    	
    	dir = (dir << 1) | (mod < ch ? 0x0 : 0x1);	// 0-5
    	dir = (dir << 1) | type;		// 0-11
    	dir += 2;			// 2-13

    	int HC = posA[par];
    	int MC = posA[mod];
    	int CC = posA[ch];
    	int SC = posA[sib];
    	int HL = lemma[par];
    	int ML = lemma[mod];
    	int CL = lemma[ch];
    	int SL = lemma[sib];

    	long code = 0;

    	code = createArcCodePPPP(HC_MC_CC_SC, HC, MC, CC, SC);
        addArcFeature(code | type, fv);
        addArcFeature(code | dir, fv);

    	code = createArcCodeWPPP(HL_MC_CC_SC, HL, MC, CC, SC);
        addArcFeature(code | type, fv);
        addArcFeature(code | dir, fv);

    	code = createArcCodeWPPP(HC_ML_CC_SC, ML, HC, CC, SC);
        addArcFeature(code | type, fv);
        addArcFeature(code | dir, fv);

    	code = createArcCodeWPPP(HC_MC_CL_SC, CL, HC, MC, SC);
        addArcFeature(code | type, fv);
        addArcFeature(code | dir, fv);

    	code = createArcCodeWPPP(HC_MC_CC_SL, SL, HC, MC, CC);
        addArcFeature(code | type, fv);
        addArcFeature(code | dir, fv);

    	code = createArcCodePPP(HC_CC_SC, HC, CC, SC);
        addArcFeature(code | type, fv);
        addArcFeature(code | dir, fv);

    	code = createArcCodeWPP(HL_CC_SC, HL, CC, SC);
        addArcFeature(code | type, fv);
        addArcFeature(code | dir, fv);

    	code = createArcCodeWPP(HC_CL_SC, CL, HC, SC);
        addArcFeature(code | type, fv);
        addArcFeature(code | dir, fv);

    	code = createArcCodeWPP(HC_CC_SL, SL, HC, CC);
        addArcFeature(code | type, fv);
        addArcFeature(code | dir, fv);
    	
    	return fv;
    }


    /************************************************************************
     *  Region end #
     ************************************************************************/
    
    
    /************************************************************************
     * Region start #
     * 
     *  Functions that create global feature vectors
     *  
     ************************************************************************/

    public int findLeftNearestChild(DependencyArcList arclis, int pid, int id) {
    	// find the pid's child which is left closest to id
    	int ret = -1;
    	int st = arclis.startIndex(pid);
    	int en = arclis.endIndex(pid);
    	
    	for (int i = en - 1; i >= st; --i)
    		if (arclis.get(i) < id) {
    			ret = arclis.get(i);
    			break;
    		}
    	return ret;
    }

    public int findRightNearestChild(DependencyArcList arclis, int pid, int id) {
    	// find the pid's child which is right closest to id
    	int ret = -1;
    	int st = arclis.startIndex(pid);
    	int en = arclis.endIndex(pid);

    	for (int i = st; i < en; ++i)
    		if (arclis.get(i) > id) {
    			ret = arclis.get(i);
    			break;
    		}
    	return ret;
    }

    public int[] findConjArg(DependencyArcList arclis, int[] deps, int arg) {
    	// 0: head; 1:left; 2:right
    	int head = -1;
    	int left = -1;
    	int right = -1;

    	if (ccDepType == 0) {
    		// head left arg right
    		//   0   2    1    2
    		right = findRightNearestChild(arclis, arg, arg);
    		left = findLeftNearestChild(arclis, arg, arg);
    		head = deps[arg];
    	} else if (ccDepType == 1) {
    		// head left arg right
    		//   0   1    2    2
    		if (deps[arg] < arg) {
    			left = deps[arg];
    			if (left != -1) {
    				right = findRightNearestChild(arclis, left, arg);
    				head = deps[left];
    			}
    		}
    	} else if (ccDepType == 2) {
    		// head left arg right
    		//   0   1    2    3
    		if (deps[arg] < arg) {
    			left = deps[arg];
    			if (left != -1)
    				head = deps[left];
    			right = findRightNearestChild(arclis, arg, arg);
    		}
    	} else if (ccDepType == 3) {
    		// left arg right head
    		//  3    3   4     0
    		if (deps[arg] > arg && deps[deps[arg]] > deps[arg]) {
    			right = deps[arg];
    			left = findLeftNearestChild(arclis, right, arg);
    			head = deps[right];
    		}

    	} else if (ccDepType == 4) {
    		// head left arg right
    		//  0    1    4    2
    		if (deps[arg] > arg) {
    			right = deps[arg];
    			if (deps[right] < arg) {
    				left = deps[right];
    				if (left != -1) {
    					head = deps[left];
    				}
    			}
    		}
    	} else if (ccDepType == 5) {
    		// left arg right head
    		//  2    3    4    0
    		if (deps[arg] > arg) {
    			right = deps[arg];
    			left = findLeftNearestChild(arclis, arg, arg);
    			if (right != -1)
    				head = deps[right];
    		}
    	} else {
    		Utils.Assert(false);
    	}

    	int[] ret = new int[3];
    	ret[0] = head;
    	ret[1] = left;
    	ret[2] = right;
    	return ret;
    }

    public int[] findPPArg(int[] heads, SpecialPos[] specialPos, DependencyArcList arclis, int arg) {
    	int st = arclis.startIndex(arg);
    	int ed = arclis.endIndex(arg);
    	
    	int c = st == ed ? -1 : arclis.get(st);
    	int c2 = -1;
    	if (c != -1 && specialPos[c] == SpecialPos.C) {
    		if (ccDepType == 0) {
    			// prep is head
    			c2 = findRightNearestChild(arclis, c, c);
    			c = findLeftNearestChild(arclis, c, c);
    		}
    		else if (ccDepType == 1) {
    			// prep is left, should not happen
    			// c = findRightNearestChild(s.child[arg], c);
    		}
    		else if (ccDepType == 2) {
    			c = findRightNearestChild(arclis, arg, c);
    		}
    		// others not valid
    	}
    	int len = 0;
    	int head = heads[arg];
    	if (c != -1 && c != head)
    		len++;
    	if (c2 != -1 && c2 != head)
    		len++;

    	int[] ret = new int[len];
    	len = 0;
    	if (c != -1 && c != head) {
    		ret[len] = c;
    		len++;
    	}
    	if (c2 != -1 && c2 != head) {
    		ret[len] = c2;
    		len++;
    	}
    	return ret;
    }

    public int getMSTRightBranch(SpecialPos[] specialPos, DependencyArcList arclis, int id, int dep) {
    	int node = 1;
    	int st = arclis.startIndex(id);
    	int en = arclis.endIndex(id);
    	
    	if (dep > 10000) {
    		System.out.println("get right branch bug");
    		System.exit(0);
    	}
    	for (int i = en - 1; i >= st; --i) {
    		//if (s.pos[s.child[id][i]].equals("PNX"))
    		if (SpecialPos.PNX == specialPos[arclis.get(i)])
    			continue;
    		node += getMSTRightBranch(specialPos, arclis, arclis.get(i), dep + 1);
    		break;
    	}
    	return node;
    }

    public FeatureVector createPPFeatureVector(DependencyInstance inst, int gp, int par, int c) {
    	FeatureVector fv = new FeatureVector(numArcFeats);

    	int[] posA = inst.cpostagids;
    	int[] lemma = inst.lemmaids != null ? inst.lemmaids : inst.formids;

    	int HC = posA[gp];
    	int MC = posA[c];
    	int HL = lemma[gp];
    	int ML = lemma[c];
    	int PL = lemma[par];

    	long code = 0;

    	code = createArcCodePP(PP_HC_MC, HC, MC);
    	addArcFeature(code, fv);

    	code = createArcCodeWP(PP_HL_MC, HL, MC);
    	addArcFeature(code, fv);

    	code = createArcCodeWP(PP_HC_ML, ML, HC);
    	addArcFeature(code, fv);

    	code = createArcCodeWW(PP_HL_ML, HL, ML);
    	addArcFeature(code, fv);

    	code = createArcCodeWPP(PP_PL_HC_MC, PL, HC, MC);
    	addArcFeature(code, fv);

    	code = createArcCodeWWP(PP_PL_HL_MC, PL, HL, MC);
    	addArcFeature(code, fv);

    	code = createArcCodeWWP(PP_PL_HC_ML, PL, ML, HC);
    	addArcFeature(code, fv);

    	//code = fe->genCodeWWW(HighOrder::PP_PL_HL_ML, HL, ML, PL);
    	//addArcFeature(code, fv);
    	
    	return fv;
    }

    public FeatureVector createCC1FeatureVector(DependencyInstance inst, int left, int arg, int right) {
    	FeatureVector fv = new FeatureVector(numArcFeats);
    	
    	int[] pos = inst.postagids;
    	int[] word = inst.formids;
    	int[] posA = inst.cpostagids;
    	int[][] feats = inst.featids;

    	int CP = pos[arg];
    	int CW = word[arg];
    	int LP = pos[left];
    	int RP = pos[right];
    	int LC = posA[left];
    	int RC = posA[right];

    	long code = 0;

    	code = createArcCodePPP(CC_CP_LP_RP, CP, LP, RP);
    	addArcFeature(code, fv);

    	code = createArcCodePPP(CC_CP_LC_RC, CP, LC, RC);
    	addArcFeature(code, fv);

    	code = createArcCodeWPP(CC_CW_LP_RP, CW, LP, RP);
    	addArcFeature(code, fv);

    	code = createArcCodeWPP(CC_CW_LC_RC, CW, LC, RC);
    	addArcFeature(code, fv);
    	
    	if (feats[left] != null && feats[right] != null) {
        	for (int i = 0; i < feats[left].length; ++i) {
        		if (feats[left][i] <= 0)
        			continue;
        		for (int j = 0; j < feats[right].length; ++j) {
        			if (feats[right][j] <= 0)
        				continue;
        			if (feats[left][i] == feats[right][j]) {
        				code = createArcCodePPP(CC_LC_RC_FID, feats[left][i], LC, RC);
        				addArcFeature(code, fv);
        				break;
        			}
        		}
        	}
    	}
    	
    	return fv;
    }

    public FeatureVector createCC2FeatureVector(DependencyInstance inst, int arg, int head, int child) {
    	FeatureVector fv = new FeatureVector(numArcFeats);

    	int[] pos = inst.postagids;
    	int[] word = inst.formids;
    	int[] posA = inst.cpostagids;
    	int[] lemma = inst.lemmaids != null ? inst.lemmaids : inst.formids;
    	int[][] feats = inst.featids;

    	int CP = pos[arg];
    	int CW = word[arg];
    	int HC = posA[head];
    	int HL = lemma[head];
    	int AC = posA[child];
    	int AL = lemma[child];

    	long code = 0;

    	code = createArcCodePPP(CC_CP_HC_AC, CP, HC, AC);
    	addArcFeature(code, fv);

    	code = createArcCodeWWP(CC_CP_HL_AL, HL, AL, CP);
    	addArcFeature(code, fv);

    	code = createArcCodeWPP(CC_CW_HC_AC, CW, HC, AC);
    	addArcFeature(code, fv);

    	//code = fe->genCodeWWW(HighOrder::CC_CW_HL_AL, CW, HL, AL);
    	//addArcFeature(code, fv);

    	code = createArcCodePP(HP_MP, pos[head], pos[child]);
    	addArcFeature(code, fv);

    	code = createArcCodePP(HP_MP, posA[head], posA[child]);
    	addArcFeature(code, fv);

    	code = createArcCodeWP(HW_MP, lemma[head], pos[child]);
    	addArcFeature(code, fv);

    	code = createArcCodeWP(MW_HP, lemma[child], pos[head]);
    	addArcFeature(code, fv);

    	code = createArcCodeWW(HW_MW, lemma[head], lemma[child]);
    	addArcFeature(code, fv);

    	if (feats[head] != null && feats[child] != null) {
    		for (int fh = 0; fh < feats[head].length; ++fh) {
    			if (feats[head][fh] <= 0)
    				continue;
    			for (int fc = 0; fc < feats[child].length; ++fc) {
    				if (feats[child][fc] <= 0)
    					continue;

    				int IDH = feats[head][fh];
    				int IDM = feats[child][fc];

    				code = createArcCodePP(HP_MP, IDH, IDM);
    				addArcFeature(code, fv);
    			}
    		}
    	}
    	
    	return fv;
    }

    public FeatureVector createPNXFeatureVector(DependencyInstance inst, int head, int arg, int pair) {
    	FeatureVector fv = new FeatureVector(numArcFeats);
    	
    	int[] pos = inst.postagids;
    	int[] word = inst.formids;

    	int flag = (head - arg) * (head - pair) < 0 ? 0 : 1;
    	flag = (flag + 1);

    	long code = 0;

    	code = createArcCodeW(PNX_MW, word[arg]);
    	addArcFeature(code | flag, fv);

    	//code = createArcCodeWP(PNX_HP_MW, pos[head], word[arg]);
	code = createArcCodeWP(PNX_HP_MW, word[arg], pos[head]);
    	addArcFeature(code | flag, fv);
    	
    	return fv;
    }

    public FeatureVector createChildNumFeatureVector(DependencyInstance s, int id, int leftNum, int rightNum) {
    	FeatureVector fv = new FeatureVector(numArcFeats);
    	
    	int childNum = Math.min(GlobalFeatureData.MAX_CHILD_NUM, leftNum + rightNum);
    	int HP = s.postagids[id];
    	int HL = s.lemmaids != null ? s.lemmaids[id] : s.formids[id];
    	
    	long code = 0;

    	code = createArcCodePP(CN_HP_NUM, HP, childNum);
    	addArcFeature(code, fv);

    	code = createArcCodeWP(CN_HL_NUM, HL, childNum);
    	addArcFeature(code, fv);

    	code = createArcCodePPP(CN_HP_LNUM_RNUM, HP, leftNum, rightNum);
    	addArcFeature(code, fv);
    	
    	return fv;
    }

    public FeatureVector createSpanFeatureVector(DependencyInstance s, int id, int end, int punc, int bin) {
    	FeatureVector fv = new FeatureVector(numArcFeats);
    	
    	int HP = s.postagids[id];
    	int HC = s.cpostagids[id];
    	int epFlag = (end << 1) | punc;

    	long code = 0;

    	code = createArcCodePPP(HV_HP, HP, epFlag, bin);
    	addArcFeature(code, fv);

    	code = createArcCodePPP(HV_HC, HC, epFlag, bin);
    	addArcFeature(code, fv);
    	
    	return fv;
    }

    public FeatureVector createNeighborFeatureVector(DependencyInstance s, int par, int id, int left, int right) {
    	FeatureVector fv = new FeatureVector(numArcFeats);
    	
    	int HP = s.postagids[id];
    	int HC = s.cpostagids[id];
    	int HL = s.lemmaids != null ? s.lemmaids[id] : s.formids[id];
    	int GC = s.cpostagids[par];
    	int GL = s.lemmaids != null ? s.lemmaids[par] : s.formids[par];

    	long code = 0;

    	code = createArcCodePPP(NB_HP_LC_RC, HP, left, right);
    	addArcFeature(code, fv);

    	code = createArcCodePPP(NB_HC_LC_RC, HC, left, right);
    	addArcFeature(code, fv);

    	code = createArcCodeWPP(NB_HL_LC_RC, HL, left, right);
    	addArcFeature(code, fv);

    	code = createArcCodePPPP(NB_GC_HC_LC_RC, GC, HC, left, right);
    	addArcFeature(code, fv);

    	code = createArcCodeWPPP(NB_GC_HL_LC_RC, HL, GC, left, right);
    	addArcFeature(code, fv);

    	code = createArcCodeWPPP(NB_GL_HC_LC_RC, GL, HC, left, right);
    	addArcFeature(code, fv);
    	
    	return fv;
    }

    public int findPuncCounterpart(int[] word, int arg) {
    	int quoteID = TOKEN_QUOTE;
    	int lrbID = TOKEN_LRB;
    	int rrbID = TOKEN_RRB;
    	if (word[arg] == quoteID) {
    		boolean left = false;
    		int prev = -1;
    		int curr = -1;
    		for (int i = 1; i < word.length; ++i) {
    			if (word[i] == quoteID) {
    				left = !left;
    				prev = curr;
    				curr = i;
    			}
    			if (i == arg) {
    				break;
    			}
    		}
    		if (left) {
    			// left quote
    			curr = -1;
    			for (int i = arg + 1; i < word.length; ++i) {
    				if (word[i] == quoteID) {
    					curr = i;
    					break;
    				}
    			}
    		} else {
    			// right quote
    			curr = prev;
    		}
    		return curr;
    	} else if (word[arg] == lrbID) {
    		// left bracket
    		int curr = -1;
    		for (int i = arg + 1; i < word.length; ++i) {
    			if (word[i] == rrbID) {
    				curr = i;
    				break;
    			}
    		}
    		return curr;
    	} else if (word[arg] == rrbID) {
    		int curr = -1;
    		for (int i = arg - 1; i >= 0; --i) {
    			if (word[i] == lrbID) {
    				curr = i;
    				break;
    			}
    		}
    		return curr;
    	}

    	return -1;
    }

    public FeatureVector createNonprojFeatureVector(DependencyInstance inst, 
    		int num, int head, int mod) {
    	FeatureVector fv = new FeatureVector(numArcFeats);

    	int[] posA = inst.cpostagids;
    	int[] lemma = inst.lemmaids != null ? inst.lemmaids : inst.formids;

    	int distFlag = getBinnedDistance(head - mod);

    	// use head->mod, rather than small->large
    	int HC = posA[head];
    	int MC = posA[mod];
    	int HL = lemma[head];
    	int ML = lemma[mod];

    	int projFlag = num;		// 0..7

    	long code = 0;

    	code = createArcCodeP(NP, projFlag);
    	addArcFeature(code, fv);
    	addArcFeature(code | distFlag, fv);

    	code = createArcCodePP(NP_MC, projFlag, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | distFlag, fv);

    	code = createArcCodePP(NP_HC, projFlag, HC);
    	addArcFeature(code, fv);
    	addArcFeature(code | distFlag, fv);

    	code = createArcCodeWP(NP_HL, HL, projFlag);
    	addArcFeature(code, fv);
    	addArcFeature(code | distFlag, fv);

    	code = createArcCodeWP(NP_ML, ML, projFlag);
    	addArcFeature(code, fv);
    	addArcFeature(code | distFlag, fv);

    	code = createArcCodePPP(NP_HC_MC, projFlag, HC, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | distFlag, fv);

    	code = createArcCodeWPP(NP_HL_MC, HL, projFlag, MC);
    	addArcFeature(code, fv);
    	addArcFeature(code | distFlag, fv);

    	code = createArcCodeWPP(NP_HC_ML, ML, projFlag, HC);
    	addArcFeature(code, fv);
    	addArcFeature(code | distFlag, fv);

    	code = createArcCodeWWP(NP_HL_ML, HL, ML, projFlag);
    	addArcFeature(code, fv);
    	addArcFeature(code | distFlag, fv);
    	
    	return fv;
    }

    /************************************************************************
     *  Region end #
     ************************************************************************/
   
    /************************************************************************
     * Region start #
     * 
     *  Functions that add feature codes into feature vectors and alphabets
     *  
     ************************************************************************/
    
    private final int hashcode2int(long code)
    {
    	long hash = (code ^ (code&0xffffffff00000000L) >>> 32)*31;
    	int id = (int)((hash < 0 ? -hash : hash) % 115911564);
    	return id;
    }
    
    //private final int hashcode2int(long l) {
    //    long r= l;// 27
    //    l = (l>>13)&0xffffffffffffe000L;
    //    r ^= l;   // 40
    //    l = (l>>11)&0xffffffffffff0000L;
    //    r ^= l;   // 51
    //    l = (l>>9)& 0xfffffffffffc0000L; //53
    //    r ^= l;  // 60
    //    l = (l>>7)& 0xfffffffffff00000L; //62
    //    r ^=l;    //67
    //    int x = ((int)r) % 115911564;
    //
    //    return x >= 0 ? x : -x ; 
    //}
    
    public final void addArcFeature(long code, FeatureVector mat) {
    	long hash = (code ^ (code&0xffffffff00000000L) >>> 32)*31;
    	int id = (int)((hash < 0 ? -hash : hash) % 115911564);
    	//int id = ((hash ^ (hash >> 31)) - (hash >> 31)) % 115911564;
    	mat.addEntry(id, 1.0);
    	if (!stoppedGrowth)
    		featureHashSet.add(code);
    }
    
    public final void addArcFeature(long code, double value, FeatureVector mat) {
    	long hash = (code ^ (code&0xffffffff00000000L) >>> 32)*31;
    	int id = (int)((hash < 0 ? -hash : hash) % 115911564);
    	//int id = ((hash ^ (hash >> 31)) - (hash >> 31)) % 115911564;    	
    	mat.addEntry(id, value);
    	if (!stoppedGrowth)
    		featureHashSet.add(code);
    }
    
    public final void addLabeledArcFeature(long code, FeatureVector mat) {
    	long hash = (code ^ (code&0xffffffff00000000L) >>> 32)*31;
    	int id = (int)((hash < 0 ? -hash : hash) % 115911564);
    	//int id = ((hash ^ (hash >> 31)) - (hash >> 31)) % 115911564;    	
    	mat.addEntry(id, 1.0);
    }
    
    public final void addLabeledArcFeature(long code, double value, FeatureVector mat) {
    	long hash = (code ^ (code&0xffffffff00000000L) >>> 32)*31;
    	int id = (int)((hash < 0 ? -hash : hash) % 115911564);
    	//int id = ((hash ^ (hash >> 31)) - (hash >> 31)) % 115911564;    	
    	mat.addEntry(id, value);
    }
    
    public final void addWordFeature(long code, FeatureVector mat) {
    	int id = wordAlphabet.lookupIndex(code, numWordFeats);
    	if (id >= 0) {
    		mat.addEntry(id, 1.0);
    		if (id == numWordFeats) ++numWordFeats;
    	}
    }
    
    public final void addWordFeature(long code, double value, FeatureVector mat) {
    	int id = wordAlphabet.lookupIndex(code, numWordFeats);
    	if (id >= 0) {
    		mat.addEntry(id, value);
    		if (id == numWordFeats) ++numWordFeats;
    	}
    }
    
    /************************************************************************
     *  Region end #
     ************************************************************************/
    
    
    
    /************************************************************************
     * Region start #
     * 
     *  Functions to create or parse 64-bit feature code
     *  
     *  A feature code is like:
     *  
     *    X1 X2 .. Xk TEMP DIST
     *  
     *  where Xi   is the integer id of a word, pos tag, etc.
     *        TEMP is the integer id of the feature template
     *        DIST is the integer binned length  (4 bits)
     ************************************************************************/
    
    public final int getBinnedDistance(int x) {
    	int flag = 0;
    	int add = 0;
    	if (x < 0) {
    		x = -x;
    		//flag = 8;
    		add = 7;
    	}
    	if (x > 10)          // x > 10
    		flag |= 0x7;
    	else if (x > 5)		 // x = 6 .. 10
    		flag |= 0x6;
    	else
    		flag |= x;   	 // x = 1 .. 5
    	return flag+add;
    }
    
    private final long extractArcTemplateCode(long code) {
    	return (code >> flagBits) & ((1 << numArcFeatBits)-1);
    }
    
    private final long extractDistanceCode(long code) {
    	return code & 15;
    }
    
    private final long extractLabelCode(long code) {
    	return (code >> 4) & ((1 << labelNumBits)-1);
    }
    
    private final void extractArcCodeP(long code, int[] x) {
    	code = (code >> flagBits) >> numArcFeatBits;
	    x[0] = (int) (code & ((1 << tagNumBits)-1));
    }
    
    private final void extractArcCodePP(long code, int[] x) {
    	code = (code >> flagBits) >> numArcFeatBits;
	    x[1] = (int) (code & ((1 << tagNumBits)-1));
	    code = code >> tagNumBits;
	    x[0] = (int) (code & ((1 << tagNumBits)-1));
    }
    
    private final void extractArcCodePPP(long code, int[] x) {
    	code = (code >> flagBits) >> numArcFeatBits;
	    x[2] = (int) (code & ((1 << tagNumBits)-1));
	    code = code >> tagNumBits;
	    x[1] = (int) (code & ((1 << tagNumBits)-1));
	    code = code >> tagNumBits;
	    x[0] = (int) (code & ((1 << tagNumBits)-1));
    }
    
    private final void extractArcCodePPPP(long code, int[] x) {
    	code = (code >> flagBits) >> numArcFeatBits;
	    x[3] = (int) (code & ((1 << tagNumBits)-1));
	    code = code >> tagNumBits;
	    x[2] = (int) (code & ((1 << tagNumBits)-1));
	    code = code >> tagNumBits;
	    x[1] = (int) (code & ((1 << tagNumBits)-1));
	    code = code >> tagNumBits;
	    x[0] = (int) (code & ((1 << tagNumBits)-1));
    }
    
    private final void extractArcCodeW(long code, int[] x) {
    	code = (code >> flagBits) >> numArcFeatBits;
	    x[0] = (int) (code & ((1 << wordNumBits)-1));
    }
    
    private final void extractArcCodeWW(long code, int[] x) {
    	code = (code >> flagBits) >> numArcFeatBits;
	    x[1] = (int) (code & ((1 << wordNumBits)-1));
	    code = code >> wordNumBits;
	    x[0] = (int) (code & ((1 << wordNumBits)-1));
    }
    
    private final void extractArcCodeWP(long code, int[] x) {
    	code = (code >> flagBits) >> numArcFeatBits;
	    x[1] = (int) (code & ((1 << tagNumBits)-1));
	    code = code >> tagNumBits;
	    x[0] = (int) (code & ((1 << wordNumBits)-1));
    }
    
    private final void extractArcCodeWPP(long code, int[] x) {
    	code = (code >> flagBits) >> numArcFeatBits;
	    x[2] = (int) (code & ((1 << tagNumBits)-1));
	    code = code >> tagNumBits;
	    x[1] = (int) (code & ((1 << tagNumBits)-1));
	    code = code >> tagNumBits;
	    x[0] = (int) (code & ((1 << wordNumBits)-1));
    }
    
    private final void extractArcCodeWWPP(long code, int[] x) {
    	code = (code >> flagBits) >> numArcFeatBits;
	    x[3] = (int) (code & ((1 << tagNumBits)-1));
	    code = code >> tagNumBits;
	    x[2] = (int) (code & ((1 << tagNumBits)-1));
	    code = code >> tagNumBits;
	    x[1] = (int) (code & ((1 << wordNumBits)-1));
	    code = code >> wordNumBits;
	    x[0] = (int) (code & ((1 << wordNumBits)-1));
    }
    
    public final long createArcCodeP(FeatureTemplate.Arc temp, long x) {
    	return ((x << numArcFeatBits) | temp.ordinal()) << flagBits;
    }
    
    public final long createArcCodePP(FeatureTemplate.Arc temp, long x, long y) {
    	return ((((x << tagNumBits) | y) << numArcFeatBits) | temp.ordinal()) << flagBits;
    }
    
    public final long createArcCodePPP(FeatureTemplate.Arc temp, long x, long y, long z) {
    	return ((((((x << tagNumBits) | y) << tagNumBits) | z) << numArcFeatBits)
    			| temp.ordinal()) << flagBits;
    }
    
    public final long createArcCodePPPP(FeatureTemplate.Arc temp, long x, long y, long u, long v) {
    	return ((((((((x << tagNumBits) | y) << tagNumBits) | u) << tagNumBits) | v)
    			<< numArcFeatBits) | temp.ordinal()) << flagBits;
    }
    
    public final long createArcCodePPPPP(FeatureTemplate.Arc temp, long x, long y, long u, long v, long w) {
    	return ((((((((((x << tagNumBits) | y) << tagNumBits) | u) << tagNumBits) | v) << tagNumBits) | w)
    			<< numArcFeatBits) | temp.ordinal()) << flagBits;
    }
    
    public final long createArcCodeW(FeatureTemplate.Arc temp, long x) {
    	return ((x << numArcFeatBits) | temp.ordinal()) << flagBits;
    }
    
    public final long createArcCodeWW(FeatureTemplate.Arc temp, long x, long y) {
    	return ((((x << wordNumBits) | y) << numArcFeatBits) | temp.ordinal()) << flagBits;
    }
    
    public final long createArcCodeWP(FeatureTemplate.Arc temp, long x, long y) {
    	return ((((x << tagNumBits) | y) << numArcFeatBits) | temp.ordinal()) << flagBits;
    }
    
    public final long createArcCodeWPP(FeatureTemplate.Arc temp, long x, long y, long z) {
    	return ((((((x << tagNumBits) | y) << tagNumBits) | z) << numArcFeatBits)
    			| temp.ordinal()) << flagBits;
    }
    
    public final long createArcCodeWPPP(FeatureTemplate.Arc temp, long x, long y, long u, long v) {
    	return ((((((((x << tagNumBits) | y) << tagNumBits) | u) << tagNumBits) | v) << numArcFeatBits)
    			| temp.ordinal()) << flagBits;
    }
    
    public final long createArcCodeWWP(FeatureTemplate.Arc temp, long x, long y, long z) {
    	return ((((((x << wordNumBits) | y) << tagNumBits) | z) << numArcFeatBits) | temp.ordinal()) << flagBits;
    }
    
    public final long createArcCodeWWPP(FeatureTemplate.Arc temp, long x, long y, long u, long v) {
    	return ((((((((x << wordNumBits) | y) << tagNumBits) | u) << tagNumBits) | v)
    			<< numArcFeatBits) | temp.ordinal()) << flagBits;
    }
    
    public final long createWordCodeW(FeatureTemplate.Word temp, long x) {//push this X left for the count needed to write the word deatures according to the 
    																	//word feature template, then write in these new bits the word feature #, 
    																	//then move again left with flagbits # which is 4 if no learning 
    	return ((x << numWordFeatBits) | temp.ordinal()) << flagBits;
    }
    public final long createWordVisualCodeW(FeatureTemplate.Word temp, long x) {//push this X left for the count needed to write the word deatures according to the 
		//word feature template, then write in these new bits the word feature #, 
		//then move again left with flagbits # which is 4 if no learning 
    	return ((x << numWordFeatBits) | temp.ordinal()) << flagBits;
    }
    public final long createWordVisualCodeBigram(FeatureTemplate.Word temp, long x, long y) {
    	return ((((x << tagNumBits) | y) << numWordFeatBits) | temp.ordinal()) << flagBits;
    }
    public final long createWordCodeP(FeatureTemplate.Word temp, long x) {//example: move the word index 4 bits, in these 4 write the word feature number,
    																	//move again left digits=flagbits, this is not clear why and what flagbits
    	return ((x << numWordFeatBits) | temp.ordinal()) << flagBits;
    }
    
    public final long createWordCodePP(FeatureTemplate.Word temp, long x, long y) {
    	return ((((x << tagNumBits) | y) << numWordFeatBits) | temp.ordinal()) << flagBits;
    }
    
    public final long createWordCodePPP(FeatureTemplate.Word temp, long x, long y, long z) {
    	return ((((((x << tagNumBits) | y) << tagNumBits) | z) << numWordFeatBits)
    			| temp.ordinal()) << flagBits;
    }
    
    public final long createWordCodeWP(FeatureTemplate.Word temp, long x, long y) {
    	return ((((x << tagNumBits) | y) << numWordFeatBits) | temp.ordinal()) << flagBits;
    }
    
    /************************************************************************
     *  Region end #
     ************************************************************************/
    
    public void fillParameters(LowRankParam tensor, Parameters params) {
        //System.out.println(arcAlphabet.size());	
    	long[] codes = //arcAlphabet.toArray();
    					featureHashSet.toArray();
    	int[] x = new int[4];
    	
    	for (long code : codes) {
    		
    		//int id = arcAlphabet.lookupIndex(code);
    		int id = hashcode2int(code);
    		if (id < 0) continue;
    		
    		int dist = (int) extractDistanceCode(code);
    		int temp = (int) extractArcTemplateCode(code);
    		
    		int label = (int) extractLabelCode(code);
    		if (label != 0) continue;
    		
    		long head = 0, mod = 0;

        	//code = createArcCodePPPP(CORE_POS_PT0, pHeadLeft, pHead, pMod, pModRight);
    		if (temp == HPp_HP_MP_MPn.ordinal()) {
    			extractArcCodePPPP(code, x);
    			head = createWordCodePP(WORDFV_PpP0, x[0], x[1]);
    			mod = createWordCodePP(WORDFV_P0Pn, x[2], x[3]);
    		}
    		
        	//code = createArcCodePPP(CORE_POS_PT1, pHead, pMod, pModRight);
    		else if (temp == HP_MP_MPn.ordinal()) {
    			extractArcCodePPP(code, x);
    			head = createWordCodeP(WORDFV_P0, x[0]);
    			mod = createWordCodePP(WORDFV_P0Pn, x[1], x[2]);
    		}

        	//code = createArcCodePPP(CORE_POS_PT2, pHeadLeft, pHead, pMod);
    		else if (temp == HPp_HP_MP.ordinal()) {
    			extractArcCodePPP(code, x);
    			head = createWordCodePP(WORDFV_PpP0, x[0], x[1]);
    			mod = createWordCodeP(WORDFV_P0, x[2]);
    		}
    		
        	//code = createArcCodePPP(CORE_POS_PT3, pHeadLeft, pMod, pModRight);
    		else if (temp == HPp_MP_MPn.ordinal()) {
    			extractArcCodePPP(code, x);
    			head = createWordCodeP(WORDFV_Pp, x[0]);
    			mod = createWordCodePP(WORDFV_P0Pn, x[1], x[2]);
    		}
    		
        	//code = createArcCodePPP(CORE_POS_PT4, pHeadLeft, pHead, pModRight);
    		else if (temp == HPp_HP_MPn.ordinal()) {
    			extractArcCodePPP(code, x);
    			head = createWordCodePP(WORDFV_PpP0, x[0], x[1]);
    			mod = createWordCodeP(WORDFV_Pn, x[2]);
    		}
        	        	
    		//code = createArcCodePPPP(CORE_POS_APT0, pHead, pHeadRight, pModLeft, pMod);
    		else if (temp == HP_HPn_MPp_MP.ordinal()) {
    			extractArcCodePPPP(code, x);
    			head = createWordCodePP(WORDFV_P0Pn, x[0], x[1]);
    			mod = createWordCodePP(WORDFV_PpP0, x[2], x[3]);
    		}
    		
    		//code = createArcCodePPP(CORE_POS_APT1, pHead, pModLeft, pMod);
    		else if (temp == HP_MPp_MP.ordinal()) {
    			extractArcCodePPP(code, x);
    			head = createWordCodeP(WORDFV_P0, x[0]);
    			mod = createWordCodePP(WORDFV_PpP0, x[1], x[2]);
    		}
    		
    		//code = createArcCodePPP(CORE_POS_APT2, pHead, pHeadRight, pMod);
    		else if (temp == HP_HPn_MP.ordinal()) {
    			extractArcCodePPP(code, x);
    			head = createWordCodePP(WORDFV_P0Pn, x[0], x[1]);
    			mod = createWordCodeP(WORDFV_P0, x[2]);
    		}
    		
    		//code = createArcCodePPP(CORE_POS_APT3, pHeadRight, pModLeft, pMod);
    		else if (temp == HPn_MPp_MP.ordinal()) {
    			extractArcCodePPP(code, x);
    			head = createWordCodeP(WORDFV_Pn, x[0]);
    			mod = createWordCodePP(WORDFV_PpP0, x[1], x[2]);
    		}
    		
    		//code = createArcCodePPP(CORE_POS_APT4, pHead, pHeadRight, pModLeft);
    		else if (temp == HP_HPn_MPp.ordinal()) {
    			extractArcCodePPP(code, x);
    			head = createWordCodePP(WORDFV_P0Pn, x[0], x[1]);
    			mod = createWordCodeP(WORDFV_Pp, x[2]);
    		}

    		//code = createArcCodePPPP(CORE_POS_BPT, pHeadLeft, pHead, pModLeft, pMod);
    		else if (temp == HPp_HP_MPp_MP.ordinal()) {
    			extractArcCodePPPP(code, x);
    			head = createWordCodePP(WORDFV_PpP0, x[0], x[1]);
    			mod = createWordCodePP(WORDFV_PpP0, x[2], x[3]);
    		}
    		
    		//code = createArcCodePPPP(CORE_POS_CPT, pHead, pHeadRight, pMod, pModRight);
    		else if (temp == HP_HPn_MP_MPn.ordinal()) {
    			extractArcCodePPPP(code, x);
    			head = createWordCodePP(WORDFV_P0Pn, x[0], x[1]);
    			mod = createWordCodePP(WORDFV_P0Pn, x[2], x[3]);
    		}
    		
        	//code = createArcCodeWWPP(CORE_BIGRAM_A, head, mod, headP, modP);
    		else if (temp == HW_MW_HP_MP.ordinal()) {
    			extractArcCodeWWPP(code, x);
    			head = createWordCodeWP(WORDFV_W0P0, x[0], x[2]);
    			mod = createWordCodeWP(WORDFV_W0P0, x[1], x[3]);
    		}
        	
        	//code = createArcCodeWPP(CORE_BIGRAM_B, mod, headP, modP);
    		else if (temp == MW_HP_MP.ordinal()) {
    			extractArcCodeWPP(code, x);
    			head = createWordCodeP(WORDFV_P0, x[1]);
    			mod = createWordCodeWP(WORDFV_W0P0, x[0], x[2]);
    		}
        	
        	//code = createArcCodeWPP(CORE_BIGRAM_C, head, headP, modP);
    		else if (temp == HW_HP_MP.ordinal()) {
    			extractArcCodeWPP(code, x);
    			head = createWordCodeWP(WORDFV_W0P0, x[0], x[1]);
    			mod = createWordCodeP(WORDFV_P0, x[2]);
    		}
        	
        	//code = createArcCodeWP(CORE_BIGRAM_D, mod, headP);
    		else if (temp == MW_HP.ordinal()) {
    			extractArcCodeWP(code, x);
    			head = createWordCodeP(WORDFV_P0, x[1]);
    			mod = createWordCodeW(WORDFV_W0, x[0]);
    		}
        	
        	//code = createArcCodeWP(CORE_BIGRAM_E, head, modP);
    		else if (temp == HW_MP.ordinal()) {
    			extractArcCodeWP(code, x);
    			head = createWordCodeW(WORDFV_W0, x[0]);
    			mod = createWordCodeP(WORDFV_P0, x[1]);
    		}
        	
            //code = createArcCodeWW(CORE_BIGRAM_F, head, mod);
    		else if (temp == HW_MW.ordinal()) {
    			extractArcCodeWW(code, x);
    			head = createWordCodeW(WORDFV_W0, x[0]);
    			mod = createWordCodeW(WORDFV_W0, x[1]);
    		}
        	
            //code = createArcCodePP(CORE_BIGRAM_G, headP, modP);
    		else if (temp == HP_MP.ordinal()) {
    			extractArcCodePP(code, x);
    			head = createWordCodeW(WORDFV_P0, x[0]);
    			mod = createWordCodeW(WORDFV_P0, x[1]);
    		}
    		
        	//code = createArcCodeWP(CORE_BIGRAM_H, head, headP);
    		else if (temp == HW_HP.ordinal()) {
    			extractArcCodeWP(code, x);
    			head = createWordCodeWP(WORDFV_W0P0, x[0], x[1]);
    			mod = createWordCodeP(WORDFV_BIAS, 0);
    		}
    		
        	//code = createArcCodeWP(CORE_BIGRAM_K, mod, modP);
    		else if (temp == MW_MP.ordinal()) {
    			extractArcCodeWP(code, x);    			
    			head = createWordCodeP(WORDFV_BIAS, 0);
    			mod = createWordCodeWP(WORDFV_W0P0, x[0], x[1]);
    		}
    		
    		else if (temp == CORE_HEAD_WORD.ordinal()) {
    			extractArcCodeW(code, x);
    			head = createWordCodeW(WORDFV_W0, x[0]);
    			mod = createWordCodeP(WORDFV_BIAS, 0);
    		}
    		
    		else if (temp == CORE_HEAD_POS.ordinal()) {
    			extractArcCodeP(code, x);
    			head = createWordCodeP(WORDFV_P0, x[0]);
    			mod = createWordCodeP(WORDFV_BIAS, 0);	
    		}
    		
    		else if (temp == CORE_MOD_WORD.ordinal()) {
    			extractArcCodeW(code, x);
    			head = createWordCodeP(WORDFV_BIAS, 0);
    			mod = createWordCodeW(WORDFV_W0, x[0]);    			
    		}
    		
    		else if (temp == CORE_MOD_POS.ordinal()) {
    			extractArcCodeP(code, x);
    			head = createWordCodeP(WORDFV_BIAS, 0);	
    			mod = createWordCodeP(WORDFV_P0, x[0]);
    		}
    		
    		else if (temp == CORE_HEAD_pWORD.ordinal()) {
    			extractArcCodeW(code, x);
    			head = createWordCodeW(WORDFV_Wp, x[0]);
    			mod = createWordCodeP(WORDFV_BIAS, 0);
    		}
    		
    		else if (temp == CORE_HEAD_nWORD.ordinal()) {
    			extractArcCodeW(code, x);
    			head = createWordCodeW(WORDFV_Wn, x[0]);
    			mod = createWordCodeP(WORDFV_BIAS, 0);
    		}
    		
    		else if (temp == CORE_MOD_pWORD.ordinal()) {
    			extractArcCodeW(code, x);
    			mod = createWordCodeW(WORDFV_Wp, x[0]);
    			head = createWordCodeP(WORDFV_BIAS, 0);
    		}
    		
    		else if (temp == CORE_MOD_nWORD.ordinal()) {
    			extractArcCodeW(code, x);
    			mod = createWordCodeW(WORDFV_Wn, x[0]);
    			head = createWordCodeP(WORDFV_BIAS, 0);
    		}
    		
    		else if (temp == HEAD_EMB.ordinal()) {
    			extractArcCodeW(code, x);
    			head = createWordCodeW(WORDFV_EMB, x[0]);
    			mod = createWordCodeP(WORDFV_BIAS, 0);
    		}
    		
    		else if (temp == MOD_EMB.ordinal()) {
    			extractArcCodeW(code, x);
    			mod = createWordCodeW(WORDFV_EMB, x[0]);
    			head = createWordCodeP(WORDFV_BIAS, 0);
    		}
    		
    		else {
    			//System.out.println(temp);
    			continue;
    		}
    		
    		int headId = wordAlphabet.lookupIndex(head);
    		int modId = wordAlphabet.lookupIndex(mod);
    		if (headId >= 0 && modId >= 0) {
    			double value = params.params[id];
    			tensor.putEntry(headId, modId, dist, value);
            }
    	}
    	
    }
}
