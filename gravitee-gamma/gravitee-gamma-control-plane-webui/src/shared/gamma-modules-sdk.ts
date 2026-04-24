/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
 * Host barrel for `@gravitee/gamma-modules-sdk` — replaces the package stubs
 * with the real implementation via rspack alias + MF singleton.
 *
 * Only domains that need host override belong here (e.g. permissions).
 * Standalone domains like routing live on their own subpath
 * (`@gravitee/gamma-modules-sdk/routing`) and are NOT re-exported here.
 */
export * from './permissions/index';
