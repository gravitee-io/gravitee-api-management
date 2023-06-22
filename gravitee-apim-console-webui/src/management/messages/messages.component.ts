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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { sortBy } from 'lodash';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { RoleService } from '../../services-ngx/role.service';
import { UIRouterStateParams } from '../../ajs-upgraded-providers';
import { HttpMessagePayload, MessageScope, TextMessagePayload } from '../../entities/message/messagePayload';
import { MessageService } from '../../services-ngx/message.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';

@Component({
  selector: 'messages',
  template: require('./messages.component.html'),
  styles: [require('./messages.component.scss')],
})
export class MessagesComponent implements OnInit, OnDestroy {
  channels = [
    { id: 'PORTAL', name: 'Portal notifications' },
    { id: 'MAIL', name: 'Email' },
    { id: 'HTTP', name: 'POST HTTP message' },
  ];

  form: FormGroup;
  recipients: { name: string; displayName: string }[];
  scope: MessageScope;
  sending = false;
  private apiId: string;
  private unsubscribe$: Subject<void> = new Subject<void>();

  constructor(
    private readonly roleService: RoleService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly messageService: MessageService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.apiId = this.ajsStateParams.apiId;
    this.scope = this.apiId ? 'APPLICATION' : 'ENVIRONMENT';
    this.roleService
      .list(this.scope)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((roles) => {
        const sortedRoles = sortBy(roles, ['name']);
        this.recipients = sortedRoles.map((role) => {
          const displayName =
            `Members with the ${role.name} role on ` + (this.scope === 'APPLICATION' ? `subscribing applications` : `ENVIRONMENT scope`);
          return {
            name: role.name,
            displayName,
          };
        });
        if (this.apiId) {
          this.recipients.unshift({ name: 'API_SUBSCRIBERS', displayName: 'API subscribers' });
        }
        this.form = new FormGroup({
          channel: new FormControl('PORTAL', [Validators.required]),
          recipients: new FormControl([], [Validators.required]),
          title: new FormControl('', [Validators.required]),
          url: new FormControl('', [Validators.required]),
          text: new FormControl('', [Validators.required]),
          useSystemProxy: new FormControl(false),
          headers: new FormControl([]),
        });
        // Disable URL field as initial value for channel is PORTAL.
        this.form.controls['url'].disable();

        // eslint-disable-next-line rxjs/no-nested-subscribe
        this.form.controls['channel'].valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((values) => {
          if (values === 'HTTP') {
            this.form.controls['title'].disable();
            this.form.controls['url'].enable();
          } else {
            this.form.controls['title'].enable();
            this.form.controls['url'].disable();
          }
        });
      });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  sendMessage() {
    this.sending = true;
    const val = this.form.getRawValue();
    const payload = val.channel === 'HTTP' ? this.getHttpPayload() : this.getTextMessagePayload();
    const obs = this.apiId ? this.messageService.sendFromApi(this.apiId, payload) : this.messageService.sendFromPortal(payload);

    obs.subscribe({
      next: (res) => {
        this.sending = false;
        this.snackBarService.success(`Message sent to ${res} recipient${res > 1 ? 's' : ''}`);
      },
      error: (error) => {
        this.sending = false;
        let message = `Message could not be sent`;
        if (error?.error?.message) message += ` because of ${error.error.message}`;
        this.snackBarService.error(message);
      },
    });
  }

  requiredPermission() {
    return this.scope === 'APPLICATION' ? { anyOf: ['api-message-c'] } : { anyOf: ['environment-message-c'] };
  }

  private getHttpPayload(): HttpMessagePayload {
    const val = this.form.getRawValue();
    const params = val.headers.reduce((params, header) => {
      params[header.key] = header.value;
      return params;
    }, {});

    return {
      channel: 'HTTP',
      text: val.text,
      recipient: {
        url: val.url,
      },
      params,
      useSystemProxy: val.useSystemProxy,
    };
  }

  private getTextMessagePayload(): TextMessagePayload {
    const val = this.form.getRawValue();
    return {
      channel: val.channel,
      title: val.title,
      text: val.text,
      recipient: {
        role_scope: this.scope,
        role_value: val.recipients,
      },
    };
  }
}
