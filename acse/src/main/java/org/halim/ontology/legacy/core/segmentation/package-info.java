/***
 * This package is to define all the core responsibilities of the Ontology package
 * so they can be developed and initialized separately while communicating properly.
 * For each responsibility, in order to have high performance and full coverage,
 * functions that already exist in other interfaces must also be declared again.
 * DRY is not high priority as much as architectural clarity or performance.
 * For an example the "int[] getChildren(int classIndex)" function can be used to
 * learn the count of children classes, but creating an array is suboptimal for JVM.
 * Another example would be the fact both "OntologyHierarchy" and "OntologyAnalytics"
 * interfaces declaring the "int getChildrenCount(int classIdentity)" function.
 */
package org.halim.ontology.legacy.core.segmentation;