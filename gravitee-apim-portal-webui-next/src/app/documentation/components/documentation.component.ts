/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, computed, effect, inject, input } from '@angular/core';
import { Router } from '@angular/router';

import { DocumentationFolderComponent } from './documentation-folder/documentation-folder.component';
import { NavigationPageFullWidthComponent } from '../../../components/navigation-page-full-width/navigation-page-full-width.component';
import { PortalNavigationItem } from '../../../entities/portal-navigation/portal-navigation-item';

@Component({
  selector: 'app-documentation',
  imports: [DocumentationFolderComponent, NavigationPageFullWidthComponent],
  template: `
    @if (isItemFolder()) {
      <app-documentation-folder [navItem]="navItem()!" />
    } @else if (isItemPage()) {
      <app-navigation-page-full-width [navItem]="navItem()!" />
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
  readonly router = inject(Router);
  
  navItem = input.required<PortalNavigationItem>();
  isItemFolder = computed(() => this.navItem()?.type === 'FOLDER');
  isItemPage = computed(() => this.navItem()?.type === 'PAGE');

  constructor() {
    effect(() => {
      const itemType = this.navItem()?.type;
      if (itemType !== 'FOLDER' && itemType !== 'PAGE') {
        this.router.navigate(['/404']);
      }
    });
  }
}
