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
package io.gravitee.rest.api.model;

import java.util.Map;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RatingSummaryEntity {

    private String api;
    private int numberOfRatings;
    private Double averageRate;
    private Map<Byte, Long> numberOfRatingsByRate;

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public int getNumberOfRatings() {
        return numberOfRatings;
    }

    public void setNumberOfRatings(int numberOfRatings) {
        this.numberOfRatings = numberOfRatings;
    }

    public Double getAverageRate() {
        return averageRate;
    }

    public void setAverageRate(Double averageRate) {
        this.averageRate = averageRate;
    }

    public Map<Byte, Long> getNumberOfRatingsByRate() {
        return numberOfRatingsByRate;
    }

    public void setNumberOfRatingsByRate(Map<Byte, Long> numberOfRatingsByRate) {
        this.numberOfRatingsByRate = numberOfRatingsByRate;
    }

    @Override
    public String toString() {
        return "RatingSummaryEntity{" +
                "api='" + api + '\'' +
                ", numberOfRatings=" + numberOfRatings +
                ", averageRate=" + averageRate +
                ", numberOfRatingsByRate=" + numberOfRatingsByRate +
                '}';
    }
}
