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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { OpenApiConfigDialogComponent } from './openapi-config-dialog.component';
import { OpenApiConfigDialogHarness } from './openapi-config-dialog.harness';

import {
  OpenApiDocExpansion,
  OpenApiViewer,
  OpenApiViewerConfiguration,
} from '../../../entities/management-api-v2/portalPageContent/openApiViewerConfiguration';

describe('OpenApiConfigDialogComponent', () => {
  let fixture: ComponentFixture<OpenApiConfigDialogComponent>;
  let component: OpenApiConfigDialogComponent;
  let harness: OpenApiConfigDialogHarness;
  let dialogRefClose: jest.Mock;

  async function init(configuration: Partial<OpenApiViewerConfiguration> = {}): Promise<void> {
    dialogRefClose = jest.fn();

    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, OpenApiConfigDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: { close: dialogRefClose } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            configuration: {
              viewer: OpenApiViewer.Swagger,
              tryItURL: 'https://api.example.com',
              displayOperationId: true,
              docExpansion: OpenApiDocExpansion.Full,
              enableFiltering: true,
              maxDisplayedTags: 10,
              ...configuration,
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OpenApiConfigDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, OpenApiConfigDialogHarness);
  }

  it('should emit camelCase configuration keys on save', async () => {
    await init();

    component.form.patchValue({
      viewer: OpenApiViewer.Redoc,
      tryItURL: 'https://try-it.example.com',
      tryIt: true,
      disableSyntaxHighlight: true,
      tryItAnonymous: true,
      showURL: true,
      entrypointsAsServers: true,
      contextPathAsServerPath: true,
      displayOperationId: true,
      usePkce: true,
      docExpansion: OpenApiDocExpansion.List,
      enableFiltering: true,
      showExtensions: true,
      showCommonExtensions: true,
      maxDisplayedTags: 12,
    });

    component.onSave();

    expect(dialogRefClose).toHaveBeenCalledWith({
      viewer: OpenApiViewer.Redoc,
      tryItURL: 'https://try-it.example.com',
      tryIt: true,
      disableSyntaxHighlight: true,
      tryItAnonymous: true,
      showURL: true,
      entrypointsAsServers: true,
      contextPathAsServerPath: true,
      displayOperationId: true,
      usePkce: true,
      docExpansion: OpenApiDocExpansion.List,
      enableFiltering: true,
      showExtensions: true,
      showCommonExtensions: true,
      maxDisplayedTags: 12,
    });
  });

  it('should default missing entrypoint configuration keys to false on save', async () => {
    await init();

    component.onSave();

    expect(dialogRefClose).toHaveBeenCalledWith(
      expect.objectContaining({
        entrypointsAsServers: false,
        contextPathAsServerPath: false,
      }),
    );
  });

  it('should disable and clear Base URL when API entrypoints are used as server URLs', async () => {
    await init();

    const baseUrlInput = await harness.getBaseUrlInput();
    const entrypointsAsServersToggle = await harness.getEntrypointsAsServersToggle();
    const contextPathAsServerToggle = await harness.getContextPathAsServerToggle();

    expect(await baseUrlInput.isDisabled()).toBe(false);
    expect(await baseUrlInput.getValue()).toBe('https://api.example.com');
    expect(await entrypointsAsServersToggle.isChecked()).toBe(false);
    expect(await contextPathAsServerToggle.isChecked()).toBe(false);

    await entrypointsAsServersToggle.toggle();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await baseUrlInput.isDisabled()).toBe(true);
    expect(await baseUrlInput.getValue()).toBe('');
    expect(await contextPathAsServerToggle.isChecked()).toBe(false);
  });

  it('should keep saved context path choice when API entrypoints are enabled', async () => {
    await init({
      contextPathAsServerPath: true,
    });
    const entrypointsAsServersToggle = await harness.getEntrypointsAsServersToggle();
    const contextPathAsServerToggle = await harness.getContextPathAsServerToggle();

    await entrypointsAsServersToggle.toggle();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await contextPathAsServerToggle.isChecked()).toBe(true);
  });
});
