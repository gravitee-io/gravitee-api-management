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
import { UserEnvironmentPermissions } from '../permission/permission';

export interface UserConfig {
  /**
   * The URL of the Gravitee management UI
   */
  management_url?: string;
}

export interface UserLinks {
  self?: string;
  avatar?: string;
  notifications?: string;
}

export interface User {
  /**
   * Unique identifier of a user.
   */
  id?: string;
  /**
   * Unique reference if user comes from external source. Use for search only.
   */
  reference?: string;
  first_name?: string;
  last_name?: string;
  display_name?: string;
  email?: string;
  /**
   * True if the user can edit the MyAccount information
   */
  editable_profile?: boolean;
  permissions?: UserEnvironmentPermissions;
  /**
   * Values for CustomUserFields
   */
  customFields?: { [key: string]: object };
  config?: UserConfig;
  _links?: UserLinks;
}
