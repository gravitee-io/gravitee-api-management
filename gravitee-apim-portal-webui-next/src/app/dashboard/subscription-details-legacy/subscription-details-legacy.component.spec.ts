import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SubscriptionDetailsLegacyComponent } from './subscription-details-legacy.component';

describe('SubscriptionDetailsLegacyComponent', () => {
  let component: SubscriptionDetailsLegacyComponent;
  let fixture: ComponentFixture<SubscriptionDetailsLegacyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionDetailsLegacyComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SubscriptionDetailsLegacyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
