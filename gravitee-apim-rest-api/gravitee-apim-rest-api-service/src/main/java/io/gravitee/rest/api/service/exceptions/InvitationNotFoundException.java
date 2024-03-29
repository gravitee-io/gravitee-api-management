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
package io.gravitee.rest.api.service.exceptions;

import static java.util.Collections.singletonMap;

import java.util.Map;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InvitationNotFoundException extends AbstractNotFoundException {

    private final String invitation;

    public InvitationNotFoundException(String invitation) {
        this.invitation = invitation;
    }

    @Override
    public String getMessage() {
        return "Invitation [" + invitation + "] cannot be found";
    }

    @Override
    public String getTechnicalCode() {
        return "invitation.notFound";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("invitation", invitation);
    }
}
