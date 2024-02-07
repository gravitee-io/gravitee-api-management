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

import { Page } from '../../../projects/portal-webclient-sdk/src/lib';

import { MarkdownService } from './markdown.service';

describe('MarkdownService', () => {
  let service: SpectatorService<MarkdownService>;
  let renderer: RendererObject;

  const BASE_URL = 'my-base-url';
  const PAGE_BASE_URL = '/catalog/api/1234/doc';
  const PAGES: Page[] = [
    {
      id: '123456789',
      name: 'myPage',
      type: 'MARKDOWN',
      order: 0,
    },
    {
      id: '22',
      name: 'myPage',
      type: 'SWAGGER',
      order: 1,
    },
    {
      id: '33',
      name: 'My Page',
      type: 'MARKDOWN',
      order: 2,
    },
    {
      id: 'parent-id',
      name: 'parent',
      type: 'FOLDER',
      order: 3,
    },
    {
      id: '44',
      name: 'myPage',
      type: 'MARKDOWN',
      order: 0,
      parent: 'parent-id',
    },
    {
      id: 'grand-parent-id',
      name: 'Grand parent',
      type: 'FOLDER',
      order: 0,
    },
    {
      id: 'my-parent-id',
      name: 'my Parent',
      type: 'FOLDER',
      order: 0,
      parent: 'grand-parent-id',
    },
    {
      id: '55',
      name: 'mY pAgE',
      type: 'MARKDOWN',
      order: 0,
      parent: 'my-parent-id',
    },
    {
      id: '66',
      name: 'my#$%^&*(){}?>.\\|éàêcrazy@ page',
      type: 'MARKDOWN',
      order: 10,
    },
  ];

  const createService = createServiceFactory({
    service: MarkdownService,
    imports: [RouterTestingModule],
    providers: [mockProvider(HttpClient)],
  });

  beforeEach(() => {
    service = createService();
    renderer = service.service.renderer(BASE_URL, PAGE_BASE_URL, PAGES);
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

  describe('Relative link -- /#!/documentation', () => {
    describe('within Api scope -- /api', () => {
      it('should find page by its name', () => {
        const renderedLink = renderer.link('/#!/documentation/api/myPage#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=123456789">text</a>');
      });

      it('should find page by its name and file type', () => {
        const renderedLink = renderer.link('/#!/documentation/api/myPage#SWAGGER', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=22">text</a>');
      });

      it('should find page by its name with spaces', () => {
        const renderedLink = renderer.link('/#!/documentation/api/my%20Page#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=33">text</a>');
      });

      it('should find page by its name and path', () => {
        const renderedLink = renderer.link('/#!/documentation/api/parent/myPage#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=44">text</a>');
      });

      it('should find page with multi layer path with spaces', () => {
        const renderedLink = renderer.link('/#!/documentation/api/grand%20parent/my%20parent/my%20page#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=55">text</a>');
      });

      it('should find page with symbols its name', () => {
        const renderedLink = renderer.link('/#!/documentation/api/my#$%^&*(){}?>.\\|éàêcrazy@%20page#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=66">text</a>');
      });

      it('should return link with file name even if not found', () => {
        const renderedLink = renderer.link('/#!/documentation/api/doesNotExist#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=doesNotExist">text</a>');
      });

      it('should return link with file name even if parent id invalid', () => {
        const renderedLink = renderer.link('/#!/documentation/api/doesNotExist/myPage#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/doc?page=myPage">text</a>');
      });

      it.each(['myPage#typeDoesNotExist', 'myPage', 'myPage#', '#MARKDOWN', '#', 'parent#FOLDER'])(
        'Bad format path: %s -- should return original link',
        async (path: string) => {
          const link = `/#!/documentation/api/${path}`;
          const renderedLink = renderer.link(link, 'title', 'text');

          expect(renderedLink).not.toBeNull();
          expect(renderedLink).toEqual(`<a href="${link}" title="title">text</a>`);
        },
      );
    });
  });
});
