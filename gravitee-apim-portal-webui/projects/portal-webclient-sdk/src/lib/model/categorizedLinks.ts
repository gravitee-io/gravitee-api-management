/**
 * Gravitee.io Portal Rest API
 * API dedicated to the devportal part of Gravitee
 *
 * The version of the OpenAPI document: 3.14.0-SNAPSHOT
 * Contact: contact@graviteesource.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { Link } from './link';


export interface CategorizedLinks { 
    /**
     * true if the links of this category are in system folder and not in a subfolder.
     */
    root?: boolean;
    /**
     * name of the group of links
     */
    category?: string;
    links?: Array<Link>;
}

