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
import { ApiLifecycleState, ApiVisibility, DefinitionVersion, Property, Resource, ResponseTemplate } from '../api';

export interface UpdateBaseApi {
  /**
   * @description API's name. Duplicate names can exists.
   * @example My Api
   */
  name: string;
  /**
   * @description API's version. It's a simple string only used in the portal.
   * @example v1.0
   */
  apiVersion: string;
  /**
   * @description API's description. A short description of your API.
   * @example I can use many characters to describe this API.
   */
  description?: string;
  definitionVersion: DefinitionVersion;
  /**
   * @description API's groups. Used to add team in your API.
   * @example [
   *   "MY_GROUP1",
   *   "MY_GROUP2"
   * ]
   */
  groups?: string[];
  /**
   * @description The list of sharding tags associated with this API.
   * @example [
   *   "public",
   *   "private"
   * ]
   */
  tags?: string[];
  resources?: Resource[];
  responseTemplates?: { [key: string]: { [key: string]: ResponseTemplate } };
  visibility?: ApiVisibility;
  /**
   * @description The list of category ids associated with this API.
   * @example [
   *   "6c530064-0b2c-4004-9300-640b2ce0047b",
   *   "12559b64-0b2c-4004-9300-640b2ce0047b"
   * ]
   */
  categories?: string[];
  /**
   * @description The free list of labels associated with this API.
   * @example [
   *   "json",
   *   "read_only",
   *   "awesome"
   * ]
   */
  labels?: string[];
  lifecycleState?: ApiLifecycleState;
  /**
   * @description Disable membership notifications.
   * @default false
   */
  disableMembershipNotifications?: boolean;
  /**
   * Indicates whether this API is allowed to be used in API Products. Only applicable for V4 HTTP Proxy APIs.
   */
  allowedInApiProducts?: boolean;
  /**
   * @description Allow an application to subscribe to more than one JWT/OAuth2 plan. Selection rules or sharding tags should be configured on plans (V4 only).
   * @default false
   */
  allowMultiJwtOauth2Subscriptions?: boolean;
  properties?: Property[];
}
