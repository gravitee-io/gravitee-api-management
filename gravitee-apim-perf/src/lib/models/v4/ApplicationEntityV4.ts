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
import { PrimaryOwnerEntity } from '@models/v3/ApiEntity';

/**
 *
 * @export
 * @interface ApplicationEntityV4
 */
export interface ApplicationEntityV4 {
  /**
   *
   * @type {ApiKeyMode}
   * @memberof ApplicationEntity
   */
  api_key_mode?: ApiKeyMode;
  /**
   *
   * @type {string}
   * @memberof ApplicationEntity
   */
  background?: string;
  /**
   * The date (as a timestamp) when the application was created.
   * @type {Date}
   * @memberof ApplicationEntity
   */
  created_at?: Date;
  /**
   * Application's description. A short description of your App.
   * @type {string}
   * @memberof ApplicationEntity
   */
  description?: string;
  /**
   *
   * @type {boolean}
   * @memberof ApplicationEntity
   */
  disable_membership_notifications?: boolean;
  /**
   * Domain used by the application, if relevant
   * @type {string}
   * @memberof ApplicationEntity
   */
  domain?: string;
  /**
   * Application groups. Used to add teams to your application.
   * @type {Array<string>}
   * @memberof ApplicationEntity
   */
  groups?: Array<string>;
  /**
   * Application's uuid.
   * @type {string}
   * @memberof ApplicationEntity
   */
  id?: string;
  /**
   * Application's name. Duplicate names can exists.
   * @type {string}
   * @memberof ApplicationEntity
   */
  name?: string;
  /**
   *
   * @type {PrimaryOwnerEntity}
   * @memberof ApplicationEntity
   */
  owner?: PrimaryOwnerEntity;
  /**
   *
   * @type {string}
   * @memberof ApplicationEntity
   */
  picture?: string;
  /**
   *
   * @type {ApplicationSettings}
   * @memberof ApplicationEntity
   */
  settings?: {};
  /**
   * if the app is ACTIVE or ARCHIVED.
   * @type {string}
   * @memberof ApplicationEntity
   */
  status?: string;
  /**
   * a string to describe the type of your app.
   * @type {string}
   * @memberof ApplicationEntity
   */
  type?: string;
  /**
   * The last date (as a timestamp) when the application was updated.
   * @type {Date}
   * @memberof ApplicationEntity
   */
  updated_at?: Date;
}

export const ApiKeyMode = {
  SHARED: 'SHARED',
  EXCLUSIVE: 'EXCLUSIVE',
  UNSPECIFIED: 'UNSPECIFIED',
} as const;
export type ApiKeyMode = (typeof ApiKeyMode)[keyof typeof ApiKeyMode];

export function ApiKeyModeFromJSON(json: any): ApiKeyMode {
  return ApiKeyModeFromJSONTyped(json, false);
}

export function ApiKeyModeFromJSONTyped(json: any, ignoreDiscriminator: boolean): ApiKeyMode {
  return json as ApiKeyMode;
}

export function ApiKeyModeToJSON(value?: ApiKeyMode | null): any {
  return value as any;
}
