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
export function addAnchorLinks(containerSelector: string): void {
  setTimeout(() => {
    const container = document.querySelector(containerSelector);
    if (!container) return;

    const headings = container.querySelectorAll('h1, h2, h3, h4, h5, h6');
    headings.forEach((heading: Element) => {
      if (!heading.hasAttribute('id') && heading.textContent?.trim()) {
        const slug = createSlugFromText(heading.textContent);
        heading.setAttribute('id', slug);
      }
    });

    if (!container.hasAttribute('data-anchor-handler-attached')) {
      container.setAttribute('data-anchor-handler-attached', 'true');
      container.addEventListener('click', (event: MouseEvent) => {
        const target = event.target as HTMLElement;
        if (target.tagName.toLowerCase() === 'a') {
          const href = (target as HTMLAnchorElement).getAttribute('href') ?? '';
          if (href.startsWith('#')) {
            event.preventDefault();
            const anchorId = decodeURIComponent(href.slice(1));
            const anchorEl = container.querySelector(`#${anchorId}, a[name='${anchorId}']`);
            anchorEl?.scrollIntoView({ behavior: 'smooth' });
          }
        }
      });
    }
  }, 1);
}

function createSlugFromText(text: string): string {
  return text
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/gi, '-')
    .replace(/^-+/, '')
    .replace(/-+$/, '');
}
