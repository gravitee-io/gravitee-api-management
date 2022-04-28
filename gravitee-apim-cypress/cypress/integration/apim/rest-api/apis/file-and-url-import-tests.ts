/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ADMIN_USER, API_PUBLISHER_USER } from '@fakers/users/users';
import { deleteApi, deployApi, importSwaggerApi } from '@commands/management/api-management-commands';
import { getPages } from '@commands/management/api-pages-management-commands';
import { ApiImport, ImportSwaggerDescriptorEntityFormat, ImportSwaggerDescriptorEntityType } from '@model/api-imports';
import { requestGateway } from 'support/common/http.commands';
import swaggerv2 from 'fixtures/json/petstore_swaggerv2.json';
import openavpiv3 from 'fixtures/json/petstore_openapiv3.json';

describe('Parameterized tests for API import via file/URL', () => {
  const wsdlapi = `<?xml version="1.0" encoding="UTF-8"?>
  <definitions xmlns="http://schemas.xmlsoap.org/wsdl/" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" xmlns:soap12="http://schemas.xmlsoap.org/wsdl/soap12/" xmlns:tns="http://www.oorsprong.org/websamples.countryinfo" name="CountryInfoService" targetNamespace="http://www.oorsprong.org/websamples.countryinfo">
    <types>
      <xs:schema elementFormDefault="qualified" targetNamespace="http://www.oorsprong.org/websamples.countryinfo">
        <xs:complexType name="tContinent">
          <xs:sequence>
            <xs:element name="sCode" type="xs:string"/>
            <xs:element name="sName" type="xs:string"/>
          </xs:sequence>
        </xs:complexType>
        <xs:complexType name="tCurrency">
          <xs:sequence>
            <xs:element name="sISOCode" type="xs:string"/>
            <xs:element name="sName" type="xs:string"/>
          </xs:sequence>
        </xs:complexType>
        <xs:complexType name="tCountryCodeAndName">
          <xs:sequence>
            <xs:element name="sISOCode" type="xs:string"/>
            <xs:element name="sName" type="xs:string"/>
          </xs:sequence>
        </xs:complexType>
        <xs:complexType name="tCountryCodeAndNameGroupedByContinent">
          <xs:sequence>
            <xs:element name="Continent" type="tns:tContinent"/>
            <xs:element name="CountryCodeAndNames" type="tns:ArrayOftCountryCodeAndName"/>
          </xs:sequence>
        </xs:complexType>
        <xs:complexType name="tCountryInfo">
          <xs:sequence>
            <xs:element name="sISOCode" type="xs:string"/>
            <xs:element name="sName" type="xs:string"/>
            <xs:element name="sCapitalCity" type="xs:string"/>
            <xs:element name="sPhoneCode" type="xs:string"/>
            <xs:element name="sContinentCode" type="xs:string"/>
            <xs:element name="sCurrencyISOCode" type="xs:string"/>
            <xs:element name="sCountryFlag" type="xs:string"/>
            <xs:element name="Languages" type="tns:ArrayOftLanguage"/>
          </xs:sequence>
        </xs:complexType>
        <xs:complexType name="tLanguage">
          <xs:sequence>
            <xs:element name="sISOCode" type="xs:string"/>
            <xs:element name="sName" type="xs:string"/>
          </xs:sequence>
        </xs:complexType>
        <xs:complexType name="ArrayOftCountryCodeAndName">
          <xs:sequence>
            <xs:element name="tCountryCodeAndName" type="tns:tCountryCodeAndName" minOccurs="0" maxOccurs="unbounded" nillable="true"/>
          </xs:sequence>
        </xs:complexType>
        <xs:complexType name="ArrayOftLanguage">
          <xs:sequence>
            <xs:element name="tLanguage" type="tns:tLanguage" minOccurs="0" maxOccurs="unbounded" nillable="true"/>
          </xs:sequence>
        </xs:complexType>
        <xs:complexType name="ArrayOftContinent">
          <xs:sequence>
            <xs:element name="tContinent" type="tns:tContinent" minOccurs="0" maxOccurs="unbounded" nillable="true"/>
          </xs:sequence>
        </xs:complexType>
        <xs:complexType name="ArrayOftCurrency">
          <xs:sequence>
            <xs:element name="tCurrency" type="tns:tCurrency" minOccurs="0" maxOccurs="unbounded" nillable="true"/>
          </xs:sequence>
        </xs:complexType>
        <xs:complexType name="ArrayOftCountryCodeAndNameGroupedByContinent">
          <xs:sequence>
            <xs:element name="tCountryCodeAndNameGroupedByContinent" type="tns:tCountryCodeAndNameGroupedByContinent" minOccurs="0" maxOccurs="unbounded" nillable="true"/>
          </xs:sequence>
        </xs:complexType>
        <xs:complexType name="ArrayOftCountryInfo">
          <xs:sequence>
            <xs:element name="tCountryInfo" type="tns:tCountryInfo" minOccurs="0" maxOccurs="unbounded" nillable="true"/>
          </xs:sequence>
        </xs:complexType>
        <xs:element name="ListOfContinentsByName">
          <xs:complexType>
            <xs:sequence/>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfContinentsByNameResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="ListOfContinentsByNameResult" type="tns:ArrayOftContinent"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfContinentsByCode">
          <xs:complexType>
            <xs:sequence/>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfContinentsByCodeResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="ListOfContinentsByCodeResult" type="tns:ArrayOftContinent"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfCurrenciesByName">
          <xs:complexType>
            <xs:sequence/>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfCurrenciesByNameResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="ListOfCurrenciesByNameResult" type="tns:ArrayOftCurrency"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfCurrenciesByCode">
          <xs:complexType>
            <xs:sequence/>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfCurrenciesByCodeResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="ListOfCurrenciesByCodeResult" type="tns:ArrayOftCurrency"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CurrencyName">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="sCurrencyISOCode" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CurrencyNameResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="CurrencyNameResult" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfCountryNamesByCode">
          <xs:complexType>
            <xs:sequence/>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfCountryNamesByCodeResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="ListOfCountryNamesByCodeResult" type="tns:ArrayOftCountryCodeAndName"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfCountryNamesByName">
          <xs:complexType>
            <xs:sequence/>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfCountryNamesByNameResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="ListOfCountryNamesByNameResult" type="tns:ArrayOftCountryCodeAndName"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfCountryNamesGroupedByContinent">
          <xs:complexType>
            <xs:sequence/>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfCountryNamesGroupedByContinentResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="ListOfCountryNamesGroupedByContinentResult" type="tns:ArrayOftCountryCodeAndNameGroupedByContinent"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CountryName">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="sCountryISOCode" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CountryNameResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="CountryNameResult" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CountryISOCode">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="sCountryName" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CountryISOCodeResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="CountryISOCodeResult" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CapitalCity">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="sCountryISOCode" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CapitalCityResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="CapitalCityResult" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CountryCurrency">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="sCountryISOCode" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CountryCurrencyResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="CountryCurrencyResult" type="tns:tCurrency"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CountryFlag">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="sCountryISOCode" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CountryFlagResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="CountryFlagResult" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CountryIntPhoneCode">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="sCountryISOCode" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CountryIntPhoneCodeResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="CountryIntPhoneCodeResult" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="FullCountryInfo">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="sCountryISOCode" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="FullCountryInfoResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="FullCountryInfoResult" type="tns:tCountryInfo"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="FullCountryInfoAllCountries">
          <xs:complexType>
            <xs:sequence/>
          </xs:complexType>
        </xs:element>
        <xs:element name="FullCountryInfoAllCountriesResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="FullCountryInfoAllCountriesResult" type="tns:ArrayOftCountryInfo"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CountriesUsingCurrency">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="sISOCurrencyCode" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="CountriesUsingCurrencyResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="CountriesUsingCurrencyResult" type="tns:ArrayOftCountryCodeAndName"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfLanguagesByName">
          <xs:complexType>
            <xs:sequence/>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfLanguagesByNameResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="ListOfLanguagesByNameResult" type="tns:ArrayOftLanguage"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfLanguagesByCode">
          <xs:complexType>
            <xs:sequence/>
          </xs:complexType>
        </xs:element>
        <xs:element name="ListOfLanguagesByCodeResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="ListOfLanguagesByCodeResult" type="tns:ArrayOftLanguage"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="LanguageName">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="sISOCode" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="LanguageNameResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="LanguageNameResult" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="LanguageISOCode">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="sLanguageName" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
        <xs:element name="LanguageISOCodeResponse">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="LanguageISOCodeResult" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
      </xs:schema>
    </types>
    <message name="ListOfContinentsByNameSoapRequest">
      <part name="parameters" element="tns:ListOfContinentsByName"/>
    </message>
    <message name="ListOfContinentsByNameSoapResponse">
      <part name="parameters" element="tns:ListOfContinentsByNameResponse"/>
    </message>
    <message name="ListOfContinentsByCodeSoapRequest">
      <part name="parameters" element="tns:ListOfContinentsByCode"/>
    </message>
    <message name="ListOfContinentsByCodeSoapResponse">
      <part name="parameters" element="tns:ListOfContinentsByCodeResponse"/>
    </message>
    <message name="ListOfCurrenciesByNameSoapRequest">
      <part name="parameters" element="tns:ListOfCurrenciesByName"/>
    </message>
    <message name="ListOfCurrenciesByNameSoapResponse">
      <part name="parameters" element="tns:ListOfCurrenciesByNameResponse"/>
    </message>
    <message name="ListOfCurrenciesByCodeSoapRequest">
      <part name="parameters" element="tns:ListOfCurrenciesByCode"/>
    </message>
    <message name="ListOfCurrenciesByCodeSoapResponse">
      <part name="parameters" element="tns:ListOfCurrenciesByCodeResponse"/>
    </message>
    <message name="CurrencyNameSoapRequest">
      <part name="parameters" element="tns:CurrencyName"/>
    </message>
    <message name="CurrencyNameSoapResponse">
      <part name="parameters" element="tns:CurrencyNameResponse"/>
    </message>
    <message name="ListOfCountryNamesByCodeSoapRequest">
      <part name="parameters" element="tns:ListOfCountryNamesByCode"/>
    </message>
    <message name="ListOfCountryNamesByCodeSoapResponse">
      <part name="parameters" element="tns:ListOfCountryNamesByCodeResponse"/>
    </message>
    <message name="ListOfCountryNamesByNameSoapRequest">
      <part name="parameters" element="tns:ListOfCountryNamesByName"/>
    </message>
    <message name="ListOfCountryNamesByNameSoapResponse">
      <part name="parameters" element="tns:ListOfCountryNamesByNameResponse"/>
    </message>
    <message name="ListOfCountryNamesGroupedByContinentSoapRequest">
      <part name="parameters" element="tns:ListOfCountryNamesGroupedByContinent"/>
    </message>
    <message name="ListOfCountryNamesGroupedByContinentSoapResponse">
      <part name="parameters" element="tns:ListOfCountryNamesGroupedByContinentResponse"/>
    </message>
    <message name="CountryNameSoapRequest">
      <part name="parameters" element="tns:CountryName"/>
    </message>
    <message name="CountryNameSoapResponse">
      <part name="parameters" element="tns:CountryNameResponse"/>
    </message>
    <message name="CountryISOCodeSoapRequest">
      <part name="parameters" element="tns:CountryISOCode"/>
    </message>
    <message name="CountryISOCodeSoapResponse">
      <part name="parameters" element="tns:CountryISOCodeResponse"/>
    </message>
    <message name="CapitalCitySoapRequest">
      <part name="parameters" element="tns:CapitalCity"/>
    </message>
    <message name="CapitalCitySoapResponse">
      <part name="parameters" element="tns:CapitalCityResponse"/>
    </message>
    <message name="CountryCurrencySoapRequest">
      <part name="parameters" element="tns:CountryCurrency"/>
    </message>
    <message name="CountryCurrencySoapResponse">
      <part name="parameters" element="tns:CountryCurrencyResponse"/>
    </message>
    <message name="CountryFlagSoapRequest">
      <part name="parameters" element="tns:CountryFlag"/>
    </message>
    <message name="CountryFlagSoapResponse">
      <part name="parameters" element="tns:CountryFlagResponse"/>
    </message>
    <message name="CountryIntPhoneCodeSoapRequest">
      <part name="parameters" element="tns:CountryIntPhoneCode"/>
    </message>
    <message name="CountryIntPhoneCodeSoapResponse">
      <part name="parameters" element="tns:CountryIntPhoneCodeResponse"/>
    </message>
    <message name="FullCountryInfoSoapRequest">
      <part name="parameters" element="tns:FullCountryInfo"/>
    </message>
    <message name="FullCountryInfoSoapResponse">
      <part name="parameters" element="tns:FullCountryInfoResponse"/>
    </message>
    <message name="FullCountryInfoAllCountriesSoapRequest">
      <part name="parameters" element="tns:FullCountryInfoAllCountries"/>
    </message>
    <message name="FullCountryInfoAllCountriesSoapResponse">
      <part name="parameters" element="tns:FullCountryInfoAllCountriesResponse"/>
    </message>
    <message name="CountriesUsingCurrencySoapRequest">
      <part name="parameters" element="tns:CountriesUsingCurrency"/>
    </message>
    <message name="CountriesUsingCurrencySoapResponse">
      <part name="parameters" element="tns:CountriesUsingCurrencyResponse"/>
    </message>
    <message name="ListOfLanguagesByNameSoapRequest">
      <part name="parameters" element="tns:ListOfLanguagesByName"/>
    </message>
    <message name="ListOfLanguagesByNameSoapResponse">
      <part name="parameters" element="tns:ListOfLanguagesByNameResponse"/>
    </message>
    <message name="ListOfLanguagesByCodeSoapRequest">
      <part name="parameters" element="tns:ListOfLanguagesByCode"/>
    </message>
    <message name="ListOfLanguagesByCodeSoapResponse">
      <part name="parameters" element="tns:ListOfLanguagesByCodeResponse"/>
    </message>
    <message name="LanguageNameSoapRequest">
      <part name="parameters" element="tns:LanguageName"/>
    </message>
    <message name="LanguageNameSoapResponse">
      <part name="parameters" element="tns:LanguageNameResponse"/>
    </message>
    <message name="LanguageISOCodeSoapRequest">
      <part name="parameters" element="tns:LanguageISOCode"/>
    </message>
    <message name="LanguageISOCodeSoapResponse">
      <part name="parameters" element="tns:LanguageISOCodeResponse"/>
    </message>
    <portType name="CountryInfoServiceSoapType">
      <operation name="ListOfContinentsByName">
        <documentation>Returns a list of continents ordered by name.</documentation>
        <input message="tns:ListOfContinentsByNameSoapRequest"/>
        <output message="tns:ListOfContinentsByNameSoapResponse"/>
      </operation>
      <operation name="ListOfContinentsByCode">
        <documentation>Returns a list of continents ordered by code.</documentation>
        <input message="tns:ListOfContinentsByCodeSoapRequest"/>
        <output message="tns:ListOfContinentsByCodeSoapResponse"/>
      </operation>
      <operation name="ListOfCurrenciesByName">
        <documentation>Returns a list of currencies ordered by name.</documentation>
        <input message="tns:ListOfCurrenciesByNameSoapRequest"/>
        <output message="tns:ListOfCurrenciesByNameSoapResponse"/>
      </operation>
      <operation name="ListOfCurrenciesByCode">
        <documentation>Returns a list of currencies ordered by code.</documentation>
        <input message="tns:ListOfCurrenciesByCodeSoapRequest"/>
        <output message="tns:ListOfCurrenciesByCodeSoapResponse"/>
      </operation>
      <operation name="CurrencyName">
        <documentation>Returns the name of the currency (if found)</documentation>
        <input message="tns:CurrencyNameSoapRequest"/>
        <output message="tns:CurrencyNameSoapResponse"/>
      </operation>
      <operation name="ListOfCountryNamesByCode">
        <documentation>Returns a list of all stored counties ordered by ISO code</documentation>
        <input message="tns:ListOfCountryNamesByCodeSoapRequest"/>
        <output message="tns:ListOfCountryNamesByCodeSoapResponse"/>
      </operation>
      <operation name="ListOfCountryNamesByName">
        <documentation>Returns a list of all stored counties ordered by country name</documentation>
        <input message="tns:ListOfCountryNamesByNameSoapRequest"/>
        <output message="tns:ListOfCountryNamesByNameSoapResponse"/>
      </operation>
      <operation name="ListOfCountryNamesGroupedByContinent">
        <documentation>Returns a list of all stored counties grouped per continent</documentation>
        <input message="tns:ListOfCountryNamesGroupedByContinentSoapRequest"/>
        <output message="tns:ListOfCountryNamesGroupedByContinentSoapResponse"/>
      </operation>
      <operation name="CountryName">
        <documentation>Searches the database for a country by the passed ISO country code</documentation>
        <input message="tns:CountryNameSoapRequest"/>
        <output message="tns:CountryNameSoapResponse"/>
      </operation>
      <operation name="CountryISOCode">
        <documentation>This function tries to found a country based on the passed country name.</documentation>
        <input message="tns:CountryISOCodeSoapRequest"/>
        <output message="tns:CountryISOCodeSoapResponse"/>
      </operation>
      <operation name="CapitalCity">
        <documentation>Returns the  name of the captial city for the passed country code</documentation>
        <input message="tns:CapitalCitySoapRequest"/>
        <output message="tns:CapitalCitySoapResponse"/>
      </operation>
      <operation name="CountryCurrency">
        <documentation>Returns the currency ISO code and name for the passed country ISO code</documentation>
        <input message="tns:CountryCurrencySoapRequest"/>
        <output message="tns:CountryCurrencySoapResponse"/>
      </operation>
      <operation name="CountryFlag">
        <documentation>Returns a link to a picture of the country flag</documentation>
        <input message="tns:CountryFlagSoapRequest"/>
        <output message="tns:CountryFlagSoapResponse"/>
      </operation>
      <operation name="CountryIntPhoneCode">
        <documentation>Returns the internation phone code for the passed ISO country code</documentation>
        <input message="tns:CountryIntPhoneCodeSoapRequest"/>
        <output message="tns:CountryIntPhoneCodeSoapResponse"/>
      </operation>
      <operation name="FullCountryInfo">
        <documentation>Returns a struct with all the stored country information. Pass the ISO country code</documentation>
        <input message="tns:FullCountryInfoSoapRequest"/>
        <output message="tns:FullCountryInfoSoapResponse"/>
      </operation>
      <operation name="FullCountryInfoAllCountries">
        <documentation>Returns an array with all countries and all the language information stored</documentation>
        <input message="tns:FullCountryInfoAllCountriesSoapRequest"/>
        <output message="tns:FullCountryInfoAllCountriesSoapResponse"/>
      </operation>
      <operation name="CountriesUsingCurrency">
        <documentation>Returns a list of all countries that use the same currency code. Pass a ISO currency code</documentation>
        <input message="tns:CountriesUsingCurrencySoapRequest"/>
        <output message="tns:CountriesUsingCurrencySoapResponse"/>
      </operation>
      <operation name="ListOfLanguagesByName">
        <documentation>Returns an array of languages ordered by name</documentation>
        <input message="tns:ListOfLanguagesByNameSoapRequest"/>
        <output message="tns:ListOfLanguagesByNameSoapResponse"/>
      </operation>
      <operation name="ListOfLanguagesByCode">
        <documentation>Returns an array of languages ordered by code</documentation>
        <input message="tns:ListOfLanguagesByCodeSoapRequest"/>
        <output message="tns:ListOfLanguagesByCodeSoapResponse"/>
      </operation>
      <operation name="LanguageName">
        <documentation>Find a language name based on the passed ISO language code</documentation>
        <input message="tns:LanguageNameSoapRequest"/>
        <output message="tns:LanguageNameSoapResponse"/>
      </operation>
      <operation name="LanguageISOCode">
        <documentation>Find a language ISO code based on the passed language name</documentation>
        <input message="tns:LanguageISOCodeSoapRequest"/>
        <output message="tns:LanguageISOCodeSoapResponse"/>
      </operation>
    </portType>
    <binding name="CountryInfoServiceSoapBinding" type="tns:CountryInfoServiceSoapType">
      <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
      <operation name="ListOfContinentsByName">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfContinentsByCode">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfCurrenciesByName">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfCurrenciesByCode">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="CurrencyName">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfCountryNamesByCode">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfCountryNamesByName">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfCountryNamesGroupedByContinent">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="CountryName">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="CountryISOCode">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="CapitalCity">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="CountryCurrency">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="CountryFlag">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="CountryIntPhoneCode">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="FullCountryInfo">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="FullCountryInfoAllCountries">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="CountriesUsingCurrency">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfLanguagesByName">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfLanguagesByCode">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="LanguageName">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
      <operation name="LanguageISOCode">
        <soap:operation soapAction="" style="document"/>
        <input>
          <soap:body use="literal"/>
        </input>
        <output>
          <soap:body use="literal"/>
        </output>
      </operation>
    </binding>
    <binding name="CountryInfoServiceSoapBinding12" type="tns:CountryInfoServiceSoapType">
      <soap12:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
      <operation name="ListOfContinentsByName">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfContinentsByCode">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfCurrenciesByName">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfCurrenciesByCode">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="CurrencyName">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfCountryNamesByCode">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfCountryNamesByName">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfCountryNamesGroupedByContinent">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="CountryName">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="CountryISOCode">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="CapitalCity">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="CountryCurrency">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="CountryFlag">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="CountryIntPhoneCode">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="FullCountryInfo">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="FullCountryInfoAllCountries">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="CountriesUsingCurrency">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfLanguagesByName">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="ListOfLanguagesByCode">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="LanguageName">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
      <operation name="LanguageISOCode">
        <soap12:operation soapAction="" style="document"/>
        <input>
          <soap12:body use="literal"/>
        </input>
        <output>
          <soap12:body use="literal"/>
        </output>
      </operation>
    </binding>
    <service name="CountryInfoService">
      <documentation>This DataFlex Web Service opens up country information. 2 letter ISO codes are used for Country code. There are functions to retrieve the used Currency, Language, Capital City, Continent and Telephone code.</documentation>
      <port name="CountryInfoServiceSoap" binding="tns:CountryInfoServiceSoapBinding">
        <soap:address location="http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso"/>
      </port>
      <port name="CountryInfoServiceSoap12" binding="tns:CountryInfoServiceSoapBinding12">
        <soap12:address location="http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso"/>
      </port>
    </service>
  </definitions>`;
  const apiImportArray = [
    ['Swagger v2 file', JSON.stringify(swaggerv2)],
    ['OpenAPI v3 file', JSON.stringify(openavpiv3)],
    ['Swagger v2 URL', `${Cypress.env('localPetstore_v2')}/swagger.json`],
    ['OpenAPI v3 URL', `${Cypress.env('localPetstore_v3')}/openapi.json`],
    ['WSDL file', wsdlapi],
    ['WSDL URL', 'http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL'],
  ];

  apiImportArray.forEach((importType) => {
    const [apiDescription, swaggerImport] = importType;

    describe(`API import via ${apiDescription}`, () => {
      let apiId: string;
      let importAttributes = {
        with_policies: [],
        format: ImportSwaggerDescriptorEntityFormat.API,
        type: ImportSwaggerDescriptorEntityType.INLINE,
      };

      if (apiDescription.includes('WSDL')) {
        importAttributes.format = ImportSwaggerDescriptorEntityFormat.WSDL;
      }

      if (swaggerImport.startsWith('http')) {
        importAttributes.type = ImportSwaggerDescriptorEntityType.URL;
      }

      afterEach(() => deleteApi(ADMIN_USER, apiId));

      it('should import API without creating a documentation', () => {
        importSwaggerApi(API_PUBLISHER_USER, swaggerImport, importAttributes)
          .created()
          .its('body')
          .then((api) => {
            apiId = api.id;
            expect(api.id).to.be.a('string').and.not.to.be.empty;
            expect(api.visibility).to.equal('PRIVATE');
            expect(api.state).to.equal('STOPPED');
            getPages(API_PUBLISHER_USER, apiId).ok().its('body').should('have.length', 1).should('not.have.a.property', 'SWAGGER');
          });
      });

      it('should import API and create a swagger documentation', () => {
        importSwaggerApi(API_PUBLISHER_USER, swaggerImport, { ...importAttributes, with_documentation: true })
          .created()
          .its('body')
          .then((api) => {
            apiId = api.id;
            expect(api.id).to.be.a('string').and.not.to.be.empty;
            expect(api.visibility).to.equal('PRIVATE');
            expect(api.state).to.equal('STOPPED');
            getPages(API_PUBLISHER_USER, apiId)
              .ok()
              .its('body')
              .should('have.length', 2)
              .its(1)
              .should((swaggerEntry) => {
                expect(swaggerEntry).to.have.property('id').and.not.to.be.empty;
                expect(swaggerEntry).to.have.property('type', 'SWAGGER');
                expect(swaggerEntry).to.have.property('content').and.not.to.be.empty;
              });
          });
      });

      it('should fail to import the same Swagger API again', () => {
        importSwaggerApi(API_PUBLISHER_USER, swaggerImport, importAttributes)
          .created()
          .its('body')
          .then((api) => {
            apiId = api.id;
            importSwaggerApi(API_PUBLISHER_USER, swaggerImport, importAttributes)
              .badRequest()
              .its('body.message')
              .should('equal', `The path [${api.context_path}/] is already covered by an other API.`);
          });
      });

      it('should import API and create a path (to add policies) for every declared Swagger path', () => {
        importSwaggerApi(API_PUBLISHER_USER, swaggerImport, { ...importAttributes, with_policy_paths: true })
          .created()
          .its('body')
          .then((api) => {
            apiId = api.id;
            if (apiDescription.includes('WSDL')) {
              deployApi(API_PUBLISHER_USER, apiId).its('body.flows').should('have.length', 42);
            } else {
              if (apiDescription.includes('Swagger v2')) {
                deployApi(API_PUBLISHER_USER, apiId).its('body.flows').should('have.length', 20);
              } else {
                deployApi(API_PUBLISHER_USER, apiId).its('body.flows').should('have.length', 19);
              }
            }
          });
      });
    });
  });
});

