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
/**
 * @deprecated Story G1: replaced by `useEnvironment()` hook from
 * `app/lib/env/EnvironmentContext`. The constant survives as the resolution
 * fallback inside `resolveEnvironmentId` only — production code paths now
 * read the env id from the host-injected provider, not from this literal.
 *
 * Eslint guard (`no-restricted-syntax`) blocks any new `'DEFAULT'` literal in
 * `app/**` so this fallback can't be re-introduced by accident.
 */
export const DEFAULT_ENVIRONMENT_ID = 'DEFAULT';
