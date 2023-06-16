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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { SafePipe } from '../../pipes/safe.pipe';
import { Page } from '../../../../projects/portal-webclient-sdk/src/lib';
import { PageService } from '../../services/page.service';

import { GvPageAsciiDocComponent } from './gv-page-asciidoc.component';

describe('GvPageAsciiDocComponent', () => {
  const createComponent = createComponentFactory({
    component: GvPageAsciiDocComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    declarations: [SafePipe],
    imports: [HttpClientTestingModule],
    providers: [
      mockProvider(PageService, {
        getCurrentPage: () => docPage,
      }),
    ],
  });

  let spectator: Spectator<GvPageAsciiDocComponent>;
  let component: GvPageAsciiDocComponent;
  const docPage: Page = { name: 'A Page', id: '86de4f08-aa02-40f0-aa73-4b3e0e97fef4', content: '', type: 'ASCIIDOC', order: 1 };

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should sanitize Asciidoc with JavaScript in it', async () => {
    docPage.content = '```test"><img src=x onerror=alert(1)></img>';
    await component.ngOnInit();
    expect(component.pageContent).toEqual(
      '<div class="listingblock">&#10;<div class="content">&#10;<pre class="highlight"><code class="language-test"><img src="x">&#34; data-lang=&#34;test&#34;&gt;<img src="x">&#34;&gt;</code></pre>&#10;</div>&#10;</div>',
    );
  });
});
