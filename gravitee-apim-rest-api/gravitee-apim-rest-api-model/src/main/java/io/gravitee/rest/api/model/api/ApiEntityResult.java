/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model.api;

public class ApiEntityResult {

    private ApiEntity result;
    private String error;

    private ApiEntityResult(ApiEntity result) {
        this.result = result;
    }

    private ApiEntityResult(String error) {
        this.error = error;
    }

    public static ApiEntityResult failure(String message) {
        return new ApiEntityResult(message);
    }

    public static ApiEntityResult success(ApiEntity result) {
        return new ApiEntityResult(result);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public ApiEntity getApi() {
        return result;
    }

    public String getErrorMessage() {
        return error;
    }
}
