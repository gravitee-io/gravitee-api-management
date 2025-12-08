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
import { Component, computed, effect, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';

import { DocumentationFolderComponent } from './documentation-folder/documentation-folder.component';
import { DocumentationPageComponent } from './documentation-page/documentation-page.component';
import { PortalNavigationItem } from '../../../entities/portal-navigation/portal-navigation-item';

export interface DocumentationData {
  navItem: PortalNavigationItem;
}

@Component({
  selector: 'app-documentation',
  imports: [DocumentationFolderComponent, DocumentationPageComponent],
  standalone: true,
  template: `
    @if (isItemFolder()) {
      <app-documentation-folder [navItem]="navItem()!" />
    } @else {
      <app-documentation-page />
    }
  `,
  styles: `
    :host {
      display: flex;
      flex: 1 1 100%;
    }
  `,
})
export class DocumentationComponent {
  navItem = toSignal<PortalNavigationItem>(inject(ActivatedRoute).data.pipe(map(({ data }) => data?.navItem)));
  isItemFolder = computed(() => this.navItem()?.type === 'FOLDER');
  constructor(private router: Router) {
    effect(() => {
      const itemType = this.navItem()?.type;
      if (itemType !== 'FOLDER' && itemType !== 'PAGE') {
        this.router.navigate(['/404']);
      }
    });
  }
}
