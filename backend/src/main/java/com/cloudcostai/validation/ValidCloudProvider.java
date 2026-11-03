package com.cloudcostai.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marker and validation architecture preparation for FinOps DTO models.
 */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCloudProvider {
    String message() default "Invalid cloud provider specified. Supported: AWS, AZURE, GCP";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
