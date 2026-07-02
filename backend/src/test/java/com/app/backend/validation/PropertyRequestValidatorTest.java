package com.app.backend.validation;

import com.app.backend.dto.CreatePropertyRequest;
import com.app.backend.dto.LocalizedText;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static com.app.backend.testutil.TestData.createPropertyRequest;
import static org.assertj.core.api.Assertions.assertThat;

class PropertyRequestValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<ConstraintViolation<CreatePropertyRequest>> violations(CreatePropertyRequest req) {
        return validator.validate(req);
    }

    @Test
    void valid_whenOneCompleteTranslationPresent() {
        assertThat(violations(createPropertyRequest())).isEmpty();
    }

    @Test
    void invalid_whenTitleBlank() {
        CreatePropertyRequest req = createPropertyRequest().toBuilder()
                .translations(Map.of("lv", new LocalizedText(null, "Desc LV")))
                .build();

        assertThat(violations(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("translations"));
    }

    @Test
    void invalid_whenDescriptionBlank() {
        CreatePropertyRequest req = createPropertyRequest().toBuilder()
                .translations(Map.of("lv", new LocalizedText("Title LV", null)))
                .build();

        assertThat(violations(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("translations"));
    }

    @Test
    void invalid_whenNotReadyHasYearBuilt() {
        CreatePropertyRequest req = createPropertyRequest().toBuilder()
                .type(ListingType.NEW_PROJECT)
                .completion(PropertyCompletion.NOT_READY)
                .build();

        assertThat(violations(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("details.yearBuilt"));
    }

    @Test
    void valid_whenNotReadyWithoutYearBuilt() {
        CreatePropertyRequest req = createPropertyRequest().toBuilder()
                .type(ListingType.NEW_PROJECT)
                .completion(PropertyCompletion.NOT_READY)
                .details(createPropertyRequest().details().toBuilder().yearBuilt(null).build())
                .build();

        assertThat(violations(req)).isEmpty();
    }

    @Test
    void invalid_whenCompletionSetOnNonNewProjectListing() {
        CreatePropertyRequest req = createPropertyRequest().toBuilder()
                .type(ListingType.BUY)
                .completion(PropertyCompletion.READY)
                .build();

        assertThat(violations(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("completion"));
    }

    @Test
    void valid_whenCompletionSetOnNewProjectListing() {
        CreatePropertyRequest req = createPropertyRequest().toBuilder()
                .type(ListingType.NEW_PROJECT)
                .completion(PropertyCompletion.READY)
                .build();

        assertThat(violations(req)).isEmpty();
    }

    @Test
    void invalid_whenLandM2SetOnApartment() {
        CreatePropertyRequest req = createPropertyRequest().toBuilder()
                .propertyKind(PropertyCategory.APARTMENT)
                .details(createPropertyRequest().details().toBuilder()
                        .landM2(new BigDecimal("100.00")).build())
                .build();

        assertThat(violations(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("details.landM2"));
    }

    @Test
    void valid_whenLandM2SetOnHouse() {
        CreatePropertyRequest req = createPropertyRequest().toBuilder()
                .propertyKind(PropertyCategory.HOUSE)
                .details(createPropertyRequest().details().toBuilder()
                        .landM2(new BigDecimal("100.00")).build())
                .build();

        assertThat(violations(req)).isEmpty();
    }
}
