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

import { addAnchorLinks } from '../../../util/document.util';

describe('addAnchorLinks', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('should add an id to the heading based on its text content (Positive test case)', fakeAsync(() => {
    document.body.innerHTML = `
      <div class="toastui-editor-contents">
        <h1>Heading 1</h1>
      </div>
    `;
    addAnchorLinks('.toastui-editor-contents');
    tick(1);
    const h1 = document.querySelector('h1');
    expect(h1?.id).toBe('heading-1');
  }));

  it('should not add an id if the heading is empty (Negative test case)', fakeAsync(() => {
    document.body.innerHTML = `
      <div class="toastui-editor-contents">
        <h1></h1>
      </div>
    `;
    addAnchorLinks('.toastui-editor-contents');
    tick(1);
    const h1 = document.querySelector('h1');
    expect(h1?.id).toBe('');
  }));
});