describe('API import with no provided import data', () => {
  it('should fail when trying to import an empty file/URL', () => {
    importSwaggerApi(API_PUBLISHER_USER, '').its('status').should('equal', 500);
  });
});

describe('Test API endpoint policies (Swagger v2 only)', () => {
  let mockPolicyApi: ApiImport;
  let jsonValidationPolicyApi: ApiImport;
  let noExtrasApi: ApiImport;
  let xmlValidationPolicyApi: ApiImport;
  let pathMappingApi: ApiImport;
  let requestValidationApi: ApiImport;
  let swaggerImport = JSON.stringify(swaggerv2);

  before(() => {
    {
      cy.log('-----  Import a swagger API without any extra options selected  -----');
      const swaggerImportAttributes = {
        with_path_mapping: false,
      };
      cy.createAndStartApiFromSwagger(swaggerImport, swaggerImportAttributes).then((api) => (noExtrasApi = api));
    }

    {
      cy.log('-----  Import a swagger API with mock policies  -----');
      const swaggerImportAttributes = {
        with_policy_paths: true,
        with_policies: ['mock'],
      };
      cy.createAndStartApiFromSwagger(swaggerImport, swaggerImportAttributes).then((api) => (mockPolicyApi = api));
    }

    {
      cy.log('-----  Import a swagger API with JSON-Validation policies  -----');
      const swaggerImportAttributes = {
        with_policy_paths: true,
        with_policies: ['json-validation'],
      };
      cy.createAndStartApiFromSwagger(swaggerImport, swaggerImportAttributes).then((api) => (jsonValidationPolicyApi = api));
    }

    {
      cy.log('-----  Import a swagger API with XML-Validation policies  -----');
      const swaggerImportAttributes = {
        with_policy_paths: true,
        with_policies: ['xml-validation'],
      };
      cy.createAndStartApiFromSwagger(swaggerImport, swaggerImportAttributes).then((api) => (xmlValidationPolicyApi = api));
    }

    {
      cy.log('-----  Import a swagger API with Path-Mapping  -----');
      const swaggerImportAttributes = {
        with_path_mapping: true,
      };
      cy.createAndStartApiFromSwagger(swaggerImport, swaggerImportAttributes).then((api) => (pathMappingApi = api));
    }

    {
      cy.log('-----  Import a swagger API with Validate-Request policy  -----');
      const swaggerImportAttributes = {
        with_policy_paths: true,
        with_policies: ['policy-request-validation'],
      };
      cy.createAndStartApiFromSwagger(swaggerImport, swaggerImportAttributes).then((api) => (requestValidationApi = api));
    }
  });

  after(() => {
    cy.teardownApi(noExtrasApi);
    cy.teardownApi(mockPolicyApi);
    cy.teardownApi(jsonValidationPolicyApi);
    cy.teardownApi(xmlValidationPolicyApi);
    cy.teardownApi(pathMappingApi);
    cy.teardownApi(requestValidationApi);
  });

  describe('Test without any extra options selected', () => {
    it('should successfully connect to API endpoint', () => {
      requestGateway({ url: `${Cypress.env('gatewayServer')}${noExtrasApi.context_path}/pet/findByStatus?status=available` })
        .its('body')
        .should('have.length', 7)
        .its('0.name')
        .should('equal', 'Cat 1');
    });
  });

  describe('Tests mock path policy', () => {
    it('should get a mocked response when trying to reach API endpoint', () => {
      requestGateway({ url: `${Cypress.env('gatewayServer')}${mockPolicyApi.context_path}/pet/findByStatus?status=available` })
        .its('body.category.name')
        .should('equal', 'Mocked string');
    });
  });

  describe('Tests JSON-Validation path policy', () => {
    it('should fail with BAD REQUEST (400) when sending data using an invalid JSON schema', () => {
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${jsonValidationPolicyApi.context_path}/pet`,
          method: 'PUT',
          body: {
            invalidProperty: 'invalid value',
          },
        },
        {
          validWhen: (response) => response.status === 400,
        },
      ).should((response) => {
        expect(response.body.message).to.equal('Bad Request');
      });
    });

    it('should successfully connect to API endpoint if JSON schema is valid', () => {
      const body = {
        id: 2,
        category: {
          id: 0,
          name: 'string',
        },
        name: 'doggie',
        photoUrls: ['string'],
        tags: [
          {
            id: 0,
            name: 'string',
          },
        ],
        status: 'available',
      };
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${jsonValidationPolicyApi.context_path}/pet`,
          method: 'PUT',
          body,
        },
        {
          validWhen: (response) => response.status === 200,
        },
      ).should((response) => {
        expect(response.body.name).to.equal('doggie');
      });
    });
  });

  describe('Tests XML-Validation path policy', () => {
    it('should fail with BAD REQUEST (400) when sending data using an invalid XML schema', () => {
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${xmlValidationPolicyApi.context_path}/pet`,
          method: 'PUT',
          headers: {
            'Content-Type': 'application/xml',
          },
          body: {
            invalidProperty: 'invalid value',
          },
        },
        {
          validWhen: (response) => response.status === 400,
        },
      ).should((response) => {
        expect(response.body.message).to.equal('Bad Request');
      });
    });

    // test not working yet, needs investigation to figure out if there's an issue with the gateway
    it.skip('should successfully connect to API endpoint if XML schema is valid', () => {
      const body = `<?xml version="1.0" encoding="UTF-8"?><Pet><id>2</id><Category><id>0</id><name>string</name></Category><name>Cat 9</name><photoUrls><photoUrl>string</photoUrl></photoUrls><tags><Tag><id>0</id><name>string</name></Tag></tags><status>available</status></Pet>`;
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${xmlValidationPolicyApi.context_path}/pet`,
          method: 'PUT',
          headers: {
            'Content-Type': 'application/xml',
          },
          body,
        },
        {
          validWhen: (response) => response.status === 200,
        },
      ).should((response) => {
        expect(response.body.name).to.equal('Cat 9');
      });
    });
  });

  describe('Tests Path-Mapping (Analytics)', () => {
    before(() => {
      for (let petId of [1, 2, 2, 2, 5]) {
        requestGateway(
          { url: `${Cypress.env('gatewayServer')}${noExtrasApi.context_path}/pet/${petId}` },
          { validWhen: (response) => response.body !== 'No context-path matches the request URI.' },
        );
        requestGateway(
          { url: `${Cypress.env('gatewayServer')}${pathMappingApi.context_path}/pet/${petId}` },
          { validWhen: (response) => response.body !== 'No context-path matches the request URI.' },
        );
      }
    });

    it('should have paths set up (mentioned) in API definition if path mapping chosen', () => {
      expect(pathMappingApi.path_mappings).to.deep.equal([
        '/pet/:petId',
        '/store/order/:orderId',
        '/pet',
        '/user/:username',
        '/pet/findByStatus',
        '/user/createWithList',
        '/store/inventory',
        '/user/login',
        '/user',
        '/user/createWithArray',
        '/pet/findByTags',
        '/store/order',
        '/user/logout',
        '/pet/:petId/uploadImage',
      ]);
    });

    it('should not have paths set up (mentioned) in API definition if no path mapping enabled', () => {
      expect(noExtrasApi.path_mappings).to.deep.equal(['/']);
    });

    it('should have 5 requests mapped in path /pet/:petId after requests were sent', () => {
      requestGateway(
        {
          url: `${Cypress.config().baseUrl}${Cypress.env('managementApi')}/apis/${pathMappingApi.id}/analytics`,
          auth: API_PUBLISHER_USER,
          qs: {
            type: 'group_by',
            field: 'mapped-path',
            interval: 10000,
            from: Date.now() - 4 * 60 * 1000,
            to: Date.now() + 1 * 60 * 1000,
          },
        },
        {
          validWhen: (response) => response.body.values['/pet/:petId'] === 5,
        },
      ).should((response) => {
        expect(Object.keys(response.body.values)).to.have.lengthOf(1);
      });
    });

    it('should not have any mapped paths in analytics response if path-mapping was not set', () => {
      cy.wait(5000); // some time needed to gather potential analytics data
      requestGateway(
        {
          url: `${Cypress.config().baseUrl}${Cypress.env('managementApi')}/apis/${noExtrasApi.id}/analytics`,
          auth: API_PUBLISHER_USER,
          qs: {
            type: 'group_by',
            field: 'mapped-path',
            interval: 10000,
            from: Date.now() - 4 * 60 * 1000,
            to: Date.now() + 1 * 60 * 1000,
          },
        },
        {
          validWhen: (response) => response.body.values,
        },
      ).should((response) => {
        expect(Cypress._.isEmpty(response.body.values));
      });
    });
  });

  describe('Tests Request-Validation policy', () => {
    it('should not respond with an error if a request parameter is missing but Request-Validation policy not set', () => {
      requestGateway({
        url: `${Cypress.env('gatewayServer')}${noExtrasApi.context_path}/pet/findByStatus`,
      }).should((response) => {
        expect(response.body).to.be.empty;
      });
    });

    it('should get a Request-Validation error if a required parameter is missing', () => {
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${requestValidationApi.context_path}/pet/findByStatus`,
        },
        {
          validWhen: (response) => response.status === 400,
        },
      ).should((response) => {
        expect(response.body.message).to.equal(
          '{"message":"Request is not valid according to constraint rules","constraints":["status query parameter is required"]}',
        );
      });
    });

    it('should successfully reach endpoint if request is valid', () => {
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${requestValidationApi.context_path}/pet/findByStatus`,
          qs: {
            status: 'available',
          },
        },
        {
          validWhen: (response) => response.status === 200,
        },
      ).should((response) => {
        expect(response.body[0].name).to.equal('Cat 1');
        expect(response.body).to.have.lengthOf(7);
      });
    });
  });
});
