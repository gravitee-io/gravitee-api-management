/**
 * Gravitee.io Portal Rest API
 * API dedicated to the devportal part of Gravitee
 *
 * Contact: contact@graviteesource.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { DefinitionVersion } from './definitionVersion';
import { RatingSummary } from './ratingSummary';
import { User } from './user';
import { ApiType } from './apiType';
import { ListenerType } from './listenerType';
import { Page } from './page';
import { Mcp } from './mcp';
import { ApiLinks } from './apiLinks';
import { Plan } from './plan';


/**
 * Describes an API. 
 */
export interface Api { 
    /**
     * Unique identifier of an API.
     */
    id: string;
    definitionVersion?: DefinitionVersion;
    /**
     * Name of the API.
     */
    name: string;
    /**
     * Version of the API.
     */
    version: string;
    /**
     * Description of the API.
     */
    description: string;
    type?: ApiType;
    /**
     * Whether or not the API is in draft.
     */
    draft?: boolean;
    /**
     * Whether or not the API is public.
     */
    _public?: boolean;
    /**
     * Whether or not the API is running.
     */
    running?: boolean;
    /**
     * List of all the available endpoints to call the API.
     */
    entrypoints?: Array<string>;
    listener_type?: ListenerType;
    /**
     * List of labels linked to this API.
     */
    labels?: Array<string>;
    mcp?: Mcp;
    owner: User;
    /**
     * Create date and time.
     */
    created_at?: Date;
    /**
     * Last update date and time.
     */
    updated_at?: Date;
    /**
     * List of category keys this API belongs to. Example: [\"media\", \"health\"] 
     */
    categories?: Array<string>;
    rating_summary?: RatingSummary;
    _links?: ApiLinks;
    /**
     * Only returned with (*)/apis/{apiId}*. Need *include* query param to contain \'pages\'.  The documentation pages of this API. Same as (*)/apis/{apiId}/pages*. 
     */
    pages?: Array<Page>;
    /**
     * Only returned with (*)/apis/{apiId}*. Need *include* query param to contain \'plans\'.  The plans of this API. Same as (*)/apis/{apiId}/plans*. 
     */
    plans?: Array<Plan>;
}

