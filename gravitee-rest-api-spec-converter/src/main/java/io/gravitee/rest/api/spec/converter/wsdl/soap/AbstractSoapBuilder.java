/**
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
package io.gravitee.rest.api.spec.converter.wsdl.soap;

import io.gravitee.rest.api.spec.converter.wsdl.utils.SampleXmlUtil;
import io.gravitee.rest.api.spec.converter.wsdl.binding.SoapVersion;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.SchemaTypeSystem;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.impl.schema.BuiltinSchemaTypeSystem;

import javax.wsdl.BindingOperation;
import javax.wsdl.Part;
import javax.xml.namespace.QName;
import java.util.Map;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractSoapBuilder {

    private SchemaTypeSystem shemaTypeSystem;
    private Map<Object, Object> namespaceMappings;


    protected SoapVersion version;
    protected BindingOperation bindingOperation;
    protected XmlCursor xmlCursor;

    public AbstractSoapBuilder withVersion(SoapVersion version) {
        this.version = version;
        return this;
    }

    public AbstractSoapBuilder withBindingOperation(BindingOperation bindingOperation) {
        this.bindingOperation = bindingOperation;
        return this;
    }

    public AbstractSoapBuilder withCursor(XmlCursor cursor) {
        this.xmlCursor = cursor;
        return this;
    }

    public AbstractSoapBuilder withShemaTypeSystem(SchemaTypeSystem shemaTypeSystem) {
        this.shemaTypeSystem = shemaTypeSystem;
        return this;
    }

    public AbstractSoapBuilder withNamespaceMappings(Map<Object, Object> namespaceMappings) {
        this.namespaceMappings = namespaceMappings;
        return this;
    }

    public abstract XmlCursor build();

    protected void generateXml(Part part, XmlCursor cursor, boolean encoded) {
        // part uses a complex type
        if (part.getElementName() != null) {
            QName rootName = new QName(part.getElementName().getNamespaceURI(), part.getElementName().getLocalPart());

            SchemaType[] globalElems = shemaTypeSystem.documentTypes();
            SchemaType elem = null;
            for (int j = 0; j < globalElems.length; ++j) {
                if (rootName.equals(globalElems[j].getDocumentElementName())) {
                    elem = globalElems[j];
                    break;
                }
            }

            if (encoded) {
                cursor.insertAttributeWithValue(SampleXmlUtil.XSI_TYPE, buildPrefixedName(elem));
            }

            new SampleXmlUtil(encoded).createSampleForType(elem, cursor);
        } else {
            SchemaType type = shemaTypeSystem.findType(part.getTypeName());
            if (type == null) {
                type = BuiltinSchemaTypeSystem.get().findType(part.getTypeName());
            }
            if (type != null) {
                if (encoded) {
                    cursor.insertAttributeWithValue(SampleXmlUtil.XSI_TYPE, buildPrefixedName(type));
                }
                new SampleXmlUtil(encoded).createSampleForType(type, cursor);
            }
        }
    }

    protected String buildPrefixedName(SchemaType type) {
        String prefix = (String)this.namespaceMappings.get(type.getName().getNamespaceURI());
        return prefix +":"+type.getName().getLocalPart();
    }
}
