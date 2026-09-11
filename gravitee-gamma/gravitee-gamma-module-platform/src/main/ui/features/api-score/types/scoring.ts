/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** GET /v2/environments/{env}/scoring/overview */
export interface EnvironmentScoringOverview {
    id: string;
    score: number | null;
    errors: number | null;
    warnings: number | null;
    infos: number | null;
    hints: number | null;
}

/** One row of GET /v2/environments/{env}/scoring/apis */
export interface EnvironmentApiScore {
    id: string;
    name: string;
    pictureUrl?: string;
    type?: string;
    score?: number | null;
    errors?: number | null;
    warnings?: number | null;
    infos?: number | null;
    hints?: number | null;
}

export interface ScoringPagination {
    page: number;
    perPage: number;
    pageCount: number;
    pageItemsCount: number;
    totalCount: number;
}

export interface EnvironmentApisScoringResponse {
    data: EnvironmentApiScore[];
    pagination: ScoringPagination;
}
