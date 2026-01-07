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
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegistrationComponent } from './registration.component';
import { AppTestingModule } from '../../testing/app-testing.module';
import { DivHarness } from '../../testing/div.harness';

describe('RegistrationComponent', () => {
  let fixture: ComponentFixture<RegistrationComponent>;
  let harnessLoader: HarnessLoader;

  const init = async () => {
    await TestBed.configureTestingModule({
      imports: [RegistrationComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(RegistrationComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges(); // ngOnInit -> listCustomUserFields()

    fixture.detectChanges();
  };

  it('should display registration form by default (submitted=false)', async () => {
    await init();

    const cardEl: HTMLElement | null = fixture.nativeElement.querySelector('mat-card.registration__form__container');
    expect(cardEl).not.toBeNull();

    const formEl: HTMLFormElement | null = fixture.nativeElement.querySelector('mat-card.registration__form__container form');
    expect(formEl).not.toBeNull();

    const success = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.registration__success__container' }));
    expect(success).toBeNull();
  });
});
