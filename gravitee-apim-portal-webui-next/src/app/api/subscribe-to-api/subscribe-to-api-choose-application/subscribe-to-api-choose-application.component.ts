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
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { PaginationComponent } from '../../../../components/pagination/pagination.component';
import { PictureComponent } from '../../../../components/picture/picture.component';
import { SearchBarComponent } from '../../../../components/search-bar/search-bar.component';
import { Application } from '../../../../entities/application/application';

export interface ApplicationVM extends Application {
  disabled?: boolean;
  disabledMessage?: string;
}

export const APPLICATIONS_PAGE_SIZE_OPTIONS: number[] = [10, 15, 20, 25];
export const DEFAULT_APPLICATIONS_PAGE_SIZE = 10;

export interface ApplicationsPagination {
  currentPage: number;
  totalApplications: number;
  pageSize: number;
}

@Component({
  selector: 'app-subscribe-to-api-choose-application',
  imports: [
    MatButtonModule,
    MatIconModule,
    MatRadioModule,
    MatTableModule,
    MatTooltipModule,
    PaginationComponent,
    PictureComponent,
    SearchBarComponent,
  ],
  templateUrl: './subscribe-to-api-choose-application.component.html',
  styleUrl: './subscribe-to-api-choose-application.component.scss',
})
export class SubscribeToApiChooseApplicationComponent {
  @Input()
  applications: ApplicationVM[] = [];

  @Input()
  selectedApplication?: Application;

  @Input()
  pagination: ApplicationsPagination = { currentPage: 0, totalApplications: 0, pageSize: DEFAULT_APPLICATIONS_PAGE_SIZE };

  @Input()
  searchQuery = '';

  @Input()
  canCreateApplication = false;

  @Output()
  selectApplication = new EventEmitter<ApplicationVM>();

  @Output()
  pageChange = new EventEmitter<number>();

  @Output()
  pageSizeChange = new EventEmitter<number>();

  @Output()
  searchChange = new EventEmitter<string>();

  @Output()
  createApplication = new EventEmitter<void>();

  protected readonly pageSizeOptions = APPLICATIONS_PAGE_SIZE_OPTIONS;
  protected readonly tableColumns = ['select', 'name', 'description'] as const;

  protected selectRow(application: ApplicationVM): void {
    if (application.disabled) {
      return;
    }
    this.selectApplication.emit(application);
  }
}
