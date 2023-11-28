import { Component, ElementRef, Injector, SimpleChange } from '@angular/core';
import { UpgradeComponent } from '@angular/upgrade/static';
import { ActivatedRoute } from '@angular/router';

@Component({
  template: '',
  selector: 'settings-analytics',
  host: {
    class: 'bootstrap',
  },
})
export class SettingsAnalyticsComponent extends UpgradeComponent {
  constructor(elementRef: ElementRef, injector: Injector, private readonly activatedRoute: ActivatedRoute) {
    super('settingsAnalyticsAjs', elementRef, injector);
  }

  ngOnInit() {
    // Hack to Force the binding between Angular and AngularJS
    this.ngOnChanges({
      activatedRoute: new SimpleChange(null, this.activatedRoute, true),
    });

    super.ngOnInit();
  }
}
