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
package io.gravitee.apim.reporter.elasticsearch.mapping.serverless;

/**
 * A template has the name of one the reporter manages but was not written by APIM: the reporter changes nothing it does not
 * manage, and retrying would not help until an operator acts.
 */
public class UnmanagedTemplateException extends IllegalStateException {

    public UnmanagedTemplateException(String templateName) {
        super(
            String.format(
                "The index template [%s] has the name of a template APIM manages but carries no '_meta.managed_by: %s', so the " +
                    "reporter leaves it unchanged. Delete it so that the reporter writes its own, or set " +
                    "'reporters.elasticsearch.index' to another prefix",
                templateName,
                ServerlessIndexPreparer.MANAGED_BY
            )
        );
    }
}
