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
export interface Application {
  name: string;
  description: string;
  settings: ApplicationSettings;
  type: string;
  clientId: string;
  groups: string[];
  picture: string;
  background: string;
}

export interface ApplicationSettings {
  app: SimpleApplicationSettings;
  oauth: OAuthClientSettings;
}

export interface SimpleApplicationSettings {
  type: string;
  client_id: string;
}

export interface OAuthClientSettings {
  client_id: string;
  client_secret: string;
  redirect_uris: string[];
  items: string;
  client_uri: string;
  logo_uri: string;
  response_types: string[];
  grant_types: string[];
  application_type: string;
  renew_client_secret_supported: boolean;
}

export interface ApplicationEntity {
  id: string;
  name: string;
  description: string;
  groups: string[];
  status: string;
  type: string;
  picture: string;
  background: string;
  created_at: number;
  updated_at: number;
  owner: PrimaryOwnerEntity;
  settings: ApplicationSettings;
  disable_membership_notifications: boolean;
}

export interface PrimaryOwnerEntity {
  id: string;
  email: string;
  displayName: string;
  type: string;
}
