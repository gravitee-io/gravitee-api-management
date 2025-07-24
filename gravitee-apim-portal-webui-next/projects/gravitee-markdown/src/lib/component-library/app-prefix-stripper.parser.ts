import { HookParserEntry } from 'ngx-dynamic-hooks';
import { CopyCodeComponent } from './components/copy-code/copy-code.component';
import { ButtonComponent } from './components/button/button.component';
import { CardComponent } from './components/card/card.component';
import { CardActionsComponent } from './components/card/card-actions.component';
import { ImageComponent } from './components/image/image.component';
import { GridComponent } from './components/grid/grid.component';
import { GridCellComponent } from './components/grid/grid-cell.component';

export const appPrefixStripperParsers: HookParserEntry[] = [
  {
    component: CopyCodeComponent,
    selector: 'copy-code',
  },
  {
    component: ButtonComponent,
    selector: 'app-button',
  },
  {
    component: CardComponent,
    selector: 'app-card',
  },
  {
    component: CardActionsComponent,
    selector: 'card-actions',
  },
  {
    component: ImageComponent,
    selector: 'app-image',
  },
  {
    component: GridComponent,
    selector: 'app-grid',
  },
  {
    component: GridCellComponent,
    selector: 'app-grid-cell',
  },
];
