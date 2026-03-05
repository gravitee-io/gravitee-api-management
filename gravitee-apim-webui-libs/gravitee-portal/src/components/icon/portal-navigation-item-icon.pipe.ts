import { Pipe, PipeTransform } from '@angular/core';

import { PortalNavigationItemType } from '../../entities/portalNavigationItem';

const portalNavigationItemTypeToSvgIcon: Readonly<Record<PortalNavigationItemType, string>> = {
  PAGE: 'gio:page',
  LINK: 'gio:link',
  FOLDER: 'gio:folder',
  API: 'gio:folder-api',
};

@Pipe({
  name: 'portalNavigationItemIcon',
  standalone: true,
})
export class PortalNavigationItemIconPipe implements PipeTransform {
  transform(type: PortalNavigationItemType): string {
    return portalNavigationItemTypeToSvgIcon[type];
  }
}
