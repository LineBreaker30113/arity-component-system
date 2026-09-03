package org.halim.ontology.incubation;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.List;


/**
 * For now, not multi thread safe.
 * Doesn't know how the elements are represented.
 * */
public interface IntSet {

int get(int i);
boolean add(int i);
boolean remove(int i);

int size();

IntListerIterator iterator();


interface IntListerIterator {
	boolean hasNext();
	boolean hasPrevious();
	int next();
	int previous();
	void remove();
}


	
	List<Integer> getIntList();
	IntArrayList toList();


}
