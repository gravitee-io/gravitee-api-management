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
 * TODO: to complete, contains only one part used in the Ui console
 */
export interface PortalSettings {
  portal?: PortalSettingsPortal;
  metadata?: PortalSettingsMetadata;
}

export type PortalSettingsMetadata = Record<string, string[]>;

export interface PortalSettingsPortal {
  analytics?: {
    enabled: boolean;
  };
  apikeyHeader?: string;
  apis?: {
    tilesMode: {
      enabled: boolean;
    };
    categoryMode: {
      enabled: boolean;
    };
    apiHeaderShowTags: {
      enabled: boolean;
    };
    apiHeaderShowCategories: {
      enabled: boolean;
    };
  };
  entrypoint?: string;
  rating?: {
    enabled: boolean;
    comment: {
      mandatory: boolean;
    };
  };
  support?: {
    enabled: boolean;
  };
  uploadMedia?: {
    enabled: boolean;
    maxSizeInOctet: number;
  };
  userCreation?: {
    enabled: boolean;
    automaticValidation: {
      enabled: boolean;
    };
  };
}
