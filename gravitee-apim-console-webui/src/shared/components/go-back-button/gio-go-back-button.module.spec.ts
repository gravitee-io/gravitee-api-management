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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { GioGoBackButtonModule } from './gio-go-back-button.module';

import { UIRouterState } from '../../../ajs-upgraded-providers';

@Component({
  template: `<gio-go-back-button [ajsGo]="{ to: 'state', params: { id: '42' } }"></gio-go-back-button>`,
})
class TestComponent {}

describe('GioFormTagsInputModule', () => {
  let fixture: ComponentFixture<TestComponent>;
  const fakeAjsState = {
    go: jest.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, GioGoBackButtonModule],
      providers: [{ provide: UIRouterState, useValue: fakeAjsState }],
    });
    fixture = TestBed.createComponent(TestComponent);
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should go to state', async () => {
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button').click();

    expect(fakeAjsState.go).toHaveBeenCalledWith('state', { id: '42' }, undefined);
  });
});
