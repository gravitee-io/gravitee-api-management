import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GenericFilterBarComponent } from './generic-filter-bar.component';

describe('GenericFilterBarComponent', () => {
  let component: GenericFilterBarComponent;
  let fixture: ComponentFixture<GenericFilterBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GenericFilterBarComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GenericFilterBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
