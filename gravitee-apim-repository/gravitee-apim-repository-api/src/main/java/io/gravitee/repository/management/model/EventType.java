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
package io.gravitee.repository.management.model;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum EventType {
    PUBLISH_API,
    PUBLISH_API_RESULT,
    UNPUBLISH_API,
    UNPUBLISH_API_RESULT,
    START_API,
    STOP_API,
    GATEWAY_STARTED,
    GATEWAY_STOPPED,
    PUBLISH_DICTIONARY,
    UNPUBLISH_DICTIONARY,
    START_DICTIONARY,
    STOP_DICTIONARY,
    PUBLISH_ORGANIZATION,
    PUBLISH_ORGANIZATION_LICENSE,
    DEBUG_API,
    DEPLOY_SHARED_POLICY_GROUP,
    UNDEPLOY_SHARED_POLICY_GROUP,
}
