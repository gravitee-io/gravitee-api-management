/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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

import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

import { ApiProduct } from '../../../../entities/management-api-v2/api-product';

@Component({
  selector: 'api-product-danger-zone',
  templateUrl: './api-product-danger-zone.component.html',
  styleUrls: ['./api-product-danger-zone.component.scss'],
  standalone: true,
  imports: [MatCardModule, MatButtonModule],
})
export class ApiProductDangerZoneComponent {
  apiProduct = input.required<ApiProduct>();
  isReadOnly = input<boolean>(false);

  removeApisClick = output<void>();
  deleteApiProductClick = output<void>();

  onRemoveApis(): void {
    this.removeApisClick.emit();
  }

  onDeleteApiProduct(): void {
    this.deleteApiProductClick.emit();
  }
}
