package org.halim.ontology.legacy.core.segmentation.helper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@BypassCrossCuttingConcerns({BypassCrossCuttingConcerns.CrossCuttingConcern.LOCKING_R})
public @interface OntologyTransaction {
    String action();
}