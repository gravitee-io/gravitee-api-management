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
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs/internal/observable/of';

import { RegistrationConfirmationComponent } from './registration-confirmation.component';
import { TokenService } from '../../../services/token.service';
import { AppTestingModule } from '../../../testing/app-testing.module';

describe('RegistrationConfirmationComponent', () => {
  let fixture: ComponentFixture<RegistrationConfirmationComponent>;

  let httpTestingController: HttpTestingController;

  const init = async (params?: { token?: string; parsedToken?: { firstname: string; lastname: string; email: string } | null }) => {
    const token = params?.token ?? 'token-123';

    const defaultParsed = { firstname: 'John', lastname: 'Doe', email: 'john@doe.com' };
    const parsedToken = params && 'parsedToken' in params ? params.parsedToken : defaultParsed;

    await TestBed.configureTestingModule({
      imports: [RegistrationConfirmationComponent, AppTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ token })) },
        },
        {
          provide: TokenService,
          useValue: { parseToken: jest.fn().mockReturnValue(parsedToken) } as Partial<TokenService>,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegistrationConfirmationComponent);

    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display invalid token view when token cannot be parsed', async () => {
    await init({ parsedToken: null });

    // error() === 401 -> "Invalid token" + "Bad request. Invalid token value"
    const titleEl: HTMLElement | null = fixture.nativeElement.querySelector('.registration-confirmation__form__title');
    expect(titleEl).not.toBeNull();
    expect(titleEl!.textContent).toContain('Invalid token');

    const errorEl: HTMLElement | null = fixture.nativeElement.querySelector('mat-error');
    expect(errorEl).not.toBeNull();
    expect(errorEl!.textContent).toContain('Bad request. Invalid token value');
  });
});
