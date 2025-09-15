/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
/*
 * Copyright (C) 2025 The Gravitee team
 */
import { Component, computed, input } from '@angular/core';

import { GraviteeMarkdownViewerModule } from '../../../projects/gravitee-markdown/src/lib/gravitee-markdown-viewer/gravitee-markdown-viewer.module';
import { PortalPage } from '../../entities/portal/portal-page';

@Component({
  selector: 'app-homepage',
  imports: [GraviteeMarkdownViewerModule],
  template: ` <gmd-viewer [content]="homepageContent()" /> `,
})
export class HomepageComponent {
  homepage = input<PortalPage>({
    page: { id: '', name: 'Homepage', pageContent: '', type: 'GRAVITEE_MARKDOWN' },
    viewDetails: {},
  });

  homepageContent = computed(() => this.homepage().page.pageContent ?? '');
}
