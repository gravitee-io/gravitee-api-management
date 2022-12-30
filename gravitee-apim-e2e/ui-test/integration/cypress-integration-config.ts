/*
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
import { defineConfig } from 'cypress';
import cypressConfig from '../../cypress.config';

export default defineConfig({
  env: {
    failOnStatusCode: false,
    api_publisher_user_login: 'api1',
    api_publisher_user_password: 'api1',
    application_user_login: 'application1',
    application_user_password: 'application1',
    low_permission_user_login: 'user',
    low_permission_user_password: 'password',
    admin_user_login: 'admin',
    admin_user_password: 'admin',
    am_admin_user_login: 'admin',
    am_admin_user_password: 'adminadmin',
    managementOrganizationApi: '/management/organizations/DEFAULT',
    managementApi: '/management/organizations/DEFAULT/environments/DEFAULT',
    managementUI: 'https://apim.gravitee.io/console',
    gatewayServer: 'https://apim.gravitee.io',
    am_gatewayServer: 'https://am.gravitee.io',
    am_managementAPI: 'https://am.gravitee.io/management/organizations/DEFAULT/environments/DEFAULT',
    portalApi: '/portal/environments/DEFAULT',
    localPetstore_v2: 'http://petstore:8080/v2',
    localPetstore_v3: 'http://petstore3:8080/api/v3',
  },
  e2e: {
    ...cypressConfig.e2e,
  },
});
