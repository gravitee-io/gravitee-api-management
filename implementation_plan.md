# Implementation Plan: GKO-2324

## Bug Summary

The logs search endpoint (`POST /management/v2/environments/{envId}/logs/search`) returns HTTP 500 when a `RESPONSE_TIME` numeric filter has a `null` value. It should return HTTP 400 with a validation error.

**Root Cause:** In `SearchEnvironmentLogsUseCase.applyNumericFilter()`, calling `filter.value().longValue()` throws a `NullPointerException` when the value is `null`, which propagates as an unhandled 500 error.

## Fix Strategy

Add a null-check with validation in the use case layer before calling `.longValue()`. If the value is null, throw an appropriate validation exception that the REST layer maps to HTTP 400.

## Files to Modify

### 1. `SearchEnvironmentLogsUseCase.java`
**Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/logs_engine/use_case/SearchEnvironmentLogsUseCase.java`

- Add a null-check in `applyNumericFilter()` for `filter.value()` before calling `.longValue()`
- Throw a domain validation exception (e.g., `IllegalArgumentException` or an existing validation exception used in the codebase) when value is null

### 2. `LogsSearchResourceTest.java`
**Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/test/java/io/gravitee/rest/api/management/v2/rest/resource/logs/LogsSearchResourceTest.java`

- Add a test in the `RequestValidation` nested class: `should_return_400_if_response_time_filter_value_is_null()`
- Follow the existing test patterns in the class

### 3. `SearchEnvironmentLogsUseCaseTest.java`
**Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/core/logs_engine/use_case/SearchEnvironmentLogsUseCaseTest.java`

- Add a unit test verifying that a null numeric filter value produces a validation error