import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SectionEditorDialogComponent } from './section-editor-dialog.component';

describe('AddSectionDialogComponent', () => {
  let component: SectionEditorDialogComponent;
  let fixture: ComponentFixture<SectionEditorDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SectionEditorDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SectionEditorDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
