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

/**
 * Default values for Redoc (OpenAPI viewer) when CSS custom properties are not set.
 * Keep these in sync with scss/theme/variables.scss:
 *   - REDOC_PRIMARY_COLOR_FALLBACK ↔ $primary-main-color-fallback
 *   - REDOC_BREAKPOINT_MEDIUM / REDOC_BREAKPOINT_LARGE ↔ $redoc-breakpoint-medium / $redoc-breakpoint-large
 */
export const REDOC_PRIMARY_COLOR_FALLBACK = '#32329f';
export const REDOC_BREAKPOINT_MEDIUM = '50rem';
export const REDOC_BREAKPOINT_LARGE = '75rem';
