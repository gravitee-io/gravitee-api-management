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
import { ApiLinks } from './api-links';
import { ListenerType } from './listener-type';
import { User } from '../user/user';

/**
 * Describes an API.
 */
export interface Api {
  /**
   * Unique identifier of an API.
   */
  id: string;
  /**
   * Name of the API.
   */
  name: string;
  /**
   * Version of the API.
   */
  version: string;
  /**
   * Description of the API.
   */
  description: string;
  /**
   * Whether or not the API is public.
   */
  _public?: boolean;
  /**
   * Whether or not the API is running.
   */
  running?: boolean;
  /**
   * List of all the available endpoints to call the API.
   */
  entrypoints: Array<string>;
  listener_type?: ListenerType;
  /**
   * List of labels linked to this API.
   */
  labels?: Array<string>;
  owner: User;
  /**
   * create date and time.
   */
  created_at?: Date;
  /**
   * Last update date and time.
   */
  updated_at?: Date;
  /**
   * List of categories this API belongs to.
   */
  categories?: Array<string>;
  _links?: ApiLinks;
}
