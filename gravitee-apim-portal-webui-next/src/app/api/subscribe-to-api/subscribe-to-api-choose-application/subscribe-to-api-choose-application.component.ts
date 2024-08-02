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
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';

import { RadioCardComponent } from '../../../../components/radio-card/radio-card.component';
import { Application } from '../../../../entities/application/application';
import { ApplicationVM } from '../subscribe-to-api.component';

export interface ApplicationsPagination {
  currentPage: number;
  totalApplications: number;
  start: number;
  end: number;
}

@Component({
  selector: 'app-subscribe-to-api-choose-application',
  imports: [RadioCardComponent, MatIcon, MatIconButton, MatButton],
  templateUrl: './subscribe-to-api-choose-application.component.html',
  styleUrl: './subscribe-to-api-choose-application.component.scss',
  standalone: true,
})
export class SubscribeToApiChooseApplicationComponent {
  @Input()
  applications: ApplicationVM[] = [];

  @Input()
  selectedApplication?: Application;

  @Input()
  pagination: ApplicationsPagination = { currentPage: 0, totalApplications: 0, start: 0, end: 0 };

  @Output()
  selectApplication = new EventEmitter<ApplicationVM>();

  @Output()
  previousPage = new EventEmitter();

  @Output()
  nextPage = new EventEmitter();
}
