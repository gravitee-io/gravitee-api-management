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
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ActivatedRoute, convertToParamMap, ParamMap, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BehaviorSubject } from 'rxjs';

import { Api } from '../../../../../projects/portal-webclient-sdk/src/lib';
import { TranslateTestingModule } from '../../../test/translate-testing-module';
import { NotificationService } from '../../../services/notification.service';
import { ConfigurationService } from '../../../services/configuration.service';
import { ApiStatesPipe } from '../../../pipes/api-states.pipe';
import { ApiLabelsPipe } from '../../../pipes/api-labels.pipe';
import { MarkdownDescriptionPipe } from '../../../pipes/markdown-description.pipe';

import { CatalogSearchComponent } from './catalog-search.component';

describe('CatalogSearchComponent', () => {
  const api: Api = { id: 'api-id', name: 'API', description: 'Description', version: '1', owner: { id: 'owner-id' } };
  const config: Record<string, unknown> = { baseURL: '/portal', 'pagination.size.default': 3, 'pagination.size.values': [3, 5] };
  const createComponent = createComponentFactory({
    component: CatalogSearchComponent,
    imports: [RouterTestingModule, TranslateTestingModule, FormsModule, ReactiveFormsModule, HttpClientTestingModule],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
<<<<<<< HEAD
    declarations: [ApiStatesPipe, ApiLabelsPipe],
    providers: [mockProvider(NotificationService), mockProvider(ConfigurationService)],
=======
    providers: [
      MarkdownDescriptionPipe,
      mockProvider(NotificationService),
      mockProvider(ConfigurationService, { get: (key: string, defaultValue?: unknown) => config[key] ?? defaultValue }),
    ],
>>>>>>> 76ee0e6 (fix(portal): sanitize API descriptions in search results and homepage)
  });

  let spectator: Spectator<CatalogSearchComponent>;
  let httpMock: HttpTestingController;
  let queryParams: BehaviorSubject<ParamMap>;

  beforeEach(() => {
    queryParams = new BehaviorSubject(convertToParamMap({ q: 'sample' }));
    spectator = createComponent({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { queryParamMap: queryParams.asObservable(), snapshot: { queryParamMap: queryParams.value } },
        },
      ],
    });
    httpMock = spectator.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    queryParams.complete();
  });

  function expectSearchRequest(page = 1) {
    const request = httpMock.expectOne(request => request.url.endsWith('/apis/_search'));
    expect(request.request.method).toBe('POST');
    expect(request.request.params.get('q')).toBe('sample');
    expect(request.request.params.get('page')).toBe(String(page));
    expect(request.request.params.get('size')).toBe('3');
    return request;
  }

  async function displayedApis(): Promise<(Api | undefined)[]> {
    await spectator.fixture.whenStable();
    spectator.detectChanges();
    const rows = spectator.queryAll<HTMLElement & { item: Promise<Api | undefined> }>('gv-row');
    return Promise.all(rows.map(row => row.item));
  }

  async function flushResults(apis: Api[], page = 1, total = apis.length) {
    expectSearchRequest(page).flush({ data: apis, metadata: { pagination: { page, size: 3, total } } });
    return displayedApis();
  }

  function descriptionElement(description: string): HTMLElement {
    const element = document.createElement('div');
    element.innerHTML = description;
    return element;
  }

  it('should sanitize every result before passing it to the search rows', async () => {
    const results = await flushResults([
      { ...api, description: '<p onclick="void(0)">Description</p><img src="/logo.png" onerror="void(0)"><script>void(0)</script>' },
      { ...api, id: 'other-api', description: '[Unsafe link](javascript:void(0))' },
    ]);

    expect(results.map(result => result?.id)).toEqual(['api-id', 'other-api', undefined]);
    const firstDescription = descriptionElement(results[0].description);
    expect(firstDescription.querySelector('script, [onclick], [onerror]')).toBeNull();
    expect(firstDescription.querySelector('img').getAttribute('src')).toBe('/logo.png');
    expect(firstDescription.querySelector('p').textContent).toBe('Description');
    const secondDescription = descriptionElement(results[1].description);
    expect(secondDescription.querySelector('a')).toBeNull();
    expect(secondDescription.querySelector('b').textContent).toBe('Unsafe link');
  });

  it('should preserve formatting, API metadata, pagination and navigation', async () => {
    const navigate = jest.spyOn(spectator.inject(Router), 'navigate').mockResolvedValue(true);
    const results = await flushResults(
      [{ ...api, description: '**Bold** and *emphasis* `code`\n\n[Docs](/docs)\n\n![Logo](/logo.png)' }],
      1,
      7,
    );
    const description = descriptionElement(results[0].description);

    expect(description.querySelector('strong').textContent).toBe('Bold');
    expect(description.querySelector('em').textContent).toBe('emphasis');
    expect(description.querySelector('code').textContent).toBe('code');
    expect(description.querySelector('img').getAttribute('src')).toBe('/logo.png');
    expect(description.querySelector('a')).toBeNull();
    expect(description.querySelector('b').textContent).toBe('Docs');
    expect(results[0]).toEqual({ ...api, description: results[0].description });
    expect(spectator.query<HTMLElement & { data: { total: number; size: number } }>('gv-pagination').data).toMatchObject({
      total: 7,
      size: 3,
    });

    spectator.click('gv-row');
    await spectator.fixture.whenStable();
    expect(navigate).toHaveBeenCalledWith(['/catalog/api/api-id'], { queryParamsHandling: 'merge' });
  });

  it('should sanitize results loaded on subsequent pages', async () => {
    await flushResults([api], 1, 4);

    queryParams.next(convertToParamMap({ q: 'sample', page: '2' }));
    const results = await flushResults([{ ...api, id: 'second-page-api', description: '<img src="/logo.png" onerror="void(0)">' }], 2, 4);

    expect(results.map(result => result?.id)).toEqual(['second-page-api', undefined, undefined]);
    expect(descriptionElement(results[0].description).querySelector('[onerror]')).toBeNull();
  });

  it('should preserve results with empty or absent descriptions', async () => {
    const descriptions = ['', null, undefined];
    const results = await flushResults(descriptions.map((description, index) => ({ ...api, id: `api-${index}`, description })));

    expect(results.map(result => result.description)).toEqual(descriptions);
  });

  it('should clear previous results when the next search returns no APIs', async () => {
    await flushResults([api]);

    queryParams.next(convertToParamMap({ q: 'sample', page: '2' }));
    const results = await flushResults([], 2);

    expect(results).toEqual([undefined, undefined, undefined]);
    expect(spectator.query<HTMLElement & { data: { total: number } }>('gv-pagination').data.total).toBe(0);
  });

  it('should resolve empty rows when the search request fails', async () => {
    expectSearchRequest().flush({}, { status: 500, statusText: 'Server error' });

    expect(await displayedApis()).toEqual([undefined, undefined, undefined]);
  });
});
