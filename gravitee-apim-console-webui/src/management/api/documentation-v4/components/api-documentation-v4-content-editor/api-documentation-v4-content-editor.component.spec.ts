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
import { fakeAsync, tick } from '@angular/core/testing';

import { addAnchorLinks } from '../../../../../util/document.util';

describe('addAnchorLinks (Angular fakeAsync)', () => {
  beforeEach(() => {
    Element.prototype.scrollIntoView = jest.fn();
  });

  it('should add an id to a heading based on its text content', fakeAsync(() => {
    document.body.innerHTML = `
      <div class="markdown-content">
        <h1>My Heading</h1>
        <a href="#my-heading">Jump</a>
      </div>
    `;
    addAnchorLinks('.markdown-content');
    tick(1);

    const heading = document.querySelector('h1');
    expect(heading?.id).toBe('my-heading');
  }));

  it('should not add an id to a heading if it is empty', fakeAsync(() => {
    document.body.innerHTML = `
      <div class="markdown-content">
        <h1></h1>
      </div>
    `;
    addAnchorLinks('.markdown-content');
    tick(1);

    const heading = document.querySelector('h1');
    expect(heading?.id).toBe('');
  }));

  it('should not add an id to a heading if it already has one', fakeAsync(() => {
    document.body.innerHTML = `
      <div class="markdown-content">
        <h2 id="existing-id">Existing Heading</h2>
      </div>
    `;
    addAnchorLinks('.markdown-content');
    tick(1);
    const heading = document.querySelector('h2');
    expect(heading?.id).toBe('existing-id');
  }));

  it('should create a slug-based ID for headings with special characters or spaces', fakeAsync(() => {
    document.body.innerHTML = `
      <div class="markdown-content">
        <h3>Heading with Special #Characters!</h3>
      </div>
    `;
    addAnchorLinks('.markdown-content');
    tick(1);
    const heading = document.querySelector('h3');
    expect(heading?.id).toBe('heading-with-special-characters');
  }));

  it('should handle multiple headings and add unique IDs to each one', fakeAsync(() => {
    document.body.innerHTML = `
      <div class="markdown-content">
        <h1>Heading 1</h1>
        <h2>Heading 2</h2>
        <h3>Heading 3</h3>
      </div>
    `;
    addAnchorLinks('.markdown-content');
    tick(1);
    const h1 = document.querySelector('h1');
    const h2 = document.querySelector('h2');
    const h3 = document.querySelector('h3');
    expect(h1?.id).toBe('heading-1');
    expect(h2?.id).toBe('heading-2');
    expect(h3?.id).toBe('heading-3');
  }));
});
