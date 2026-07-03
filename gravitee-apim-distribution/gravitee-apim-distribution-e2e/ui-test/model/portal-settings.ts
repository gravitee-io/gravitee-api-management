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
export interface PortalSettings {
  apiReview?: ApiReview;
  apiQualityMetrics?: ApiQualityMetrics;
  portal?: PortalConfiguration;
}

export interface ApiReview {
  enabled: boolean;
}

export interface ApiQualityMetrics {
  enabled: boolean;
  functionalDocumentationWeight: number;
  technicalDocumentationWeight: number;
  descriptionWeight: number;
  descriptionMinLength: number;
  logoWeight: number;
  categoriesWeight: number;
  labelsWeight: number;
  healthcheckWeight: number;
}

export interface PortalConfiguration {
  rating?: Rating;
}

export interface Rating {
  enabled: boolean;
  comment: RatingComment;
}

export interface RatingComment {
  mandatory: boolean;
}
