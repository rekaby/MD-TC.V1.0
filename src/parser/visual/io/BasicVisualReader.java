package parser.visual.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parser.DependencyInstance;
import parser.Options;
import parser.visual.ContextInstance;
import parser.visual.ContextRelation;
import parser.visual.ContextRelationType;
import parser.visual.LocationVerbs;


public class BasicVisualReader extends ContextReader {

	public BasicVisualReader(Options options) {
		this.options = options;
	}
	
	@Override
	public ContextInstance nextInstance() throws IOException {
		
	    ArrayList<String[]> lstLines = new ArrayList<String[]>();
	    ContextInstance contextInstance=new ContextInstance();
	    
	    String line = reader.readLine();
	    line= line!=null?line.trim():line;
	    while (line != null && !line.equals("") && !line.startsWith("*")) {
	    	lstLines.add(line.split("\t"));
	    	line = reader.readLine();
	    }
	    System.out.println("LINE------"+lstLines.size());
	   // if (lstLines.size() == 0) return null;//TODO
	    
	    //int length = lstLines.size();
	    
	    //Object Relation Verb
	    for (int i = 0; i < lstLines.size(); ++i) {
	    	String[] parts = lstLines.get(i);
	    	if (parts.length<3) {//relation should have minimum 3 entities
				continue;
			}
	    	String verb , object;
	    	ContextRelationType relation=null;
	    	ContextRelation contextRelation;
	    	
	    	object = parts[0].trim();
	    	verb = parts[2].trim();
	    	contextRelation=null;// new ContextRelation(verb, object);
	    	for (ContextRelationType type : ContextRelationType.values()) {
	            if (parts[1].trim().equalsIgnoreCase(type.name())) {
	            	//if(type!=ContextRelationType.$own$)
	            	//{contextRelation= new ContextRelation(verb, object,type);}
	            	//else//reverse coz context is written in reverse way for Owner relation
	            	//{
	            	contextRelation= new ContextRelation( verb,object,type);
	            	//}
	            	relation=type;
	            	break;
	            }
	          }
	    	if (relation==null) {
				System.out.println("Debug");
			}
	    	addRelation(contextInstance,contextRelation,relation);
	    	
	    }
	    contextInstance.setNextToRelations(inferLocationsToNexts(contextInstance.getNextToRelations(),contextInstance.getLocationRelations()));
	    
	   
	   // List<ContextRelation>[] results=contextInstance.inferLocationsToAgentsAndThemes(contextInstance.getAgentRelations(),contextInstance.getThemeRelations(),contextInstance.getLocationRelations());
	   // contextInstance.setAgentRelations(results[0]);
	    //contextInstance.setThemeRelations(results[1]);
	    return contextInstance;
	}
	private void addRelation(ContextInstance contextInstance, ContextRelation contextRelation,ContextRelationType relationType)
	{
		switch (relationType) {
		case $Agent$:
			contextInstance.addAgentRelation(contextRelation);
		break;
		case $Theme$:
			contextInstance.addThemeRelation(contextRelation);
			break;
		case $Own$:
			contextInstance.addOwnershipRelation(contextRelation);
			break;
		case $Location$:
			contextInstance.addLocationRelation(contextRelation);
			//contextInstance.addAgentRelation(inferLocationToAgent(contextRelation));
			//contextInstance.addThemeRelation(inferLocationToTheme(contextRelation));
			break;
		case $NextTo$:
			contextInstance.addNextToRelation(contextRelation);
			break;
		case $Property$:
			contextInstance.addPropertyRelation(contextRelation);
			break;

		default:
			System.out.println("------------EXCEPTIOPNNNNNNNN- strange relation from input file------------------------------");
			break;
		} 
			
    	/*
    	if(relationType==ContextRelationType.$Agent$)
    	{
    		contextInstance.addAgentRelation(contextRelation);
    	}
    	if(relationType==ContextRelationType.$Theme$)
    	{
	    	contextInstance.addThemeRelation(contextRelation);
    	}
//    	if(relationType==ContextRelationType.$Instrument$)
//    	{
//	    	contextInstance.addInstrumentRelation(contextRelation);
//    	}
    	if(relationType==ContextRelationType.$Own$)
    	{
	    	contextInstance.addOwnershipRelation(contextRelation);
    	}
    	if(relationType==ContextRelationType.$Location$)
    	{
	    	contextInstance.addLocationRelation(contextRelation);
    	}
    	if(relationType==ContextRelationType.$NextTo$)
    	{
	    	contextInstance.addNextToRelation(contextRelation);
    	}
    	if(relationType==ContextRelationType.$Property$)
    	{
	    	contextInstance.addPropertyRelation(contextRelation);
    	}
    	*/
	}
	private ContextRelation inferLocationToAgentOld(ContextRelation contextRelation)
	{
		return new ContextRelation("lie", contextRelation.getEntity(), ContextRelationType.$Agent$);
	}
	private ContextRelation inferLocationToThemeOld(ContextRelation contextRelation)
	{
		return new ContextRelation("lie", contextRelation.getVerb(), ContextRelationType.$Theme$);
	}
	
	private List<ContextRelation> inferLocationsToNexts(List<ContextRelation> nextToRelations,List<ContextRelation> locationRelations)
	{
		Set<ContextRelation> relations=new HashSet<ContextRelation>();
		relations.addAll(nextToRelations);
		Map<String, Set<String>> locationElementsMap=new HashMap<String, Set<String>>();
		for (ContextRelation contextRelation2 : locationRelations) {//we build a map, key is veb of location relation...value is a set of all elements on this location
			if (contextRelation2.getType()!=ContextRelationType.$Location$) {
				continue;//deal just with locations
			}
			if (!locationElementsMap.keySet().contains(contextRelation2.getVerb())) {
				Set<String> temp=new HashSet<String>();
				temp.add(contextRelation2.getEntity());
				locationElementsMap.put(contextRelation2.getVerb(), temp);
			}
			else
			{
				Set<String> temp= locationElementsMap.get(contextRelation2.getVerb());
				temp.add(contextRelation2.getEntity());
				locationElementsMap.put(contextRelation2.getVerb(), temp);
			}
		}
		
		for (String key : locationElementsMap.keySet()) {
			Object[] values=locationElementsMap.get(key).toArray();
			for (int i = 0; i < values.length; i++) {
				for (int j = i+1; j < values.length; j++) {
					relations.add(new ContextRelation(values[i].toString(), values[j].toString(), ContextRelationType.$NextTo$));
				}
			}
		}
		List<ContextRelation> finalResults=new ArrayList<ContextRelation>();
		finalResults.addAll(relations);
		return finalResults;
	}
	
	

}
