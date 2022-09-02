/* tslint:disable */
/* eslint-disable */
/**
 * Gravitee.io - Management API
 * Some news resources are in alpha version. This implies that they are likely to be modified or even removed in future versions. They are marked with the 🧪 symbol
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
import type { Enabled } from './Enabled';
import {
    EnabledFromJSON,
    EnabledFromJSONTyped,
    EnabledToJSON,
} from './Enabled';

/**
 * 
 * @export
 * @interface PortalUserCreation
 */
export interface PortalUserCreation {
    /**
     * 
     * @type {Enabled}
     * @memberof PortalUserCreation
     */
    automaticValidation?: Enabled;
    /**
     * 
     * @type {boolean}
     * @memberof PortalUserCreation
     */
    enabled?: boolean;
}

/**
 * Check if a given object implements the PortalUserCreation interface.
 */
export function instanceOfPortalUserCreation(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PortalUserCreationFromJSON(json: any): PortalUserCreation {
    return PortalUserCreationFromJSONTyped(json, false);
}

export function PortalUserCreationFromJSONTyped(json: any, ignoreDiscriminator: boolean): PortalUserCreation {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'automaticValidation': !exists(json, 'automaticValidation') ? undefined : EnabledFromJSON(json['automaticValidation']),
        'enabled': !exists(json, 'enabled') ? undefined : json['enabled'],
    };
}

export function PortalUserCreationToJSON(value?: PortalUserCreation | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'automaticValidation': EnabledToJSON(value.automaticValidation),
        'enabled': value.enabled,
    };
}

