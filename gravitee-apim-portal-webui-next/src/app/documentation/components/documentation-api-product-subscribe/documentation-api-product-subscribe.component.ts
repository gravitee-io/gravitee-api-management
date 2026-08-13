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
import { Component, inject, input } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';

import { ApiProduct } from '../../../../entities/api-product/api-product';
import { SubscribeToApiProductComponent } from '../../../api-product/subscribe-to-api-product/subscribe-to-api-product.component';

@Component({
  selector: 'app-documentation-api-product-subscribe',
  imports: [MatIcon, RouterLink, SubscribeToApiProductComponent],
  templateUrl: './documentation-api-product-subscribe.component.html',
  styleUrl: './documentation-api-product-subscribe.component.scss',
})
export class DocumentationApiProductSubscribeComponent {
  private readonly router = inject(Router);

  readonly apiProduct = input.required<ApiProduct>();
  readonly navId = input.required<string>();
  readonly selectedId = input<string>();

  readonly cancel = () => this.router.navigate(['/documentation', this.navId()], { queryParams: { selectedId: this.selectedId() } });
}
