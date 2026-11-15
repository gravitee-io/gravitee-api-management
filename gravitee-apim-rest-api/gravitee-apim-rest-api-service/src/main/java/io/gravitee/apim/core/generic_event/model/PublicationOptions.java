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
package io.gravitee.apim.core.generic_event.model;

/**
 * How a generic event is written.
 *
 * @param historized also records this version in deployment history; otherwise only the current version is kept.
 * @param deploymentLabel an optional label shown with the recorded version.
 */
public record PublicationOptions(boolean historized, String deploymentLabel) {
    public static PublicationOptions latestOnly() {
        return new PublicationOptions(false, null);
    }

    public static PublicationOptions withHistory() {
        return new PublicationOptions(true, null);
    }

    public static PublicationOptions withHistory(String deploymentLabel) {
        return new PublicationOptions(true, deploymentLabel);
    }
}
