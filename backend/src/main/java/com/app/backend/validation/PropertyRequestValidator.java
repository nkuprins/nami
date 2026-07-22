package com.app.backend.validation;

import com.app.backend.category.CategoryField;
import com.app.backend.category.CategoryProfile;
import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.model.PropertyDetails;
import com.app.backend.dto.property.request.PropertyRequest;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

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

        // Per-category required/forbidden conditional fields (see CategoryProfile).
        PropertyCategory category = req.propertyKind();
        if (category != null) {
            CategoryProfile profile = CategoryProfile.of(category);
            for (CategoryField field : CategoryField.values()) {
                Object value = valueOf(field, req);
                if (profile.requires(field) && value == null) {
                    reportField(ctx, field, field.node + " is required for a "
                            + category.getDbValue() + " listing");
                    valid = false;
                } else if (profile.forbids(field) && value != null) {
                    reportField(ctx, field, field.node + " is not valid for a "
                            + category.getDbValue() + " listing");
                    valid = false;
                }
            }
        }

        Short yearBuilt = req.details() != null ? req.details().yearBuilt() : null;
        if (req.completion() == PropertyCompletion.NOT_READY && yearBuilt != null) {
            ctx.buildConstraintViolationWithTemplate(
                            "A 'not_ready' new project cannot have a year_built")
                    .addPropertyNode("details").addPropertyNode("yearBuilt").addConstraintViolation();
            valid = false;
        }

        Short floor = req.details() != null ? req.details().floor() : null;
        Short totalFloors = req.details() != null ? req.details().totalFloors() : null;
        if (floor != null && totalFloors == null) {
            ctx.buildConstraintViolationWithTemplate("floor requires total_floors")
                    .addPropertyNode("details").addPropertyNode("totalFloors").addConstraintViolation();
            valid = false;
        }
        if (floor != null && totalFloors != null && floor > totalFloors) {
            ctx.buildConstraintViolationWithTemplate("floor cannot exceed total_floors")
                    .addPropertyNode("details").addPropertyNode("floor").addConstraintViolation();
            valid = false;
        }

        Short rooms = req.details() != null ? req.details().rooms() : null;
        Short bedrooms = req.details() != null ? req.details().bedrooms() : null;
        if (rooms != null && bedrooms != null && bedrooms > rooms) {
            ctx.buildConstraintViolationWithTemplate("bedrooms cannot exceed rooms")
                    .addPropertyNode("details").addPropertyNode("bedrooms").addConstraintViolation();
            valid = false;
        }

        return valid;
    }

    private static boolean isComplete(LocalizedText t) {
        return t != null && StringUtils.hasText(t.title()) && StringUtils.hasText(t.description());
    }

    private static Object valueOf(CategoryField field, PropertyRequest req) {
        PropertyDetails d = req.details();
        return switch (field) {
            case ROOMS -> d == null ? null : d.rooms();
            case M2 -> d == null ? null : d.m2();
            case LAND_M2 -> d == null ? null : d.landM2();
            case LAND_USE -> req.landUse();
            case NEW_PROJECT_KIND -> req.newProjectKind();
            case COMMERCIAL_SUBTYPE -> req.commercialSubtype();
            case COMPLETION -> req.completion();
        };
    }

    private static void reportField(ConstraintValidatorContext ctx, CategoryField field, String message) {
        var builder = ctx.buildConstraintViolationWithTemplate(message);
        if (field.parentNode != null) {
            builder.addPropertyNode(field.parentNode).addPropertyNode(field.node).addConstraintViolation();
        } else {
            builder.addPropertyNode(field.node).addConstraintViolation();
        }
    }
}
