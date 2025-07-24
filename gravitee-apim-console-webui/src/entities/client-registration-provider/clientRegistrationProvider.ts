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

import { KeyStore, PEMKeyStore, PEMTrustStore, TrustStore } from '../management-api-v2';

export class ClientRegistrationProvider {
  public id: string;
  public name: string;
  public description: string;
  public discovery_endpoint: string;
  public initial_access_token_type: 'INITIAL_ACCESS_TOKEN' | 'CLIENT_CREDENTIALS';
  public client_id: string;
  public client_secret: string;
  public scopes: string[];
  public initial_access_token: string;
  public renew_client_secret_support: boolean;
  public renew_client_secret_endpoint: string;
  public renew_client_secret_method: string;
  public software_id: string;
  public updated_at: number;
  public trust_store?: Exclude<TrustStore, PEMTrustStore>;
  public key_store?: Exclude<KeyStore, PEMKeyStore>;
}
