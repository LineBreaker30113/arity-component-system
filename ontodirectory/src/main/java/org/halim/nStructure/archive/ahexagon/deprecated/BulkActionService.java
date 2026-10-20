package org.halim.nStructure.archive.ahexagon.deprecated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BulkActionService {
    enum ActionBulkingStrategy {
        ARRAY,
        ONE_ARRAY,
        ARRAY_ONE,
        A_A_ZIP,
        A_A_Cartesian
    }
    ActionBulkingStrategy value();
}