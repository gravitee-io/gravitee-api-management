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

import { booleanAttribute, Component, EventEmitter, Input, Output } from '@angular/core';

import { PageType } from '../../../../../entities/management-api-v2';

@Component({
  selector: 'api-documentation-empty-state',
  templateUrl: './api-documentation-v4-empty-state.component.html',
  styleUrls: ['./api-documentation-v4-empty-state.component.scss'],
  standalone: false,
})
export class ApiDocumentationV4EmptyStateComponent {
  @Output()
  addPage = new EventEmitter<PageType>();

  @Input()
  isReadOnly = false;

  @Input({ transform: booleanAttribute })
  showAddPageButton: boolean = false;

  @Input()
  emptyPageTitle!: string;

  @Input()
  emptyPageMessage!: string;
}
