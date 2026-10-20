package org.halim.legacy.pd;

import java.util.LinkedList;

/**
 * A specialized LinkedList that automatically evicts the oldest element
 * when the specified capacity is reached.
 */
public class CircularFifoQueue<T> extends LinkedList<T> {
private final int capacity;

public CircularFifoQueue(int capacity) {
	this.capacity = capacity;
}

@Override
public boolean add(T e) {
	if (size() >= capacity) {
		removeFirst();
	}
	return super.add(e);
}

}