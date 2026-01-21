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

import { PrimaryOwner } from '../api/primaryOwner';

/**
 * API Product entity
 */
export interface ApiProduct {
  /**
   * The unique technical identifier of the API Product.
   */
  id: string;

  /**
   * The environment ID where this API Product is defined.
   */
  environmentId?: string;

  /**
   * The display name.
   */
  name: string;

  /**
   * A short description of the product.
   */
  description?: string;

  /**
   * The version of the API Product.
   */
  version?: string;

  /**
   * The APIs contained in this product.
   * Useful to verify the current API is indeed in the list.
   */
  apiIds?: string[];

  /**
   * Creation timestamp (ISO 8601).
   */
  createdAt?: string;

  /**
   * Last update timestamp (ISO 8601).
   */
  updatedAt?: string;

  /**
   * Owner information.
   */
  primaryOwner?: PrimaryOwner;
}
