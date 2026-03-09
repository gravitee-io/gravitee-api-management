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
import { Component, DestroyRef, Inject, inject } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { filter, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Tenant } from '../../../entities/tenant/tenant';
import { sanitizeKeyBase, sanitizeKeyFinal } from '../../../shared/utils/key-sanitizer.util';

export type OrgSettingAddTenantDialogData = {
  tenant?: Tenant;
};

@Component({
  selector: 'org-settings-add-tenant',
  templateUrl: './org-settings-add-tenant.component.html',
  styleUrls: ['./org-settings-add-tenant.component.scss'],
  standalone: false,
})
export class OrgSettingAddTenantComponent {
  private readonly destroyRef = inject(DestroyRef);

  tenant?: Tenant;
  isUpdate = false;
  tenantForm: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<OrgSettingAddTenantComponent>,
    @Inject(MAT_DIALOG_DATA) confirmDialogData: OrgSettingAddTenantDialogData,
  ) {
    this.tenant = confirmDialogData.tenant;
    this.isUpdate = !!this.tenant;

    this.tenantForm = new FormGroup({
      name: new FormControl<string>(this.tenant?.name, [Validators.required, Validators.minLength(1), Validators.maxLength(40)]),
      key: new FormControl<string>({ value: this.tenant?.key ?? '', disabled: this.isUpdate }, [
        Validators.required,
        Validators.minLength(1),
        Validators.maxLength(64),
      ]),
      description: new FormControl<string>(this.tenant?.description, [Validators.maxLength(160)]),
    });

    this.tenantForm.controls.key.valueChanges
      .pipe(
        filter(value => value !== null),
        tap(value => {
          const sanitized = sanitizeKeyBase(value);
          if (sanitized !== value) {
            this.tenantForm.controls.key.setValue(sanitized, { emitEvent: false });
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  onSubmit() {
    const updatedTenant = this.tenantForm.getRawValue();
    this.dialogRef.close(updatedTenant);
  }

  onKeyBlur(): void {
    const value = this.tenantForm.controls.key.value;
    if (value == null) {
      return;
    }
    const sanitized = sanitizeKeyFinal(value);
    if (sanitized !== value) {
      this.tenantForm.controls.key.setValue(sanitized, { emitEvent: false });
    }
  }
}
