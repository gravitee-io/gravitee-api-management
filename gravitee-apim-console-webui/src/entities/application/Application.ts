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
export enum ApiKeyMode {
  UNSPECIFIED = 'UNSPECIFIED',
  SHARED = 'SHARED',
  EXCLUSIVE = 'EXCLUSIVE',
}

export interface Application {
  id: string;
  created_at: number;
  updated_at: number;
  name: string;
  description?: string;
  domain?: string;
  groups?: string[];
  status?: string;
  type?: string;
  picture_url?: string;
  picture?: string;
  background?: string;
  owner?: any;
  settings?: ApplicationSettings;
  disable_membership_notifications?: boolean;
  api_key_mode?: ApiKeyMode;
  origin?: string;
}

export interface ApplicationSettings {
  app?: {
    client_id?: string;
    type?: string;
  };
  oauth?: {
    client_id?: string;
    client_secret?: string;
    grant_types?: string[];
    response_types?: string[];
    application_type?: string;
    redirect_uris?: string[];
    renew_client_secret_supported?: boolean;
  };
  tls?: {
    client_certificate?: string;
  };
}

export interface ApplicationType {
  id: string;
  name: string;
  description: string;
  requires_redirect_uris: boolean;
  allowed_grant_types: GrantTypes[];
  default_grant_types: GrantTypes[];
  mandatory_grant_types: GrantTypes[];
}

export interface GrantTypes {
  code?: string;
  type: string;
  name: string;
  response_types: string[];
}

export interface ApplicationTransferOwnership {
  id?: string;
  reference?: string;
  role?: string;
}
