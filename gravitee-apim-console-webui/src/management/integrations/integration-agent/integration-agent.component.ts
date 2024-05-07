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

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder } from '@angular/forms';
import { EMPTY } from 'rxjs';
import { catchError, debounceTime } from 'rxjs/operators';

import { agentConfiguration } from './integration-agent.configuration';

import { Integration } from '../integrations.model';
import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'app-integration-agent',
  templateUrl: './integration-agent.component.html',
  styleUrl: './integration-agent.component.scss',
})
export class IntegrationAgentComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  private AWS_ACCESS_KEY_ID: string = '';
  private AWS_SECRET_ACCESS_KEY: string = '';
  public isLoading: boolean = true;
  public integration: Integration;
  public code: string = agentConfiguration;
  public codeForEditor: string = this.code;
  public form = this.formBuilder.group({
    accessKeyId: [''],
    secretAccessKey: [''],
  });

  constructor(
    private formBuilder: FormBuilder,
    private integrationsService: IntegrationsService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.getIntegration();
    this.generateCodeForEditor();
  }

  generateCodeForEditor() {
    this.form
      .get('accessKeyId')
      .valueChanges.pipe(debounceTime(50), takeUntilDestroyed(this.destroyRef))
      .subscribe((accessKeyId: string): void => {
        this.codeForEditor = this.code
          .replace('${AWS_ACCESS_KEY_ID}', accessKeyId || '${AWS_ACCESS_KEY_ID}')
          .replace('${AWS_SECRET_ACCESS_KEY}', this.AWS_SECRET_ACCESS_KEY || '${AWS_SECRET_ACCESS_KEY}');
        this.AWS_ACCESS_KEY_ID = accessKeyId;
      });

    this.form
      .get('secretAccessKey')
      .valueChanges.pipe(debounceTime(50), takeUntilDestroyed(this.destroyRef))
      .subscribe((secretAccessKey: string): void => {
        this.codeForEditor = this.code
          .replace('${AWS_SECRET_ACCESS_KEY}', secretAccessKey || '${AWS_SECRET_ACCESS_KEY}')
          .replace('${AWS_ACCESS_KEY_ID}', this.AWS_ACCESS_KEY_ID || '${AWS_ACCESS_KEY_ID}');
        this.AWS_SECRET_ACCESS_KEY = secretAccessKey;
      });
  }

  private getIntegration(): void {
    this.isLoading = true;
    const { integrationId } = this.activatedRoute.snapshot.params;
    this.integrationsService
      .getIntegration(integrationId)
      .pipe(
        catchError(({ error }) => {
          this.isLoading = false;
          this.snackBarService.error(error.message);
          this.router.navigate(['../..'], {
            relativeTo: this.activatedRoute,
          });
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((integration: Integration): void => {
        this.integration = integration;
        this.isLoading = false;
      });
  }

  public refreshStatus(): void {
    this.getIntegration();
  }
}
