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

/**
 * List of links for pagination
 */
export interface Links {
  /**
   * Link to current resource
   */
  self?: string;
  /**
   * In a paginated response, link to the first page
   */
  first?: string;
  /**
   * In a paginated response, link to the last page
   */
  last?: string;
  /**
   * In a paginated response, link to the previous page. Maybe null if current is the first page
   */
  previous?: string;
  /**
   * In a paginated response, link to the next page. Maybe null if current is the last page
   */
  next?: string;
}
