package parser.visual.test;

import parser.DependencyParser;

public class ExpiermentsTest {

	public static void main(String[] args) throws Exception{
		double sum=0;
		int count=5;
		for (int i = 0; i < count; i++) {
			DependencyParser.main1(args);
			args[0]="test";
			sum+=DependencyParser.main1(args);
			args[0]="train";
		}
		System.out.println("Average:"+ sum/count);

	}

}
