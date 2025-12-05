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
import {Component, effect, inject, signal} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router} from '@angular/router';
import {firstValueFrom, forkJoin, map, Observable, of, tap} from 'rxjs';

import {PortalNavigationItem} from "../../../entities/portal-navigation/portal-navigation-item";
import {DocumentationFolderComponent} from "./documentation-folder/documentation-folder.component";
import {DocumentationPageComponent} from "./documentation-page/documentation-page.component";
import {PortalNavigationItemsService} from "../../../services/portal-navigation-items.service";

interface DocumentationData {
  navItem: PortalNavigationItem,
  children?: PortalNavigationItem[],
  selectedPageContent?: string,
}

@Component({
  selector: 'app-documentation',
  imports: [DocumentationFolderComponent, DocumentationPageComponent],
  standalone: true,
  template: `
    @let type = documentationData()?.navItem?.type;
    @if (type === 'FOLDER') {
      <app-documentation-folder [navItem]="documentationData()?.navItem!" />
    } @else if (type === 'PAGE') {
      <app-documentation-page/>
    } @else {
      <!--   TODO show a 404?     -->
    }
  `,
  styles: `
    :host {
      display: flex;
      flex: 1 1 100%;
    }
  `
})
export class DocumentationComponent {
  documentationData = toSignal<DocumentationData>(inject(ActivatedRoute).data.pipe(map(data => data['data'])));
}
