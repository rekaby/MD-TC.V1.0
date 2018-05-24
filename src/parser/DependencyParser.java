package parser;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import parser.Options.LearningMode;
import parser.decoding.ChuLiuEdmondDecoder;
import parser.decoding.DependencyDecoder;
import parser.decoding.HillClimbingDecoder;
import parser.io.DependencyReader;
import parser.io.DependencyWriter;
import parser.pruning.BasicArcPruner;
import parser.sampling.RandomWalkSampler;
import parser.visual.ContextConstances;
import parser.visual.ContextRelation;
import parser.visual.LocationVerbs;
import parser.visual.io.ContextReader;
import parser.visual.io.PossibleGapsPoss;
import utils.TestIteration;
import utils.DictionarySet.DictionaryTypes;

public class DependencyParser implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	protected Options options;
	protected DependencyPipe pipe;
	protected Parameters parameters;
	
	DependencyParser pruner;
	
	double pruningGoldHits = 0;
	double pruningTotGold = 1e-30;
	double pruningTotUparcs = 0;
	double pruningTotArcs = 1e-30;
	
	public static void main(String[] args) 
			throws IOException, ClassNotFoundException, CloneNotSupportedException
		{
			main1(args);
		
		}
	public static double main1(String[] args) 
		throws IOException, ClassNotFoundException, CloneNotSupportedException
	{
		
		Options options = Options.getInstance();//new Options();
		options.processArguments(args);		
		
		DependencyParser pruner = null;
		if (options.train && options.pruning && options.learningMode != LearningMode.Basic) {
			Options prunerOptions = Options.getInstance();//new Options();
			prunerOptions.processArguments(args);
			prunerOptions.maxNumIters = 10;
			
			prunerOptions.learningMode = LearningMode.Basic;
			prunerOptions.pruning = false;
			prunerOptions.test = false;
			prunerOptions.learnLabel = false;
			prunerOptions.gamma = 1.0;
			prunerOptions.gammaLabel = 1.0;
			prunerOptions.R = 0;
			
			//pruner = new DependencyParser();
			pruner = new BasicArcPruner();
			pruner.options = prunerOptions;
			
			DependencyPipe pipe = new DependencyPipe(prunerOptions);
			pruner.pipe = pipe;
			
			pipe.createAlphabets(prunerOptions.trainFile);
			DependencyInstance[] lstTrain = pipe.createInstances(prunerOptions.trainFile);
			
			Parameters parameters = new Parameters(pipe, prunerOptions);
			pruner.parameters = parameters;
			
			pruner.train(lstTrain);
		}
		
		if (options.train) {
			DependencyParser parser = new DependencyParser();
			parser.options = options;
			options.printOptions();
			
			DependencyPipe pipe = new DependencyPipe(options);
			parser.pipe = pipe;
			
			if (options.pruning) parser.pruner = pruner;
			
			pipe.createAlphabets(options.trainFile);
			DependencyInstance[] lstTrain = pipe.createInstances(options.trainFile);
			
			Parameters parameters = new Parameters(pipe, options);
			parser.parameters = parameters;
			
			parser.train(lstTrain);
			if (options.dev && options.learningMode != LearningMode.Basic) 
				parser.tuneSpeed();
			parser.saveModel();
		}
		
		if (options.test) {
			DependencyParser parser = new DependencyParser();
			parser.options = options;			
			
			parser.loadModel();
			parser.options.processArguments(args);
			if (!options.train) parser.options.printOptions(); 
			if (options.dev && parser.options.learningMode != LearningMode.Basic) {
				parser.tuneSpeed();
				parser.saveModel();
			}
			
			System.out.printf(" Evaluating: %s%n", options.testFile);
			
			return parser.evaluateSet(true, false);
		}
		return 0.0;
	}
	
	public void tuneSpeed() throws IOException, CloneNotSupportedException
	{
		if (options.numTestConverge < 10) return;
		System.out.println(" Tuning hill-climbing converge number on eval set...");
		double maxUAS = evaluateWithConvergeNum(options.numTrainConverge);
		System.out.printf("\tconverge=%d\tUAS=%f%n", options.numTrainConverge, maxUAS);
		int max = options.numTrainConverge / 5;
		int min = 2;
		while (min < max) {
			int mid = (min+max)/2;
			double uas = evaluateWithConvergeNum(mid*5);
			System.out.printf("\tconverge=%d\tUAS=%f%n", mid*5, uas);
			if (uas + 0.0005 <= maxUAS)
				min = mid+1;
			else
				max = mid;
		}
		options.numTestConverge = min * 5;
		options.dev = false;	// set dev=false because already tuned
		System.out.printf(" final converge=%d%n%n", options.numTestConverge);
	}
	
    public void saveModel() throws IOException 
    {
    	ObjectOutputStream out = new ObjectOutputStream(
    			new GZIPOutputStream(new FileOutputStream(options.modelFile)));
    	out.writeObject(pipe);
    	out.writeObject(parameters);
    	out.writeObject(options);
    	if (options.pruning && options.learningMode != LearningMode.Basic) 
    		out.writeObject(pruner);
    	out.close();
    }
	
    public void loadModel() throws IOException, ClassNotFoundException 
    {
        ObjectInputStream in = new ObjectInputStream(
                new GZIPInputStream(new FileInputStream(options.modelFile)));    
        pipe = (DependencyPipe) in.readObject();
        parameters = (Parameters) in.readObject();
        options = (Options) in.readObject();
        if (options.pruning && options.learningMode != LearningMode.Basic)
        	//pruner = (DependencyParser) in.readObject();
        	pruner = (BasicArcPruner) in.readObject();
        pipe.options = options;
        parameters.options = options;        
        in.close();
        pipe.closeAlphabets();
    }
    
	public void printPruningStats()
	{
		System.out.printf("  Pruning Recall: %.4f\tEffcy: %.4f%n",
				pruningGoldHits / pruningTotGold,
				pruningTotUparcs / pruningTotArcs);
	}
	
	public double pruningRecall()
	{
		return pruningGoldHits / pruningTotGold;
	}
	
	public void resetPruningStats()
	{
		pruningGoldHits = 0;
		pruningTotGold = 1e-30;
		pruningTotUparcs = 0;
		pruningTotArcs = 1e-30;
	}
	
    public void train(DependencyInstance[] lstTrain) 
    	throws IOException, CloneNotSupportedException 
    {
    	long start = 0, end = 0;
    	
        if (options.R > 0 && options.gamma < 1 && options.initTensorWithPretrain) {

        	Options optionsBak = (Options) options.clone();
        	options.learningMode = LearningMode.Basic;
        	options.R = 0;
        	options.gamma = 1.0;
        	options.gammaLabel = 1.0;
        	options.maxNumIters = options.numPretrainIters;
            options.useHO = false;
        	parameters.gamma = 1.0;
        	parameters.gammaLabel = 1.0;
        	parameters.rank = 0;
    		System.out.println("=============================================");
    		System.out.printf(" Pre-training:%n");
    		System.out.println("=============================================");
    		
    		start = System.currentTimeMillis();

    		System.out.println("Running MIRA ... ");
    		trainIter(lstTrain, false);
    		System.out.println();
    		
    		System.out.println("Init tensor ... ");
    		LowRankParam tensor = new LowRankParam(parameters);
    		pipe.synFactory.fillParameters(tensor, parameters);
    		tensor.decompose(1, parameters);
            System.out.println();
    		end = System.currentTimeMillis();
    		
    		options.learningMode = optionsBak.learningMode;
    		options.R = optionsBak.R;
    		options.gamma = optionsBak.gamma;
    		options.gammaLabel = optionsBak.gammaLabel;
    		options.maxNumIters = optionsBak.maxNumIters;
            options.useHO = optionsBak.useHO;
    		parameters.rank = optionsBak.R;
    		parameters.gamma = optionsBak.gamma;
    		parameters.gammaLabel = optionsBak.gammaLabel;
    		parameters.clearTheta();
            parameters.printUStat();
            parameters.printVStat();
            parameters.printWStat();
            System.out.println();
            System.out.printf("Pre-training took %d ms.%n", end-start);    		
    		System.out.println("=============================================");
    		System.out.println();	    

        } else {
        	parameters.randomlyInitUVW();
        }
        
		System.out.println("=============================================");
		System.out.printf(" Training:%n");
		System.out.println("=============================================");
		
		start = System.currentTimeMillis();

		System.out.println("Running MIRA ... ");
		trainIter(lstTrain, true);
		System.out.println();
		
		end = System.currentTimeMillis();
		
		System.out.printf("Training took %d ms.%n", end-start);    		
		System.out.println("=============================================");
		System.out.println();		    	
    }
    
    public void trainIter(DependencyInstance[] lstTrain, boolean evalAndSave) throws IOException
    {

    	DependencyDecoder decoder = DependencyDecoder
    			.createDependencyDecoder(options);
    	
    	int N = lstTrain.length;
    	int printPeriod = 10000 < N ? N/10 : 1000;
    	System.out.println(" Dependency Instance Length"+lstTrain.length);
    	System.out.println(" Option Max Iter"+options.maxNumIters);
    	for (int iIter = 0; iIter < options.maxNumIters; ++iIter) {
    		System.out.println("______________________________");
    		System.out.println("Iteration : "+iIter);
        	
    		if (pruner != null) pruner.resetPruningStats();
    		
            // use this offset to change the udpate ordering of U, V and W
            // when N is a multiple of 3, such that U, V and W get updated
            // on each sentence.
            int offset = (N % 3 == 0) ? iIter : 0;

    		long start = 0;
    		double loss = 0;
    		int uas = 0, tot = 0;
    		start = System.currentTimeMillis();
                		    		
    		for (int i = 0; i < N; ++i) {
    			//System.out.println("++++++++++++++++++++++++++");
        		//System.out.println("Instance : "+i);
    			if ((i + 1) % printPeriod == 0) {
				System.out.printf("  %d (time=%ds)", (i+1),
					(System.currentTimeMillis()-start)/1000);
    			}

    			//DependencyInstance inst = new DependencyInstance(lstTrain[i]);
    			DependencyInstance inst = lstTrain[i];
    			
        		//System.out.println("Create new Local Feature Data");
    			
        		LocalFeatureData lfd = new LocalFeatureData(inst, this, true);
        		
        		//for (int j = 0;  j < lfd.wordFvs.length; j++) {
        		//	System.out.println("lfd "+i+" :"+lfd.wordFvs[j].size());	
				//}
        		        		
    		    GlobalFeatureData gfd = new GlobalFeatureData(lfd);
    		    
    		    int n = inst.length;
    		    
    		    DependencyInstance predInst = decoder.decode(inst, lfd, gfd, true);

        		int ua = evaluateUnlabelCorrect(inst, predInst), la = 0;
    		
        		uas += ua;
        		tot += n-1;        		
        		if (ua != n-1) {
        			loss += parameters.update(inst, predInst, lfd, gfd,
        					iIter * N + i + 1, offset);
                }
        		
        		if (options.learnLabel) {
        			predInst.heads = inst.heads;
        			lfd.predictLabels(predInst.heads, predInst.deplbids, true);
        			la = evaluateLabelCorrect(inst, predInst);
        			if (la != n-1) {
        				loss += parameters.updateLabel(inst, predInst, lfd, gfd,
        						iIter * N + i + 1, offset);
        			}
        		}

    		}
    		System.out.printf("%n  Iter %d\tloss=%.4f\tuas=%.4f\t[%ds]%n", iIter+1,
    				loss, uas/(tot+0.0),
    				(System.currentTimeMillis() - start)/1000);
    		
    		if (options.learningMode != LearningMode.Basic && options.pruning && pruner != null)
    			pruner.printPruningStats();
    		
    		// evaluate on a development set
    		if (evalAndSave && options.test && ((iIter+1) % 1 == 0 || iIter+1 == options.maxNumIters)) {		
    			System.out.println();
	  			System.out.println("_____________________________________________");
	  			System.out.println();
	  			System.out.printf(" Evaluation: %s%n", options.testFile);
	  			System.out.println(); 
                if (options.average) 
                	parameters.averageParameters((iIter+1)*N);
                int cnvg = options.numTestConverge;
                options.numTestConverge = options.numTrainConverge;
	  			double res = evaluateSet(false, false);
                options.numTestConverge = cnvg;
                System.out.println();
	  			System.out.println("_____________________________________________");
	  			System.out.println();
                if (options.average) 
                	parameters.unaverageParameters();
    		} 
    	}
    	
    	if (evalAndSave && options.average) {
            parameters.averageParameters(options.maxNumIters * N);
    	}

        decoder.shutdown();
    }
    
    public int evaluateUnlabelCorrect(DependencyInstance act, DependencyInstance pred) 
    {
    	int nCorrect = 0;
    	for (int i = 1, N = act.length; i < N; ++i) {
    		if (act.heads[i] == pred.heads[i])
    			++nCorrect;
    	}    		
    	return nCorrect;
    }
    
    public int evaluateLabelCorrect(DependencyInstance act, DependencyInstance pred) 
    {
    	int nCorrect = 0;
    	for (int i = 1, N = act.length; i < N; ++i) {
    		if (act.heads[i] == pred.heads[i] && act.deplbids[i] == pred.deplbids[i])
    			++nCorrect;
    	}    		  		
    	return nCorrect;
    }
    
    public static int evaluateUnlabelCorrect(DependencyInstance inst, 
    		DependencyInstance pred, boolean evalWithPunc) 
    {
    	int nCorrect = 0;    	
    	for (int i = 1, N = inst.length; i < N; ++i) {

            if (!evalWithPunc)
            	if (inst.forms[i].matches("[-!\"#%&'()*,./:;?@\\[\\]_{}ã€]+")) continue;

    		if (inst.heads[i] == pred.heads[i]) ++nCorrect;
    	}    		
    	return nCorrect;
    }
    
    public static int evaluateLabelCorrect(DependencyInstance inst, 
    		DependencyInstance pred, boolean evalWithPunc) 
    {
    	int nCorrect = 0;    	
    	for (int i = 1, N = inst.length; i < N; ++i) {

            if (!evalWithPunc)
            	if (inst.forms[i].matches("[-!\"#%&'()*,./:;?@\\[\\]_{}ã€]+")) continue;

    		if (inst.heads[i] == pred.heads[i] && inst.deplbids[i] == pred.deplbids[i]) ++nCorrect;
    	}    		
    	return nCorrect;
    }
    
    public double evaluateSet(boolean output, boolean evalWithPunc)throws IOException {
    	if (pruner != null) pruner.resetPruningStats();
    	double sentencesCount=0;
    	double correctWord=0;
    	double correctReference=0;
    	double correctPos=0;
    	double correctCPos=0;
    	
    	double nonGapAllReferences=0;
    	double nonGapReferences=0;
    	double completeSentenceCorrectReference=0;
    	
    	DependencyReader reader = DependencyReader.createDependencyReader(options);
    	reader.startReading(options.testFile);
    	
    	ContextReader contextReader = ContextReader.createContextReader(options);
		if(options.context && options.contextFile!=null)
		{
			contextReader.startReading(options.contextFile);
		}


    	DependencyWriter writer = null;
    	if (output && options.outFile != null) {
    		writer = DependencyWriter.createDependencyWriter(options, pipe);
    		writer.startWriting(options.outFile);
    	}
    	
    	DependencyDecoder decoder = DependencyDecoder.createDependencyDecoder(options);   	
    	
    	Evaluator eval = new Evaluator(options, pipe);
    	
		long start = System.currentTimeMillis();
    	int instanceId=0;
    	DependencyInstance inst = pipe.createInstance(reader);   
    	//TODO here we have to fill gap and send to evaluate
    	DependencyInstance OriginalInst = null;//inst.clone(inst); //new DependencyInstance(inst);
    	while (inst != null) {
    		inst.setTrain(false);
    		instanceId++;
    		if (instanceId==8) {
				//System.out.println("DEBUG");
			}
    		List<List<String>> possibleCompleteContext =null;
    		if(options.context && options.contextFile!=null)
			{
				inst.contextInstance=contextReader.nextInstance();
				inst.enrichDependencyInstanceContext();
				inst.definePossibleContextReferences();
				
				possibleCompleteContext =TestIteration.createIteratorsUnique(inst.possibleContextReferences);
				if (possibleCompleteContext==null || possibleCompleteContext.size()==0) {//means two words not including the gaps are already redundant, so just ignore the uniqueness in this case
					possibleCompleteContext =TestIteration.createIterators(inst.possibleContextReferences);
				}
				//System.out.println("KOKO");
			}
    		OriginalInst =inst.clone(inst);
    		
    		//List<String> posList=getPossFromDictionary(pipe.dictionaries.get(DictionaryTypes.POS).toArray());
    		int gapId=inst.getGapId();
    		double bestscore=Double.NEGATIVE_INFINITY;
    		String bestReference="";
    		String bestPOSReference="";
    		DependencyInstance bestTree=null;
    		//for (Iterator iterator = inst.possibleContextReferences.get(gapId).iterator(); iterator.hasNext();) {
    		//String filler = (String) iterator.next();
    		for (Iterator iterator = possibleCompleteContext.iterator(); iterator.hasNext();) {
    			List<String> fillers=(List<String>)iterator.next();
    			String filler = fillers.get(gapId);
	    		for (int i = 0; i < inst.length; i++) {
					inst.contextReferences[i]=fillers.get(i);
				}
	    		inst.contextInstance.filterContextRelations(inst.isTrain(), Arrays.asList( inst.contextReferences));//to filter the relations
    	    	for (int i = 0; i < PossibleGapsPoss.values().length; i++) {
					String posFiller=(String) PossibleGapsPoss.values()[i].toString();
	    			if (ContextConstances.DEBUG) {
	    				System.out.println(Arrays.toString(fillers.toArray()) + " "+posFiller);	
					}
					
	    			double tempScore=0;
					inst.forms[gapId]=filler.substring(0,filler.indexOf(ContextConstances.REFERENCE_SEPARATOR)!=-1?
							filler.indexOf(ContextConstances.REFERENCE_SEPARATOR):filler.length());
					inst.lemmas[gapId]=filler.substring(0,filler.indexOf(ContextConstances.REFERENCE_SEPARATOR)!=-1?
							filler.indexOf(ContextConstances.REFERENCE_SEPARATOR):filler.length());
					inst.postags[gapId]=posFiller;
					inst.cpostags[gapId]=posFiller.substring(0, 2);//just first two characters of POS
			//		inst.contextReferences[gapId]=filler;
					
					inst=pipe.setInstanceIds(inst);
					LocalFeatureData lfd = new LocalFeatureData(inst, this, true);
		    		GlobalFeatureData gfd = new GlobalFeatureData(lfd); 
		            DependencyInstance predInst = decoder.decode(inst, lfd, gfd, false);
		            //System.out.println(filler+"-"+posFiller);
		            if (decoder  instanceof ChuLiuEdmondDecoder) {
		            	ChuLiuEdmondDecoder newDecoder = (ChuLiuEdmondDecoder) decoder;
		            	tempScore=newDecoder.calcScore(predInst,lfd);
		            	//System.out.println(newDecoder.calcScore(predInst,lfd));
						//System.out.println("----------");
		            }
		            if (decoder  instanceof HillClimbingDecoder) {
		            	HillClimbingDecoder newDecoder = (HillClimbingDecoder) decoder;
		            	tempScore=newDecoder.calcScore(predInst);
						//System.out.println(newDecoder.calcScore(predInst));
						//System.out.println("----------");
		            }
		            if (ContextConstances.DEBUG) {
		            	System.out.println("Tree: "+Arrays.toString(predInst.heads));
	    				System.out.println("Score: "+tempScore+ "   Best Sofar:"+bestscore);	
					}
		            if (options.learnLabel)
		            	lfd.predictLabels(predInst.heads, predInst.deplbids, false);
		            
		            if (tempScore>bestscore) {
						bestscore=tempScore;
						bestReference=filler;
						bestPOSReference=posFiller;
						bestTree=predInst.clone(predInst);
					}
		            
		       }
				
    	    }
    		 eval.add(OriginalInst, bestTree, evalWithPunc);
    		if (writer != null) {
    			
    			writer.writeInstance(bestTree);
    		}
    		
    		System.out.println("BEST: "+bestReference+"-"+bestPOSReference+"="+bestscore);
    		sentencesCount++;
    		List correctList= Arrays.asList( OriginalInst.references[gapId].split(ContextConstances.MULTI_ANSWER_SPLITTER));
    		for (Object correctAnswer : correctList) {
    			if (((String)correctAnswer).equalsIgnoreCase(bestReference)) {
    				correctReference++;
    				break;
    			}
			}
    		
    		if (bestPOSReference.equalsIgnoreCase(OriginalInst.postags[gapId])) {
				correctPos++;
			}
    		if (bestPOSReference.startsWith(OriginalInst.cpostags[gapId])) {
				correctCPos++;
			}
    		for (Object correctAnswer : correctList) {
    			if (((String)correctAnswer).substring(0,((String)correctAnswer).indexOf(ContextConstances.REFERENCE_SEPARATOR)!=-1?
    					((String)correctAnswer).indexOf(ContextConstances.REFERENCE_SEPARATOR):((String)correctAnswer).length())
        				.equalsIgnoreCase(bestReference.substring(0,bestReference.indexOf(ContextConstances.REFERENCE_SEPARATOR)!=-1?
        						bestReference.indexOf(ContextConstances.REFERENCE_SEPARATOR):bestReference.length()))) {
    				correctWord++;
    			}
    		}
    		
    		
    		
    		
    		//Complete sentence statistics////////////////////////////
    		for (int i = 0; i < OriginalInst.length; i++) {
    			if (i==gapId) {
					continue;
				}
    			if (!OriginalInst.references[i].trim().equals("")) {
					
    				nonGapAllReferences++;
    			
    			if ( OriginalInst.references[i].trim().equalsIgnoreCase(bestTree.contextReferences[i].trim())) {
    					//completeSentenceCorrectReference++;
    				nonGapReferences++;
    				}
    			}
    		}
    		
    		boolean completeSentenceIdentification=true;
    		for (int i = 0; i < OriginalInst.length; i++) {
    			//if (i==gapId) {
				//	continue;
				//}
    			if (!OriginalInst.references[i].trim().equals("")) {
					
    				//completeSentenceAllReferences++;
    			if (i==gapId && !(Arrays.asList( OriginalInst.references[i].split(ContextConstances.MULTI_ANSWER_SPLITTER)).contains(bestTree.contextReferences[i].trim()))) {
    				completeSentenceIdentification=false;
        				break;
        		}	
    			if (i!=gapId && !OriginalInst.references[i].trim().equalsIgnoreCase(bestTree.contextReferences[i].trim())) {
    					//completeSentenceCorrectReference++;
    				completeSentenceIdentification=false;
    				break;
    				}
    			}
    		}
    		if (completeSentenceIdentification) {
    			completeSentenceCorrectReference++;
			}
    		//////////////////////////////
    		
    		
    		
    		inst = pipe.createInstance(reader);
    		//OriginalInst = inst.clone(inst); //new DependencyInstance(inst);
    		System.out.println("------------------");
        	System.out.println("DEBUGGap Filler Word Accuracy:\t"+correctWord+"/"+sentencesCount);
        	System.out.println("DEBUGGap Filler Reference Accuracy:\t"+correctReference+"/"+sentencesCount);
        	System.out.println("DEBUGGap Filler POS Accuracy:\t"+correctPos+"/"+sentencesCount);
        	System.out.println("DEBUGGap Filler CPOS Accuracy:\t"+correctCPos+"/"+sentencesCount);
        	System.out.println("DEBUGGap COMPLETE sentence Reference Accuracy:\t"+completeSentenceCorrectReference+"/"+sentencesCount);
        	System.out.println("DEBUGGap NonGap Reference Accuracy:\t"+nonGapReferences+"/"+nonGapAllReferences);
        	
    	}
    	
    	reader.close();
    	if (writer != null) writer.close();
    	
    	System.out.printf("  Tokens: %d%n", eval.tot);
    	System.out.printf("  Sentences: %d%n", eval.nsents);
    	System.out.printf("  UAS=%.6f\tLAS=%.6f\tCAS=%.6f\t[%.2fs]%n",
    			eval.UAS(), eval.LAS(), eval.CAS(),
    			(System.currentTimeMillis() - start)/1000.0);
    	if (options.pruning && options.learningMode != LearningMode.Basic && pruner != null) {
    		pruner.printPruningStats();
    		if (pruner.pruningRecall() < 0.99) {
    			System.out.printf("%nWARNING: Pruning recall is less than 99%%!%n"
    					+"Current pruning-weight=%.2f. Consider using a smaller value between (0,1)%n%n",
    					options.pruningCoeff);
    		}
    	}
    	System.out.println("------------------");
    	System.out.println("Gap Filler Word Accuracy:\t"+correctWord/sentencesCount);
    	System.out.println("Gap Filler Reference Accuracy:\t"+correctReference/sentencesCount);
    	System.out.println("Gap Filler POS Accuracy:\t"+correctPos/sentencesCount);
    	System.out.println("Gap Filler CPOS Accuracy:\t"+correctCPos/sentencesCount);
    	System.out.println("COMPLETE sentence Reference Accuracy:\t"+completeSentenceCorrectReference/sentencesCount);

    	decoder.shutdown();

        return eval.UAS();
    }
    
    private List<String> getPossFromDictionary(Object[] dictValues)
    {
    	List<String> values=new ArrayList<String>();
    	for (Object dictValue : dictValues) {
    		String dictValueStr=(String)dictValue;
			if (dictValueStr.startsWith("pos=")) {
				values.add(dictValueStr.substring(dictValueStr.indexOf("=")+1));
			}
		}
    	return values;
    	
    }
    public double evaluateWithConvergeNum(int converge) throws IOException, CloneNotSupportedException 
    {
    	
    	if (pruner != null) pruner.resetPruningStats();
    	
    	Options options = (Options) this.options.clone();
    	options.numTestConverge = converge;
    	DependencyReader reader = DependencyReader.createDependencyReader(options);
    	reader.startReading(options.testFile);

    	ContextReader contextReader = ContextReader.createContextReader(options);
		if(options.context && options.contextFile!=null)
		{
			contextReader.startReading(options.contextFile);
		}

    	DependencyDecoder decoder = DependencyDecoder.createDependencyDecoder(options);   	
    	
    	Evaluator eval = new Evaluator(options, pipe);
    	
    	DependencyInstance inst = pipe.createInstance(reader);    	
    	while (inst != null) {
    		inst.setTrain(false);
    		if(options.context && options.contextFile!=null)
			{
				inst.contextInstance=contextReader.nextInstance();
				inst.enrichDependencyInstanceContext();
				inst.definePossibleContextReferences();
				inst.contextInstance.filterContextRelations(inst.isTrain(), Arrays.asList( inst.contextReferences));
			}
    		LocalFeatureData lfd = new LocalFeatureData(inst, this, true);
    		GlobalFeatureData gfd = new GlobalFeatureData(lfd); 
    		
            DependencyInstance predInst = decoder.decode(inst, lfd, gfd, false);
            
            eval.add(inst, predInst, false);
    		
    		inst = pipe.createInstance(reader);
    	}
    	
    	reader.close();
    	
        decoder.shutdown();

        return eval.UAS();
    }
    
}
