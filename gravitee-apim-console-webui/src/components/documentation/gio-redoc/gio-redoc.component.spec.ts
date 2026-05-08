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
import { Component } from '@angular/core';
import { ComponentFixture, fakeAsync, flushMicrotasks, TestBed, tick } from '@angular/core/testing';

import { GioRedocComponent } from './gio-redoc.component';
import { GioRedocService, type RedocApi } from './gio-redoc.service';

type RedocWindow = Window &
  typeof globalThis & {
    Redoc?: RedocApi;
  };

@Component({
  template: `<gio-redoc [spec]="spec" [tryItURL]="tryItURL" />`,
  standalone: true,
  imports: [GioRedocComponent],
})
class TestHostComponent {
  spec = JSON.stringify({
    openapi: '3.0.0',
    info: {
      title: 'Test API',
      version: '1.0.0',
    },
    paths: {},
  });
  tryItURL = '';
}

describe('GioRedocComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;
  let redocApi: RedocApi;
  let redocService: jest.Mocked<Pick<GioRedocService, 'load'>>;

  beforeEach(async () => {
    redocApi = { init: jest.fn() };
    redocService = { load: jest.fn() };
    redocService.load.mockResolvedValue(redocApi);
    delete (window as RedocWindow).Redoc;

    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [{ provide: GioRedocService, useValue: redocService }],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
  });

  afterEach(() => {
    jest.restoreAllMocks();
    fixture.destroy();
  });

  it('should initialize Redoc with the API returned by the loader', fakeAsync(() => {
    host.tryItURL = 'https://api.example.com';

    fixture.detectChanges();
    tick(300);
    flushMicrotasks();

    expect(redocService.load).toHaveBeenCalledTimes(1);
    expect(redocApi.init).toHaveBeenCalledWith(
      expect.objectContaining({
        openapi: '3.0.0',
        servers: [{ url: 'https://api.example.com' }],
      }),
      expect.objectContaining({
        hideDownloadButton: true,
        hideLoading: true,
        disableSearch: true,
      }),
      expect.any(HTMLElement),
      expect.any(Function),
    );
  }));

  it('should not initialize Redoc when the loader fails', fakeAsync(() => {
    const error = new Error('Failed to load Redoc');
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
    redocService.load.mockRejectedValue(error);

    fixture.detectChanges();
    tick(300);
    flushMicrotasks();

    expect(redocApi.init).not.toHaveBeenCalled();
    expect(consoleErrorSpy).toHaveBeenCalledWith('[gio-redoc] Failed to load Redoc script:', error);
  }));
});
