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
import { TestBed } from '@angular/core/testing';
import { RouterModule } from '@angular/router';
import { RendererObject } from 'marked';

import { MarkdownService } from './markdown.service';
import { Page } from '../entities/page/page';
import { AppTestingModule } from '../testing/app-testing.module';

describe('MarkdownService', () => {
  let service: MarkdownService;
  let renderer: RendererObject;

  const BASE_URL = 'my-base-url';
  const PAGE_BASE_URL = '/catalog/api/1234/documentation';
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

  const init = (pageBaseUrl = PAGE_BASE_URL) => {
    TestBed.configureTestingModule({
      imports: [RouterModule.forRoot([]), AppTestingModule],
    });
    service = TestBed.inject(MarkdownService);
    renderer = service.renderer(BASE_URL, pageBaseUrl, PAGES);
  };

  describe('Legacy support', () => {
    beforeEach(() => {
      init();
    });

    it('should use correct portal media url', () => {
      // @ts-expect-error possibly undefined
      const renderedImage = renderer.image(
        'https://host:port/contextpath/management/organizations/DEFAULT/environments/DEFAULT/portal/media/123456789',
        'title',
        'text',
      );

      expect(renderedImage).toBeDefined();
      expect(renderedImage).toEqual(`<img alt="text" title="title" src="${BASE_URL}/media/123456789" />`);
    });

    it('should use correct api media url', () => {
      // @ts-expect-error possibly undefined
      const renderedImage = renderer.image(
        'https://host:port/contextpath/management/organizations/DEFAULT/environments/DEFAULT/apis/1A2Z3E4R5T6Y/media/123456789',
        'title',
        'text',
      );

      expect(renderedImage).not.toBeNull();
      expect(renderedImage).toEqual(`<img alt="text" title="title" src="${BASE_URL}/apis/1A2Z3E4R5T6Y/media/123456789" />`);
    });

    it('should use a.internal-link for render an portal page link', () => {
      // @ts-expect-error possibly undefined
      const renderedLink = renderer.link('/#!/settings/pages/123456789', 'title', 'text');

      expect(renderedLink).not.toBeNull();
      expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/documentation?page=123456789">text</a>');
    });

    it('should use a.internal-link for render an api page link', () => {
      // @ts-expect-error possibly undefined
      const renderedLink = renderer.link('/#!/apis/1A2Z3E4R5T6Y/documentation/123456789', 'title', 'text');

      expect(renderedLink).not.toBeNull();
      expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/documentation?page=123456789">text</a>');
    });

    it('should use a.anchor for render an anchor', () => {
      // @ts-expect-error possibly undefined
      const renderedLink = renderer.link('#anchor', 'Anchor', '');

      expect(renderedLink).not.toBeNull();
      expect(renderedLink).toEqual('<a class="anchor" href="#anchor"></a>');
    });
  });

  describe('Relative link -- /#!/documentation', () => {
    describe('within Api scope -- /api', () => {
      beforeEach(() => {
        init();
      });
      it('should find page by its name', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/api/myPage#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/documentation?page=123456789">text</a>');
      });

      it('should find page by its name and file type', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/api/myPage#SWAGGER', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/documentation?page=22">text</a>');
      });

      it('should find SWAGGER page by OPENAPI file reference', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/api/myPage#OPENAPI', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/documentation?page=22">text</a>');
      });

      it('should find page by its name with spaces', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/api/my%20Page#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/documentation?page=33">text</a>');
      });

      it('should find page by its name and path', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/api/parent/myPage#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/documentation?page=44">text</a>');
      });

      it('should find page with multi layer path with spaces', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/api/grand%20parent/my%20parent/my%20page#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/documentation?page=55">text</a>');
      });

      it('should find page with symbols its name', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/api/my#$%^&*(){}?>.\\|éàêcrazy@%20page#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/documentation?page=66">text</a>');
      });

      it('should return link with file name even if not found', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/api/doesNotExist#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/documentation?page=doesNotExist">text</a>');
      });

      it('should return link with file name even if parent id invalid', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/api/doesNotExist/myPage#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/catalog/api/1234/documentation?page=myPage">text</a>');
      });

      it.each(['myPage#typeDoesNotExist', 'myPage', 'myPage#', '#MARKDOWN', '#', 'parent#FOLDER'])(
        'Bad format path: %s -- should return original link',
        async (path: string) => {
          const link = `/documentation/api/${path}`;
          // @ts-expect-error possibly undefined
          const renderedLink = renderer.link(link, 'title', 'text');

          expect(renderedLink).not.toBeNull();
          expect(renderedLink).toEqual(`<a href="${link}" title="title">text</a>`);
        },
      );
    });

    describe('within Guides scope -- /guides', () => {
      beforeEach(() => {
        init('/guides');
      });
      it('should find page by its name', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/environment/myPage#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/guides?page=123456789">text</a>');
      });

      it('should find page by its name and file type', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/environment/myPage#SWAGGER', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/guides?page=22">text</a>');
      });

      it('should find SWAGGER page by OPENAPI file reference', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/environment/myPage#OPENAPI', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/guides?page=22">text</a>');
      });

      it('should find page by its name with spaces', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/environment/my%20Page#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/guides?page=33">text</a>');
      });

      it('should find page by its name and path', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/environment/parent/myPage#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/guides?page=44">text</a>');
      });

      it('should find page with multi layer path with spaces', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/environment/grand%20parent/my%20parent/my%20page#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/guides?page=55">text</a>');
      });

      it('should find page with symbols its name', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/environment/my#$%^&*(){}?>.\\|éàêcrazy@%20page#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/guides?page=66">text</a>');
      });

      it('should return link with file name even if not found', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/environment/doesNotExist#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/guides?page=doesNotExist">text</a>');
      });

      it('should return link with file name even if parent id invalid', () => {
        // @ts-expect-error possibly undefined
        const renderedLink = renderer.link('/#!/documentation/environment/doesNotExist/myPage#MARKDOWN', 'title', 'text');

        expect(renderedLink).not.toBeNull();
        expect(renderedLink).toEqual('<a class="internal-link" href="/guides?page=myPage">text</a>');
      });

      it.each(['myPage#typeDoesNotExist', 'myPage', 'myPage#', '#MARKDOWN', '#', 'parent#FOLDER'])(
        'Bad format path: %s -- should return original link',
        async (path: string) => {
          const link = `/documentation/guides/${path}`;
          // @ts-expect-error possibly undefined
          const renderedLink = renderer.link(link, 'title', 'text');

          expect(renderedLink).not.toBeNull();
          expect(renderedLink).toEqual(`<a href="${link}" title="title">text</a>`);
        },
      );
    });
  });
});
