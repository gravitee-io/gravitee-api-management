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

import { Api } from '../../../projects/portal-webclient-sdk/src/lib';
import { ConfigurationService } from '../services/configuration.service';

import { MarkdownDescriptionPipe } from './markdown-description.pipe';

describe('MarkdownDescriptionPipe', () => {
  let pipe: MarkdownDescriptionPipe;

  const api: Api = { id: 'api-id', name: 'API', version: '1', description: '', owner: { id: 'owner-id' } };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [MarkdownDescriptionPipe, { provide: ConfigurationService, useValue: { get: () => '/portal' } }],
    });
    pipe = TestBed.inject(MarkdownDescriptionPipe);
  });

  function renderDescription(description: string): HTMLElement {
    const element = document.createElement('div');
    element.innerHTML = pipe.transform({ ...api, description }).description;
    return element;
  }

  it('should remove scripts and event handlers from HTML', () => {
    const description = renderDescription(
      '<p onclick="void(0)">Description</p><img src="/logo.png" onerror="void(0)"><script>void(0)</script>',
    );

    expect(description.querySelector('p').textContent).toBe('Description');
    expect(description.querySelector('img').getAttribute('src')).toBe('/logo.png');
    expect(description.querySelector('script, [onclick], [onerror]')).toBeNull();
  });

  describe.each([
    { scope: 'portal', path: '/environments/DEFAULT/portal/media/image-id', expectedSrc: '/portal/media/image-id' },
    { scope: 'API', path: '/environments/DEFAULT/apis/api-id/media/image-id', expectedSrc: '/portal/apis/api-id/media/image-id' },
  ])('$scope media images', ({ path, expectedSrc }) => {
    it.each(['alt', 'title'])('should sanitize HTML generated from a Markdown image %s', field => {
      const markdown = field === 'alt' ? `![Logo" onerror="void(0)](${path})` : `![Logo](${path} 'Title" onerror="void(0)')`;
      const description = renderDescription(markdown);

      expect(description.querySelector('img').getAttribute('src')).toBe(expectedSrc);
      expect(description.querySelector('[onerror]')).toBeNull();
    });
  });

  it.each(['javascript:void(0)', 'JaVaScRiPt:void(0)', 'java&#x73;cript:void(0)'])(
    'should replace unsafe links using %s with bold text',
    url => {
      const description = renderDescription(`[Link](${url})`);

      expect(description.querySelector('a')).toBeNull();
      expect(description.querySelector('b').textContent).toBe('Link');
    },
  );

  it('should preserve Markdown formatting and images while replacing links with bold text', () => {
    const description = renderDescription(
      '**Important** and *useful* `code`\n\n[Website](https://example.com) [Section](#section)\n\n![Logo](/logo.png)',
    );

    expect(description.querySelector('strong').textContent).toBe('Important');
    expect(description.querySelector('em').textContent).toBe('useful');
    expect(description.querySelector('code').textContent).toBe('code');
    expect(description.querySelector('a')).toBeNull();
    expect(Array.from(description.querySelectorAll('b'), element => element.textContent)).toEqual(['Website', 'Section']);
    expect(description.querySelector('img').getAttribute('src')).toBe('/logo.png');
    expect(description.querySelector('img').getAttribute('alt')).toBe('Logo');
  });

  it.each([null, undefined])('should preserve an absent API (%s)', value => {
    expect(pipe.transform(value)).toBe(value);
  });

  it.each(['', null, undefined])('should preserve an API without a description (%s)', description => {
    const input = { ...api, description };

    expect(pipe.transform(input)).toBe(input);
  });

  it('should not mutate the API while rendering its description', () => {
    const input = { ...api, description: '**Important**' };
    const output = pipe.transform(input);

    expect(output).not.toBe(input);
    expect(output).toEqual({ ...input, description: expect.stringContaining('<strong>Important</strong>') });
    expect(input.description).toBe('**Important**');
  });
});
