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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ApiV4PolicyStudioDesignComponent } from './api-v4-policy-studio-design.component';

import { GioHttpTestingModule } from '../../../../shared/testing';
import { ApiV4PolicyStudioModule } from '../api-v4-policy-studio.module';

describe('ApiV4PolicyStudioDesignComponent', () => {
  let fixture: ComponentFixture<ApiV4PolicyStudioDesignComponent>;
  let component: ApiV4PolicyStudioDesignComponent;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiV4PolicyStudioModule],
      providers: [],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiV4PolicyStudioDesignComponent);
    component = fixture.componentInstance;

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  describe('ngOnInit', () => {
    it('should work', async () => {
      expect(component).toBeTruthy();
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
