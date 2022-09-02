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
import type { ThemeDefinition } from './ThemeDefinition';
import {
    ThemeDefinitionFromJSON,
    ThemeDefinitionFromJSONTyped,
    ThemeDefinitionToJSON,
} from './ThemeDefinition';

/**
 * 
 * @export
 * @interface UpdateThemeEntity
 */
export interface UpdateThemeEntity {
    /**
     * 
     * @type {string}
     * @memberof UpdateThemeEntity
     */
    backgroundImage?: string;
    /**
     * 
     * @type {ThemeDefinition}
     * @memberof UpdateThemeEntity
     */
    definition: ThemeDefinition;
    /**
     * 
     * @type {boolean}
     * @memberof UpdateThemeEntity
     */
    enabled?: boolean;
    /**
     * 
     * @type {string}
     * @memberof UpdateThemeEntity
     */
    favicon?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateThemeEntity
     */
    id?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateThemeEntity
     */
    logo?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateThemeEntity
     */
    name: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateThemeEntity
     */
    optionalLogo?: string;
}

/**
 * Check if a given object implements the UpdateThemeEntity interface.
 */
export function instanceOfUpdateThemeEntity(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "definition" in value;
    isInstance = isInstance && "name" in value;

    return isInstance;
}

export function UpdateThemeEntityFromJSON(json: any): UpdateThemeEntity {
    return UpdateThemeEntityFromJSONTyped(json, false);
}

export function UpdateThemeEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): UpdateThemeEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'backgroundImage': !exists(json, 'backgroundImage') ? undefined : json['backgroundImage'],
        'definition': ThemeDefinitionFromJSON(json['definition']),
        'enabled': !exists(json, 'enabled') ? undefined : json['enabled'],
        'favicon': !exists(json, 'favicon') ? undefined : json['favicon'],
        'id': !exists(json, 'id') ? undefined : json['id'],
        'logo': !exists(json, 'logo') ? undefined : json['logo'],
        'name': json['name'],
        'optionalLogo': !exists(json, 'optionalLogo') ? undefined : json['optionalLogo'],
    };
}

export function UpdateThemeEntityToJSON(value?: UpdateThemeEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'backgroundImage': value.backgroundImage,
        'definition': ThemeDefinitionToJSON(value.definition),
        'enabled': value.enabled,
        'favicon': value.favicon,
        'id': value.id,
        'logo': value.logo,
        'name': value.name,
        'optionalLogo': value.optionalLogo,
    };
}

