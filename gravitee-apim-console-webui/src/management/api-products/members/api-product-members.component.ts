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
import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { GioAvatarModule, GioFormSlideToggleModule, GioIconsModule } from '@gravitee/ui-particles-angular';

import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

@Component({
  selector: 'api-product-members',
  templateUrl: './api-product-members.component.html',
  styleUrls: ['./api-product-members.component.scss'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatTableModule,
    GioIconsModule,
    GioAvatarModule,
    GioFormSlideToggleModule,
    GioTableWrapperModule,
  ],
})
export class ApiProductMembersComponent {
  protected readonly displayedColumns = ['picture', 'displayName', 'role'] as const;

  protected readonly notifyForm = new FormGroup({
    isNotificationsEnabled: new FormControl(true),
  });

  protected readonly filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };

  protected membersTableDSUnpaginatedLength = 0;

  protected onFiltersChanged(_filters: GioTableWrapperFilters): void {}
}
