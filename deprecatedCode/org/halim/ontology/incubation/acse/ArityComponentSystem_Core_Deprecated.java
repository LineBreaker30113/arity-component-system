package org.halim.ontology.incubation.acse;

public interface ArityComponentSystem_Core_Deprecated {
	
	
	void delegateRemoval(int arity, int instance);
	
	/** Should return the description (also capabilities) of the system. */
	String getDescription();
	

/** should explain what that arity represents. */
String getArityDescription(int index);

/** should explain what that arity represents. */
void setArityDescription(int index, String description);

/** should explain what that arity represents. */
String getStructureDescription(int index);

/** should explain what that arity represents. */
void setStructureDescription(int index, String description);



}
