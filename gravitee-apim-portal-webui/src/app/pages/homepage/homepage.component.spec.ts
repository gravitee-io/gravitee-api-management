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
import { Router } from '@angular/router';

import { Api, ApiMetrics, BASE_PATH } from '../../../../projects/portal-webclient-sdk/src/lib';
import { GvPageComponent } from '../../components/gv-page/gv-page.component';
import { ApiStatesPipe } from '../../pipes/api-states.pipe';
import { ApiLabelsPipe } from '../../pipes/api-labels.pipe';
import { MarkdownDescriptionPipe } from '../../pipes/markdown-description.pipe';
import { ConfigurationService } from '../../services/configuration.service';

import { HomepageComponent } from './homepage.component';

describe('HomepageComponent', () => {
  const api: Api = { id: 'api-one', name: 'API one', version: '1', description: 'Description', owner: { id: 'owner' } };

  const createComponent = createComponentFactory({
    component: HomepageComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
<<<<<<< HEAD
    imports: [RouterTestingModule, HttpClientTestingModule],
    declarations: [GvPageComponent, ApiStatesPipe, ApiLabelsPipe],
    providers: [ApiStatesPipe, ApiLabelsPipe],
=======
    imports: [RouterTestingModule, HttpClientTestingModule, GvPageComponent, ApiStatesPipe, ApiLabelsPipe],
    providers: [
      ApiStatesPipe,
      ApiLabelsPipe,
      MarkdownDescriptionPipe,
      { provide: BASE_PATH, useValue: '/portal' },
      {
        provide: ConfigurationService,
        useValue: { get: (key: string, defaultValue?: unknown) => (key === 'baseURL' ? '/portal' : defaultValue) },
      },
    ],
>>>>>>> 76ee0e6 (fix(portal): sanitize API descriptions in search results and homepage)
  });

  let spectator: Spectator<HomepageComponent>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    spectator = createComponent();
    httpMock = spectator.inject(HttpTestingController);
    httpMock.expectOne(request => request.url === '/portal/pages' && request.params.get('homepage') === 'true').flush({ data: [] });
  });

  afterEach(() => {
    httpMock.verify();
  });

  async function loadFeaturedApis(apis: Api[], metrics: ApiMetrics[] = apis.map(() => ({}))) {
    const request = httpMock.expectOne(request => request.url === '/portal/apis');
    expect(request.request.params.get('filter')).toBe('FEATURED');
    expect(request.request.params.get('size')).toBe('9');
    request.flush({ data: apis });
    apis.forEach((featuredApi, index) => {
      httpMock.expectOne(`/portal/apis/${featuredApi.id}/metrics`).flush(metrics[index]);
    });
    await spectator.fixture.whenStable();
    spectator.detectChanges();
  }

  function getCardItems() {
    return spectator.query<HTMLElement & { items: { item: Api; metric: Promise<ApiMetrics> }[] }>('gv-card-list').items;
  }

  function getDescription() {
    const element = document.createElement('div');
    element.innerHTML = getCardItems()[0].item.description;
    return element;
  }

  it('should sanitize HTML and unsafe links before passing descriptions to the top API cards', async () => {
    await loadFeaturedApis([
      {
        ...api,
        description:
          '<p onclick="alert(332)">Description</p><img src="/logo.png" onerror="alert(332)"><script>alert(332)</script>\n\n[Link](javascript:alert(332))',
      },
    ]);

    const description = getDescription();
    expect(description.querySelector('p').textContent).toBe('Description');
    expect(description.querySelector('img').getAttribute('src')).toBe('/logo.png');
    expect(description.querySelector('script, [onclick], [onerror], a')).toBeNull();
    expect(description.querySelector('b').textContent).toBe('Link');
  });

  it('should preserve safe Markdown, images and non-clickable preview links', async () => {
    await loadFeaturedApis([
      {
        ...api,
        description:
          '**Important** and *useful* with `code` and [Docs](/docs)\n\n![Logo](/logo.png)\n\n![Media](/management/organizations/DEFAULT/environments/DEFAULT/portal/media/media-id)',
      },
    ]);

    const description = getDescription();
    expect(description.querySelector('strong').textContent).toBe('Important');
    expect(description.querySelector('em').textContent).toBe('useful');
    expect(description.querySelector('code').textContent).toBe('code');
    expect(description.querySelector('b').textContent).toBe('Docs');
    expect(description.querySelector('a')).toBeNull();
    expect(Array.from(description.querySelectorAll('img'), image => image.getAttribute('src'))).toEqual([
      '/logo.png',
      '/portal/media/media-id',
    ]);
  });

  it.each(['', null, undefined])('should preserve APIs with an absent description (%s)', async description => {
    await loadFeaturedApis([{ ...api, description }]);

    expect(getCardItems()[0].item).toMatchObject({ id: api.id, description });
  });

  it('should preserve API order, metadata, states, labels and associated metrics', async () => {
    const apis: Api[] = [
      { ...api, description: '**First**', running: true, labels: ['first'] },
      { ...api, id: 'api-two', name: 'API two', version: '2', description: '**Second**', draft: true, labels: ['second'] },
    ];
    const metrics: ApiMetrics[] = [
      { subscribers: 3, hits: 10 },
      { subscribers: 7, hits: 20 },
    ];
    await loadFeaturedApis(apis, metrics);

    const items = getCardItems();
    expect(items.map(({ item }) => item)).toEqual([
      expect.objectContaining({
        id: 'api-one',
        name: 'API one',
        version: '1',
        owner: api.owner,
        labels: ['first'],
        states: [{ value: 'running', major: true }],
        description: expect.stringContaining('<strong>First</strong>'),
      }),
      expect.objectContaining({
        id: 'api-two',
        name: 'API two',
        version: '2',
        owner: api.owner,
        labels: ['second'],
        states: [{ value: 'draft' }],
        description: expect.stringContaining('<strong>Second</strong>'),
      }),
    ]);
    await expect(Promise.all(items.map(({ metric }) => metric))).resolves.toEqual(metrics);
  });

  it('should hide top API cards when no featured APIs are returned', async () => {
    await loadFeaturedApis([]);

    expect(spectator.query('gv-card-list')).toBeNull();
    expect(spectator.query('h3')).toBeNull();
  });

  it('should navigate to the API selected from a top API card', async () => {
    await loadFeaturedApis([api]);
    const navigate = jest.spyOn(spectator.inject(Router), 'navigate').mockResolvedValue(true);

    spectator.element.dispatchEvent(new CustomEvent('gv-card-full:click', { detail: Promise.resolve(getCardItems()[0].item) }));
    await spectator.fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith(['/catalog/api/api-one']);
  });
});
