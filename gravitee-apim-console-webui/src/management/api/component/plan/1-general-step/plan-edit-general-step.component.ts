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
import { BehaviorSubject, combineLatest, ReplaySubject } from 'rxjs';
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

    const hasStart = Number.isFinite(brokerRangeStart as number);
    const hasEnd = Number.isFinite(brokerRangeEnd as number);
    const hasBootstrap = Number.isFinite(bootstrapPort as number);

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

  /**
   * Tags allowed for this reference (e.g. an API Product's sharding tags) when there is no API context.
   * When set (even to an empty array), the Deployment / sharding-tags section is shown and the selectable
   * tags are constrained to this list (intersected with the user's allowed tags). When left undefined, the
   * section is driven by the API ({@link api}) as before.
   */
  private readonly referenceTags$ = new BehaviorSubject<string[] | null>(null);
  public hasReferenceTags = false;

  public generalForm: UntypedFormGroup;

  @Input()
  public set api(api: ApiV2 | ApiV4 | ApiFederated) {
    if (api) {
      this.api$.next(api);
    }
  }

  @Input()
  public set referenceTags(tags: string[] | undefined) {
    this.hasReferenceTags = tags != null;
    this.referenceTags$.next(tags ?? null);
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

  // Gated by the environment-level "Kafka port routing" setting (console.kafka.portRouting.enabled).
  // When false, the Kafka port-routing fields are hidden even for native APIs.
  @Input()
  kafkaPortRoutingEnabled = false;

  @Input()
  showBrokerRangeChangeWarning = false;

  // Surface the cross-field group errors on the individual Kafka port-routing inputs.
  protected readonly bootstrapInRangeErrorMatcher = new GroupErrorStateMatcher('bootstrapInRange');
  protected readonly rangeOrderErrorMatcher = new GroupErrorStateMatcher('rangeOrder');

  // Default broker range: 3 brokers ([bootstrap+1 .. bootstrap+3], inclusive).
  private static readonly DEFAULT_BROKER_RANGE_SIZE = 3;
  // The last range values we auto-filled, used to keep following the bootstrap port until the user
  // edits the range themselves.
  private autoFilledBrokerRangeStart?: number;
  private autoFilledBrokerRangeEnd?: number;

  conditionPages$ = this.api$.pipe(
    switchMap(api =>
      this.documentationService.apiSearch(api.id, {
        type: 'MARKDOWN',
        api: api.id,
      }),
    ),
    map(pages => pages.filter(page => page.published)),
  );
  shardingTags$ = combineLatest([
    this.referenceTags$,
    this.api$.pipe(startWith(null)),
    this.tagService.list(),
    this.currentUserService.getTags(),
  ]).pipe(
    map(([referenceTags, api, tags, userTags]) => {
      // Reference tags (API Product) take precedence; otherwise fall back to the API's tags.
      const allowedTags = referenceTags ?? api?.tags ?? [];
      return tags.map(tag => ({
        ...tag,
        disabled: !includes(userTags, tag.key) || !includes(allowedTags, tag.key),
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

    // Pre-fill the broker range from the bootstrap port (start = bootstrap + 1, end = bootstrap + size).
    // The fields stay editable: we only update a range field while it is still empty or still holds the
    // value we last auto-filled, so a user-entered range (or a loaded one) is never overwritten.
    this.generalForm
      .get('bootstrapPort')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(raw => {
        const bootstrapPort = Number.isFinite(raw as number) ? (raw as number) : null;
        if (bootstrapPort == null) {
          return;
        }
        const startControl = this.generalForm.get('brokerRangeStart');
        const endControl = this.generalForm.get('brokerRangeEnd');

        const isEmpty = (value: unknown) => value === null || value === undefined || (value as unknown) === '';
        const defaultStart = bootstrapPort + 1;
        const defaultEnd = bootstrapPort + PlanEditGeneralStepComponent.DEFAULT_BROKER_RANGE_SIZE;

        if (isEmpty(startControl.value) || startControl.value === this.autoFilledBrokerRangeStart) {
          startControl.setValue(defaultStart);
          this.autoFilledBrokerRangeStart = defaultStart;
        }
        if (isEmpty(endControl.value) || endControl.value === this.autoFilledBrokerRangeEnd) {
          endControl.setValue(defaultEnd);
          this.autoFilledBrokerRangeEnd = defaultEnd;
        }
      });
  }
}
