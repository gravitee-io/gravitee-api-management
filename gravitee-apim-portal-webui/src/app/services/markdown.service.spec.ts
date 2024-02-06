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
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClient } from '@angular/common/http';
import { RendererObject } from 'marked';

import { MarkdownService } from './markdown.service';

describe('MarkdownService', () => {
  let service: SpectatorService<MarkdownService>;
  let renderer: RendererObject;

  const BASE_URL = 'my-base-url';
  const PAGE_BASE_URL = '/catalog/api/1234/doc';

  const createService = createServiceFactory({
    service: MarkdownService,
    imports: [RouterTestingModule],
    providers: [mockProvider(HttpClient)],
  });

  beforeEach(() => {
    service = createService();
    renderer = service.service.renderer(BASE_URL, PAGE_BASE_URL);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should use correct portal media url', () => {
    const renderedImage = renderer.image(
      'https://host:port/contextpath/management/organizations/DEFAULT/environments/DEFAULT/portal/media/123456789',
      'title',
      'text',
    );

    expect(renderedImage).not.toBeNull();
    expect(renderedImage).toEqual(`<img alt="text" title="title" src="${BASE_URL}/media/123456789" />`);
  });

  it('should use correct api media url', () => {
    const renderedImage = renderer.image(
      'https://host:port/contextpath/management/organizations/DEFAULT/environments/DEFAULT/apis/1A2Z3E4R5T6Y/media/123456789',
      'title',
      'text',
    );

    expect(renderedImage).not.toBeNull();
    expect(renderedImage).toEqual(`<img alt="text" title="title" src="${BASE_URL}/apis/1A2Z3E4R5T6Y/media/123456789" />`);
  });

  it('should use a.internal-link for render an portal page link', () => {
    const renderedLink = renderer.link('/#!/settings/pages/123456789', 'title', 'text');

    expect(renderedLink).not.toBeNull();
    expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=123456789">text</a>');
  });

  it('should use a.internal-link for render an api page link', () => {
    const renderedLink = renderer.link('/#!/apis/1A2Z3E4R5T6Y/documentation/123456789', 'title', 'text');

    expect(renderedLink).not.toBeNull();
    expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=123456789">text</a>');
  });

  it('should use a.anchor for render an anchor', () => {
    const renderedLink = renderer.link('#anchor', 'Anchor', '');

    expect(renderedLink).not.toBeNull();
    expect(renderedLink).toEqual('<a class="anchor" href="#anchor"></a>');
  });
});
