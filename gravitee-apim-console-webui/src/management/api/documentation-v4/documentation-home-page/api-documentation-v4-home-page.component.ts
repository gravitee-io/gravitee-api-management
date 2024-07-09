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
import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { combineLatest, EMPTY, Observable, of, Subject } from 'rxjs';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiDocumentationV2Service } from '../../../../services-ngx/api-documentation-v2.service';
import {
  Breadcrumb,
  getLogoForPageType,
  Page,
  CreateDocumentation,
  CreateDocumentationType,
  PageType,
  Api,
  getTooltipForPageType,
  AccessControl,
  Visibility,
  Group,
  EditDocumentation,
  PageSource, getTitleForPageType,
} from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { GroupV2Service } from '../../../../services-ngx/group-v2.service';
import { FetcherService } from '../../../../services-ngx/fetcher.service';

interface EditPageForm {
  stepOne: FormGroup<{
    name: FormControl<string>;
    visibility: FormControl<Visibility>;
    accessControlGroups: FormControl<string[]>;
    excludeGroups: FormControl<boolean>;
  }>;
  content: FormControl<string>;
  source: FormControl<string>;
  sourceConfiguration: FormControl<undefined | unknown>;
}

@Component({
  selector: 'api-documentation-home-page',
  templateUrl: './api-documentation-v4-home-page.component.html',
  styleUrls: ['./api-documentation-v4-home-page.component.scss'],
})
export class ApiDocumentationV4HomePageComponent implements OnInit, OnDestroy {
  @Input()
  breadcrumbs: Breadcrumb[];
  @Input()
  isReadOnly: boolean;
  @Input()
  hasPages: boolean;
  @Output()
  onAddPage = new EventEmitter<PageType>();
  @Output()
  onNavigateTo = new EventEmitter<string>();

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
  }

  protected readonly getTitleForPageType = getTitleForPageType;
  protected readonly getLogoForPageType = getLogoForPageType;
  pageTypes: PageType[] = ['MARKDOWN', 'SWAGGER', 'ASYNCAPI'];
}
