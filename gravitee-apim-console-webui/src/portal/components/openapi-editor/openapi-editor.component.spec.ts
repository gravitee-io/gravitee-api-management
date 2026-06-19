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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component, viewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { OpenApiEditorComponent } from './openapi-editor.component';
import { OpenApiEditorHarness } from './openapi-editor.harness';

import { GioRedocService, type RedocApi } from '../../../components/documentation/gio-redoc/gio-redoc.service';
import {
  OpenApiDocExpansion,
  OpenApiViewer,
  OpenApiViewerConfiguration,
} from '../../../entities/management-api-v2/portalPageContent/openApiViewerConfiguration';

@Component({
  template: ` <openapi-editor [formControl]="contentControl" [configuration]="configuration" [showPreview]="showPreview" /> `,
  standalone: true,
  imports: [OpenApiEditorComponent, ReactiveFormsModule],
})
class TestHostComponent {
  readonly editor = viewChild.required(OpenApiEditorComponent);
  contentControl = new FormControl('openapi: 3.0.0\ninfo:\n  title: Test');
  configuration: Partial<OpenApiViewerConfiguration> | null = null;
  showPreview = true;
}

describe('OpenApiEditorComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;
  let loader: HarnessLoader;
  let redocApi: RedocApi;

  beforeEach(async () => {
    redocApi = { init: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, TestHostComponent],
      providers: [{ provide: GioRedocService, useValue: { load: () => Promise.resolve(redocApi) } }],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it('should create and render', async () => {
    const openApiEditor = await loader.getHarness(OpenApiEditorHarness);
    expect(openApiEditor).toBeTruthy();
  });

  it('should reflect value from form control', async () => {
    const openApiEditor = await loader.getHarness(OpenApiEditorHarness);
    expect(openApiEditor).toBeTruthy();
    expect(host.contentControl.value).toBe('openapi: 3.0.0\ninfo:\n  title: Test');
  });

  it('should accept updated value from parent via writeValue', async () => {
    host.contentControl.setValue('openapi: 3.0.0\ninfo:\n  title: Updated');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(host.contentControl.value).toBe('openapi: 3.0.0\ninfo:\n  title: Updated');
  });

  it('should read OpenAPI viewer settings from camelCase configuration', () => {
    host.configuration = {
      viewer: OpenApiViewer.Redoc,
      tryItURL: 'https://try-it.example.com',
      docExpansion: OpenApiDocExpansion.Full,
      displayOperationId: true,
      enableFiltering: true,
      showExtensions: true,
      showCommonExtensions: true,
      maxDisplayedTags: 5,
    };
    fixture.detectChanges();

    const editor = host.editor();

    expect(editor.isRedocViewer()).toBe(true);
    expect(editor.swaggerTryItURL()).toBe('https://try-it.example.com');
    expect(editor.swaggerDocExpansion()).toBe(OpenApiDocExpansion.Full);
    expect(editor.swaggerDisplayOperationId()).toBe(true);
    expect(editor.swaggerFilter()).toBe(true);
    expect(editor.swaggerShowExtensions()).toBe(true);
    expect(editor.swaggerShowCommonExtensions()).toBe(true);
    expect(editor.swaggerMaxDisplayedTags()).toBe(5);
  });

  it('should not apply custom Base URL in Swagger preview when API entrypoints are selected', () => {
    host.configuration = {
      viewer: OpenApiViewer.Swagger,
      tryItURL: 'https://try-it.example.com',
      entrypointsAsServers: true,
    };
    fixture.detectChanges();

    expect(host.editor().swaggerTryItURL()).toBe('');
  });

  describe('showPreview input', () => {
    it('should render gio-swagger-ui when showPreview is true (default)', async () => {
      const openApiEditor = await loader.getHarness(OpenApiEditorHarness);
      expect(await openApiEditor.getPreview()).not.toBeNull();
    });

    it('should not render gio-swagger-ui when showPreview is false', async () => {
      host.showPreview = false;
      fixture.detectChanges();

      const openApiEditor = await loader.getHarness(OpenApiEditorHarness);
      expect(await openApiEditor.getPreview()).toBeNull();
    });
  });
});
