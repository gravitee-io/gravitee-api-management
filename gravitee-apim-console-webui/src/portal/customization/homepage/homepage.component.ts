import { Component } from '@angular/core';
import {GraviteeMarkdownEditorComponent} from 'gravitee-markdown';

@Component({
  selector: 'homepage',
  imports: [GraviteeMarkdownEditorComponent],
  templateUrl: './homepage.component.html',
  styleUrl: './homepage.component.scss'
})
export class HomepageComponent {

}
