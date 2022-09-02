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
import type { DictionaryProviderEntity } from './DictionaryProviderEntity';
import {
    DictionaryProviderEntityFromJSON,
    DictionaryProviderEntityFromJSONTyped,
    DictionaryProviderEntityToJSON,
} from './DictionaryProviderEntity';
import type { DictionaryTriggerEntity } from './DictionaryTriggerEntity';
import {
    DictionaryTriggerEntityFromJSON,
    DictionaryTriggerEntityFromJSONTyped,
    DictionaryTriggerEntityToJSON,
} from './DictionaryTriggerEntity';
import type { DictionaryType } from './DictionaryType';
import {
    DictionaryTypeFromJSON,
    DictionaryTypeFromJSONTyped,
    DictionaryTypeToJSON,
} from './DictionaryType';

/**
 * 
 * @export
 * @interface NewDictionaryEntity
 */
export interface NewDictionaryEntity {
    /**
     * 
     * @type {string}
     * @memberof NewDictionaryEntity
     */
    description?: string;
    /**
     * 
     * @type {string}
     * @memberof NewDictionaryEntity
     */
    name: string;
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof NewDictionaryEntity
     */
    properties?: { [key: string]: string; };
    /**
     * 
     * @type {DictionaryProviderEntity}
     * @memberof NewDictionaryEntity
     */
    provider?: DictionaryProviderEntity;
    /**
     * 
     * @type {DictionaryTriggerEntity}
     * @memberof NewDictionaryEntity
     */
    trigger?: DictionaryTriggerEntity;
    /**
     * 
     * @type {DictionaryType}
     * @memberof NewDictionaryEntity
     */
    type: DictionaryType;
}

/**
 * Check if a given object implements the NewDictionaryEntity interface.
 */
export function instanceOfNewDictionaryEntity(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "type" in value;

    return isInstance;
}

export function NewDictionaryEntityFromJSON(json: any): NewDictionaryEntity {
    return NewDictionaryEntityFromJSONTyped(json, false);
}

export function NewDictionaryEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): NewDictionaryEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'description': !exists(json, 'description') ? undefined : json['description'],
        'name': json['name'],
        'properties': !exists(json, 'properties') ? undefined : json['properties'],
        'provider': !exists(json, 'provider') ? undefined : DictionaryProviderEntityFromJSON(json['provider']),
        'trigger': !exists(json, 'trigger') ? undefined : DictionaryTriggerEntityFromJSON(json['trigger']),
        'type': DictionaryTypeFromJSON(json['type']),
    };
}

export function NewDictionaryEntityToJSON(value?: NewDictionaryEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'description': value.description,
        'name': value.name,
        'properties': value.properties,
        'provider': DictionaryProviderEntityToJSON(value.provider),
        'trigger': DictionaryTriggerEntityToJSON(value.trigger),
        'type': DictionaryTypeToJSON(value.type),
    };
}

