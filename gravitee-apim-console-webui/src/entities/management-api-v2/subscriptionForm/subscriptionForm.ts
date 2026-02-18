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
 * Subscription form definition used by API consumers when subscribing to APIs
 */
export interface SubscriptionForm {
  /**
   * Unique identifier of the subscription form
   */
  id: string;
  /**
   * Gravitee Markdown (GMD) content defining the form.
   * Supports form components like gmd-input, gmd-textarea, gmd-select, gmd-checkbox, gmd-radio.
   */
  gmdContent: string;
  /**
   * Whether the form is enabled and visible to API consumers in the Developer Portal
   */
  enabled: boolean;
}

/**
 * Payload for creating or updating a subscription form
 */
export interface CreateOrUpdateSubscriptionForm {
  /**
   * Gravitee Markdown (GMD) content defining the form.
   * Content is validated for security - malicious HTML/scripts will be rejected.
   */
  gmdContent: string;
}
