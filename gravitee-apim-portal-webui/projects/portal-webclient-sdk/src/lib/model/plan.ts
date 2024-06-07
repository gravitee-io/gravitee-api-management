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
import { PlanUsageConfiguration } from './planUsageConfiguration';
import { PlanMode } from './planMode';


export interface Plan { 
    /**
     * Unique identifier of a plan.
     */
    id: string;
    /**
     * Name of the plan.
     */
    name: string;
    /**
     * Security used with this plan.
     */
    security: Plan.SecurityEnum;
    /**
     * Description of the plan.
     */
    description: string;
    /**
     * List of additional terms to describe the plan.
     */
    characteristics?: Array<string>;
    /**
     * Type of validation for subscription requests.
     */
    validation: Plan.ValidationEnum;
    /**
     * Priority order
     */
    order: number;
    /**
     * True if a comment is required when a subscription is created.
     */
    comment_required: boolean;
    /**
     * Content of the message sent to a user creating a subscription.
     */
    comment_question?: string;
    /**
     * The  page reference with general conditions of use for the API.
     */
    general_conditions?: string;
    mode: PlanMode;
    usage_configuration?: PlanUsageConfiguration;
}
export namespace Plan {
    export type SecurityEnum = 'API_KEY' | 'KEY_LESS' | 'JWT' | 'OAUTH2';
    export const SecurityEnum = {
        APIKEY: 'API_KEY' as SecurityEnum,
        KEYLESS: 'KEY_LESS' as SecurityEnum,
        JWT: 'JWT' as SecurityEnum,
        OAUTH2: 'OAUTH2' as SecurityEnum
    };
    export type ValidationEnum = 'AUTO' | 'MANUAL';
    export const ValidationEnum = {
        AUTO: 'AUTO' as ValidationEnum,
        MANUAL: 'MANUAL' as ValidationEnum
    };
}


