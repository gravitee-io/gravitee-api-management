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
import { DefinitionVersion } from './api';

export interface ApiSearchQuery {
  /**
   * The query to search for.
   */
  query?: string;
  /**
   * List of ids to find
   */
  ids?: string[];
  definitionVersion?: DefinitionVersion;
  definitionVersions?: DefinitionVersion[];
  apiTypes?: string[];
  statuses?: string[];
  tags?: string[];
  categories?: string[];
  published?: string[];
  visibilities?: string[];
  /**
   * When true, only returns APIs that are allowed to be used in API Products. Only applicable for V4 HTTP Proxy APIs.
   */
  allowedInApiProducts?: boolean;
}
