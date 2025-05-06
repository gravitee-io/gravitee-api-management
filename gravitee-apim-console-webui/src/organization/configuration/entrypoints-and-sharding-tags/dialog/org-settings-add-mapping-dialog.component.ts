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
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { shareReplay } from 'rxjs/operators';

import { Entrypoint } from '../../../../entities/entrypoint/entrypoint';
import { TagService } from '../../../../services-ngx/tag.service';
import { kafkaBootstrapDomainPatternValidator, portValidator } from '../org-settings-entrypoints-and-sharding-tags.utils';

export type OrgSettingAddMappingDialogData = {
  target: Entrypoint['target'];
  entrypoint?: Entrypoint;
};

type MappingFormGroup = {
  tags: FormControl<string[]>;
  httpValue?: FormControl<string>;
  kafkaDomain?: FormControl<string>;
  kafkaPort?: FormControl<string>;
  tcpPort?: FormControl<string>;
};

@Component({
  selector: 'org-settings-add-mapping-dialog',
  templateUrl: './org-settings-add-mapping-dialog.component.html',
  styleUrls: ['./org-settings-add-mapping-dialog.component.scss'],
  standalone: false,
})
export class OrgSettingAddMappingDialogComponent {
  entrypoint?: Entrypoint;
  target: Entrypoint['target'];
  isUpdate = false;
  mappingForm: FormGroup<MappingFormGroup>;
  tags$? = this.tagService.list().pipe(shareReplay(1));

  constructor(
    public dialogRef: MatDialogRef<OrgSettingAddMappingDialogComponent>,
    @Inject(MAT_DIALOG_DATA) confirmDialogData: OrgSettingAddMappingDialogData,
    private readonly tagService: TagService,
  ) {
    this.entrypoint = confirmDialogData.entrypoint;
    this.target = confirmDialogData.target;
    this.isUpdate = !!this.entrypoint;

    this.mappingForm = new FormGroup({
      tags: new FormControl(this.entrypoint?.tags ?? []),
    });

    switch (this.target) {
      case 'HTTP': {
        this.mappingForm.addControl('httpValue', new FormControl(this.entrypoint?.value, [Validators.required]));
        break;
      }
      case 'KAFKA': {
        const [kafkaDomain, kafkaPort] = this.entrypoint?.value?.split(':') ?? ['{apiHost}', '9092'];

        this.mappingForm.addControl(
          'kafkaDomain',
          new FormControl(kafkaDomain, [Validators.required, kafkaBootstrapDomainPatternValidator]),
        );
        this.mappingForm.addControl('kafkaPort', new FormControl(kafkaPort, [Validators.required, portValidator]));
        break;
      }
      case 'TCP': {
        this.mappingForm.addControl('tcpPort', new FormControl(this.entrypoint?.value, [Validators.required]));
        break;
      }
    }
  }

  onSubmit() {
    if (this.mappingForm.invalid) {
      return;
    }
    const formValue = this.mappingForm.value;
    let value;
    switch (this.target) {
      case 'HTTP':
        value = formValue.httpValue;
        break;
      case 'KAFKA':
        value = formValue.kafkaDomain + ':' + formValue.kafkaPort;
        break;
      case 'TCP':
        value = formValue.tcpPort;
        break;
    }

    const updatedEntrypoint = {
      ...(this.entrypoint?.id ? { id: this.entrypoint.id } : {}),
      tags: formValue.tags,
      value,
      target: this.target,
    };
    this.dialogRef.close(updatedEntrypoint);
  }
}
