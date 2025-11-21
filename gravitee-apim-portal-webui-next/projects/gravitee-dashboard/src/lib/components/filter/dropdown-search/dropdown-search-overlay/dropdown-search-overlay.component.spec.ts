import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DropdownSearchOverlayComponent } from './dropdown-search-overlay.component';

describe('DropdownSearchOverlayComponent', () => {
  let component: DropdownSearchOverlayComponent;
  let fixture: ComponentFixture<DropdownSearchOverlayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DropdownSearchOverlayComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DropdownSearchOverlayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
