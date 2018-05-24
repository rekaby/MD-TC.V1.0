package parser.visual.io;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import parser.DependencyInstance;
import parser.Options;
import parser.visual.ContextInstance;

public abstract class ContextReader {
	
	BufferedReader reader;
	//boolean isLabeled;
	Options options;
	
	public static ContextReader createContextReader(Options options) {
		//TODO check for the diff types of context then create the proper one
			return new BasicVisualReader(options);
		
	}
	
	public abstract ContextInstance nextInstance() throws IOException;
	//public abstract boolean IsLabeledDependencyFile(String file) throws IOException;
	
	public void startReading(String file) throws IOException {
		//isLabeled = IsLabeledDependencyFile(file);
		reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
		//return isLabeled;
	}
	
	public void close() throws IOException { if (reader != null) reader.close(); }

    
}
