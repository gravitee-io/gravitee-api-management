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

import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { combineLatest, Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { UntypedFormControl } from '@angular/forms';

import { OrganizationService } from '../../../../services-ngx/organization.service';
import { OrgSettingsPlatformPoliciesService } from '../org-settings-platform-policies.service';
import { DefinitionVM } from '../org-settings-platform-policies.component';
import { FlowConfigurationSchema } from '../../../../entities/flow/configurationSchema';

@Component({
  selector: 'org-settings-platform-policies-config',
  templateUrl: './org-settings-platform-policies-config.component.html',
  styleUrls: ['./org-settings-platform-policies-config.component.scss'],
  standalone: false,
})
export class OrgSettingsPlatformPoliciesConfigComponent implements OnInit, OnDestroy {
  flowConfigurationSchema: FlowConfigurationSchema;

  formControl: UntypedFormControl;
  isLoading = true;

  @Output()
  change = new EventEmitter<DefinitionVM['flow_mode']>();

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    private readonly organizationService: OrganizationService,
    private readonly orgSettingsPlatformPoliciesService: OrgSettingsPlatformPoliciesService,
  ) {}

  ngOnInit(): void {
    this.isLoading = true;

    combineLatest([this.orgSettingsPlatformPoliciesService.getConfigurationSchemaForm(), this.organizationService.get()])
      .pipe(
        tap(([flowSchema, organization]) => {
          this.flowConfigurationSchema = flowSchema;

          this.formControl = new UntypedFormControl({
            flow_mode: organization.flowMode,
          });

          this.formControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((value) => {
            this.change.emit(value.flow_mode);
          });
          this.isLoading = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
