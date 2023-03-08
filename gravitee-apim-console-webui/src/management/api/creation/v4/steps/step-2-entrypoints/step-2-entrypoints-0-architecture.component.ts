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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { of, Subject } from 'rxjs';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';

import { Step2Entrypoints2ConfigComponent } from './step-2-entrypoints-2-config.component';
import { Step2Entrypoints1ListComponent } from './step-2-entrypoints-1-list.component';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { EntrypointService } from '../../../../../../services-ngx/entrypoint.service';
import { ConnectorListItem } from '../../../../../../entities/connector/connector-list-item';
import {
  GioConnectorDialogComponent,
  GioConnectorDialogData,
} from '../../../../../../components/gio-connector-dialog/gio-connector-dialog.component';
import { EndpointService } from '../../../../../../services-ngx/endpoint.service';
import { IconService } from '../../../../../../services-ngx/icon.service';

@Component({
  selector: 'step-2-entrypoints-0-architecture',
  template: require('./step-2-entrypoints-0-architecture.component.html'),
  styles: [require('./step-2-entrypoints-0-architecture.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class Step2Entrypoints0ArchitectureComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  form: FormGroup;
  private httpProxyEntrypoint: ConnectorListItem;

  private initialValue: { type: 'proxy' | 'message'[] };

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly stepService: ApiCreationStepService,
    private readonly entrypointService: EntrypointService,
    private readonly endpointService: EndpointService,
    private readonly confirmDialog: MatDialog,
    private readonly matDialog: MatDialog,
    private readonly iconService: IconService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;

    this.entrypointService
      .v4ListSyncEntrypointPlugins()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((entrypoints) => {
        this.httpProxyEntrypoint = entrypoints.find((e) => e.id === 'http-proxy');

        this.form = this.formBuilder.group({
          type: this.formBuilder.control(currentStepPayload.type ? [currentStepPayload.type] : null, [Validators.required]),
        });

        this.initialValue = this.form.getRawValue();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  goBack() {
    this.stepService.goToPreviousStep();
  }

  save() {
    const previousType = this.stepService.payload.type;
    const selectedType = this.form.value.type[0];

    if (previousType && selectedType !== previousType) {
      // When changing the type, all previously filled steps must be deleted to restart from scratch.
      this.confirmDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
          data: {
            title: 'Are you sure?',
            content:
              'By changing the architecture type, all previously entered data in Endpoints, Security and Document steps will be reset.',
            confirmButton: 'Validate',
            cancelButton: 'Discard',
          },
        })
        .afterClosed()
        .subscribe((confirmed) => {
          if (confirmed) {
            this.stepService.removeAllNextSteps();
            this.form.value.type[0] === 'proxy' ? this.doSaveSync() : this.doSaveAsync();
          } else {
            this.form.setValue(this.initialValue);
          }
        });
      return;
    }
    this.form.value.type[0] === 'proxy' ? this.doSaveSync() : this.doSaveAsync();
  }

  private doSaveSync() {
    this.endpointService
      .v4Get('http-proxy')
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((httpProxyEndpoint) => {
          this.stepService.validStep((previousPayload) => ({
            ...previousPayload,
            type: 'proxy',
            selectedEntrypoints: [
              {
                id: this.httpProxyEntrypoint.id,
                name: this.httpProxyEntrypoint.name,
                icon: this.iconService.registerSvg(this.httpProxyEntrypoint.id, this.httpProxyEntrypoint.icon),
                supportedListenerType: this.httpProxyEntrypoint.supportedListenerType,
              },
            ],
            selectedEndpoints: [{ id: httpProxyEndpoint.id, name: httpProxyEndpoint.name, icon: httpProxyEndpoint.icon }],
          }));
          this.stepService.goToNextStep({
            groupNumber: 2,
            component: Step2Entrypoints2ConfigComponent,
          });
        }),
      )
      .subscribe();
  }

  private doSaveAsync() {
    this.stepService.validStep((previousPayload) => ({
      ...previousPayload,
      type: 'message',
    }));
    this.stepService.goToNextStep({
      groupNumber: 2,
      component: Step2Entrypoints1ListComponent,
    });
  }

  onMoreInfoClick(event, entrypoint: ConnectorListItem) {
    event.stopPropagation();

    this.entrypointService
      .v4GetMoreInformation(entrypoint.id)
      .pipe(
        takeUntil(this.unsubscribe$),
        catchError(() =>
          of({
            description: `${entrypoint.description} <br/><br/> 🚧 More information coming soon 🚧 <br/>`,
            documentationUrl: 'https://docs.gravitee.io',
          }),
        ),
        tap((pluginMoreInformation) => {
          this.matDialog
            .open<GioConnectorDialogComponent, GioConnectorDialogData, boolean>(GioConnectorDialogComponent, {
              data: {
                name: entrypoint.name,
                pluginMoreInformation,
              },
              role: 'alertdialog',
              id: 'moreInfoDialog',
            })
            .afterClosed()
            .pipe(takeUntil(this.unsubscribe$))
            .subscribe();
        }),
      )
      .subscribe();
  }

  onMoreInfoMessageClick(event) {
    event.stopPropagation();

    this.matDialog
      .open<GioConnectorDialogComponent, GioConnectorDialogData, boolean>(GioConnectorDialogComponent, {
        data: {
          name: 'Message',
          pluginMoreInformation: {
            description: `
              <p>
                Includes asynchronous and event-driven API entrypoints. Typically used for streaming APIs. If you want to learn more about
                Gravitee’s new capabilities, you can read about our Service Management Ecosystem.
              </p>
              <p>
                Such as:
                <span class="gio-badge-neutral">HTTP Get</span>
                <span class="gio-badge-neutral">HTTP Post</span>
                <span class="gio-badge-neutral">SSE</span>
                <span class="gio-badge-neutral">Webhook</span>
                <span class="gio-badge-neutral">Websocket</span>
              </p>
            `,
            documentationUrl: 'https://docs.gravitee.io',
          },
        },
        role: 'alertdialog',
        id: 'moreInfoDialog',
      })
      .afterClosed()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe();
  }
}
