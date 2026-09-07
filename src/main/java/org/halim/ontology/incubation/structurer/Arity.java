package org.halim.ontology.incubation.structurer;

import org.halim.ontology.incubation.structurer.catalogs.LongIntCatalog;

import java.util.ArrayList;




public class Arity {

LongIntCatalog occupied;

public ArrayList<LongIntCatalog> catalogs;

public String[] catalogNames;

public Object getCatalog(String name) {
	for (int i = 0; i < catalogNames.length; i++) if(name == catalogNames[i]) return catalogs.get(i);
	return null;
}

}
