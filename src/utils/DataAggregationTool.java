package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import parser.DependencyInstance;
import parser.DependencyPipe;
import parser.Options;
import parser.io.Conll06Writer;
import parser.io.DependencyReader;
import parser.io.DependencyWriter;
import parser.visual.ContextInstance;
import parser.visual.io.ContextReader;

public class DataAggregationTool {
	public static String folderName="testdata\\txt_compl11\\";
	public static String outFolderName=folderName+"concatenated20Folds\\";
	
	public static void main(String[] args) {
	/*
		main1("Train1.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14","15","16","17","18","19","21","22","23"      	),".train.txt");
		main1("Train2.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14","15","16","17","18","19","21",          "26","30"),".train.txt");
		main1("Train3.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14","15","16","17","18"          ,"22","23","26","30"),".train.txt");
		main1("Train4.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14","15","16",          "19","21","22","23","26","30"),".train.txt");
		main1("Train5.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14",          "17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train6.txt", Arrays.asList("2","3","6","8","9","10","11","12",          "15","16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train7.txt", Arrays.asList("2","3","6","8","9","10",          "13","14","15","16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train8.txt", Arrays.asList("2","3","6","8",         "11","12","13","14","15","16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train9.txt", Arrays.asList("2","3",        "9","10","11","12","13","14","15","16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train10.txt", Arrays.asList(       "6","8","9","10","11","12","13","14","15","16","17","18","19","21","22","23","26","30"),".train.txt");

		main1("Test1.txt", Arrays.asList("26","30"),".test.txt");
		main1("Test2.txt", Arrays.asList("22","23"),".test.txt");
		main1("Test3.txt", Arrays.asList("19","21"),".test.txt");
		main1("Test4.txt", Arrays.asList("17","18"),".test.txt");
		main1("Test5.txt", Arrays.asList("15","16"),".test.txt");
		main1("Test6.txt", Arrays.asList("13","14"),".test.txt");
		main1("Test7.txt", Arrays.asList("11","12"),".test.txt");
		main1("Test8.txt", Arrays.asList("9","10"),".test.txt");
		main1("Test9.txt", Arrays.asList("6","8"),".test.txt");
		main1("Test10.txt", Arrays.asList("2","3"),".test.txt");
*/
	
		main1("Train1.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14","15","16","17","18","19","21","22","23","26"     ),".train.txt");
		main1("Train2.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14","15","16","17","18","19","21","22","23",     "30"),".train.txt");
		main1("Train3.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14","15","16","17","18","19","21","22",     "26","30"),".train.txt");
		main1("Train4.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14","15","16","17","18","19","21",     "23","26","30"),".train.txt");
		main1("Train5.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14","15","16","17","18","19",     "22","23","26","30"),".train.txt");
		main1("Train6.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14","15","16","17","18",     "21","22","23","26","30"),".train.txt");
		main1("Train7.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14","15","16","17",     "19","21","22","23","26","30"),".train.txt");
		main1("Train8.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14","15","16",     "18","19","21","22","23","26","30"),".train.txt");
		main1("Train9.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14","15",     "17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train10.txt", Arrays.asList("2","3","6","8","9","10","11","12","13","14",     "16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train11.txt", Arrays.asList("2","3","6","8","9","10","11","12","13",     "15","16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train12.txt", Arrays.asList("2","3","6","8","9","10","11","12",     "14","15","16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train13.txt", Arrays.asList("2","3","6","8","9","10","11",     "13","14","15","16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train14.txt", Arrays.asList("2","3","6","8","9","10",     "12","13","14","15","16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train15.txt", Arrays.asList("2","3","6","8","9",     "11","12","13","14","15","16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train16.txt", Arrays.asList("2","3","6","8",    "10","11","12","13","14","15","16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train17.txt", Arrays.asList("2","3","6",    "9","10","11","12","13","14","15","16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train18.txt", Arrays.asList("2","3",    "8","9","10","11","12","13","14","15","16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train19.txt", Arrays.asList("2",    "6","8","9","10","11","12","13","14","15","16","17","18","19","21","22","23","26","30"),".train.txt");
		main1("Train20.txt", Arrays.asList(    "3","6","8","9","10","11","12","13","14","15","16","17","18","19","21","22","23","26","30"),".train.txt");
		
		
		main1("Test1.txt", Arrays.asList("30"),".test.txt");
		main1("Test2.txt", Arrays.asList("26"),".test.txt");
		main1("Test3.txt", Arrays.asList("23"),".test.txt");
		main1("Test4.txt", Arrays.asList("22"),".test.txt");
		main1("Test5.txt", Arrays.asList("21"),".test.txt");
		main1("Test6.txt", Arrays.asList("19"),".test.txt");
		main1("Test7.txt", Arrays.asList("18"),".test.txt");
		main1("Test8.txt", Arrays.asList("17"),".test.txt");
		main1("Test9.txt", Arrays.asList("16"),".test.txt");
		main1("Test10.txt", Arrays.asList("15"),".test.txt");
		main1("Test11.txt", Arrays.asList("14"),".test.txt");
		main1("Test12.txt", Arrays.asList("13"),".test.txt");
		main1("Test13.txt", Arrays.asList("12"),".test.txt");
		main1("Test14.txt", Arrays.asList("11"),".test.txt");
		main1("Test15.txt", Arrays.asList("10"),".test.txt");
		main1("Test16.txt", Arrays.asList("9"),".test.txt");
		main1("Test17.txt", Arrays.asList("8"),".test.txt");
		main1("Test18.txt", Arrays.asList("6"),".test.txt");
		main1("Test19.txt", Arrays.asList("3"),".test.txt");
		main1("Test20.txt", Arrays.asList("2"),".test.txt");
	}
	
	
	public static void main1(String outName, List<String> fileNames,String suffix) {
		  String contextName="_context.txt";
		  
		  String outFile=outFolderName+outName;
		  
		  List<String> trainingFilesList=new ArrayList<String>();//you have to either add training or testing files
			for (String fileName : fileNames) {
				trainingFilesList.add(folderName+fileName+suffix);
			}
		  
		  /*
		  	//trainingFilesList.add("testdata\\txt_compl3\\2.train.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\3.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\6.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\8.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\9.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\10.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\11.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\12.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\13.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\14.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\15.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\16.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\17.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\18.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\19.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\21.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\22.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\23.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\26.train.txt");
			trainingFilesList.add("testdata\\txt_compl3\\30.train.txt");
			*/
			//trainingFilesList.add("testdata\\txt_compl3\\2.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\3.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\6.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\8.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\9.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\10.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\11.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\12.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\13.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\14.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\15.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\16.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\17.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\18.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\19.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\21.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\22.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\23.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\26.test.txt");
			//trainingFilesList.add("testdata\\txt_compl3\\30.test.txt");
			
			
			BufferedReader reader;
			BufferedWriter writer;
			BufferedReader contextReader;
			BufferedWriter contextWriter;
			ArrayList<String> lstLines = new ArrayList<String>();
			ArrayList<String> contextLstLines = new ArrayList<String>();
		 
		 
			try {
				writer = new BufferedWriter(new FileWriter(outFile));
				contextWriter = new BufferedWriter(new FileWriter(outFile+contextName));
				
				for (String trainingFile : trainingFilesList) {
					reader = new BufferedReader(new InputStreamReader(new FileInputStream(trainingFile), "UTF8"));
					contextReader= new BufferedReader(new InputStreamReader(new FileInputStream(trainingFile.replace("train.txt", "train"+contextName).replace("test.txt", "train"+contextName)), "UTF8"));
					
					contextLstLines=readInstance(contextReader);
					lstLines=readInstance(reader);
					
					while (lstLines.size()>0) {
						
						
						writeInstance(lstLines,writer);
						writeInstance(contextLstLines,contextWriter);
						lstLines=readInstance(reader);
					}
					reader.close();
					contextReader.close();
				}	
				writer.close();
				contextWriter.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		 
		 
		 
		 
		    
		    
	}
	private static void writeInstance(ArrayList<String> lstLines,BufferedWriter writer) throws Exception
	{
		for (String string : lstLines) {
			writer.write(string+"\n");
		} 
		writer.write("\n");
	}
	private static ArrayList<String> readInstance(BufferedReader reader) throws Exception
	{
		 ArrayList<String> lstLines = new ArrayList<String>();
		String line = reader.readLine();
		 while (line != null && !line.equals("") && !line.startsWith("*")&& !line.startsWith("\t")) {
		    	lstLines.add(line);
		    	line = reader.readLine();
		    }
		 return lstLines;
	}
	

}
