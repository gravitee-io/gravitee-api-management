/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegistrationComponent } from './registration.component';
import { CustomUserFields } from '../../entities/user/custom-user-fields';
import { AppTestingModule } from '../../testing/app-testing.module';
import { DivHarness } from '../../testing/div.harness';

describe('RegistrationComponent', () => {
  let fixture: ComponentFixture<RegistrationComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const flushListCustomUserFields = (customFields: CustomUserFields[] = []) => {
    // ngOnInit() robi listCustomUserFields() => 1× GET
    const req = httpTestingController.expectOne(r => r.method === 'GET');
    req.flush(customFields);
  };

  const init = async (params?: { submitted?: boolean; customFields?: CustomUserFields[] }) => {
    await TestBed.configureTestingModule({
      imports: [RegistrationComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(RegistrationComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.componentInstance.submitted = params?.submitted ?? false;

    fixture.detectChanges(); // uruchamia ngOnInit
    flushListCustomUserFields(params?.customFields ?? []);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display registration form when submitted=false', async () => {
    await init({ submitted: false });

    const formEl: HTMLFormElement | null = fixture.nativeElement.querySelector('form.registration__form__container');
    expect(formEl).not.toBeNull();

    const success = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.registration__success__container' }));
    expect(success).toBeNull();
  });

  it('should display success view when submitted=true', async () => {
    await init({ submitted: true });

    const formEl: HTMLFormElement | null = fixture.nativeElement.querySelector('form.registration__form__container');
    expect(formEl).toBeNull();

    const success = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.registration__success__container' }));
    expect(success).not.toBeNull();
  });
  //
});
