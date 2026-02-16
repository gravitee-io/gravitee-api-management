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
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { includes } from 'lodash';
import { combineLatest, of, ReplaySubject, Subject } from 'rxjs';
import { map, startWith, switchMap, takeUntil } from 'rxjs/operators';

import { ApiFederated, ApiV2, ApiV4, PlanStatus } from '../../../../../entities/management-api-v2';
import { CurrentUserService } from '../../../../../services-ngx/current-user.service';
import { DocumentationService } from '../../../../../services-ngx/documentation.service';
import { GroupService } from '../../../../../services-ngx/group.service';
import { TagService } from '../../../../../services-ngx/tag.service';

@Component({
  selector: 'plan-edit-general-step',
  templateUrl: './plan-edit-general-step.component.html',
  styleUrls: ['./plan-edit-general-step.component.scss'],
  standalone: false,
})
export class PlanEditGeneralStepComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public api$ = new ReplaySubject<ApiV2 | ApiV4 | ApiFederated>(1);

  public generalForm: UntypedFormGroup;

  @Input()
  public set api(api: ApiV2 | ApiV4 | ApiFederated) {
    if (api) {
      this.api$.next(api);
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
        disabled: !includes(userTags, tag.id) || !includes(api.tags, tag.id),
      }));
    }),
  );

  groups$ = this.groupService.list();

  constructor(
    private readonly tagService: TagService,
    private readonly groupService: GroupService,
    private readonly documentationService: DocumentationService,
    private readonly currentUserService: CurrentUserService,
  ) {}

  ngOnInit(): void {
    this.generalForm = new UntypedFormGroup({
      name: new UntypedFormControl('', [Validators.required, Validators.maxLength(50)]),
      description: new UntypedFormControl(''),
      characteristics: new UntypedFormControl([]),
      generalConditions: new UntypedFormControl(''),
      autoValidation: new UntypedFormControl(false),
      commentRequired: new UntypedFormControl(false),
      commentMessage: new UntypedFormControl(''),
      shardingTags: new UntypedFormControl([]),
      excludedGroups: new UntypedFormControl([]),
    });

    // Enable comment message only if comment required is checked
    this.generalForm
      .get('commentRequired')
      .valueChanges.pipe(startWith(this.generalForm.get('commentRequired').value), takeUntil(this.unsubscribe$))
      .subscribe(value => {
        value ? this.generalForm.get('commentMessage').enable() : this.generalForm.get('commentMessage').disable();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
