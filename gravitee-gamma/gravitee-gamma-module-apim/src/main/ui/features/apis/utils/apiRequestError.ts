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

/** Duck-type 403 so Module Federation duplicate `ApimApiError` classes still match. */
export function isForbiddenError(error: unknown): boolean {
    if (typeof error !== 'object' || error === null || !('status' in error)) return false;
    return Number((error as { status: unknown }).status) === 403;
}
