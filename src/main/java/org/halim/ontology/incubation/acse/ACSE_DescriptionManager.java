package org.halim.ontology.incubation.acse;

import org.halim.ontology.incubation.acse.arity.ACS_ArityComponentBase;
import org.halim.ontology.incubation.acse.arity.catalog.ACS_ArityCatalogRowBase;
import org.halim.ontology.incubation.acse.structure.ACS_StructureComponentBase;


public interface ACSE_DescriptionManager {

String getCoreDescription();
void setCoreDescription(String coreDescription);

String getArityDescription(int arityIndex);
void setArityDescription(int arityIndex, String arityDescription);

String getArityCatalogRowDescription(int arityIndex, int rowIndex);
void setArityCatalogRowDescription(int arityIndex, int rowIndex, String arityDescription);

String getStructureDescription(int structureIndex);
void setStructureDescription(int structureIndex, String structureDescription);

String getArityClassDescription(Class<? extends ACS_ArityComponentBase> arityClass);
void setArityClassDescription(Class<? extends ACS_ArityComponentBase> arityClass, String arityDescription);

String getArityCatalogRowClassDescription(Class<? extends ACS_ArityCatalogRowBase> arityClass);
void setArityCatalogRowClassDescription(Class<? extends ACS_ArityCatalogRowBase> arityClass, String arityDescription);

String getStructureClassDescription(Class<? extends ACS_StructureComponentBase> arityClass);
void setStructureClassDescription(Class<? extends ACS_StructureComponentBase> arityClass, String arityDescription);


}
