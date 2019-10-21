import {TestBed, async} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {AppComponent} from './app.component';
import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {TranslateService} from '@ngx-translate/core';
import {provideMagicalMock} from './mock.helper.spec';
import {of} from 'rxjs';
import any = jasmine.any;

describe('AppComponent', () => {
  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule
      ],
      declarations: [
        AppComponent
      ],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ],
      providers: [
        AppComponent,
        provideMagicalMock(TranslateService),
        provideMagicalMock(Title),
      ]
    }).compileComponents();
  }));


  let fixture;
  let app;
  let translateServiceMock: jasmine.SpyObj<TranslateService>;
  let titleMock: jasmine.SpyObj<Title>;
  beforeEach(() => {
    translateServiceMock = TestBed.get(TranslateService);
    translateServiceMock.getBrowserLang.and.returnValue('fr');
    translateServiceMock.get.and.returnValue(of(''));
    titleMock = TestBed.get(Title);

    fixture = TestBed.createComponent(AppComponent);
    app = fixture.componentInstance;
    fixture.whenStable().then(() => {
      fixture.detectChanges();
    });
  });


  it('should create the app', () => {
    fixture.whenStable().then(() => {
      fixture.detectChanges();
      expect(app).toBeTruthy();
    });
  });

  it(`should create routes'`, () => {
    fixture.whenStable().then(() => {
      fixture.detectChanges();
      expect(app.routes).toBeDefined();
      expect(app.routes.length).toBeGreaterThan(1);
      expect(app.routes[0]).toEqual(any(Promise));
    });

  });

});
