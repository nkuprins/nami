package com.app.backend.validation;

import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.request.PropertyRequest;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

public class PropertyRequestValidator implements ConstraintValidator<ValidPropertyRequest, PropertyRequest> {

    @Override
    public boolean isValid(PropertyRequest req, ConstraintValidatorContext ctx) {
        ctx.disableDefaultConstraintViolation();
        boolean valid = true;

        boolean anyComplete = req.translations() != null && req.translations().values().stream()
                .anyMatch(PropertyRequestValidator::isComplete);
        if (!anyComplete) {
            ctx.buildConstraintViolationWithTemplate(
                            "At least one complete translation (title + description) is required")
                    .addPropertyNode("translations").addConstraintViolation();
            valid = false;
        }

        Short yearBuilt = req.details() != null ? req.details().yearBuilt() : null;
        if (req.completion() == PropertyCompletion.NOT_READY && yearBuilt != null) {
            ctx.buildConstraintViolationWithTemplate(
                            "A 'not_ready' new project cannot have a year_built")
                    .addPropertyNode("details").addPropertyNode("yearBuilt").addConstraintViolation();
            valid = false;
        }

        if (req.completion() != null && req.type() != ListingType.NEW_PROJECT) {
            ctx.buildConstraintViolationWithTemplate("completion is only valid for a new_project listing")
                    .addPropertyNode("completion").addConstraintViolation();
            valid = false;
        }

        BigDecimal landM2 = req.details() != null ? req.details().landM2() : null;
        if (landM2 != null && req.propertyKind() == PropertyCategory.APARTMENT) {
            ctx.buildConstraintViolationWithTemplate("land_m2 is not valid for an apartment")
                    .addPropertyNode("details").addPropertyNode("landM2").addConstraintViolation();
            valid = false;
        }

        return valid;
    }

    private static boolean isComplete(LocalizedText t) {
        return t != null && StringUtils.hasText(t.title()) && StringUtils.hasText(t.description());
    }
}
