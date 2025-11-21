package com.oliverdroop.chess.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {AiMoveRequestValidator.class})
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AiMoveRequestValidation {

    String message() default "Move history does not result in FEN";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
