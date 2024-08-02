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
 * the context where the api definition was created
 */
export interface OriginContext {
  /** The origin of the API. */
  origin: Origin;
}

export class ManagementContext implements OriginContext {
  public origin: Origin = 'MANAGEMENT';
}

export class KubernetesContext implements OriginContext {
  public origin: Origin = 'KUBERNETES';
  /** The mode of the API. fully_managed: Mode indicating the api is fully managed by the origin and so, only the origin should be able to manage the api. api_definition_only: Mode indicating the api is partially managed by the origin and so, only the origin should be able to manage the api definition part of the api. This includes everything regarding the definition of the apis (plans, flows, metadata, ...) */
  public mode: Mode;
}

export class IntegrationContext implements OriginContext {
  public origin: Origin = 'INTEGRATION';
  public integrationId: string;
  public integrationName: string;
  public provider: string;
}

export type Origin = 'MANAGEMENT' | 'KUBERNETES' | 'INTEGRATION';
export type Mode = 'FULLY_MANAGED';
