import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-integrations',
  template: require('./integrations.component.html'),
  styles: [require('./integrations.component.scss')]
})
export class IntegrationsComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }

}
