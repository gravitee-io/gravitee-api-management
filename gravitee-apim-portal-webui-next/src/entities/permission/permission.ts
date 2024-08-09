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
export interface UserEnvironmentPermissions {
  APPLICATION?: string[];
  USER?: string[];
}

export interface UserApplicationPermissions {
  DEFINITION?: string[];
  MEMBER?: string[];
  ANALYTICS?: string[];
  LOG?: string[];
  SUBSCRIPTION?: string[];
  NOTIFICATION?: string[];
  ALERT?: string[];
  METADATA?: string[];
}

export interface UserApiPermissions {
  DEFINITION?: string[];
  PLAN?: string[];
  SUBSCRIPTION?: string[];
  MEMBER?: string[];
  METADATA?: string[];
  ANALYTICS?: string[];
  EVENT?: string[];
  HEALTH?: string[];
  LOG?: string[];
  DOCUMENTATION?: string[];
  GATEWAY_DEFINITION?: string[];
  AUDIT?: string[];
  RATING?: string[];
  RATING_ANSWER?: string[];
  NOTIFICATION?: string[];
  MESSAGE?: string[];
  ALERT?: string[];
  RESPONSE_TEMPLATES?: string[];
  REVIEWS?: string[];
  QUALITY_RULE?: string[];
}
