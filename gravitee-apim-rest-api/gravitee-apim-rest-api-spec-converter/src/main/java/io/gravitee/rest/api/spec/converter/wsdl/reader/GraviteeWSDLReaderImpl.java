/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.spec.converter.wsdl.reader;

import com.ibm.wsdl.xml.WSDLReaderImpl;
import java.io.IOException;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author GraviteeSource Team
 *
 * see: <a href="https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html">XML External Entity Prevention Cheat Sheet</a>
 */
@Slf4j
public class GraviteeWSDLReaderImpl extends WSDLReaderImpl {

    @Override
    public Definition readWSDL(String documentBaseURI, InputSource inputSource) throws WSDLException {
        String location = inputSource.getSystemId() != null ? inputSource.getSystemId() : "- WSDL Document -";
        return this.readWSDL(documentBaseURI, getDocument(inputSource, location));
    }

    private static Document getDocument(InputSource inputSource, String desc) throws WSDLException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        String FEATURE = null;
        try {
            // This is the PRIMARY defense. If DTDs (doctypes) are disallowed, almost all
            // XML entity attacks are prevented
            // Xerces 2 only - http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl
            FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
            dbf.setFeature(FEATURE, true);

            // If you can't completely disable DTDs, then at least do the following:
            // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
            // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
            // JDK7+ - http://xml.org/sax/features/external-general-entities
            //This feature has to be used together with the following one, otherwise it will not protect you from XXE for sure
            FEATURE = "http://xml.org/sax/features/external-general-entities";
            dbf.setFeature(FEATURE, false);

            // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-parameter-entities
            // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities
            // JDK7+ - http://xml.org/sax/features/external-parameter-entities
            //This feature has to be used together with the previous one, otherwise it will not protect you from XXE for sure
            FEATURE = "http://xml.org/sax/features/external-parameter-entities";
            dbf.setFeature(FEATURE, false);

            // Disable external DTDs as well
            FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
            dbf.setFeature(FEATURE, false);

            // and these as well, per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
        } catch (ParserConfigurationException e) {
            // This should catch a failed setFeature feature
            log.warn(
                "ParserConfigurationException was thrown. The feature '{}' is probably not supported by your XML processor.",
                FEATURE,
                e
            );
        }

        dbf.setNamespaceAware(true);
        dbf.setValidating(false);

        // Load XML file or stream using a XXE agnostic configured parser...
        try {
            DocumentBuilder safebuilder = dbf.newDocumentBuilder();
            return safebuilder.parse(inputSource);
        } catch (ParserConfigurationException e) {
            log.error("ParserConfigurationException while creating DocumentBuilder for '{}'.", desc, e);
            throw new WSDLException("PARSER_ERROR", "Problem parsing '" + desc + "'.", e);
        } catch (IOException e) {
            log.error("IOException while parsing '{}'.", desc, e);
            throw new WSDLException("IO_ERROR", "Problem parsing '" + desc + "'.", e);
        } catch (SAXException e) {
            log.error("SAXException while parsing '{}'.", desc, e);
            throw new WSDLException("SAX_ERROR", "Problem parsing '" + desc + "'.", e);
        }
    }
}
