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
package io.gravitee.management.service.exceptions;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RatingNotFoundException extends AbstractNotFoundException {

    private final String rating;
    private final String api;

    public RatingNotFoundException(String rating) {
        this.rating = rating;
        this.api = null;
    }

    public RatingNotFoundException(String rating, String api) {
        this.rating = rating;
        this.api = api;
    }

    @Override
    public String getMessage() {
        return "Rating [" + rating + "] can not be found" + (api == null ? "":" on the api [" + api + "]");
    }
}
