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
import { EMPTY_PROVIDER_FORM, formToWrite } from './providerForm';
import { validateProviderForm } from './validateProviderForm';

describe('formToWrite', () => {
    it('trims secret and token fields so validation and the payload agree', () => {
        const form = {
            ...EMPTY_PROVIDER_FORM,
            name: '  Okta DCR  ',
            description: '  Prod  ',
            discovery_endpoint: '  https://idp.example.com/.well-known/openid-configuration  ',
            initial_access_token_type: 'CLIENT_CREDENTIALS' as const,
            client_id: '  id  ',
            client_secret: '  secret\n',
            software_id: '  tpl  ',
            initial_access_token: '  tok  ',
            renew_client_secret_endpoint: '  https://idp.example.com/renew  ',
        };

        expect(validateProviderForm(form).client_secret).toBeUndefined();
        expect(formToWrite(form)).toEqual(
            expect.objectContaining({
                name: 'Okta DCR',
                description: 'Prod',
                discovery_endpoint: 'https://idp.example.com/.well-known/openid-configuration',
                client_id: 'id',
                client_secret: 'secret',
                software_id: 'tpl',
                initial_access_token: 'tok',
                renew_client_secret_endpoint: 'https://idp.example.com/renew',
            }),
        );
    });
});
