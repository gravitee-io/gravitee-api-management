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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateTestingModule } from '../../../../test/translate-testing-module';

import { ApplicationCreationStep3Component } from './application-creation-step3.component';

describe('ApplicationCreationStep3Component', () => {
  let component: ApplicationCreationStep3Component;
  let fixture: ComponentFixture<ApplicationCreationStep3Component>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        TranslateTestingModule,
        FormsModule,
        ReactiveFormsModule
      ],
      declarations: [ApplicationCreationStep3Component],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ],
    })
      .compileComponents();
  }));


  beforeEach(() => {
    fixture = TestBed.createComponent(ApplicationCreationStep3Component);
    component = fixture.componentInstance;
  });

  it('should create', (done) => {
    fixture.whenStable().then(() => {
      fixture.detectChanges();
      expect(component).toBeTruthy();
      done();
    });
  });

});
