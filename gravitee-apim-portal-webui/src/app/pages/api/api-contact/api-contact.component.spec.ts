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
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { mockProvider } from '@ngneat/spectator/jest';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { TokenService } from '../../../services/token.service';
import { PortalService } from '../../../../../projects/portal-webclient-sdk/src/lib';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ResetPasswordComponent } from '../../reset-password/reset-password.component';

describe('ApiContactComponent', () => {
  const createComponent = createComponentFactory({
    component: ResetPasswordComponent,
    imports: [RouterTestingModule, HttpClientTestingModule, FormsModule, ReactiveFormsModule],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    providers: [mockProvider(PortalService)],
  });

  let spectator: Spectator<ResetPasswordComponent>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    spectator.inject(TokenService);
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
