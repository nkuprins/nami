package com.app.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Cross-field rules for a create/update property request:
 * <ul>
 *   <li>at least one complete translation (title + description) is present;</li>
 *   <li>a {@code not_ready} listing has no {@code yearBuilt}.</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = PropertyRequestValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface ValidPropertyRequest {
    String message() default "Invalid property request";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
