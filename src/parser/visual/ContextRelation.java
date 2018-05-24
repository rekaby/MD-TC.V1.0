package parser.visual;

import java.util.ArrayList;

public class ContextRelation implements Comparable{

	String verb=""; //action of Agent, Theme, INST and Owner in Own relation 
	String entity="";//object in Agent, Theme, Instrument, and Child (Owned)in owner relation
	ContextRelationType type;
	public String getVerb() {
		return verb;
	}
	public void setVerb(String verb) {
		this.verb = verb;
	}
	public String getEntity() {
		return entity;
	}
	public void setEntity(String object) {
		entity = object;
	}
	public ContextRelationType getType() {
		return type;
	}
	public void setType(ContextRelationType type) {
		this.type = type;
	}
	public ContextRelation(String verb, String object,ContextRelationType type) {
		super();
		this.verb = verb;
		entity = object;
		this.type=type;
	//	System.out.println("New Relation:"+object+ " "+ verb+" "+ type.name());
	}
	/*public ContextRelation(String verb, String object) {
		super();
		this.verb = verb;
		entity = object;
		System.out.println("New Relation:"+object+ " "+ verb);
	}*/
	public boolean hasAction(String[] statement, int index) {//check that the verb which has one or more tokens is same verb there in the statement
		
		String[] lstTokens ;
		int count=0;
		
		try {
			lstTokens=verb.trim().split(" ");
			for (int i = 0,j=index; i < lstTokens.length && j<statement.length; i++, j++) {
				if (lstTokens[i].equalsIgnoreCase(statement[j])) {
					count++;
				}
			}	
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		/*if (count== lstTokens.length) {
			System.out.println("Action Match "+verb +" "+index+" "+type);	
		}*/
	  return count== lstTokens.length? true:false;  
	}
	public boolean hasEntity(String[] statement, int index) {
		
		String[] lstTokens ;
		int count=0;
		
		try {
			lstTokens=entity.trim().split(" ");
			for (int i = 0,j=index; i < lstTokens.length && j<statement.length; i++, j++) {
				if (lstTokens[i].equalsIgnoreCase(statement[j])) {
					count++;
				}
			}	
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		/*if (count== lstTokens.length) {
			System.out.println("Object Match "+entity +" "+index+" "+type);	
		}*/
	  return count== lstTokens.length? true:false;  
	}
	public boolean hasOwner(String[] statement, int index) {
		return hasAction(statement, index);
	}
	public boolean hasOwned(String[] statement, int index) {
		return hasEntity(statement, index);
	}
	
	public boolean equals(Object obj) {                 // (1)
        if (obj == this)                                // (2)
            return true;
        if (!(obj instanceof ContextRelation))            // (3)
            return false;
        ContextRelation vno = (ContextRelation) obj;        // (4)
        if (this.type.ordinal()==ContextRelationType.$NextTo$.ordinal()) {
        	return (vno.verb.equals(this.verb)&&         // (5)
            		vno.entity.equals(this.entity) &&
                   vno.type.ordinal()  == this.type.ordinal())||
                   (vno.entity.equals(this.verb)&&         // (5)
                   		vno.verb.equals(this.entity) &&
                        vno.type.ordinal()  == this.type.ordinal());	
		}
        else
        {
        	return vno.verb.equals(this.verb)&&         // (5)
            		vno.entity.equals(this.entity) &&
                   vno.type.ordinal()  == this.type.ordinal();
        }
        
    }

    public int hashCode() {                             // (6)
        int hashValue = 11;
        hashValue = 31 * hashValue + (verb.hashCode()+entity.hashCode());
        //hashValue = 31 * hashValue + entity.hashCode();
        hashValue = 31 * hashValue + type.ordinal();
        return hashValue;
    }
    
    public int compareTo(Object obj) {                  // (7)
    	ContextRelation vno = (ContextRelation) obj;        // (8)

        // Compare the release numbers.                    (9)
        if (verb.hashCode() < vno.verb.hashCode())
            return -1;
        if (verb.hashCode() > vno.verb.hashCode())
            return 1;

        // Release numbers are equal,                      (10)
        // must compare revision numbers.
        if (entity.hashCode() < vno.entity.hashCode())
            return -1;
        if (entity.hashCode() > vno.entity.hashCode())
            return 1;
        // Release and revision numbers are equal,         (11)
        // must compare patch numbers.
        if (type.ordinal() < vno.type.ordinal())
            return -1;
        if (type.ordinal() > vno.type.ordinal())
            return 1;

        // All fields are equal.                           (12)
        return 0;
    }

}
