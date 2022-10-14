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

import { GioCircularPercentageComponent } from './gio-circular-percentage.component';
import { GioCircularPercentageModule } from './gio-circular-percentage.module';

describe('GioCircularPercentageComponent', () => {
  let component: GioCircularPercentageComponent;
  let fixture: ComponentFixture<GioCircularPercentageComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioCircularPercentageModule],
    });
    fixture = TestBed.createComponent(GioCircularPercentageComponent);
    component = fixture.componentInstance;
  });

  it('should complete avatar with jdenticon', () => {
    fixture.detectChanges();
    component.score = 50;

    expect(fixture.nativeElement.querySelector('.circular__percentage').innerHTML).toEqual('0%');
  });
});
