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
import { NgClass } from '@angular/common';
import { Component, EventEmitter, inject, Input, Output } from '@angular/core';

import { PaginationComponent } from '../../../../components/pagination/pagination.component';
import { RadioCardComponent } from '../../../../components/radio-card/radio-card.component';
import { Application } from '../../../../entities/application/application';
import { ObservabilityBreakpointService } from '../../../../services/observability-breakpoint.service';
import { ApplicationVM } from '../subscribe-to-api.component';

export interface ApplicationsPagination {
  currentPage: number;
  totalApplications: number;
}

@Component({
  selector: 'app-subscribe-to-api-choose-application',
  imports: [RadioCardComponent, NgClass, PaginationComponent],
  templateUrl: './subscribe-to-api-choose-application.component.html',
  styleUrl: './subscribe-to-api-choose-application.component.scss',
})
export class SubscribeToApiChooseApplicationComponent {
  @Input()
  applications: ApplicationVM[] = [];

  @Input()
  selectedApplication?: Application;

  @Input()
  pagination: ApplicationsPagination = { currentPage: 0, totalApplications: 0 };

  @Output()
  selectApplication = new EventEmitter<ApplicationVM>();

  @Output()
  pageChange = new EventEmitter<number>();

  // TODO: potentially reuse CardsGridComponent introduced in https://github.com/gravitee-io/gravitee-api-management/pull/15436
  protected readonly isMobile = inject(ObservabilityBreakpointService).isMobile;
  protected readonly isNarrow = inject(ObservabilityBreakpointService).isNarrow;
}
