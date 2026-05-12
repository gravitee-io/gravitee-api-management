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
import type { ApiCreationState } from '../types/models';

export function formatSecurityType(type: ApiCreationState['security']['type']): string {
    switch (type) {
        case 'keyless':
            return 'Keyless (Open)';
        case 'api-key':
            return 'API Key';
        case 'jwt':
            return 'JWT';
        case 'oauth2':
            return 'OAuth 2.0';
        case 'mtls':
            return 'mTLS';
    }
}
