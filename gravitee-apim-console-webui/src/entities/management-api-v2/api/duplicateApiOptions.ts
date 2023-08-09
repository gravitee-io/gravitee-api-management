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

export interface DuplicateApiOptions {
  /** The context path of the duplicated API */
  contextPath: string;
  /** The version of the duplicated API. If it is not defined, the value of the source API is used. */
  version?: string;
  /** The list of API fields that can be excluded to create the new API. */
  filteredFields: DuplicateFilteredField[];
}

export type DuplicateFilteredField = 'GROUPS' | 'MEMBERS' | 'PAGES' | 'PLANS';
