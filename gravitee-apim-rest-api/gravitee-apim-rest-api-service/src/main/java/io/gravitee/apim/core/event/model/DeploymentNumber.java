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
package io.gravitee.apim.core.event.model;

import io.gravitee.apim.core.exception.TechnicalDomainException;

/**
 * The deployment number an API event carries.
 *
 * <p>A gateway reads it as the revision of the API it runs, and replaces what it runs only with an event of another
 * revision, however recent. An event sending the gateways a new content therefore opens the number after the one of
 * the latest event of the API: keeping that number would leave the content unseen until the next deployment.
 *
 * @param value the number; {@code 0} for an event that carries none, so the first deployment is number 1.
 */
public record DeploymentNumber(long value) {
    public static final DeploymentNumber NONE = new DeploymentNumber(0);

    /**
     * The number an event carries under its {@link Event.EventProperties#DEPLOYMENT_NUMBER} property.
     *
     * @param property the property, or {@code null} when the event carries none.
     * @throws TechnicalDomainException when the property is not a number.
     */
    public static DeploymentNumber of(String property) {
        if (property == null) {
            return NONE;
        }
        try {
            return new DeploymentNumber(Long.parseLong(property));
        } catch (NumberFormatException e) {
            throw new TechnicalDomainException("The deployment number " + property + " is not a number", e);
        }
    }

    public DeploymentNumber next() {
        return new DeploymentNumber(value + 1);
    }

    /** The number as an event carries it. */
    public String asProperty() {
        return Long.toString(value);
    }
}
