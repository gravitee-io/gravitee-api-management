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

import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { ApiProduct } from '../../../../entities/management-api-v2/api-product/apiProduct';

export type ApiGeneralInfoIncludedInDialogData = {
  apiProducts: ApiProduct[];
};

@Component({
  selector: 'api-general-info-included-in-dialog',
  templateUrl: './api-general-info-included-in-dialog.component.html',
  styleUrls: ['./api-general-info-included-in-dialog.component.scss'],
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule],
})
export class ApiGeneralInfoIncludedInDialogComponent {
  readonly data: ApiGeneralInfoIncludedInDialogData = inject(MAT_DIALOG_DATA);

  searchTerm = signal('');

  filteredApiProducts = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const products = this.data.apiProducts ?? [];
    if (!term) {
      return products;
    }
    return products.filter(p => p.name?.toLowerCase().includes(term) || p.description?.toLowerCase().includes(term));
  });
}
