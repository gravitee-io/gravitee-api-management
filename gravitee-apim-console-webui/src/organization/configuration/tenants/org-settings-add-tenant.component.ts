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
import { Component, Inject } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

import { Tenant } from '../../../entities/tenant/tenant';

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
  tenant?: Tenant;
  isUpdate = false;
  tenantForm: UntypedFormGroup;

  constructor(
    public dialogRef: MatDialogRef<OrgSettingAddTenantComponent>,
    @Inject(MAT_DIALOG_DATA) confirmDialogData: OrgSettingAddTenantDialogData,
  ) {
    this.tenant = confirmDialogData.tenant;
    this.isUpdate = !!this.tenant;

    this.tenantForm = new UntypedFormGroup({
      name: new UntypedFormControl(this.tenant?.name, [Validators.required, Validators.minLength(1), Validators.maxLength(30)]),
      description: new UntypedFormControl(this.tenant?.description, [Validators.maxLength(160)]),
    });

    if (this.isUpdate) {
      this.tenantForm.addControl('id', new UntypedFormControl({ value: this.tenant.id, disabled: true }, [Validators.required]));
    }
  }

  onSubmit() {
    const updatedTenant = this.tenantForm.getRawValue();
    this.dialogRef.close(updatedTenant);
  }
}
