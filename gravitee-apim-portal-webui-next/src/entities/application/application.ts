/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Subscription } from '../subscription/subscription';

export interface Application {
  api_key_mode?: string;
  applicationType?: string;
  created_at?: string;
  updated_at?: string;
  id: string;
  name: string;
  description?: string;
  picture?: string;
  hasClientId?: boolean;
  owner?: ApplicationOwner;
  groups?: ApplicationGroups[];
  settings: ApplicationSettings;
  _links?: ApplicationLinks;
}

export interface ApplicationOwner {
  id: string;
  first_name: string;
  last_name: string;
  display_name: string;
  email: string;
  editable_profile: boolean;
  customFields: ApplicationOwnerCustomFields;
  _links: ApplicationOwnerLinks;
}

export interface ApplicationOwnerCustomFields {
  city: string;
  job_position: string;
}

export interface ApplicationGroups {
  id: string;
  name: string;
}

export interface ApplicationOwnerLinks {
  avatar: string;
  notifications: string;
  self: string;
}

export interface ApplicationSettings {
  oauth?: ApplicationSettingsOAuth;
  app?: ApplicationSettingsApp;
}

export interface ApplicationSettingsApp {
  client_id?: string;
  type?: string;
}

export interface ApplicationSettingsOAuth {
  client_id: string;
  client_secret: string;
  redirect_uris: string[];
  renew_client_secret_supported: boolean;
  response_types: string[];
  grant_types: string[];
}

export interface ApplicationLinks {
  background: string;
  members: string;
  notifications: string;
  picture: string;
  self: string;
}

export interface ApplicationsResponse {
  data: Application[];
  metadata?: {
    pagination?: {
      current_page?: number;
      first?: number;
      last?: number;
      size?: number;
      total?: number;
      total_pages?: number;
    };
    subscriptions?: ApplicationsMetadataSubscriptions;
  };
  _links?: unknown;
}

export interface ApplicationsMetadataSubscriptions {
  [applicationId: string]: Subscription[];
}


