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
package io.gravitee.apim.reporter.elasticsearch.mapping;

/**
 * A resource the reporter needs before it can index (an index template, the initial index of an ILM alias) could not be
 * created.
 */
public class IndexPreparationException extends RuntimeException {

    public IndexPreparationException(String resource, Throwable cause) {
        super("Unable to create " + resource + ": " + cause.getMessage(), cause);
    }
}
