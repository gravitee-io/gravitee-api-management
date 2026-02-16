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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { NotificationTemplateService } from '../../../services-ngx/notification-template.service';
import { AlertService } from '../../../services-ngx/alert.service';
import { Scope } from '../../../entities/alert';
import { NotificationTemplate } from '../../../entities/notification/notificationTemplate';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'org-settings-notification-template',
  styleUrls: ['./org-settings-notification-template.component.scss'],
  templateUrl: './org-settings-notification-template.component.html',
  standalone: false,
})
export class OrgSettingsNotificationTemplateComponent implements OnInit, OnDestroy {
  notificationTemplates: NotificationTemplate[] = [];

  notificationTemplatesForm: UntypedFormGroup;
  formInitialValues: unknown;

  notificationTemplateName = '';
  isLoading = true;

  hasAlertingPlugin: boolean;

  isTemplateToInclude = false;

  private scopeParam: string;
  private hookParam: string;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly notificationTemplateService: NotificationTemplateService,
    private readonly alertService: AlertService,
    private readonly snackBarService: SnackBarService,
  ) {
    this.scopeParam = this.activatedRoute.snapshot.params.scope;
    this.hookParam = this.scopeParam.toUpperCase() === 'TEMPLATES_TO_INCLUDE' ? '' : this.activatedRoute.snapshot.params.hook;
  }

  ngOnInit(): void {
    combineLatest([
      this.notificationTemplateService.search({
        scope: this.scopeParam,
        hook: this.hookParam,
      }),
      this.alertService.getStatus(Scope.ENVIRONMENT),
    ])
      .pipe(
        tap(([notificationTemplates, alertStatus]) => {
          this.hasAlertingPlugin = alertStatus.available_plugins > 0;
          this.notificationTemplates = notificationTemplates;
          this.notificationTemplateName = this.notificationTemplates[0].name;
          this.isTemplateToInclude = this.notificationTemplates.some(template => template.scope.toUpperCase() === 'TEMPLATES_TO_INCLUDE');

          this.setupNotificationTemplateForm();

          this.isLoading = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private setupNotificationTemplateForm() {
    this.notificationTemplatesForm = new UntypedFormGroup({});

    this.notificationTemplates.forEach(notificationTemplate => {
      const useCustomTemplateFormControl = new UntypedFormControl(!!notificationTemplate.enabled);
      const titleFormControl = new UntypedFormControl({ value: notificationTemplate.title, disabled: !notificationTemplate.enabled }, [
        Validators.required,
      ]);
      const contentFormControl = new UntypedFormControl({ value: notificationTemplate.content, disabled: !notificationTemplate.enabled }, [
        Validators.required,
      ]);

      const notificationTemplateForm = new UntypedFormGroup({
        useCustomTemplate: useCustomTemplateFormControl,
        content: contentFormControl,
      });

      if (!this.isTemplateToInclude) {
        notificationTemplateForm.addControl('title', titleFormControl);
      }
      this.notificationTemplatesForm.addControl(notificationTemplate.type, notificationTemplateForm);

      useCustomTemplateFormControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(value => {
        if (value) {
          titleFormControl.enable();
          contentFormControl.enable();
        } else {
          titleFormControl.disable();
          contentFormControl.disable();
        }
      });
    });
    this.formInitialValues = this.notificationTemplatesForm.getRawValue();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  submitForm() {
    const requests = this.notificationTemplates.map(notificationTemplate => {
      const form = this.notificationTemplatesForm.getRawValue()[notificationTemplate.type];
      const updatedNotificationTemplate: NotificationTemplate = {
        ...notificationTemplate,
        enabled: form.useCustomTemplate,
        title: form.title ?? '',
        content: form.content ?? '',
      };

      return updatedNotificationTemplate.id
        ? this.notificationTemplateService.update(updatedNotificationTemplate)
        : this.notificationTemplateService.create(updatedNotificationTemplate);
    });

    combineLatest(requests)
      .pipe(
        tap(() => {
          this.snackBarService.success('Template has been successfully saved!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }
}
