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

/** A published plan a user can subscribe to on an AI Product (from GET /api-products/{id}). */
export interface AiProductPlan {
  id: string;
  name?: string;
  /** Security type of the plan, e.g. API_KEY. */
  security?: string;
  /** Subscription validation mode (AUTO or MANUAL). */
  validation?: string;
}

/** An LLM proxy bundled in an AI Product. */
export interface AiProductComponent {
  name?: string;
  /** Gateway context path of the proxy. */
  path?: string;
  models?: string[];
}

/** An AI Product in the portal catalog (a bundle of LLM proxies). */
export interface AiProduct {
  id: string;
  name: string;
  description?: string;
  version?: string;
  /** Published plans — present on the detail response, used to subscribe. */
  plans?: AiProductPlan[];
  /** All models served across the product's LLM proxies. */
  models?: string[];
  /** The bundled LLM proxies (name + path + models), for the catalog. */
  components?: AiProductComponent[];
}

export interface AiProductsResponse {
  data: AiProduct[];
}
