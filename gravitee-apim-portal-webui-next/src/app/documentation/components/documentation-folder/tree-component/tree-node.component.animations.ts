import {animate, style, transition, trigger} from "@angular/animations";

export const treeNodeComponentAnimations = [
  trigger('slideInOut', [
    transition(':enter', [
      style({opacity: 0, transform: 'translateY(-20px)'}),
      animate('80ms ease-in', style({opacity: 1, transform: 'translateY(0)'})
      )
    ]),
    transition(':leave', [
      animate('80ms ease-in', style({opacity: 0, transform: 'translateY(-20px)'}))
    ])
  ])
];
