package org.halim.ontology.core.segments.manager;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.ontology.core.TransactionContext;
import org.halim.ontology.core.segmentation.command.OntologyHierarchySegmentation;

import java.util.ArrayList;

public class OntologyHierarchyLister implements OntologyHierarchySegmentation {

public static class OntologyHierarchyError extends RuntimeException {
    public OntologyHierarchyError(String s) { super(s); }
}


//    private int parentSlabSize = 4, childrenSlabSize = 6; removed for simplicity

private ArrayList<OntologyHierarchyChangeListener> listeners = new ArrayList<>();
@Override
public void addOntologyHierarchyChangeListener(OntologyHierarchyChangeListener ohcl) { listeners.add(ohcl); }

/** Holds the parent's index, first one is empty. If index value is negative, it points to children arrays. */
private IntArrayList classes = new IntArrayList();
private ArrayList<IntArrayList> multiParents = new ArrayList<IntArrayList>(),
      multiData = new ArrayList<IntArrayList>();

public OntologyHierarchyLister() {
    classes.add(0);
    multiParents.add(null);
    multiData.add(null);
}

/** See {@link OntologyHierarchySegmentation#registerClass}. */
@Override
public void registerClass(int classIdentity) {
    if (classIdentity <= 0) throw new OntologyHierarchyError("Cannot register root or a negative id!");
    while (classes.size() <= classIdentity) {
        int newId = classes.size();
        classes.add(0);              // leaf mode, parent = root, no children yet
        addChildAtomic(0, newId);    // root becomes aware of it
    }
}

private boolean autoTighten = true, isTight = true;

@Override public boolean getAutoTightenParentsPolicy() { return autoTighten; }


@Override
public void setAutoTightenParents(boolean autoTightenRequired) {
    if(autoTightenRequired && !autoTighten) { tightenGraph(); }
    autoTighten |= autoTightenRequired;
}

private void tightenGraph() {
    for (int classIdentity = 0; classIdentity < classes.size(); classIdentity++) {
        if (getParentCount(classIdentity) <= 1) continue;
        IntArrayList currentParents = new IntArrayList(getParents(classIdentity)); // snapshot: removeParent mutates the live list
        IntArrayList toRemove = new IntArrayList();
        for (int cp : currentParents) {
            for (int p : currentParents) {
                if (cp != p && isAncestor(p, cp)) { toRemove.add(cp); break; }
            }
        }
        for (int cp : toRemove) removeParent(classIdentity, cp);
    }
}

@Override
public void addParent(int classIdentity, int parentIdentity) {
    if(classIdentity == parentIdentity) { throw new OntologyHierarchyError("Class cannot be it's parent!"); }
    if(isParent(classIdentity, parentIdentity)) { throw new OntologyHierarchyError("Class is Already Parent!"); }
    if(isHeritage(classIdentity, parentIdentity)) { throw new OntologyHierarchyError("Add Parent Causes Cycle!"); }
    for(OntologyHierarchyChangeListener ohcl : listeners) ohcl.priorClassParentAdded(classIdentity, parentIdentity);
    if(!autoTighten) {
        int parental = classes.getInt(classIdentity);
        if(parental >= 0) {
            IntArrayList children = new IntArrayList();
            multiData.add(children);
            classes.set(classIdentity, 1-multiData.size());
            IntArrayList parents = new IntArrayList();
            multiParents.add(parents);
            children.add(1-multiParents.size());
            parents.add(parental);
            parents.add(parentIdentity);
            
        } else {
            IntArrayList children = multiData.get(-parental);
            parental = children.getInt(0);
            if(parental >= 0) {
                IntArrayList parents = new IntArrayList();
                multiParents.add(parents);
                children.set(0, 1-multiParents.size());
                parents.add(parental);
                parents.add(parentIdentity);
            } else {
                multiParents.get(-parental).add(parentIdentity);
            }
        }
        addChildAtomic(parentIdentity, classIdentity);
        for(OntologyHierarchyChangeListener ohcl : listeners) ohcl.postClassParentAdded(classIdentity, parentIdentity);
        TransactionContext.record(
              () -> removeParent(classIdentity, parentIdentity),
              () -> addParent(classIdentity, parentIdentity)
        );
        return;
    }
    if(isAncestor(classIdentity, parentIdentity)) { throw new OntologyHierarchyError("Already ancestor, cannot be parent!"); }
    IntArrayList currentParents = getParents(classIdentity);
    IntArrayList parentsToRemove = new IntArrayList();
    for(int cp : currentParents) if(isAncestor(parentIdentity, cp)) { parentsToRemove.add(cp); }
    if(currentParents.size() - parentsToRemove.size() == 0) {
        for(int oldParent : currentParents) removeChildAtomic(oldParent, classIdentity);
        clearParentsTo(classIdentity, parentIdentity);
        addChildAtomic(parentIdentity, classIdentity);
        for(OntologyHierarchyChangeListener ohcl : listeners) ohcl.postClassParentAdded(classIdentity, parentIdentity);
        TransactionContext.record(
              () -> removeParent(classIdentity, parentIdentity),
              () -> addParent(classIdentity, parentIdentity)
        );
        return;
    }
    for(int i : parentsToRemove) { removeParent(classIdentity, i); }
    if(currentParents.size() == 1) ensureMultiParents(classIdentity);
    IntArrayList parents = multiParents.get(-multiData.get(-classes.getInt(classIdentity)).getInt(0));
    parents.add(parentIdentity);
    addChildAtomic(parentIdentity, classIdentity);
    for(OntologyHierarchyChangeListener ohcl : listeners) ohcl.postClassParentAdded(classIdentity, parentIdentity);
    TransactionContext.record(
          () -> removeParent(classIdentity, parentIdentity),
          () -> addParent(classIdentity, parentIdentity)
    );
}

private void ensureMultiParents(int classIdentity) {
    int parentIdentity = classes.getInt(classIdentity);
    if(parentIdentity < 0) {
        IntArrayList children = multiData.get(-parentIdentity);
        parentIdentity = children.getInt(0);
        if(parentIdentity < 0) return;
        IntArrayList parents = new IntArrayList();
        multiParents.add(parents);
        children.set(0, 1-multiParents.size());
        parents.add(parentIdentity);
        return;
    }
    IntArrayList children = new IntArrayList();
    multiData.add(children);
    classes.set(classIdentity, 1-multiData.size());
    IntArrayList parents = new IntArrayList();
    multiParents.add(parents);
    children.add(1-multiParents.size());
    parents.add(parentIdentity);
}

private void addChildAtomic(int classIdentity, int childIdentity) {
    int parental = classes.getInt(classIdentity);
    if(parental < 0) {
        multiData.get(-parental).add(childIdentity);
        return;
    }
    IntArrayList children = new IntArrayList();
    children.add(parental);
    children.add(childIdentity);
    multiData.add(children);
    classes.set(classIdentity, 1-multiData.size());
}
private void removeChildAtomic(int classIdentity, int childIdentity) {
    int parental = classes.getInt(classIdentity);
    if(parental >= 0) return;
    IntArrayList children = multiData.get(-parental);
    int index = children.indexOf(childIdentity);
    if(index == -1) return;
    children.removeInt(index);
}

//private void setFirstParentAtomic(int classIdentity, int parentIdentity) {
//    int parental = classes.getInt(classIdentity);
//    if(parental >= 0) {
//        classes.set(classIdentity, parentIdentity);
//        return;
//    }
//    IntArrayList children = multiData.get(-parental);
//    parental = children.getInt(0);
//    if(parental >= 0) {
//        children.set(0, parentIdentity);
//        return;
//    }
//    multiParents.get(-parental).set(0, parentIdentity);
//}

@Override
public void removeParent(int classIdentity, int parentIdentity) {
    if (!isParent(classIdentity, parentIdentity)) return;

    for (OntologyHierarchyChangeListener ohcl : listeners) ohcl.priorClassParentRemoved(classIdentity, parentIdentity);

    if (getParentCount(classIdentity) == 1) {
        removeChildAtomic(parentIdentity, classIdentity);
        clearParentsTo(classIdentity, 0);
        addChildAtomic(0, classIdentity);
    } else {
        removeChildAtomic(parentIdentity, classIdentity);
        int parental = classes.getInt(classIdentity);
        IntArrayList children = multiData.get(-parental);
        IntArrayList parents = multiParents.get(-children.getInt(0));
        parents.rem(parentIdentity); // fastutil: remove by value
    }

    for (OntologyHierarchyChangeListener ohcl : listeners) ohcl.postClassParentRemoved(classIdentity, parentIdentity);
    TransactionContext.record(
          () -> addParent(classIdentity, parentIdentity),
          () -> removeParent(classIdentity, parentIdentity)
    );
}

@Override
public void clearParents(int classIdentity) {
    IntArrayList priorParents = getParents(classIdentity);
    // clearParentsTo only rewires classIdentity's own parent pointer; unlike removeParent's
    // single-parent branch, it does not sever classIdentity from its *current* parents' children
    // lists (its own internal removeChildAtomic call targets the *new* parent, root, not the old
    // one) -- so that has to happen here, for every current parent (there can be more than one).
    for (int oldParent : priorParents) removeChildAtomic(oldParent, classIdentity);
    clearParentsTo(classIdentity, 0);
    addChildAtomic(0, classIdentity);
    TransactionContext.record(
          () -> { for (int p : priorParents) addParent(classIdentity, p); },
          () -> clearParents(classIdentity)
    );
}
/** Doesn't call the {@link OntologyHierarchyLister#addChildAtomic} */
public void clearParentsTo(int classIdentity, int parentIdentity) {
    int parental = classes.getInt(classIdentity);
    if(parental >= 0) {
        removeChildAtomic(parentIdentity, classIdentity);
        classes.set(classIdentity, parentIdentity);
        return;
    }
    IntArrayList children = multiData.get(-parental);
    parental = children.getInt(0);
    if(parental >= 0) {
        children.set(0, parentIdentity);
        return;
    }
    IntArrayList parents = multiParents.get(-parental);

    parents.clear();
    parents.add(parentIdentity);

}

@Override
public int getHeritageCount(int classIdentity) {
    return getHeritage(classIdentity).size();
}

@Override
public int getAncestorCount(int classIdentity) {
    return getAncestors(classIdentity).size();
}

@Override
public IntArrayList getHeritage(int classIdentity) {
    IntArrayList result = new IntArrayList();
    queryEachChildren(classIdentity, pi -> getHeritageRecursion(pi, result));
    return result;
}
private void getHeritageRecursion(int classIdentity, IntArrayList herits) {
    if(herits.contains(classIdentity)) return;
    herits.add(classIdentity);
    queryEachChildren(classIdentity, pi -> getHeritageRecursion(pi, herits));
}

@Override
public IntArrayList getAncestors(int classIdentity) {
    IntArrayList result = new IntArrayList();
    queryEachParent(classIdentity, pi -> getAncestorsRecursion(pi, result));
    return result;
}
private void getAncestorsRecursion(int classIdentity, IntArrayList ancestors) {
    if(ancestors.contains(classIdentity)) return;
    ancestors.add(classIdentity);
    queryEachParent(classIdentity, pi -> getAncestorsRecursion(pi, ancestors));
}

private static class Pointer<E> { public E point; }

@Override
public boolean isAncestor(int classIdentity, int doubtedIdentity) {
    if(classIdentity == doubtedIdentity) return true;
    Pointer<Boolean> value = new Pointer<>();
    value.point = false;
    queryEachParent(classIdentity, pi -> isAncestorRecursion(pi, doubtedIdentity, value));
    return value.point;
}
private void isAncestorRecursion(int classIdentity, int doubtedIdentity, Pointer<Boolean> target) {
    if(target.point) return;
    if(classIdentity == doubtedIdentity) { target.point = true; return; }
    if(classIdentity == 0) return; // root is its own sentinel parent; nothing further to search
    queryEachParent(classIdentity, pi -> isAncestorRecursion(pi, doubtedIdentity, target));
}

@Override
public boolean isHeritage(int classIdentity, int doubtedIdentity) {
    return isAncestor(doubtedIdentity, classIdentity);
}

@Override
public boolean isParent(int classIdentity, int parentIdentity) {
    Pointer<Boolean> value = new Pointer<>();
    value.point = false;
    queryEachParent(classIdentity, pi -> { if (pi == parentIdentity) value.point = true; });
    return value.point;
}

@Override
public int getParentCount(int classIdentity) {
    int parental = classes.getInt(classIdentity);
    if(parental >= 0) return 1;
    parental = multiData.get(-parental).getInt(0);
    if(parental >= 0) return 1;
    return multiParents.get(-parental).size();
}

@Override
public int getChildrenCount(int classIdentity) {
    int parental = classes.getInt(classIdentity);
    if(parental >= 0) return 0;
    return multiData.get(-parental).size()-1;
}

@Override
public IntArrayList getParents(int classIdentity) {
    int parentC = getParentCount(classIdentity);
    IntArrayList result = new IntArrayList();
    if(parentC == 1) {
        int parental = classes.getInt(classIdentity);
        if(parental >= 0) { result.add(parental); return result; }
        parental = multiData.get(-parental).getInt(0);
        if(parental >= 0) { result.add(parental); return result; }
        return new IntArrayList(multiParents.get(-parental));
    }
    int parental = classes.getInt(classIdentity);
    parental = multiData.get(-parental).getInt(0);
    return new IntArrayList(multiParents.get(-parental));
}

@Override
public IntArrayList getChildren(int classIdentity) {
    int childrenCount = getChildrenCount(classIdentity);
    if(childrenCount == 0) return new IntArrayList();
    IntArrayList result = new IntArrayList();
    IntArrayList children = multiData.get(-classes.getInt(classIdentity));
    for(int i = 1; i < children.size(); i++) result.add(children.getInt(i));
    return result;
}

@Override
public void queryEachParent(int classIdentity, OntologyClassQuery cq) {
    int parental = classes.getInt(classIdentity);
    if(parental >= 0) { cq.query(parental); return; }
    parental = multiData.get(-parental).getInt(0);
    if(parental >= 0) { cq.query(parental); return; }
    for(int i : multiParents.get(-parental)) cq.query(i);
}

@Override
public void queryEachChildren(int classIdentity, OntologyClassQuery cq) {
    int parental = classes.getInt(classIdentity);
    if(parental >= 0) return;
    IntArrayList children = multiData.get(-parental);
    for(int i = 1; i < children.size(); i++) cq.query(children.getInt(i));
}



//public class OntologyTreeNode implements TreeNode {
//    ArrayList<OntologyTreeNode> children = new ArrayList<OntologyTreeNode>();
//    OntologyTreeNode parent;
//
//    public OntologyTreeNode(OntologyTreeNode parent) { this.parent = parent; }
//
//    @Override public TreeNode getChildAt(int childIndex) { return children.get(childIndex); }
//    @Override public int getChildCount() { return children.size(); }
//    @Override public TreeNode getParent() { return parent; }
//    @Override public int getIndex(TreeNode node) { return children.indexOf(node); }
//    @Override public boolean getAllowsChildren() { return true; }
//    @Override public boolean isLeaf() { return children.isEmpty(); }
//    @Override
//    public Enumeration<? extends TreeNode> children() {
//        return null;
//    }
//}
//OntologyTreeNode getFullTree() {
//    OntologyTreeNode root = new OntologyTreeNode(null);
//    root.parent = root;
//    int[] children = getChildren(1);
//    for(int c : children) root.children.add(getTree(c, root));
//    return root;
//
//}
//OntologyTreeNode getTree(int index, OntologyTreeNode parent) {
//    OntologyTreeNode root = new OntologyTreeNode(parent);
//    int[] children = getChildren(index);
//    for(int c : children) root.children.add(getTree(c, root));
//    return root;
//}



}
