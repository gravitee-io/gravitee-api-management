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

import { PaginationComponent } from '../../../../components/pagination/pagination.component';
import { RadioCardComponent } from '../../../../components/radio-card/radio-card.component';
import { MobileClassDirective } from '../../../../directives/mobile-class.directive';
import { NarrowClassDirective } from '../../../../directives/narrow-class.directive';
import { Application } from '../../../../entities/application/application';
import { ApplicationVM } from '../subscribe-to-api.component';

export const APPLICATIONS_PAGE_SIZE_OPTIONS: number[] = [6, 12, 24, 48, 96];
export const DEFAULT_APPLICATIONS_PAGE_SIZE = 6;

export interface ApplicationsPagination {
  currentPage: number;
  totalApplications: number;
  pageSize: number;
}

@Component({
  selector: 'app-subscribe-to-api-choose-application',
  imports: [RadioCardComponent, PaginationComponent, MobileClassDirective, NarrowClassDirective],
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

  @Output()
  selectApplication = new EventEmitter<ApplicationVM>();

  @Output()
  pageChange = new EventEmitter<number>();

  @Output()
  pageSizeChange = new EventEmitter<number>();

  protected readonly pageSizeOptions = APPLICATIONS_PAGE_SIZE_OPTIONS;
}
