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

export const DEFAULT_HTML_BLOCK_HTML = `<div class="card">
  <h2>Hello World</h2>
  <p>Custom HTML block</p>
  <div data-gravitee-component="api-catalog"></div>
</div>`;

export const DEFAULT_HTML_BLOCK_CSS = `.card {
  padding: 24px;
  border-radius: 12px;
  background: var(--portal-color-surface);
}`;

export const DEFAULT_HTML_PAGE_HTML = `<div class="page">
  <h1>Custom HTML page</h1>
  <p>Author your page with HTML and CSS.</p>
  <div data-gravitee-component="api-catalog"></div>
</div>`;

export const DEFAULT_HTML_PAGE_CSS = `.page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}`;
