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
import {AsyncPipe, NgIf} from '@angular/common';
import {Component, DestroyRef, inject, input, InputSignal, OnInit} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import {firstValueFrom, Observable, of, tap} from 'rxjs';
import { BreadcrumbService } from 'xng-breadcrumb';

import { LoaderComponent } from '../../../components/loader/loader.component';
import { PageComponent } from '../../../components/page/page.component';
import { Page } from '../../../entities/page/page';
import { PageService } from '../../../services/page.service';
import {PortalNavigationItemsService} from "../../../services/portal-navigation-items.service";
import {PortalNavigationItem} from "../../../entities/portal-navigation/portal-navigation";
import {DocumentationFolderComponent} from "./documentation-folder/documentation-folder.component";

@Component({
  selector: 'app-documentation-item',
  imports: [LoaderComponent, DocumentationFolderComponent, NgIf],
  standalone: true,
  template: `
      @if (selectedItem?.type === 'FOLDER') {
        <app-documentation-folder />
      } @else if (selectedItem?.type === 'PAGE') {
<!--        <app-documentation-page />-->
      } @else {
        <app-loader/>
      }
  `,
  styles: `
    :host {
      display: flex;
      flex: 1 1 100%;
    }
  `
})
export class DocumentationItemComponent {
  selectedItem = inject(PortalNavigationItemsService).topNavbarItems().find(item => item.id === inject(ActivatedRoute).snapshot.data['data']['navId']);
}
