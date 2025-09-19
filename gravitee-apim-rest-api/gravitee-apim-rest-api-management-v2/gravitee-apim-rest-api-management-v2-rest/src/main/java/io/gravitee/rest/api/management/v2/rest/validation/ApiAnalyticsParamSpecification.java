/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.validation;

import io.gravitee.apim.core.analytics.model.Aggregation;
import io.gravitee.rest.api.management.v2.rest.resource.param.ApiAnalyticsParam;
import jakarta.ws.rs.BadRequestException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public interface ApiAnalyticsParamSpecification extends Predicate<ApiAnalyticsParam> {
    boolean satisfies(ApiAnalyticsParam param);

    @Override
    default boolean test(ApiAnalyticsParam param) {
        return satisfies(param);
    }

    default void throwIfNotSatisfied(ApiAnalyticsParam param) {
        if (!satisfies(param)) {
            throw new BadRequestException(getErrorMessage());
        }
    }

    String getErrorMessage();

    static ApiAnalyticsParamSpecification hasFromParam() {
        return new ApiAnalyticsParamSpecification() {
            public boolean satisfies(ApiAnalyticsParam param) {
                return param.getFrom() != null;
            }

            public String getErrorMessage() {
                return "Parameter 'from' is required";
            }
        };
    }

    static ApiAnalyticsParamSpecification hasToParam() {
        return new ApiAnalyticsParamSpecification() {
            public boolean satisfies(ApiAnalyticsParam param) {
                return param.getTo() != null;
            }

            public String getErrorMessage() {
                return "Parameter 'to' is required";
            }
        };
    }

    static ApiAnalyticsParamSpecification hasInterval() {
        return new ApiAnalyticsParamSpecification() {
            public boolean satisfies(ApiAnalyticsParam param) {
                return param.getInterval() != null && param.getInterval() > 0;
            }

            public String getErrorMessage() {
                return "Interval is required and must be a positive number.";
            }
        };
    }

    static ApiAnalyticsParamSpecification hasField() {
        return new ApiAnalyticsParamSpecification() {
            public boolean satisfies(ApiAnalyticsParam param) {
                return param.getField() != null && !param.getField().isBlank();
            }

            public String getErrorMessage() {
                return "Field must not be blank.";
            }
        };
    }

    static ApiAnalyticsParamSpecification hasType() {
        return new ApiAnalyticsParamSpecification() {
            public boolean satisfies(ApiAnalyticsParam param) {
                return param.getType() != null;
            }

            public String getErrorMessage() {
                return "Valid type parameter is required. Supported types are: [histogram, group_by, stats, count]";
            }
        };
    }

    static ApiAnalyticsParamSpecification aggregationsNotBlank() {
        return new ApiAnalyticsParamSpecification() {
            public boolean satisfies(ApiAnalyticsParam param) {
                return param
                    .getAggregations()
                    .stream()
                    .allMatch(
                        agg -> agg.getField() != null && !agg.getField().isBlank() && agg.getType() != null && !agg.getType().isBlank()
                    );
            }

            public String getErrorMessage() {
                return "Aggregations must not be blank if provided.";
            }
        };
    }

    static ApiAnalyticsParamSpecification aggregationsOfValidType() {
        return new ApiAnalyticsParamSpecification() {
            public boolean satisfies(ApiAnalyticsParam param) {
                return param
                    .getAggregations()
                    .stream()
                    .map(ApiAnalyticsParam.Aggregation::getType)
                    .map(String::toUpperCase)
                    .allMatch(at -> Arrays.stream(Aggregation.AggregationType.values()).map(Enum::name).anyMatch(at::equals));
            }

            public String getErrorMessage() {
                return "Aggregation types supported: field, avg, min, max.";
            }
        };
    }

    static ApiAnalyticsParamSpecification validOrder() {
        return new ApiAnalyticsParamSpecification() {
            public boolean satisfies(ApiAnalyticsParam param) {
                String order = param.getOrder();
                if (order == null || order.isBlank()) return true;
                String pattern = "^-?(count|avg):[a-zA-Z0-9_.-]+$";
                return order.matches(pattern);
            }

            public String getErrorMessage() {
                return "Order must be in the format [-]<aggregation type>:<field> (e.g. avg:some_field or -count:_key). Aggregation type must be one of: count, avg. Field must be a non-blank identifier.";
            }
        };
    }

    static ApiAnalyticsParamSpecification and(List<ApiAnalyticsParamSpecification> specs) {
        return new ApiAnalyticsParamSpecification() {
            public boolean satisfies(ApiAnalyticsParam param) {
                return specs.stream().allMatch(spec -> spec.satisfies(param));
            }

            public void throwIfNotSatisfied(ApiAnalyticsParam param) {
                specs.forEach(spec -> spec.throwIfNotSatisfied(param));
            }

            public String getErrorMessage() {
                return specs
                    .stream()
                    .map(ApiAnalyticsParamSpecification::getErrorMessage)
                    .reduce("", (a, b) -> a + " " + b)
                    .trim();
            }
        };
    }

    static ApiAnalyticsParamSpecification empty() {
        return new ApiAnalyticsParamSpecification() {
            public boolean satisfies(ApiAnalyticsParam param) {
                return true;
            }

            public String getErrorMessage() {
                return "Should never happen";
            }

            @Override
            public void throwIfNotSatisfied(ApiAnalyticsParam param) {
                // ignore
            }
        };
    }

    static ApiAnalyticsParamSpecification forHistogram() {
        return and(List.of(hasInterval(), aggregationsNotBlank(), aggregationsOfValidType()));
    }

    static ApiAnalyticsParamSpecification forGroupBy() {
        return and(List.of(hasField(), validOrder()));
    }

    static ApiAnalyticsParamSpecification forStats() {
        return and(List.of(hasField()));
    }

    static ApiAnalyticsParamSpecification forCount() {
        return empty();
    }

    static ApiAnalyticsParamSpecification common() {
        return and(List.of(hasType(), hasFromParam(), hasToParam()));
    }
}
