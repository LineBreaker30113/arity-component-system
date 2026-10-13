package org.halim.ontology.incubation.acse_beta;

public interface ACSE_DescriptionManager {

String getCoreDescription();
void setCoreDescription(String coreDescription);

String getArityDescription(int arityIndex);
void setArityDescription(int arityIndex, String arityDescription);

String getArityCatalogRowDescription(int arityIndex, int rowIndex);
void setArityCatalogRowDescription(int arityIndex, int rowIndex, String arityDescription);

String getStructureDescription(int structureIndex);
void setStructureDescription(int structureIndex, String structureDescription);

String getArityClassDescription(Class<? extends ACSE_ArityComponentBase> arityClass);
void setArityClassDescription(Class<? extends ACSE_ArityComponentBase> arityClass, String arityDescription);

String getArityCatalogRowClassDescription(Class<? extends ACSE_ArityCatalogRowBase> arityClass);
void setArityCatalogRowClassDescription(Class<? extends ACSE_ArityCatalogRowBase> arityClass, String arityDescription);

String getStructureClassDescription(Class<? extends ACSE_StructureComponentBase> arityClass);
void setStructureClassDescription(Class<? extends ACSE_StructureComponentBase> arityClass, String arityDescription);


}
