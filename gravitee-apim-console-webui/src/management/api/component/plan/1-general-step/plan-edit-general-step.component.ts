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
import { Component, Input, inject, OnInit, DestroyRef } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroupDirective,
  NgForm,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { includes } from 'lodash';
import { combineLatest, of, ReplaySubject } from 'rxjs';
import { map, startWith, switchMap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ApiFederated, ApiV2, ApiV4, PlanStatus } from '../../../../../entities/management-api-v2';
import { CurrentUserService } from '../../../../../services-ngx/current-user.service';
import { DocumentationService } from '../../../../../services-ngx/documentation.service';
import { GroupService } from '../../../../../services-ngx/group.service';
import { TagService } from '../../../../../services-ngx/tag.service';

/**
 * Cross-field validator for the Kafka port routing group.
 * Validates that:
 *  - brokerRangeStart <= brokerRangeEnd (error key: rangeOrder)
 *  - bootstrapPort is NOT within [brokerRangeStart, brokerRangeEnd] (error key: bootstrapInRange)
 */
function kafkaPortRoutingValidator(): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const bootstrapPort: number | null | undefined = group.get('bootstrapPort')?.value;
    const brokerRangeStart: number | null | undefined = group.get('brokerRangeStart')?.value;
    const brokerRangeEnd: number | null | undefined = group.get('brokerRangeEnd')?.value;

    const hasStart = brokerRangeStart !== null && brokerRangeStart !== undefined && !isNaN(brokerRangeStart);
    const hasEnd = brokerRangeEnd !== null && brokerRangeEnd !== undefined && !isNaN(brokerRangeEnd);
    const hasBootstrap = bootstrapPort !== null && bootstrapPort !== undefined && !isNaN(bootstrapPort);

    const errors: ValidationErrors = {};

    if (hasStart && hasEnd && brokerRangeStart > brokerRangeEnd) {
      errors['rangeOrder'] = { brokerRangeStart, brokerRangeEnd };
    }

    if (hasBootstrap && hasStart && hasEnd && bootstrapPort >= brokerRangeStart && bootstrapPort <= brokerRangeEnd) {
      errors['bootstrapInRange'] = { bootstrapPort, brokerRangeStart, brokerRangeEnd };
    }

    return Object.keys(errors).length > 0 ? errors : null;
  };
}

/**
 * Surfaces a parent group-level error (e.g. rangeOrder, bootstrapInRange) on the individual field so that
 * mat-form-field actually displays the cross-field <mat-error> (it only shows errors when the control's own
 * error state is true).
 */
class GroupErrorStateMatcher implements ErrorStateMatcher {
  constructor(private readonly groupErrorKey: string) {}

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const interacted = !!(control && (control.dirty || control.touched || form?.submitted));
    const controlInvalid = !!(control && control.invalid && interacted);
    const groupInvalid = !!(control?.parent?.hasError(this.groupErrorKey) && interacted);
    return controlInvalid || groupInvalid;
  }
}

@Component({
  selector: 'plan-edit-general-step',
  templateUrl: './plan-edit-general-step.component.html',
  styleUrls: ['./plan-edit-general-step.component.scss'],
  standalone: false,
})
export class PlanEditGeneralStepComponent implements OnInit {
  private readonly tagService = inject(TagService);
  private readonly groupService = inject(GroupService);
  private readonly documentationService = inject(DocumentationService);
  private readonly currentUserService = inject(CurrentUserService);
  private readonly destroyRef = inject(DestroyRef);

  public api$ = new ReplaySubject<ApiV2 | ApiV4 | ApiFederated>(1);

  public generalForm: UntypedFormGroup;

  @Input()
  public set api(api: ApiV2 | ApiV4 | ApiFederated) {
    if (api) {
      this.api$.next(api);
      this._api = api;
    }
  }

  @Input()
  mode: 'create' | 'edit' = 'create';

  // Allow to display subscriptions section when plan security is not KEY_LESS
  @Input()
  displaySubscriptionsSection = true;

  @Input()
  planStatus?: PlanStatus;

  @Input()
  isFederated = false;

  @Input()
  hideAccessControl = false;

  @Input()
  isNative = false;

  // Surface the cross-field group errors on the individual Kafka port-routing inputs.
  protected readonly bootstrapInRangeErrorMatcher = new GroupErrorStateMatcher('bootstrapInRange');
  protected readonly rangeOrderErrorMatcher = new GroupErrorStateMatcher('rangeOrder');

  private _api?: ApiV2 | ApiV4 | ApiFederated;

  conditionPages$ = this.api$.pipe(
    switchMap(api =>
      this.documentationService.apiSearch(api.id, {
        type: 'MARKDOWN',
        api: api.id,
      }),
    ),
    map(pages => pages.filter(page => page.published)),
  );
  shardingTags$ = this.api$.pipe(
    // Only load tags if api is defined
    switchMap(api => combineLatest([this.tagService.list(), of(api), this.currentUserService.getTags()])),
    map(([tags, api, userTags]) => {
      return tags.map(tag => ({
        ...tag,
        disabled: !includes(userTags, tag.key) || !includes(api.tags, tag.key),
      }));
    }),
  );

  groups$ = this.groupService.list();

  ngOnInit(): void {
    this.generalForm = new UntypedFormGroup(
      {
        name: new UntypedFormControl('', [Validators.required, Validators.maxLength(50)]),
        description: new UntypedFormControl(''),
        characteristics: new UntypedFormControl([]),
        generalConditions: new UntypedFormControl(''),
        autoValidation: new UntypedFormControl(false),
        commentRequired: new UntypedFormControl(false),
        commentMessage: new UntypedFormControl(''),
        shardingTags: new UntypedFormControl([]),
        excludedGroups: new UntypedFormControl([]),
        bootstrapPort: new UntypedFormControl(null, [Validators.min(1024), Validators.max(65535)]),
        brokerRangeStart: new UntypedFormControl(null, [Validators.min(1024), Validators.max(65535)]),
        brokerRangeEnd: new UntypedFormControl(null, [Validators.min(1024), Validators.max(65535)]),
      },
      { validators: kafkaPortRoutingValidator() },
    );

    // Enable comment message only if comment required is checked
    this.generalForm
      .get('commentRequired')
      .valueChanges.pipe(startWith(this.generalForm.get('commentRequired').value), takeUntilDestroyed(this.destroyRef))
      .subscribe(value => {
        value ? this.generalForm.get('commentMessage').enable() : this.generalForm.get('commentMessage').disable();
      });

    // Disable bootstrapPort in edit mode when the API has been deployed
    if (this.mode === 'edit' && (this._api as ApiV4)?.deployedAt != null) {
      this.generalForm.get('bootstrapPort').disable();
    }
  }
}
