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
import { MatInputHarness } from '@angular/material/input/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs/internal/observable/of';

import { RegistrationConfirmationComponent } from './registration-confirmation.component';
import { TokenService } from '../../../services/token.service';
import { AppTestingModule } from '../../../testing/app-testing.module';
import { DivHarness } from '../../../testing/div.harness';

describe('RegistrationConfirmationComponent', () => {
  let fixture: ComponentFixture<RegistrationConfirmationComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async (params?: {
    token?: string;
    submitted?: boolean;
    parsedToken?: { firstname: string; lastname: string; email: string };
  }) => {
    const token = params?.token ?? 'token-123';
    const parsedToken = params?.parsedToken ?? { firstname: 'John', lastname: 'Doe', email: 'john@doe.com' };

    await TestBed.configureTestingModule({
      imports: [RegistrationConfirmationComponent, AppTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ token })),
          },
        },
        {
          provide: TokenService,
          useValue: {
            parseToken: jest.fn().mockReturnValue(parsedToken),
          } as Partial<TokenService>,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegistrationConfirmationComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display confirmation form when submitted=false and form is built', async () => {
    await init({ submitted: false });

    const formEl: HTMLFormElement | null = fixture.nativeElement.querySelector('form.registration-confirmation__form__container');
    expect(formEl).not.toBeNull();

    const success = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.registration-confirmation__success__container' }));
    expect(success).toBeNull();

    const firstname = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="firstname"]' }));
    expect(await firstname.isDisabled()).toEqual(true);
    expect(await firstname.getValue()).toEqual('John');

    const lastname = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="lastname"]' }));
    expect(await lastname.isDisabled()).toEqual(true);
    expect(await lastname.getValue()).toEqual('Doe');

    const email = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="email"]' }));
    expect(await email.isDisabled()).toEqual(true);
    expect(await email.getValue()).toEqual('john@doe.com');
  });
});
