package parser.visual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContextInstance {

	List<ContextRelation> agentRelations= new ArrayList<ContextRelation>();
	List<ContextRelation> inferedAgentRelations= new ArrayList<ContextRelation>();
	List<ContextRelation> themeRelations= new ArrayList<ContextRelation>();
	List<ContextRelation> inferedThemeRelations= new ArrayList<ContextRelation>();
	List<ContextRelation> instrumentRelations= new ArrayList<ContextRelation>();
	List<ContextRelation> ownershipRelations= new ArrayList<ContextRelation>();
	List<ContextRelation> locationRelations= new ArrayList<ContextRelation>();
	List<ContextRelation> nextToRelations= new ArrayList<ContextRelation>();
	List<ContextRelation> propertyRelations= new ArrayList<ContextRelation>();
	
	List<ContextRelation> filteredAgentRelations= new ArrayList<ContextRelation>();
	List<ContextRelation> filteredThemeRelations= new ArrayList<ContextRelation>();
	List<ContextRelation> filteredInferedAgentRelations= new ArrayList<ContextRelation>();
	List<ContextRelation> filteredInferedThemeRelations= new ArrayList<ContextRelation>();
	
//	public void addFilteredAgentRelation(ContextRelation relation)
//	{
//		filteredAgentRelations.add(relation);
//	}
	public void addFilteredThemeRelation(ContextRelation relation)
	{
		filteredThemeRelations.add(relation);
	}
	public void addFilterInferedAgentRelation(ContextRelation relation)
	{
		filteredInferedAgentRelations.add(relation);
	}
	public void addFilterInferedThemeRelation(ContextRelation relation)
	{
		filteredInferedThemeRelations.add(relation);
	}
	
	public void addAgentRelation(ContextRelation relation)
	{
		agentRelations.add(relation);
	}
	public void addThemeRelation(ContextRelation relation)
	{
		themeRelations.add(relation);
	}
	public void addInstrumentRelation(ContextRelation relation)
	{
		instrumentRelations.add(relation);
	}
	public void addOwnershipRelation(ContextRelation relation)
	{
		ownershipRelations.add(relation);
	}
	public void addLocationRelation(ContextRelation relation)
	{
		locationRelations.add(relation);
	}
	public void addNextToRelation(ContextRelation relation)
	{
		nextToRelations.add(relation);
	}
	public void addNextToRelations(List<ContextRelation> relations)
	{
		nextToRelations.addAll(relations);
	}
	public void addPropertyRelation(ContextRelation relation)
	{
		propertyRelations.add(relation);
	}
	public List<ContextRelation> getAgentRelations() {
		return agentRelations;
	}
	public void setAgentRelations(List<ContextRelation> agentRelations) {
		this.agentRelations = agentRelations;
	}
	public List<ContextRelation> getThemeRelations() {
		return themeRelations;
	}
	public void setThemeRelations(List<ContextRelation> themeRelations) {
		this.themeRelations = themeRelations;
	}
	public List<ContextRelation> getInstrumentRelations() {
		return instrumentRelations;
	}
	public void setInstrumentRelations(List<ContextRelation> instrumentRelations) {
		this.instrumentRelations = instrumentRelations;
	}
	public List<ContextRelation> getOwnershipRelations() {
		return ownershipRelations;
	}
	public void setOwnershipRelations(List<ContextRelation> ownershipRelations) {
		this.ownershipRelations = ownershipRelations;
	}
	
	public List<ContextRelation> getLocationRelations() {
		return locationRelations;
	}
	public void setLocationRelations(List<ContextRelation> locationRelations) {
		this.locationRelations = locationRelations;
	}
	public List<ContextRelation> getNextToRelations() {
		return nextToRelations;
	}
	public void setNextToRelations(List<ContextRelation> nextToRelations) {
		this.nextToRelations = nextToRelations;
	}
	public List<ContextRelation> getPropertyRelations() {
		return propertyRelations;
	}
	public void setPropertyRelations(List<ContextRelation> propertyRelations) {
		this.propertyRelations = propertyRelations;
	}
	
	public List<ContextRelation> getInferedAgentRelations() {
		return inferedAgentRelations;
	}
	public void setInferedAgentRelations(List<ContextRelation> inferedAgentRelations) {
		this.inferedAgentRelations = inferedAgentRelations;
	}
	public List<ContextRelation> getInferedThemeRelations() {
		return inferedThemeRelations;
	}
	public void setInferedThemeRelations(List<ContextRelation> inferedThemeRelations) {
		this.inferedThemeRelations = inferedThemeRelations;
	}
	
	public List<ContextRelation> getFilteredAgentRelations() {
		return filteredAgentRelations;
	}
	public void setFilteredAgentRelations(
			List<ContextRelation> filteredAgentRelations) {
		this.filteredAgentRelations = filteredAgentRelations;
	}
	public List<ContextRelation> getFilteredThemeRelations() {
		return filteredThemeRelations;
	}
	public void setFilteredThemeRelations(
			List<ContextRelation> filteredThemeRelations) {
		this.filteredThemeRelations = filteredThemeRelations;
	}
	public List<ContextRelation> getFilteredInferedAgentRelations() {
		return filteredInferedAgentRelations;
	}
	public void setFilteredInferedAgentRelations(
			List<ContextRelation> filteredInferedAgentRelations) {
		this.filteredInferedAgentRelations = filteredInferedAgentRelations;
	}
	public List<ContextRelation> getFilteredInferedThemeRelations() {
		return filteredInferedThemeRelations;
	}
	public void setFilteredInferedThemeRelations(
			List<ContextRelation> filteredInferedThemeRelations) {
		this.filteredInferedThemeRelations = filteredInferedThemeRelations;
	}
	public List<ContextRelation> getAllRelations() {
		List<ContextRelation> allRelations=new ArrayList<ContextRelation>();
		
		allRelations.addAll(agentRelations);
		allRelations.addAll(inferedAgentRelations);
		allRelations.addAll(themeRelations);
		allRelations.addAll(inferedThemeRelations);
		allRelations.addAll(instrumentRelations);
		allRelations.addAll(ownershipRelations);
		allRelations.addAll(locationRelations);
		allRelations.addAll(nextToRelations);
		allRelations.addAll(propertyRelations);
		
		return allRelations;
	}
	public void initializeContextRelations()
	{
		this.filteredAgentRelations= this.agentRelations;
		this.filteredThemeRelations= this.themeRelations;
		this.filteredInferedAgentRelations= this.inferedAgentRelations;
		this.filteredInferedThemeRelations= this.inferedThemeRelations;
	}
	public void filterContextRelations(boolean train, List<String> contextReferences)
	{
		this.filteredAgentRelations= new ArrayList<ContextRelation>();
		this.filteredThemeRelations= new ArrayList<ContextRelation>();
		this.filteredInferedAgentRelations= new ArrayList<ContextRelation>();
		this.filteredInferedThemeRelations= new ArrayList<ContextRelation>();
		if (train || !ContextConstances.FILTER_AGENT_THEME) {
			initializeContextRelations();
		}
		else{
			for (ContextRelation contextRelation1 : this.agentRelations) {
				if (contextReferences.contains(contextRelation1.getVerb())&& contextReferences.contains(contextRelation1.getEntity())) {
					for (ContextRelation contextRelation2 : this.themeRelations) {
						if (contextReferences.contains(contextRelation2.getVerb())&& contextReferences.contains(contextRelation2.getEntity()) 
								&& contextRelation1.getVerb().equals(contextRelation2.getVerb())) {
							this.filteredAgentRelations.add(contextRelation1);
							break;
						}
					}		
				}
			}
			for (ContextRelation contextRelation1 : this.themeRelations) {
				if (contextReferences.contains(contextRelation1.getVerb())&& contextReferences.contains(contextRelation1.getEntity())) {
					for (ContextRelation contextRelation2 : this.agentRelations) {
						if (contextReferences.contains(contextRelation2.getVerb())&& contextReferences.contains(contextRelation2.getEntity())
								&& contextRelation1.getVerb().equals(contextRelation2.getVerb())) {
							this.filteredThemeRelations.add(contextRelation1);
							break;
						}
					}		
				}
			}
			
			for (ContextRelation contextRelation1 : this.inferedAgentRelations) {
				if (contextReferences.contains(contextRelation1.getVerb())&& contextReferences.contains(contextRelation1.getEntity())) {
					for (ContextRelation contextRelation2 : this.inferedThemeRelations) {
						if (contextReferences.contains(contextRelation2.getVerb())&& contextReferences.contains(contextRelation2.getEntity())
								&& contextRelation1.getVerb().equals(contextRelation2.getVerb())) {
							this.filteredInferedAgentRelations.add(contextRelation1);
							break;
						}
					}		
				}
			}
			for (ContextRelation contextRelation1 : this.inferedThemeRelations) {
				if (contextReferences.contains(contextRelation1.getVerb())&& contextReferences.contains(contextRelation1.getEntity())) {
					for (ContextRelation contextRelation2 : this.inferedAgentRelations) {
						if (contextReferences.contains(contextRelation2.getVerb())&& contextReferences.contains(contextRelation2.getEntity())
								&& contextRelation1.getVerb().equals(contextRelation2.getVerb())) {
							this.filteredInferedThemeRelations.add(contextRelation1);
							break;
						}
					}		
				}
			}
		}
	}
	public List<ContextRelation>[] inferLocationsToAgentsAndThemes(List<ContextRelation> agentRelations,List<ContextRelation> themeRelations,List<ContextRelation> locationRelations)
	{
		Set<ContextRelation> agentRelationsSet=new HashSet<ContextRelation>();
		Set<ContextRelation> themeRelationsSet=new HashSet<ContextRelation>();
		
		agentRelationsSet.addAll(agentRelations);
		themeRelationsSet.addAll(themeRelations);
		Map<String, Set<String>> locationElementsMap=new HashMap<String, Set<String>>();
		for (ContextRelation contextRelation : locationRelations) {//we build a map, key is veb of location relation...value is a set of all elements on this location
			if (contextRelation.getType()!=ContextRelationType.$Location$) {
				continue;//deal just with locations
			}
			if (!locationElementsMap.keySet().contains(contextRelation.getVerb())) {
				Set<String> temp=new HashSet<String>();
				temp.add(contextRelation.getEntity());
				locationElementsMap.put(contextRelation.getVerb(), temp);
			}
			else
			{
				Set<String> temp= locationElementsMap.get(contextRelation.getVerb());
				temp.add(contextRelation.getEntity());
				locationElementsMap.put(contextRelation.getVerb(), temp);
			}
		}
		int senseId=0;
		for (String key : locationElementsMap.keySet()) {
			senseId++;
			Object[] values=locationElementsMap.get(key).toArray();
			for (int i = 0; i < values.length; i++) {
				for (int t = 0; t < LocationVerbs.values().length; t++) {
					String verb=LocationVerbs.values()[t]+"_"+senseId;
					agentRelationsSet.add(new ContextRelation(verb, values[i].toString(), ContextRelationType.$Agent$));
					themeRelationsSet.add(new ContextRelation(verb, key, ContextRelationType.$Theme$));
				}
			}
		}
		List<ContextRelation>[] finalResults=(ArrayList<ContextRelation>[])new ArrayList[2];//new ArrayList<ContextRelation>();
		finalResults[0]=new ArrayList<ContextRelation>();
		finalResults[1]=new ArrayList<ContextRelation>();
		finalResults[0].addAll(agentRelationsSet);
		finalResults[1].addAll(themeRelationsSet);
		return finalResults;
	}
	
	public List<ContextRelation>[] inferLocationsToAgentsAndThemes(List<ContextRelation> locationRelations)
	{
		Set<ContextRelation> inferedAgentRelationsSet=new HashSet<ContextRelation>();
		Set<ContextRelation> inferedThemeRelationsSet=new HashSet<ContextRelation>();
		
		Map<String, Set<String>> locationElementsMap=new HashMap<String, Set<String>>();
		for (ContextRelation contextRelation : locationRelations) {//we build a map, key is veb of location relation...value is a set of all elements on this location
			if (contextRelation.getType()!=ContextRelationType.$Location$) {
				continue;//deal just with locations
			}
			if (!locationElementsMap.keySet().contains(contextRelation.getVerb())) {
				Set<String> temp=new HashSet<String>();
				temp.add(contextRelation.getEntity());
				locationElementsMap.put(contextRelation.getVerb(), temp);
			}
			else
			{
				Set<String> temp= locationElementsMap.get(contextRelation.getVerb());
				temp.add(contextRelation.getEntity());
				locationElementsMap.put(contextRelation.getVerb(), temp);
			}
		}
		int senseId=10;
		for (String key : locationElementsMap.keySet()) {
			senseId++;
			Object[] values=locationElementsMap.get(key).toArray();
			for (int i = 0; i < values.length; i++) {
				for (int t = 0; t < LocationVerbs.values().length; t++) {
					String verb=LocationVerbs.values()[t]+"_"+senseId;
					inferedAgentRelationsSet.add(new ContextRelation(verb, values[i].toString(), ContextRelationType.$InferAgent$));
					inferedThemeRelationsSet.add(new ContextRelation(verb, key, ContextRelationType.$InferTheme$));
				}
			}
		}
		List<ContextRelation>[] finalResults=(ArrayList<ContextRelation>[])new ArrayList[2];//new ArrayList<ContextRelation>();
		finalResults[0]=new ArrayList<ContextRelation>();
		finalResults[1]=new ArrayList<ContextRelation>();
		finalResults[0].addAll(inferedAgentRelationsSet);
		finalResults[1].addAll(inferedThemeRelationsSet);
		return finalResults;
	}
}
