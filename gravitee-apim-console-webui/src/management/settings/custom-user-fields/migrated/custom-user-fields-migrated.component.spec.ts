import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CustomUserFieldsMigratedComponent } from './custom-user-fields-migrated.component';

describe('CustomUserFieldsMigratedComponent', () => {
  let component: CustomUserFieldsMigratedComponent;
  let fixture: ComponentFixture<CustomUserFieldsMigratedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomUserFieldsMigratedComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CustomUserFieldsMigratedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
