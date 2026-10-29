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
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Data, Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { EMPTY, Subject } from 'rxjs';

import { BASE_PATH, Page } from '../../../../../projects/portal-webclient-sdk/src/lib';

import { ApiGeneralComponent } from './api-general.component';

describe('ApiGeneralComponent', () => {
  const createComponent = createComponentFactory({
    component: ApiGeneralComponent,
    imports: [HttpClientTestingModule, RouterTestingModule, FormsModule, ReactiveFormsModule],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
  });

  let spectator: Spectator<ApiGeneralComponent>;
  let component: ApiGeneralComponent;
  let router: Router;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
    component.apiHomepage = null;
    router = TestBed.inject(Router);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('goToSearch', () => {
    it('should navigateByUrl with built urlTree', () => {
      const navigateByUrlSpy = jest.spyOn(router, 'navigateByUrl');

      const myTestUrlTree = {};
      jest.spyOn(component as any, 'getSearchUrlTree').mockReturnValue(myTestUrlTree);

      component.goToSearch('labels', 'TheLabel');

      expect(navigateByUrlSpy).toHaveBeenCalledWith(myTestUrlTree);
    });
  });

  describe('getSearchUrl', () => {
    it('should return the search url', () => {
      const resultUrl = component.getSearchUrl('labels', 'myTestValue');

      expect(resultUrl).toBe('/catalog/search?q=labels:%22myTestValue%22');
    });
  });
});

describe.each([
  { layout: 'main', apiHomepage: null },
  { layout: 'aside', apiHomepage: { id: 'homepage', name: 'Homepage', type: Page.TypeEnum.LINK, order: 0 } },
])('ApiGeneralComponent description in $layout', ({ layout, apiHomepage }) => {
  let routeData: Subject<Data>;
  let spectator: Spectator<ApiGeneralComponent>;
  let httpTestingController: HttpTestingController;

  const createComponent = createComponentFactory({
    component: ApiGeneralComponent,
    imports: [HttpClientTestingModule, RouterTestingModule, FormsModule, ReactiveFormsModule],
    providers: [
      { provide: BASE_PATH, useValue: '/portal' },
      { provide: ActivatedRoute, useFactory: () => ({ data: routeData, params: EMPTY }) },
    ],
  });

  beforeEach(() => {
    routeData = new Subject<Data>();
    spectator = createComponent();
    httpTestingController = spectator.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function renderDescription(description: string, pagesRequestFails = false): HTMLElement {
    routeData.next({
      api: { id: 'api-id', description },
      apiHomepage,
      permissions: {},
      apiInformations: [],
    });
    const pagesRequest = httpTestingController.expectOne(request => request.url === '/portal/apis/api-id/pages');
    if (pagesRequestFails) {
      pagesRequest.flush(null, { status: 500, statusText: 'Internal Server Error' });
    } else {
      pagesRequest.flush({ data: [] });
    }
    spectator.detectChanges();

    const element = spectator.query<HTMLElement>(`.${layout} [data-testid="api-description"]`);
    expect(element).not.toBeNull();
    return element;
  }

  it.each(['javascript:void(0)', 'JaVaScRiPt:void(0)', 'java&#x73;cript:void(0)'])(
    'should prevent executable Markdown links using %s',
    url => {
      const description = renderDescription(`[link](${url})`);
      const link = description.querySelector('a');

      expect(link).not.toBeNull();
      expect(link.textContent).toBe('link');
      expect(link.protocol).not.toBe('javascript:');
    },
  );

  it.each([
    { status: 'success', pagesRequestFails: false },
    { status: 'failure', pagesRequestFails: true },
  ])('should sanitize raw HTML after a pages request $status', ({ pagesRequestFails }) => {
    const description = renderDescription(
      '<a href="javascript:void(0)" onclick="void(0)">link</a><img src="/logo.png" onerror="void(0)"><script>void(0)</script>',
      pagesRequestFails,
    );

    expect(description.querySelector('a').protocol).not.toBe('javascript:');
    expect(description.querySelector('a').textContent).toBe('link');
    expect(description.querySelector('img').getAttribute('src')).toBe('/logo.png');
    expect(description.querySelector('[onclick], [onerror], script')).toBeNull();
  });

  it('should preserve Markdown formatting, safe link destinations and images', () => {
    const description = renderDescription(
      '**Important** and *useful* `code`\n\n' +
        '[Website](https://example.com) [Documentation](/#!/apis/api-id/documentation/doc-id) [Section](#section)\n\n' +
        '![Logo](https://example.com/logo.png)',
    );

    expect(description.querySelector('strong').textContent).toBe('Important');
    expect(description.querySelector('em').textContent).toBe('useful');
    expect(description.querySelector('code').textContent).toBe('code');
    expect(Array.from(description.querySelectorAll('a'), link => link.getAttribute('href'))).toEqual([
      'https://example.com',
      '/catalog/api/api-id/doc?page=doc-id',
      '#section',
    ]);
    expect(description.querySelector('img').getAttribute('src')).toBe('https://example.com/logo.png');
    expect(description.querySelector('img').getAttribute('alt')).toBe('Logo');
  });

  it('should preserve heading text and fragment links without retaining heading IDs', () => {
    const description = renderDescription('## Setup\n\n[Go to setup](#setup)');

    expect(description.querySelector('h2').textContent).toBe('Setup');
    expect(description.querySelector('h2').hasAttribute('id')).toBe(false);
    expect(description.querySelector('a').getAttribute('href')).toBe('#setup');
    expect(description.querySelector('#setup')).toBeNull();
  });

  it('should preserve Markdown alert content and classes without custom icons or inline styles', () => {
    const description = renderDescription('> [!NOTE]\n> Check configuration.');
    const note = description.querySelector('.markdown-alert.markdown-alert-note');

    expect(note).not.toBeNull();
    expect(note.querySelector('.markdown-alert-title').textContent.trim()).toBe('Note');
    expect(note.textContent).toContain('Check configuration.');
    expect(note.querySelector('gv-icon, [style]')).toBeNull();
  });
});
