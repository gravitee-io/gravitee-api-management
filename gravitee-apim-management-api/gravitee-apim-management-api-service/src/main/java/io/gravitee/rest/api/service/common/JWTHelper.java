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
package io.gravitee.rest.api.service.common;

/**
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface JWTHelper {
    enum ACTION {
        RESET_PASSWORD,
        USER_REGISTRATION,
        GROUP_INVITATION,
        USER_CREATION,
    }

    interface Claims {
        String ISSUER = "iss";
        String SUBJECT = "sub";
        String PERMISSIONS = "permissions";
        String EMAIL = "email";
        String FIRSTNAME = "firstname";
        String LASTNAME = "lastname";
        String ACTION = "action";
        String ORG = "org";
    }

    interface DefaultValues {
        int DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER = 86400;
        int DEFAULT_JWT_EXPIRE_AFTER = 604800;
        String DEFAULT_JWT_ISSUER = "gravitee-management-auth";
    }
}
