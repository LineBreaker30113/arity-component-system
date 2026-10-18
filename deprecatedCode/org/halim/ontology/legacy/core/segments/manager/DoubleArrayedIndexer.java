package org.halim.ontology.legacy.core.segments.manager;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.ontology.legacy.core.TransactionContext;
import org.halim.ontology.legacy.core.segmentation.command.OntologyClassificationSegmentation;

import java.util.ArrayList;
import java.util.BitSet;

public class DoubleArrayedIndexer implements OntologyClassificationSegmentation {

public final ArrayList<BitSet> elements = new ArrayList<>();
public final ArrayList<IntArrayList> classes = new ArrayList<>();

// Stacks for ID reclamation
public final IntArrayList classTombs = new IntArrayList();
public final IntArrayList elementTombs = new IntArrayList();

private final ArrayList<OntologyClassificationChangeListener> listeners = new ArrayList<>();

@Override
public void addOntologyClassificationChangeListener(OntologyClassificationChangeListener occl) { listeners.add(occl); }

// =================================================================================
// 1. PRIVATE ATOMIC HELPERS (The Deterministic Mathematics - NO RECORDING HERE)
// =================================================================================

private void performEnsureLink(int cId, int eId) {
    if (isConnected(cId, eId)) return;
    classes.get(cId).add(eId);
    elements.get(eId).set(cId);
}

private void performBreakLink(int cId, int eId) {
    elements.get(eId).clear(cId);
    classes.get(cId).rem(eId); // FastUtil 'rem' removes first occurrence of value
}

private int performNewClass() {
    if (!classTombs.isEmpty()) {
        int id = classTombs.popInt();
        classes.set(id, new IntArrayList());
        return id;
    }
    classes.add(new IntArrayList());
    return classes.size() - 1;
}

private int performNewElement() {
    if (!elementTombs.isEmpty()) {
        int id = elementTombs.popInt();
        elements.set(id, new BitSet());
        return id;
    }
    elements.add(new BitSet());
    return elements.size() - 1;
}

/**
 * Destroys a class and severs all incoming links.
 * @return The array of severed connections required to reverse this action.
 */
private int[] performDeleteClass(int cId) {
    IntArrayList edges = classes.get(cId);
    int[] preserved = edges != null ? edges.toIntArray() : new int[0];

    if (edges != null) {
        for (int eId : preserved) elements.get(eId).clear(cId);
    }
    classes.set(cId, null);
    classTombs.push(cId);
    return preserved;
}

private int[] performDeleteElement(int eId) {
    BitSet edges = elements.get(eId);
    int[] preserved = edges != null ? edges.stream().toArray() : new int[0];

    if (edges != null) {
        for (int cId : preserved) classes.get(cId).rem(eId);
    }
    elements.set(eId, null);
    elementTombs.push(eId);
    return preserved;
}

/**
 * Deterministic Resurrection. Forces the ID to become active again, bypassing
 * the standard 'newClass' tombstone logic to guarantee graph integrity.
 */
private void performResurrectClass(int cId, int[] priorEdges) {
    classTombs.rem(cId); // Remove specific ID from tombstones
    IntArrayList edgeList = new IntArrayList(priorEdges.length);
    edgeList.addElements(0, priorEdges);
    classes.set(cId, edgeList);

    // Re-establish reverse connections
    for (int eId : priorEdges) elements.get(eId).set(cId);
}

private void performResurrectElement(int eId, int[] priorEdges) {
    elementTombs.rem(eId);
    BitSet bits = new BitSet();
    for (int cId : priorEdges) bits.set(cId);
    elements.set(eId, bits);

    for (int cId : priorEdges) classes.get(cId).add(eId);
}

// =================================================================================
// 2. PUBLIC SINGULAR IMPLEMENTATIONS (Intercepted by Proxy)
// =================================================================================

@Override
public void ensureConnection(int classIdentity, int elementIdentity) {
    if (isConnected(classIdentity, elementIdentity)) return;
    performEnsureLink(classIdentity, elementIdentity);
    TransactionContext.record(
          () -> performBreakLink(classIdentity, elementIdentity),
          () -> performEnsureLink(classIdentity, elementIdentity)
    );
    for (OntologyClassificationChangeListener l : listeners) l.onConnectionMade(classIdentity, elementIdentity);
}

@Override
public void breakConnection(int classIdentity, int elementIdentity) {
    if (!isConnected(classIdentity, elementIdentity)) return;
    performBreakLink(classIdentity, elementIdentity);
    TransactionContext.record(
          () -> performEnsureLink(classIdentity, elementIdentity),
          () -> performBreakLink(classIdentity, elementIdentity)
    );
    for (OntologyClassificationChangeListener l : listeners) l.onConnectionBreak(classIdentity, elementIdentity);
}


@Override
public void deleteClass(int classIdentity) {
    int[] preservedEdges = performDeleteClass(classIdentity);
    TransactionContext.record(
          () -> performResurrectClass(classIdentity, preservedEdges),
          () -> performDeleteClass(classIdentity)
    );
    for (OntologyClassificationChangeListener l : listeners) l.onClassDeleted(classIdentity);
}

@Override
public void deleteElement(int elementIdentity) {
    int[] preservedEdges = performDeleteElement(elementIdentity);
    TransactionContext.record(
          () -> performResurrectElement(elementIdentity, preservedEdges),
          () -> performDeleteElement(elementIdentity)
    );
    for (OntologyClassificationChangeListener l : listeners) l.onElementDeleted(elementIdentity);
}

@Override
public int newClass() {
    int newId = performNewClass();
    TransactionContext.record(
          () -> performDeleteClass(newId),
          () -> performResurrectClass(newId, new int[0]) // Deterministic Redo!
    );
    for (OntologyClassificationChangeListener l : listeners) l.onClassCreated(newId);
    return newId;
}

@Override
public int newElement() {
    int newId = performNewElement();
    TransactionContext.record(
          () -> performDeleteElement(newId),
          () -> performResurrectElement(newId, new int[0])
    );
    for (OntologyClassificationChangeListener l : listeners) l.onElementCreated(newId);
    return newId;
}

@Override
public void registerElement(int elementIdentity) {
    if (elementIdentity < 0) throw new IllegalArgumentException("Cannot register a negative id!");
    while (elements.size() <= elementIdentity) elements.add(new BitSet());
}

@Override
public int getLastElementIndex() { return elements.size() - 1; }

@Override
public boolean isElementTombstoned(int elementIdentity) {
    return elementIdentity < 0 || elementIdentity >= elements.size() || elements.get(elementIdentity) == null;
}

@Override
public int getLastClassIndex() { return classes.size() - 1; }

@Override
public boolean isClassTombstoned(int classIdentity) {
    return classIdentity < 0 || classIdentity >= classes.size() || classes.get(classIdentity) == null;
}

@Override
public void registerClassificationSlot(int classIdentity) {
    if (classIdentity < 0) throw new IllegalArgumentException("Cannot register a negative id!");
    while (classes.size() <= classIdentity) classes.add(new IntArrayList());
}

// =================================================================================
// 3. PUBLIC BULK IMPLEMENTATIONS (Overrides Interface for Max Optimization)
// =================================================================================

@Override
public void deleteClass(int[] classIdentities) {
    int[][] preservedMatrix = new int[classIdentities.length][];

    for (int i = 0; i < classIdentities.length; i++) {
        preservedMatrix[i] = performDeleteClass(classIdentities[i]);
    }

    TransactionContext.record(
          () -> {
              // Undo deletions in reverse order
              for (int i = classIdentities.length - 1; i >= 0; i--) {
                  performResurrectClass(classIdentities[i], preservedMatrix[i]);
              }
          },
          () -> {
              for (int i = 0; i < classIdentities.length; i++) performDeleteClass(classIdentities[i]);
          }
    );
    for (int ci : classIdentities) for (OntologyClassificationChangeListener l : listeners) l.onClassDeleted(ci);
}

@Override
public void deleteElement(int[] elementIdentities) {
    int[][] preservedMatrix = new int[elementIdentities.length][];
    for (int i = 0; i < elementIdentities.length; i++) {
        preservedMatrix[i] = performDeleteElement(elementIdentities[i]);
    }

    TransactionContext.record(
          () -> { for (int i = elementIdentities.length - 1; i >= 0; i--) performResurrectElement(elementIdentities[i], preservedMatrix[i]); },
          () -> { for (int i = 0; i < elementIdentities.length; i++) performDeleteElement(elementIdentities[i]); }
    );
    for (int ei : elementIdentities) for (OntologyClassificationChangeListener l : listeners) l.onElementDeleted(ei);
}

// =================================================================================
// 4. PUBLIC QUERIES (Intercepted by Proxy for Read-Locks only)
// =================================================================================

@Override
public int getClassesCount(int elementIdentity) {
    BitSet bits = elements.get(elementIdentity);
    return bits == null ? 0 : bits.cardinality();
}

@Override
public IntArrayList getClassesOf(int elementIdentity) {
    return new IntArrayList(elements.get(elementIdentity).stream().toArray());
}

@Override
public BitSet getClassesOfAsBitSet(int elementIdentity) {
    return elements.get(elementIdentity);
}

@Override
public ArrayList<IntArrayList> getClassesForElements() {
    ArrayList<IntArrayList> result = new ArrayList<>(elements.size());
    for (BitSet bits : elements) {
        result.add(bits != null ? new IntArrayList(bits.stream().toArray()) : null);
    }
    return result;
}

@Override
public int getElementsCount(int classIdentity) {
    IntArrayList list = classes.get(classIdentity);
    return list == null ? 0 : list.size();
}

@Override
public IntArrayList getElementsOf(int classIdentity) {
    return classes.get(classIdentity);
}

@Override
public ArrayList<IntArrayList> getElementsForClasses() {
    ArrayList<IntArrayList> result = new ArrayList<>(classes.size());
    for (IntArrayList list : classes) {
        result.add(list != null ? new IntArrayList(list) : null);
    }
    return result;
}

@Override
public boolean isConnected (int classIdentity, int elementIdentity) {
    if (classIdentity >= classes.size() || elementIdentity >= elements.size()) return false;
    IntArrayList cList = classes.get(classIdentity);
    BitSet eBits = elements.get(elementIdentity);
    if (cList == null || eBits == null) return false;

    return cList.contains(elementIdentity) && eBits.get(classIdentity);
}


}
