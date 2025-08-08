package io.gravitee.apim.core.api.model.utils;

import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MigrationResultUtils {

    public static <T> T get(MigrationResult<T> result) {
        return Objects.requireNonNull(result.value());
    }
}
