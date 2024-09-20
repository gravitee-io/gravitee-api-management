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
import { Pagination, Links } from '../../entities/management-api-v2';

export interface ApisScoring {
  id: string;
  name: string;
  pictureUrl: string;
  score?: number;
  errors?: number;
  warnings?: number;
  infos?: number;
  hints?: number;
}

export interface ApisScoringResponse {
  data: ApisScoring[];
  pagination?: Pagination;
  links?: Links;
}

export interface ApisScoringOverview {
  errors: number;
  hints: number;
  id: string;
  infos: number;
  warnings: number;
  averageScore: number;
}
