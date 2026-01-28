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

import { PrimaryOwner } from '../api';

export interface ApiProduct {
  /**
   * API Product's unique identifier.
   */
  id: string;
  /**
   * The environment's uuid.
   */
  environmentId?: string;
  /**
   * API Product's name.
   */
  name: string;
  /**
   * API Product's version.
   */
  version: string;
  /**
   * API Product's description.
   */
  description?: string;
  /**
   * List of API IDs included in the product.
   */
  apiIds?: string[];
  /**
   * The date (as timestamp) when the API Product was created.
   */
  createdAt?: Date;
  /**
   * The last date (as timestamp) when the API Product was updated.
   */
  updatedAt?: Date;
  /**
   * The primary owner of the API Product.
   */
  primaryOwner?: PrimaryOwner;
  _links?: { [key: string]: string };
}
