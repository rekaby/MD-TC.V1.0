package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TestIteration {
	
	public static void main(String[] args) {
		List<List<String>>  possibleContextReferences =new ArrayList<List<String>>();
		List<String> list1 = new ArrayList<String>();
		list1.add("1");
		list1.add("2");
		list1.add("3");
		List<String> list2 = new ArrayList<String>();
		list2.add("3");
		list2.add("4");
		list2.add("5");
		List<String> list3 = new ArrayList<String>();
		list3.add("");
		
		List<String> list4 = new ArrayList<String>();
		list4.add("");
		
		possibleContextReferences.add(list3);
		possibleContextReferences.add(list2);
		possibleContextReferences.add(list1);
		possibleContextReferences.add(list4);
		
		List<List<String>> result=createIteratorsUnique(possibleContextReferences);
		List<List<String>> result2=createIteratorsUnique(possibleContextReferences);
		
		System.out.println("KOKO");
	}
	
	public static List<List<String>> createIteratorsUnique(List<List<String>>  possibleContextReferences) {
		List<List<String>> results=createIterators(possibleContextReferences);
		List<List<String>> finalResults=new ArrayList<List<String>>();
		for (List<String> result : results) {
			boolean duplicate=false;
			for (int i = 0; i < result.size(); i++) {
				String word1=result.get(i);
				for (int j = i+1; j < result.size(); j++) {
					String word2=result.get(j);
					if (!word1.equals("")&&word1.equals(word2)) {
						duplicate=true;
						break;
					}
				}
				if (duplicate) {
					break;
				}
			}
			if (!duplicate) {
				finalResults.add(result);
			}
		}
		return finalResults;
	}
	
	public static List<List<String>> createIterators(List<List<String>>  possibleContextReferences) {
		//List<String> list1 = new ArrayList<String>();
	    //list1.add("1");
	    //list1.add("2");
	    //List<String> list2 = new ArrayList<String>();
	    //list2.add("3");
	    //list2.add("4");
	    //list2.add("5");
	    //List<String> list3 = new ArrayList<String>();
	    //list3.add("");
	    //list3.add("b");
	    //list3.add("c");
	    
	    TempContainer []container = new TempContainer[possibleContextReferences.size()];
	    for (int i = 0; i < container.length; i++) {
	    	container[i]=new TempContainer<Integer>();
	    	container[i].setItems(possibleContextReferences.get(i));
		}
	    
	    List<TempContainer> containers = new ArrayList<TempContainer>(2);
	    for (int i = 0; i < container.length; i++) {
	    	containers.add(container[i]);
		}
	    // Get combinations 
	    List<List<String>> combinations = getCombination(0, containers);
	    return combinations ;
	}
	
	private static List<List<String>> getCombination(int currentIndex, List<TempContainer> containers) {
	    if (currentIndex == containers.size()) {
	        // Skip the items for the last container
	        List<List<String>> combinations = new ArrayList<List<String>>();
	        combinations.add(new ArrayList<String>());
	        return combinations;
	    }
	    List<List<String>> combinations = new ArrayList<List<String>>();
	    TempContainer<String> container = containers.get(currentIndex);
	    List<String> containerItemList = container.getItems();
	    // Get combination from next index
	    List<List<String>> suffixList = getCombination(currentIndex + 1, containers);
	    int size = containerItemList.size();
	    for (int ii = 0; ii < size; ii++) {
	    	String containerItem = containerItemList.get(ii);
	        if (suffixList != null) {
	            for (List<String> suffix : suffixList) {
	                List<String> nextCombination = new ArrayList<String>();
	                nextCombination.add(containerItem);
	                nextCombination.addAll(suffix);
	                combinations.add(nextCombination);
	            }
	        }
	    }
	    return combinations;
	}
}
 
