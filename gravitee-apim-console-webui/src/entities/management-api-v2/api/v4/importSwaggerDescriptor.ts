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
export interface ImportSwaggerDescriptor {
  /** The raw OpenAPI/Swagger content (INLINE) or the URL to fetch it from (URL). */
  payload: string;
  /** Whether the payload is an inline OpenAPI string or a remote URL. Defaults to INLINE. */
  type?: 'INLINE' | 'URL';
  withDocumentation?: boolean;
  withOASValidationPolicy?: boolean;
}
