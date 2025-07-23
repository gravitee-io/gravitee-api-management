import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GRAVITEE_MONACO_EDITOR_CONFIG } from './data/gravitee-monaco-editor-config';
import { GraviteeMonacoWrapperComponent } from './gravitee-monaco-wrapper.component';
import { GraviteeMonacoWrapperModule } from './gravitee-monaco-wrapper.module';

describe('GraviteeMonacoWrapperComponent', () => {
  let component: GraviteeMonacoWrapperComponent;
  let fixture: ComponentFixture<GraviteeMonacoWrapperComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GraviteeMonacoWrapperModule],
      providers: [
        {
          provide: GRAVITEE_MONACO_EDITOR_CONFIG,
          useValue: {} as any,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GraviteeMonacoWrapperComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
