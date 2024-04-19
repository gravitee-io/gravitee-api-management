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
/**
 * Gravitee.io Portal Rest API
 * API dedicated to the devportal part of Gravitee
 *
 * Contact: contact@graviteesource.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

/**
 * Technical information about the page
 */
export interface PageConfiguration {
  /**
   * Enable \"Try It!\" mode in documentation page.
   */
  try_it?: boolean;
  /**
   * Enable \"Try It!\" mode in documentation page for anonymous users.
   */
  try_it_anonymous?: boolean;
  /**
   * Base URL used to try the API.
   */
  try_it_url?: string;
  /**
   * Show the URL to download the content.
   */
  show_url?: boolean;
  /**
   * Display the operationId in the operations list.
   */
  display_operation_id?: boolean;
  /**
   * Default expansion setting for the operations and tags.\\ Possibles values are :  - list : Expands only the tags  - full : Expands the tags and operations  - none : Expands nothing. DEFAULT.
   */
  doc_expansion?: DocExpansionEnum;
  /**
   * Add a top bar to filter content.
   */
  enable_filtering?: boolean;
  /**
   * Display vendor extension (X-) fields and values for Operations, Parameters, and Schema.
   */
  show_extensions?: boolean;
  /**
   * Display extensions (pattern, maxLength, minLength, maximum, minimum) fields and values for Parameters.
   */
  show_common_extensions?: boolean;
  /**
   * Number of max tagged operations displayed. \\ Limits the number of tagged operations displayed to at most this many (negative means show all operations).\\ No limit by default.
   */
  max_displayed_tags?: number;
  /**
   * Enable use of PKCE with authorization code flows in documentation page.
   */
  use_pkce?: boolean;
  /**
   * The type of viewer for OpenAPI specification. Default is \'Swagger\'
   */
  viewer?: ViewerEnum;
}

export type DocExpansionEnum = 'list' | 'full' | 'none';
export const DocExpansionEnum = {
  List: 'list' as DocExpansionEnum,
  Full: 'full' as DocExpansionEnum,
  None: 'none' as DocExpansionEnum,
};
export type ViewerEnum = 'Swagger' | 'Redoc';
export const ViewerEnum = {
  Swagger: 'Swagger' as ViewerEnum,
  Redoc: 'Redoc' as ViewerEnum,
};
