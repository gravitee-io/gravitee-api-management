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

import com.ibm.wsdl.*;
import com.ibm.wsdl.extensions.schema.SchemaConstants;
import com.ibm.wsdl.util.*;
import com.ibm.wsdl.util.xml.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.wsdl.*;
import javax.wsdl.extensions.*;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaReference;
import javax.wsdl.factory.*;
import javax.wsdl.xml.*;
import javax.xml.XMLConstants;
import javax.xml.namespace.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Vendored from wsdl4j 1.6.3 {@code com.ibm.wsdl.xml.WSDLReaderImpl}.
 *
 * <p>Upstream notice: (c) Copyright IBM Corp 2001, 2006 — Matthew J. Duftler, Nirmal Mukhi.
 * Original wsdl4j sources are licensed under the Common Public License v1.0 (CPL-1.0); keep that
 * notice when diffing/upgrading this file.
 *
 * <p>Intentional deltas vs upstream:
 * <ul>
 *   <li>{@link #getDocument} — OWASP XXE defenses on every parse (including nested imports/includes)</li>
 *   <li>{@link #getFeature} — also handles {@code FEATURE_PARSE_SCHEMA} (parity with {@link #setFeature})</li>
 *   <li>{@code parseImport} document loading extracted to {@link #resolveImportedDocument}</li>
 *   <li>Shared nesting / fetched-document budgets for WSDL imports and schema references</li>
 *   <li>Every fetched URL is allow-listed to http(s), including when no {@link WSDLLocator} is installed</li>
 * </ul>
 */
public class HardenedWSDLReaderImpl implements WSDLReader {

    // Used for determining the style of operations.
    private static final List STYLE_ONE_WAY = Arrays.asList(new String[] { Constants.ELEM_INPUT });
    private static final List STYLE_REQUEST_RESPONSE = Arrays.asList(new String[] { Constants.ELEM_INPUT, Constants.ELEM_OUTPUT });
    private static final List STYLE_SOLICIT_RESPONSE = Arrays.asList(new String[] { Constants.ELEM_OUTPUT, Constants.ELEM_INPUT });
    private static final List STYLE_NOTIFICATION = Arrays.asList(new String[] { Constants.ELEM_OUTPUT });

    private static final int MAX_DOCUMENT_NESTING_DEPTH = 64;
    private static final int MAX_FETCHED_DOCUMENTS = 128;

    protected boolean verbose = true;
    protected boolean importDocuments = true;
    protected boolean parseSchema = true;
    protected ExtensionRegistry extReg = null;
    protected String factoryImplName = null;
    protected WSDLLocator loc = null;
    protected WSDLFactory factory = null;

    private int documentNestingDepth = 0;
    private int fetchedDocumentCount = 0;

    //Contains all schemas used by this wsdl, either in-line or nested
    //via wsdl imports or schema imports, includes or redefines
    protected Map allSchemas = new Hashtable();

    /**
     * Sets the specified feature to the specified value.
     * <p>
     * The supported features are:
     * <p>
     * <table border=1>
     *   <tr>
     *     <th>Name</th>
     *     <th>Description</th>
     *     <th>Default Value</th>
     *   </tr>
     *   <tr>
     *     <td><center>javax.wsdl.verbose</center></td>
     *     <td>If set to true, status messages will be displayed.</td>
     *     <td><center>true</center></td>
     *   </tr>
     *   <tr>
     *     <td><center>javax.wsdl.importDocuments</center></td>
     *     <td>If set to true, imported WSDL documents will be
     *         retrieved and processed.</td>
     *     <td><center>true</center></td>
     *   </tr>
     *   <tr>
     *     <td><center>com.ibm.wsdl.parseXMLSchemas</center></td>
     *     <td>If set to true, the schema documents inlined and import directly
     *         or indrectly will be retrieved as javax.wsdl.extensions.schema.Schema
     *         objects and referred to in the Definition. This is the default (only)
     *         behaviour from JWSDL 1.2. Which is why the default for this feature is true.
     *         However, prior to JWSDL 1.2 the only behaviour was not to parse the schema
     *         files. Setting this feature to false will prevent the schemas being parsed.</td>
     *     <td><center>true</center></td>
     *   </tr>
     * </table>
     * <p>
     * All feature names must be fully-qualified, Java package style. All
     * names starting with javax.wsdl. are reserved for features defined
     * by the JWSDL specification. It is recommended that implementation-
     * specific features be fully-qualified to match the package name
     * of that implementation. For example: com.abc.featureName
     *
     * @param name the name of the feature to be set.
     * @param value the value to set the feature to.
     * @throws IllegalArgumentException if the feature name is not recognized.
     * @see #getFeature(String)
     */
    public void setFeature(String name, boolean value) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Feature name must not be null.");
        }

        if (name.equals(Constants.FEATURE_VERBOSE)) {
            verbose = value;
        } else if (name.equals(Constants.FEATURE_IMPORT_DOCUMENTS)) {
            importDocuments = value;
        } else if (name.equals(Constants.FEATURE_PARSE_SCHEMA)) {
            parseSchema = value;
        } else {
            throw new IllegalArgumentException("Feature name '" + name + "' not recognized.");
        }
    }

    /**
     * Gets the value of the specified feature.
     *
     * @param name the name of the feature to get the value of.
     * @return the value of the feature.
     * @throws IllegalArgumentException if the feature name is not recognized.
     * @see #setFeature(String, boolean)
     */
    public boolean getFeature(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Feature name must not be null.");
        }

        if (name.equals(Constants.FEATURE_VERBOSE)) {
            return verbose;
        } else if (name.equals(Constants.FEATURE_IMPORT_DOCUMENTS)) {
            return importDocuments;
        } else if (name.equals(Constants.FEATURE_PARSE_SCHEMA)) {
            return parseSchema;
        } else {
            throw new IllegalArgumentException("Feature name '" + name + "' not recognized.");
        }
    }

    /**
     * Set the extension registry to be used when reading
     * WSDL documents into a WSDL definition. If an
     * extension registry is set, that is the extension
     * registry that will be set as the extensionRegistry
     * property of the definitions resulting from invoking
     * readWSDL(...). Default is null.
     *
     * @param extReg the extension registry to use for new
     * definitions
     */
    public void setExtensionRegistry(ExtensionRegistry extReg) {
        this.extReg = extReg;
    }

    /**
     * Get the extension registry, if one was set. Default is
     * null.
     */
    public ExtensionRegistry getExtensionRegistry() {
        return extReg;
    }

    /**
     * Get the WSDLFactory object cached in the reader, or use lazy
     * instantiation if it is not cached yet.
     */
    protected WSDLFactory getWSDLFactory() throws WSDLException {
        if (factory == null) {
            factory = (factoryImplName != null) ? WSDLFactory.newInstance(factoryImplName) : WSDLFactory.newInstance();
        }
        return factory;
    }

    /**
     * Set a different factory implementation to use for
     * creating definitions when reading WSDL documents.
     * As some WSDLReader implementations may only be
     * capable of creating definitions using the same
     * factory implementation from which the reader was
     * obtained, this method is optional. Default is null.
     *
     * @param factoryImplName the fully-qualified class name of the
     * class which provides a concrete implementation of the abstract
     * class WSDLFactory.
     * @throws UnsupportedOperationException if this method
     * is invoked on an implementation which does not
     * support it.
     */
    public void setFactoryImplName(String factoryImplName) throws UnsupportedOperationException {
        if (
            (this.factoryImplName == null && factoryImplName != null) ||
            (this.factoryImplName != null && !this.factoryImplName.equals(factoryImplName))
        ) {
            this.factory = null;

            this.factoryImplName = factoryImplName;
        }
    }

    /**
     * Get the factoryImplName, if one was set. Default is null.
     */
    public String getFactoryImplName() {
        return factoryImplName;
    }

    protected Definition parseDefinitions(String documentBaseURI, Element defEl, Map importedDefs) throws WSDLException {
        checkElementName(defEl, Constants.Q_ELEM_DEFINITIONS);

        WSDLFactory factory = getWSDLFactory();
        Definition def = factory.newDefinition();

        if (extReg != null) {
            def.setExtensionRegistry(extReg);
        }

        String name = DOMUtils.getAttribute(defEl, Constants.ATTR_NAME);
        String targetNamespace = DOMUtils.getAttribute(defEl, Constants.ATTR_TARGET_NAMESPACE);
        NamedNodeMap attrs = defEl.getAttributes();

        if (importedDefs == null) {
            importedDefs = new Hashtable();
        }

        if (documentBaseURI != null) {
            def.setDocumentBaseURI(documentBaseURI);
            importedDefs.put(documentBaseURI, def);
        }

        if (name != null) {
            def.setQName(new QName(targetNamespace, name));
        }

        if (targetNamespace != null) {
            def.setTargetNamespace(targetNamespace);
        }

        int size = attrs.getLength();

        for (int i = 0; i < size; i++) {
            Attr attr = (Attr) attrs.item(i);
            String namespaceURI = attr.getNamespaceURI();
            String localPart = attr.getLocalName();
            String value = attr.getValue();

            if (namespaceURI != null && namespaceURI.equals(Constants.NS_URI_XMLNS)) {
                if (localPart != null && !localPart.equals(Constants.ATTR_XMLNS)) {
                    def.addNamespace(localPart, value);
                } else {
                    def.addNamespace(null, value);
                }
            }
        }

        Element tempEl = DOMUtils.getFirstChildElement(defEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_IMPORT, tempEl)) {
                def.addImport(parseImport(tempEl, def, importedDefs));
            } else if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                def.setDocumentationElement(tempEl);
            } else if (QNameUtils.matches(Constants.Q_ELEM_TYPES, tempEl)) {
                def.setTypes(parseTypes(tempEl, def));
            } else if (QNameUtils.matches(Constants.Q_ELEM_MESSAGE, tempEl)) {
                def.addMessage(parseMessage(tempEl, def));
            } else if (QNameUtils.matches(Constants.Q_ELEM_PORT_TYPE, tempEl)) {
                def.addPortType(parsePortType(tempEl, def));
            } else if (QNameUtils.matches(Constants.Q_ELEM_BINDING, tempEl)) {
                def.addBinding(parseBinding(tempEl, def));
            } else if (QNameUtils.matches(Constants.Q_ELEM_SERVICE, tempEl)) {
                def.addService(parseService(tempEl, def));
            } else {
                def.addExtensibilityElement(parseExtensibilityElement(Definition.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(defEl, Definition.class, def, def);

        return def;
    }

    protected Import parseImport(Element importEl, Definition def, Map importedDefs) throws WSDLException {
        Import importDef = def.createImport();

        try {
            String namespaceURI = DOMUtils.getAttribute(importEl, Constants.ATTR_NAMESPACE);
            String locationURI = DOMUtils.getAttribute(importEl, Constants.ATTR_LOCATION);
            String contextURI = null;

            if (namespaceURI != null) {
                importDef.setNamespaceURI(namespaceURI);
            }

            if (locationURI != null) {
                importDef.setLocationURI(locationURI);

                if (importDocuments) {
                    try {
                        contextURI = def.getDocumentBaseURI();
                        Definition importedDef = resolveImportedDocument(locationURI, contextURI, importedDefs);
                        if (importedDef != null) {
                            importDef.setDefinition(importedDef);
                        }
                    } catch (WSDLException e) {
                        throw e;
                    } catch (WsdlImportResolutionException e) {
                        throw new WSDLException(WSDLException.OTHER_ERROR, e.getMessage(), e);
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new WSDLException(
                            WSDLException.OTHER_ERROR,
                            "Unable to resolve imported document at '" +
                                locationURI +
                                (contextURI == null ? "'." : "', relative to '" + contextURI + "'"),
                            e
                        );
                    }
                } //end importDocs
            } //end locationURI
        } catch (WSDLException e) {
            if (e.getLocation() == null) {
                e.setLocation(XPathUtils.getXPathExprFromNode(importEl));
            } else {
                //If definitions are being parsed recursively for nested imports
                //the exception location must be built up recursively too so
                //prepend this element's xpath to exception location.
                String loc = XPathUtils.getXPathExprFromNode(importEl) + e.getLocation();
                e.setLocation(loc);
            }

            throw e;
        }

        //register any NS decls with the Definition
        NamedNodeMap attrs = importEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(importEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                importDef.setDocumentationElement(tempEl);
            } else {
                importDef.addExtensibilityElement(parseExtensibilityElement(Import.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(importEl, Import.class, importDef, def);

        return importDef;
    }

    /**
     * Resolve a nested WSDL/XSD import and parse it. Behavior matches wsdl4j 1.6.3
     * {@code parseImport} document-loading logic; XML parsing goes through the hardened
     * {@link #getDocument}.
     */
    private Definition resolveImportedDocument(String locationURI, String contextURI, Map importedDefs) throws Exception {
        Definition importedDef = null;
        InputStream inputStream = null;
        InputSource inputSource = null;
        URL url = null;

        if (loc != null) {
            URL resolved = SecureWSDLLocator.resolve(contextURI, locationURI);
            importedDef = (Definition) importedDefs.get(resolved.toString());
            if (importedDef == null) {
                ensureDocumentBudgetAllowsAnother();
                inputSource = loc.getImportInputSource(contextURI, locationURI);
                inputSource.setSystemId(loc.getLatestImportURI());
            }
        } else {
            url = SecureWSDLLocator.resolve(contextURI, locationURI);
            importedDef = (Definition) importedDefs.get(url.toString());
            if (importedDef == null) {
                ensureDocumentBudgetAllowsAnother();
                inputStream = StringUtils.getContentAsInputStream(url);
                if (inputStream != null) {
                    inputSource = new InputSource(inputStream);
                    inputSource.setSystemId(url.toString());
                }
            }
        }

        if (importedDef != null) {
            return importedDef;
        }

        if (inputSource == null) {
            throw new WSDLException(
                WSDLException.OTHER_ERROR,
                "Unable to locate imported document " +
                    "at '" +
                    locationURI +
                    "'" +
                    (contextURI == null ? "." : ", relative to '" + contextURI + "'.")
            );
        }

        Document doc = getDocument(inputSource, inputSource.getSystemId());
        if (inputStream != null) {
            inputStream.close();
        }

        Element documentElement = doc.getDocumentElement();
        String documentBase = (loc != null) ? loc.getLatestImportURI() : (url != null) ? url.toString() : locationURI;

        if (QNameUtils.matches(Constants.Q_ELEM_DEFINITIONS, documentElement)) {
            if (verbose) {
                System.out.println(
                    "Retrieving document at '" + locationURI + "'" + (contextURI == null ? "." : ", relative to '" + contextURI + "'.")
                );
            }
            return readWSDL(documentBase, documentElement, importedDefs);
        }

        QName docElementQName = QNameUtils.newQName(documentElement);
        if (SchemaConstants.XSD_QNAME_LIST.contains(docElementQName)) {
            // Share the same nesting / fetch budget as schema include/import/redefine.
            enterNestedDocument();
            try {
                if (verbose) {
                    System.out.println(
                        "Retrieving schema wsdl:imported from '" +
                            locationURI +
                            "'" +
                            (contextURI == null ? "." : ", relative to '" + contextURI + "'.")
                    );
                }

                WSDLFactory factory = getWSDLFactory();
                importedDef = factory.newDefinition();
                if (extReg != null) {
                    importedDef.setExtensionRegistry(extReg);
                }
                importedDef.setDocumentBaseURI(documentBase);

                Types types = importedDef.createTypes();
                types.addExtensibilityElement(parseSchema(Types.class, documentElement, importedDef));
                importedDef.setTypes(types);
                return importedDef;
            } finally {
                leaveNestedDocument();
            }
        }

        return null;
    }

    protected Types parseTypes(Element typesEl, Definition def) throws WSDLException {
        //register any NS decls with the Definition
        NamedNodeMap attrs = typesEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Types types = def.createTypes();
        Element tempEl = DOMUtils.getFirstChildElement(typesEl);
        QName tempElType;

        while (tempEl != null) {
            tempElType = QNameUtils.newQName(tempEl);

            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                types.setDocumentationElement(tempEl);
            } else if ((SchemaConstants.XSD_QNAME_LIST).contains(tempElType)) {
                if (parseSchema) {
                    //the element qname indicates it is a schema.
                    types.addExtensibilityElement(parseSchema(Types.class, tempEl, def));
                } else {
                    types.addExtensibilityElement(parseExtensibilityElementAsDefaultExtensiblityElement(Types.class, tempEl, def));
                }
            } else {
                types.addExtensibilityElement(parseExtensibilityElement(Types.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(typesEl, Types.class, types, def);

        return types;
    }

    protected ExtensibilityElement parseSchema(Class parentType, Element el, Definition def) throws WSDLException {
        QName elementType = null;
        ExtensionRegistry extReg = null;

        try {
            extReg = def.getExtensionRegistry();

            if (extReg == null) {
                throw new WSDLException(
                    WSDLException.CONFIGURATION_ERROR,
                    "No ExtensionRegistry set for this " +
                        "Definition, so unable to deserialize " +
                        "a '" +
                        elementType +
                        "' element in the " +
                        "context of a '" +
                        parentType.getName() +
                        "'."
                );
            }

            return parseSchema(parentType, el, def, extReg);
        } catch (WSDLException e) {
            if (e.getLocation() == null) {
                e.setLocation(XPathUtils.getXPathExprFromNode(el));
            }

            throw e;
        }
    }

    protected ExtensibilityElement parseSchema(Class parentType, Element el, Definition def, ExtensionRegistry extReg)
        throws WSDLException {
        /*
         * This method returns ExtensibilityElement rather than Schema because we
         * do not insist that a suitable XSD schema deserializer is registered.
         * PopulatedExtensionRegistry registers SchemaDeserializer by default, but
         * if the user chooses not to register a suitable deserializer then the
         * UnknownDeserializer will be used, returning an UnknownExtensibilityElement.
         */

        Schema schema = null;
        SchemaReference schemaRef = null;
        try {
            QName elementType = QNameUtils.newQName(el);

            ExtensionDeserializer exDS = extReg.queryDeserializer(parentType, elementType);

            //Now unmarshall the DOM element.
            ExtensibilityElement ee = exDS.unmarshall(parentType, elementType, el, def, extReg);

            if (ee instanceof Schema) {
                schema = (Schema) ee;
            } else {
                //Unknown extensibility element, so don't do any more schema parsing on it.
                return ee;
            }

            //Keep track of parsed schemas to avoid duplicating Schema objects
            //through duplicate or circular references (eg: A imports B imports A).
            if (schema.getDocumentBaseURI() != null) {
                this.allSchemas.put(schema.getDocumentBaseURI(), schema);
            }

            //At this point, any SchemaReference objects held by the schema will not
            //yet point to their referenced schemas, so we must now retrieve these
            //schemas and set the schema references.

            //First, combine the schema references for imports, includes and redefines
            //into a single list

            ArrayList allSchemaRefs = new ArrayList();

            Collection ic = schema.getImports().values();
            Iterator importsIterator = ic.iterator();
            while (importsIterator.hasNext()) {
                allSchemaRefs.addAll((Collection) importsIterator.next());
            }

            allSchemaRefs.addAll(schema.getIncludes());
            allSchemaRefs.addAll(schema.getRedefines());

            //Then, retrieve the schema referred to by each schema reference. If the
            //schema has been read in previously, use the existing schema object.
            //Otherwise unmarshall the DOM element into a new schema object.

            ListIterator schemaRefIterator = allSchemaRefs.listIterator();

            while (schemaRefIterator.hasNext()) {
                try {
                    schemaRef = (SchemaReference) schemaRefIterator.next();

                    if (schemaRef.getSchemaLocationURI() == null) {
                        //cannot get the referenced schema, so ignore this schema reference
                        continue;
                    }

                    if (verbose) {
                        System.out.println(
                            "Retrieving schema at '" +
                                schemaRef.getSchemaLocationURI() +
                                (schema.getDocumentBaseURI() == null ? "'." : "', relative to '" + schema.getDocumentBaseURI() + "'.")
                        );
                    }

                    InputStream inputStream = null;
                    InputSource inputSource = null;

                    //This is the child schema referred to by the schemaReference
                    Schema referencedSchema = null;

                    //This is the child schema's location obtained from the WSDLLocator or the URL
                    String location = null;

                    if (loc != null) {
                        URL resolved = SecureWSDLLocator.resolve(schema.getDocumentBaseURI(), schemaRef.getSchemaLocationURI());
                        location = resolved.toString();
                        referencedSchema = (Schema) this.allSchemas.get(location);
                        if (referencedSchema == null) {
                            ensureDocumentBudgetAllowsAnother();
                            inputSource = loc.getImportInputSource(schema.getDocumentBaseURI(), schemaRef.getSchemaLocationURI());
                        }
                    } else {
                        // We don't have a wsdl locator, so try to retrieve the schema by its URL
                        String contextURI = schema.getDocumentBaseURI();
                        URL url = SecureWSDLLocator.resolve(contextURI, schemaRef.getSchemaLocationURI());
                        location = url.toString();

                        //if a schema from this location has been retrieved previously, use it.
                        referencedSchema = (Schema) this.allSchemas.get(location);

                        if (referencedSchema == null) {
                            ensureDocumentBudgetAllowsAnother();
                            inputStream = StringUtils.getContentAsInputStream(url);

                            if (inputStream != null) {
                                inputSource = new InputSource(inputStream);
                            }

                            if (inputSource == null) {
                                throw new WSDLException(
                                    WSDLException.OTHER_ERROR,
                                    "Unable to locate with a url " +
                                        "the document referenced at '" +
                                        schemaRef.getSchemaLocationURI() +
                                        "'" +
                                        (contextURI == null ? "." : ", relative to '" + contextURI + "'.")
                                );
                            }
                        }
                    } //end if loc

                    // If we have not previously read the schema, get its DOM element now.
                    if (referencedSchema == null) {
                        enterNestedDocument();
                        try {
                            inputSource.setSystemId(location);
                            Document doc = getDocument(inputSource, location);

                            if (inputStream != null) {
                                inputStream.close();
                            }

                            Element documentElement = doc.getDocumentElement();

                            // If it's a schema doc process it, otherwise the schema reference remains null

                            QName docElementQName = QNameUtils.newQName(documentElement);

                            if (SchemaConstants.XSD_QNAME_LIST.contains(docElementQName)) {
                                //We now need to call parseSchema recursively to parse the referenced
                                //schema. The document base URI of the referenced schema will be set to
                                //the document base URI of the current schema plus the schemaLocation in
                                //the schemaRef. We cannot explicitly pass in a new document base URI
                                //to the schema deserializer, so instead we will create a dummy
                                //Definition and set its documentBaseURI to the new document base URI.
                                //We can leave the other definition fields empty because we know
                                //that the SchemaDeserializer.unmarshall method uses the definition
                                //parameter only to get its documentBaseURI. If the unmarshall method
                                //implementation changes (ie: its use of definition changes) we may need
                                //to rethink this approach.

                                WSDLFactory factory = getWSDLFactory();
                                Definition dummyDef = factory.newDefinition();

                                dummyDef.setDocumentBaseURI(location);

                                //By this point, we know we have a SchemaDeserializer registered
                                //so we can safely cast the ExtensibilityElement to a Schema.
                                referencedSchema = (Schema) parseSchema(parentType, documentElement, dummyDef, extReg);
                            }
                        } finally {
                            leaveNestedDocument();
                        }
                    } //end if referencedSchema

                    schemaRef.setReferencedSchema(referencedSchema);
                } catch (WSDLException e) {
                    throw e;
                } catch (WsdlImportResolutionException e) {
                    throw new WSDLException(WSDLException.OTHER_ERROR, e.getMessage(), e);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new WSDLException(
                        WSDLException.OTHER_ERROR,
                        "An error occurred trying to resolve schema referenced at '" +
                            schemaRef.getSchemaLocationURI() +
                            "'" +
                            (schema.getDocumentBaseURI() == null ? "." : ", relative to '" + schema.getDocumentBaseURI() + "'."),
                        e
                    );
                }
            } //end while loop

            return schema;
        } catch (WSDLException e) {
            if (e.getLocation() == null) {
                e.setLocation(XPathUtils.getXPathExprFromNode(el));
            } else {
                //If this method has been called recursively for nested schemas
                //the exception location must be built up recursively too so
                //prepend this element's xpath to exception location.
                String loc = XPathUtils.getXPathExprFromNode(el) + e.getLocation();
                e.setLocation(loc);
            }

            throw e;
        }
    }

    protected Binding parseBinding(Element bindingEl, Definition def) throws WSDLException {
        Binding binding = null;

        List remainingAttrs = DOMUtils.getAttributes(bindingEl);
        String name = DOMUtils.getAttribute(bindingEl, Constants.ATTR_NAME, remainingAttrs);
        QName portTypeName = getQualifiedAttributeValue(bindingEl, Constants.ATTR_TYPE, Constants.ELEM_BINDING, def, remainingAttrs);

        PortType portType = null;

        if (name != null) {
            QName bindingName = new QName(def.getTargetNamespace(), name);

            binding = def.getBinding(bindingName);

            if (binding == null) {
                binding = def.createBinding();
                binding.setQName(bindingName);
            }
        } else {
            binding = def.createBinding();
        }

        // Whether it was retrieved or created, the definition has been found.
        binding.setUndefined(false);

        if (portTypeName != null) {
            portType = def.getPortType(portTypeName);

            if (portType == null) {
                portType = def.createPortType();
                portType.setQName(portTypeName);
                def.addPortType(portType);
            }

            binding.setPortType(portType);
        }

        //register any NS decls with the Definition
        NamedNodeMap attrs = bindingEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(bindingEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                binding.setDocumentationElement(tempEl);
            } else if (QNameUtils.matches(Constants.Q_ELEM_OPERATION, tempEl)) {
                binding.addBindingOperation(parseBindingOperation(tempEl, portType, def));
            } else {
                binding.addExtensibilityElement(parseExtensibilityElement(Binding.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(bindingEl, Binding.class, binding, def);

        return binding;
    }

    protected BindingOperation parseBindingOperation(Element bindingOperationEl, PortType portType, Definition def) throws WSDLException {
        BindingOperation bindingOperation = def.createBindingOperation();

        List remainingAttrs = DOMUtils.getAttributes(bindingOperationEl);
        String name = DOMUtils.getAttribute(bindingOperationEl, Constants.ATTR_NAME, remainingAttrs);

        if (name != null) {
            bindingOperation.setName(name);
        }

        //register any NS decls with the Definition
        NamedNodeMap attrs = bindingOperationEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(bindingOperationEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                bindingOperation.setDocumentationElement(tempEl);
            } else if (QNameUtils.matches(Constants.Q_ELEM_INPUT, tempEl)) {
                bindingOperation.setBindingInput(parseBindingInput(tempEl, def));
            } else if (QNameUtils.matches(Constants.Q_ELEM_OUTPUT, tempEl)) {
                bindingOperation.setBindingOutput(parseBindingOutput(tempEl, def));
            } else if (QNameUtils.matches(Constants.Q_ELEM_FAULT, tempEl)) {
                bindingOperation.addBindingFault(parseBindingFault(tempEl, def));
            } else {
                bindingOperation.addExtensibilityElement(parseExtensibilityElement(BindingOperation.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        if (portType != null) {
            BindingInput bindingInput = bindingOperation.getBindingInput();
            BindingOutput bindingOutput = bindingOperation.getBindingOutput();
            String inputName = (bindingInput != null ? (bindingInput.getName() != null ? bindingInput.getName() : Constants.NONE) : null);
            String outputName = (bindingOutput != null
                    ? (bindingOutput.getName() != null ? bindingOutput.getName() : Constants.NONE)
                    : null);
            Operation op = portType.getOperation(name, inputName, outputName);

            /*
             * If the bindingOp input or output message names are null we will search first
             * for a porttypeOp with corresponding unnamed input or output messages (using
             * Constants.NONE for inputName or outputName, as above).
             * However, input and output message names need not be used at all if operation
             * overloading is not used, so if no match was found we will try again ignoring
             * these unnamed messages from the search criteria (i.e. using null instead of
             * Constants.NONE for inputName or outputName).
             */

            if (op == null) {
                if (Constants.NONE.equals(inputName) && Constants.NONE.equals(outputName)) {
                    //There was no porttype op with unnamed input and output messages,
                    //so ignore input and output name and search on the op name only.
                    op = portType.getOperation(name, null, null);
                } else if (Constants.NONE.equals(inputName)) {
                    //There was no porttype op with an unnamed input message,
                    //so ignore input name and search on the op name and output name only.
                    op = portType.getOperation(name, null, outputName);
                } else if (Constants.NONE.equals(outputName)) {
                    //There was no porttype op with an unnamed output message,
                    //so ignore output name and search on the op name and input name only.
                    op = portType.getOperation(name, inputName, null);
                }
            }

            if (op == null) {
                Input input = def.createInput();
                Output output = def.createOutput();

                op = def.createOperation();
                op.setName(name);
                input.setName(inputName);
                output.setName(outputName);
                op.setInput(input);
                op.setOutput(output);
                portType.addOperation(op);
            }

            bindingOperation.setOperation(op);
        }

        parseExtensibilityAttributes(bindingOperationEl, BindingOperation.class, bindingOperation, def);

        return bindingOperation;
    }

    protected BindingInput parseBindingInput(Element bindingInputEl, Definition def) throws WSDLException {
        BindingInput bindingInput = def.createBindingInput();

        List remainingAttrs = DOMUtils.getAttributes(bindingInputEl);
        String name = DOMUtils.getAttribute(bindingInputEl, Constants.ATTR_NAME, remainingAttrs);

        if (name != null) {
            bindingInput.setName(name);
        }

        //register any NS decls with the Definition
        NamedNodeMap attrs = bindingInputEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(bindingInputEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                bindingInput.setDocumentationElement(tempEl);
            } else {
                bindingInput.addExtensibilityElement(parseExtensibilityElement(BindingInput.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(bindingInputEl, BindingInput.class, bindingInput, def);

        return bindingInput;
    }

    protected BindingOutput parseBindingOutput(Element bindingOutputEl, Definition def) throws WSDLException {
        BindingOutput bindingOutput = def.createBindingOutput();

        List remainingAttrs = DOMUtils.getAttributes(bindingOutputEl);
        String name = DOMUtils.getAttribute(bindingOutputEl, Constants.ATTR_NAME, remainingAttrs);

        if (name != null) {
            bindingOutput.setName(name);
        }

        //register any NS decls with the Definition
        NamedNodeMap attrs = bindingOutputEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(bindingOutputEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                bindingOutput.setDocumentationElement(tempEl);
            } else {
                bindingOutput.addExtensibilityElement(parseExtensibilityElement(BindingOutput.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(bindingOutputEl, BindingOutput.class, bindingOutput, def);

        return bindingOutput;
    }

    protected BindingFault parseBindingFault(Element bindingFaultEl, Definition def) throws WSDLException {
        BindingFault bindingFault = def.createBindingFault();

        List remainingAttrs = DOMUtils.getAttributes(bindingFaultEl);
        String name = DOMUtils.getAttribute(bindingFaultEl, Constants.ATTR_NAME, remainingAttrs);

        if (name != null) {
            bindingFault.setName(name);
        }

        //register any NS decls with the Definition
        NamedNodeMap attrs = bindingFaultEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(bindingFaultEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                bindingFault.setDocumentationElement(tempEl);
            } else {
                bindingFault.addExtensibilityElement(parseExtensibilityElement(BindingFault.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(bindingFaultEl, BindingFault.class, bindingFault, def);

        return bindingFault;
    }

    protected Message parseMessage(Element msgEl, Definition def) throws WSDLException {
        Message msg = null;

        List remainingAttrs = DOMUtils.getAttributes(msgEl);
        String name = DOMUtils.getAttribute(msgEl, Constants.ATTR_NAME, remainingAttrs);

        if (name != null) {
            QName messageName = new QName(def.getTargetNamespace(), name);

            msg = def.getMessage(messageName);

            if (msg == null) {
                msg = def.createMessage();
                msg.setQName(messageName);
            }
        } else {
            msg = def.createMessage();
        }

        // Whether it was retrieved or created, the definition has been found.
        msg.setUndefined(false);

        //register any NS decls with the Definition
        NamedNodeMap attrs = msgEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(msgEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                msg.setDocumentationElement(tempEl);
            } else if (QNameUtils.matches(Constants.Q_ELEM_PART, tempEl)) {
                msg.addPart(parsePart(tempEl, def));
            } else {
                msg.addExtensibilityElement(parseExtensibilityElement(Message.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(msgEl, Message.class, msg, def);

        return msg;
    }

    protected Part parsePart(Element partEl, Definition def) throws WSDLException {
        Part part = def.createPart();
        String name = DOMUtils.getAttribute(partEl, Constants.ATTR_NAME);
        QName elementName = getQualifiedAttributeValue(partEl, Constants.ATTR_ELEMENT, Constants.ELEM_MESSAGE, def);
        QName typeName = getQualifiedAttributeValue(partEl, Constants.ATTR_TYPE, Constants.ELEM_MESSAGE, def);

        if (name != null) {
            part.setName(name);
        }

        if (elementName != null) {
            part.setElementName(elementName);
        }

        if (typeName != null) {
            part.setTypeName(typeName);
        }

        //register any NS decls with the Definition
        NamedNodeMap attrs = partEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(partEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                part.setDocumentationElement(tempEl);
            } else {
                part.addExtensibilityElement(parseExtensibilityElement(Part.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(partEl, Part.class, part, def);

        return part;
    }

    protected void parseExtensibilityAttributes(Element el, Class parentType, AttributeExtensible attrExt, Definition def)
        throws WSDLException {
        if (attrExt == null) return;

        List nativeAttributeNames = attrExt.getNativeAttributeNames();
        NamedNodeMap nodeMap = el.getAttributes();
        int length = nodeMap.getLength();

        for (int i = 0; i < length; i++) {
            Attr attribute = (Attr) nodeMap.item(i);
            String localName = attribute.getLocalName();
            String namespaceURI = attribute.getNamespaceURI();
            String prefix = attribute.getPrefix();
            QName qname = new QName(namespaceURI, localName);

            if (namespaceURI != null && !namespaceURI.equals(Constants.NS_URI_WSDL)) {
                if (!namespaceURI.equals(Constants.NS_URI_XMLNS)) {
                    DOMUtils.registerUniquePrefix(prefix, namespaceURI, def);

                    String strValue = attribute.getValue();
                    int attrType = AttributeExtensible.NO_DECLARED_TYPE;
                    ExtensionRegistry extReg = def.getExtensionRegistry();

                    if (extReg != null) {
                        attrType = extReg.queryExtensionAttributeType(parentType, qname);
                    }

                    Object val = parseExtensibilityAttribute(el, attrType, strValue, def);

                    attrExt.setExtensionAttribute(qname, val);
                }
            } else if (!nativeAttributeNames.contains(localName)) {
                WSDLException wsdlExc = new WSDLException(
                    WSDLException.INVALID_WSDL,
                    "Encountered illegal " +
                        "extension attribute '" +
                        qname +
                        "'. Extension " +
                        "attributes must be in " +
                        "a namespace other than " +
                        "WSDL's."
                );

                wsdlExc.setLocation(XPathUtils.getXPathExprFromNode(el));

                throw wsdlExc;
            }
        }
    }

    protected Object parseExtensibilityAttribute(Element el, int attrType, String attrValue, Definition def) throws WSDLException {
        if (attrType == AttributeExtensible.QNAME_TYPE) {
            return DOMUtils.getQName(attrValue, el, def);
        } else if (attrType == AttributeExtensible.LIST_OF_STRINGS_TYPE) {
            return StringUtils.parseNMTokens(attrValue);
        } else if (attrType == AttributeExtensible.LIST_OF_QNAMES_TYPE) {
            List oldList = StringUtils.parseNMTokens(attrValue);
            int size = oldList.size();
            List newList = new Vector(size);

            for (int i = 0; i < size; i++) {
                String str = (String) oldList.get(i);
                QName qValue = DOMUtils.getQName(str, el, def);

                newList.add(qValue);
            }

            return newList;
        } else if (attrType == AttributeExtensible.STRING_TYPE) {
            return attrValue;
        } else {
            QName qValue = null;

            try {
                qValue = DOMUtils.getQName(attrValue, el, def);
            } catch (WSDLException e) {
                qValue = new QName(attrValue);
            }

            return qValue;
        }
    }

    protected PortType parsePortType(Element portTypeEl, Definition def) throws WSDLException {
        PortType portType = null;
        String name = DOMUtils.getAttribute(portTypeEl, Constants.ATTR_NAME);

        if (name != null) {
            QName portTypeName = new QName(def.getTargetNamespace(), name);

            portType = def.getPortType(portTypeName);

            if (portType == null) {
                portType = def.createPortType();
                portType.setQName(portTypeName);
            }
        } else {
            portType = def.createPortType();
        }

        // Whether it was retrieved or created, the definition has been found.
        portType.setUndefined(false);

        //register any NS decls with the Definition
        NamedNodeMap attrs = portTypeEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(portTypeEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                portType.setDocumentationElement(tempEl);
            } else if (QNameUtils.matches(Constants.Q_ELEM_OPERATION, tempEl)) {
                Operation op = parseOperation(tempEl, portType, def);

                if (op != null) {
                    portType.addOperation(op);
                }
            } else {
                portType.addExtensibilityElement(parseExtensibilityElement(PortType.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(portTypeEl, PortType.class, portType, def);

        return portType;
    }

    protected Operation parseOperation(Element opEl, PortType portType, Definition def) throws WSDLException {
        Operation op = null;

        List remainingAttrs = DOMUtils.getAttributes(opEl);
        String name = DOMUtils.getAttribute(opEl, Constants.ATTR_NAME, remainingAttrs);
        String parameterOrderStr = DOMUtils.getAttribute(opEl, Constants.ATTR_PARAMETER_ORDER, remainingAttrs);

        //register any NS decls with the Definition
        NamedNodeMap attrs = opEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(opEl);
        List messageOrder = new Vector();
        Element docEl = null;
        Input input = null;
        Output output = null;
        List faults = new Vector();
        List extElements = new Vector();
        boolean retrieved = true;

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                docEl = tempEl;
            } else if (QNameUtils.matches(Constants.Q_ELEM_INPUT, tempEl)) {
                input = parseInput(tempEl, def);
                messageOrder.add(Constants.ELEM_INPUT);
            } else if (QNameUtils.matches(Constants.Q_ELEM_OUTPUT, tempEl)) {
                output = parseOutput(tempEl, def);
                messageOrder.add(Constants.ELEM_OUTPUT);
            } else if (QNameUtils.matches(Constants.Q_ELEM_FAULT, tempEl)) {
                faults.add(parseFault(tempEl, def));
            } else {
                extElements.add(parseExtensibilityElement(Operation.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        if (name != null) {
            String inputName = (input != null ? (input.getName() != null ? input.getName() : Constants.NONE) : null);
            String outputName = (output != null ? (output.getName() != null ? output.getName() : Constants.NONE) : null);

            op = portType.getOperation(name, inputName, outputName);

            if (op != null && !op.isUndefined()) {
                op = null;
            }

            if (op != null) {
                if (inputName == null) {
                    Input tempIn = op.getInput();

                    if (tempIn != null) {
                        if (tempIn.getName() != null) {
                            op = null;
                        }
                    }
                }
            }

            if (op != null) {
                if (outputName == null) {
                    Output tempOut = op.getOutput();

                    if (tempOut != null) {
                        if (tempOut.getName() != null) {
                            op = null;
                        }
                    }
                }
            }

            if (op == null) {
                op = def.createOperation();
                op.setName(name);
                retrieved = false;
            }
        } else {
            op = def.createOperation();
            retrieved = false;
        }

        // Whether it was retrieved or created, the definition has been found.
        op.setUndefined(false);

        if (parameterOrderStr != null) {
            op.setParameterOrdering(StringUtils.parseNMTokens(parameterOrderStr));
        }

        if (docEl != null) {
            op.setDocumentationElement(docEl);
        }

        if (input != null) {
            op.setInput(input);
        }

        if (output != null) {
            op.setOutput(output);
        }

        if (faults.size() > 0) {
            Iterator faultIterator = faults.iterator();

            while (faultIterator.hasNext()) {
                op.addFault((Fault) faultIterator.next());
            }
        }

        if (extElements.size() > 0) {
            Iterator eeIterator = extElements.iterator();

            while (eeIterator.hasNext()) {
                op.addExtensibilityElement((ExtensibilityElement) eeIterator.next());
            }
        }

        OperationType style = null;

        if (messageOrder.equals(STYLE_ONE_WAY)) {
            style = OperationType.ONE_WAY;
        } else if (messageOrder.equals(STYLE_REQUEST_RESPONSE)) {
            style = OperationType.REQUEST_RESPONSE;
        } else if (messageOrder.equals(STYLE_SOLICIT_RESPONSE)) {
            style = OperationType.SOLICIT_RESPONSE;
        } else if (messageOrder.equals(STYLE_NOTIFICATION)) {
            style = OperationType.NOTIFICATION;
        }

        if (style != null) {
            op.setStyle(style);
        }

        parseExtensibilityAttributes(opEl, Operation.class, op, def);

        if (retrieved) {
            op = null;
        }

        return op;
    }

    protected Service parseService(Element serviceEl, Definition def) throws WSDLException {
        Service service = def.createService();

        List remainingAttrs = DOMUtils.getAttributes(serviceEl);
        String name = DOMUtils.getAttribute(serviceEl, Constants.ATTR_NAME, remainingAttrs);

        if (name != null) {
            service.setQName(new QName(def.getTargetNamespace(), name));
        }

        //register any NS decls with the Definition
        NamedNodeMap attrs = serviceEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(serviceEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                service.setDocumentationElement(tempEl);
            } else if (QNameUtils.matches(Constants.Q_ELEM_PORT, tempEl)) {
                service.addPort(parsePort(tempEl, def));
            } else {
                service.addExtensibilityElement(parseExtensibilityElement(Service.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(serviceEl, Service.class, service, def);

        return service;
    }

    protected Port parsePort(Element portEl, Definition def) throws WSDLException {
        Port port = def.createPort();

        List remainingAttrs = DOMUtils.getAttributes(portEl);
        String name = DOMUtils.getAttribute(portEl, Constants.ATTR_NAME, remainingAttrs);
        QName bindingStr = getQualifiedAttributeValue(portEl, Constants.ATTR_BINDING, Constants.ELEM_PORT, def, remainingAttrs);

        if (name != null) {
            port.setName(name);
        }

        if (bindingStr != null) {
            Binding binding = def.getBinding(bindingStr);

            if (binding == null) {
                binding = def.createBinding();
                binding.setQName(bindingStr);
                def.addBinding(binding);
            }

            port.setBinding(binding);
        }

        //register any NS decls with the Definition
        NamedNodeMap attrs = portEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(portEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                port.setDocumentationElement(tempEl);
            } else {
                port.addExtensibilityElement(parseExtensibilityElement(Port.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(portEl, Port.class, port, def);

        return port;
    }

    protected ExtensibilityElement parseExtensibilityElement(Class parentType, Element el, Definition def) throws WSDLException {
        QName elementType = QNameUtils.newQName(el);

        String namespaceURI = el.getNamespaceURI();

        try {
            if (namespaceURI == null || namespaceURI.equals(Constants.NS_URI_WSDL)) {
                throw new WSDLException(
                    WSDLException.INVALID_WSDL,
                    "Encountered illegal extension element '" +
                        elementType +
                        "' in the context of a '" +
                        parentType.getName() +
                        "'. Extension elements must be in " +
                        "a namespace other than WSDL's."
                );
            }

            ExtensionRegistry extReg = def.getExtensionRegistry();

            if (extReg == null) {
                throw new WSDLException(
                    WSDLException.CONFIGURATION_ERROR,
                    "No ExtensionRegistry set for this " +
                        "Definition, so unable to deserialize " +
                        "a '" +
                        elementType +
                        "' element in the " +
                        "context of a '" +
                        parentType.getName() +
                        "'."
                );
            }

            ExtensionDeserializer extDS = extReg.queryDeserializer(parentType, elementType);
            NamedNodeMap attrs = el.getAttributes();
            registerNSDeclarations(attrs, def);

            return extDS.unmarshall(parentType, elementType, el, def, extReg);
        } catch (WSDLException e) {
            if (e.getLocation() == null) {
                e.setLocation(XPathUtils.getXPathExprFromNode(el));
            }

            throw e;
        }
    }

    /**
     * Parse the element using the ExtensionRegistry default deserializer instead using the one
     * registered. The default deserializer will create an UnknownExtensibilityElement from the element.
     * @param parentType
     * @param el
     * @param def
     * @return An instance of the default ExtensibilityElement as registered with the ExtensionRegistry
     * @throws WSDLException
     */
    protected ExtensibilityElement parseExtensibilityElementAsDefaultExtensiblityElement(Class parentType, Element el, Definition def)
        throws WSDLException {
        QName elementType = QNameUtils.newQName(el);

        String namespaceURI = el.getNamespaceURI();

        try {
            if (namespaceURI == null || namespaceURI.equals(Constants.NS_URI_WSDL)) {
                throw new WSDLException(
                    WSDLException.INVALID_WSDL,
                    "Encountered illegal extension element '" +
                        elementType +
                        "' in the context of a '" +
                        parentType.getName() +
                        "'. Extension elements must be in " +
                        "a namespace other than WSDL's."
                );
            }

            ExtensionRegistry extReg = def.getExtensionRegistry();

            if (extReg == null) {
                throw new WSDLException(
                    WSDLException.CONFIGURATION_ERROR,
                    "No ExtensionRegistry set for this " +
                        "Definition, so unable to deserialize " +
                        "a '" +
                        elementType +
                        "' element in the " +
                        "context of a '" +
                        parentType.getName() +
                        "'."
                );
            }

            ExtensionDeserializer extDS = extReg.getDefaultDeserializer();

            NamedNodeMap attrs = el.getAttributes();
            registerNSDeclarations(attrs, def);

            return extDS.unmarshall(parentType, elementType, el, def, extReg);
        } catch (WSDLException e) {
            if (e.getLocation() == null) {
                e.setLocation(XPathUtils.getXPathExprFromNode(el));
            }

            throw e;
        }
    }

    protected Input parseInput(Element inputEl, Definition def) throws WSDLException {
        Input input = def.createInput();
        String name = DOMUtils.getAttribute(inputEl, Constants.ATTR_NAME);
        QName messageName = getQualifiedAttributeValue(inputEl, Constants.ATTR_MESSAGE, Constants.ELEM_INPUT, def);

        if (name != null) {
            input.setName(name);
        }

        if (messageName != null) {
            Message message = def.getMessage(messageName);

            if (message == null) {
                message = def.createMessage();
                message.setQName(messageName);
                def.addMessage(message);
            }

            input.setMessage(message);
        }

        //register any NS decls with the Definition
        NamedNodeMap attrs = inputEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(inputEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                input.setDocumentationElement(tempEl);
            } else {
                input.addExtensibilityElement(parseExtensibilityElement(Input.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(inputEl, Input.class, input, def);

        return input;
    }

    protected Output parseOutput(Element outputEl, Definition def) throws WSDLException {
        Output output = def.createOutput();
        String name = DOMUtils.getAttribute(outputEl, Constants.ATTR_NAME);
        QName messageName = getQualifiedAttributeValue(outputEl, Constants.ATTR_MESSAGE, Constants.ELEM_OUTPUT, def);

        if (name != null) {
            output.setName(name);
        }

        if (messageName != null) {
            Message message = def.getMessage(messageName);

            if (message == null) {
                message = def.createMessage();
                message.setQName(messageName);
                def.addMessage(message);
            }

            output.setMessage(message);
        }

        //register any NS decls with the Definition
        NamedNodeMap attrs = outputEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(outputEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                output.setDocumentationElement(tempEl);
            } else {
                output.addExtensibilityElement(parseExtensibilityElement(Output.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(outputEl, Output.class, output, def);

        return output;
    }

    protected Fault parseFault(Element faultEl, Definition def) throws WSDLException {
        Fault fault = def.createFault();
        String name = DOMUtils.getAttribute(faultEl, Constants.ATTR_NAME);
        QName messageName = getQualifiedAttributeValue(faultEl, Constants.ATTR_MESSAGE, Constants.ELEM_FAULT, def);

        if (name != null) {
            fault.setName(name);
        }

        if (messageName != null) {
            Message message = def.getMessage(messageName);

            if (message == null) {
                message = def.createMessage();
                message.setQName(messageName);
                def.addMessage(message);
            }

            fault.setMessage(message);
        }

        //register any NS decls with the Definition
        NamedNodeMap attrs = faultEl.getAttributes();
        registerNSDeclarations(attrs, def);

        Element tempEl = DOMUtils.getFirstChildElement(faultEl);

        while (tempEl != null) {
            if (QNameUtils.matches(Constants.Q_ELEM_DOCUMENTATION, tempEl)) {
                fault.setDocumentationElement(tempEl);
            } else {
                fault.addExtensibilityElement(parseExtensibilityElement(Fault.class, tempEl, def));
            }

            tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        parseExtensibilityAttributes(faultEl, Fault.class, fault, def);

        return fault;
    }

    /**
     * This method should be used for elements that support extension
     * attributes because it does not track unexpected remaining attributes.
     */
    private static QName getQualifiedAttributeValue(Element el, String attrName, String elDesc, Definition def) throws WSDLException {
        try {
            return DOMUtils.getQualifiedAttributeValue(el, attrName, elDesc, false, def);
        } catch (WSDLException e) {
            if (e.getFaultCode().equals(WSDLException.NO_PREFIX_SPECIFIED)) {
                String attrValue = DOMUtils.getAttribute(el, attrName);

                return new QName(attrValue);
            } else {
                throw e;
            }
        }
    }

    /**
     * This method should be used for elements that do not support extension
     * attributes because it tracks unexpected remaining attributes.
     */
    private static QName getQualifiedAttributeValue(Element el, String attrName, String elDesc, Definition def, List remainingAttrs)
        throws WSDLException {
        try {
            return DOMUtils.getQualifiedAttributeValue(el, attrName, elDesc, false, def, remainingAttrs);
        } catch (WSDLException e) {
            if (e.getFaultCode().equals(WSDLException.NO_PREFIX_SPECIFIED)) {
                String attrValue = DOMUtils.getAttribute(el, attrName, remainingAttrs);

                return new QName(attrValue);
            } else {
                throw e;
            }
        }
    }

    private static void checkElementName(Element el, QName qname) throws WSDLException {
        if (!QNameUtils.matches(qname, el)) {
            WSDLException wsdlExc = new WSDLException(WSDLException.INVALID_WSDL, "Expected element '" + qname + "'.");

            wsdlExc.setLocation(XPathUtils.getXPathExprFromNode(el));

            throw wsdlExc;
        }
    }

    private static Document getDocument(InputSource inputSource, String desc) throws WSDLException {
        // Gravitee delta vs wsdl4j 1.6.3: OWASP XXE prevention on every parse,
        // including nested wsdl:import / schema import|include|redefine.
        // see: https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        String feature = null;
        try {
            feature = "http://apache.org/xml/features/disallow-doctype-decl";
            factory.setFeature(feature, true);

            feature = "http://xml.org/sax/features/external-general-entities";
            factory.setFeature(feature, false);

            feature = "http://xml.org/sax/features/external-parameter-entities";
            factory.setFeature(feature, false);

            feature = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
            factory.setFeature(feature, false);

            feature = XMLConstants.FEATURE_SECURE_PROCESSING;
            factory.setFeature(feature, true);

            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            try {
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            } catch (IllegalArgumentException ignored) {
                // Non-JDK DocumentBuilderFactory implementations may not support these attributes.
            }
        } catch (ParserConfigurationException e) {
            throw new WSDLException(
                WSDLException.PARSER_ERROR,
                "XML parser does not support required security feature '" + feature + "'.",
                e
            );
        }

        factory.setNamespaceAware(true);
        factory.setValidating(false);

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputSource);

            return doc;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WSDLException(WSDLException.PARSER_ERROR, "Problem parsing '" + desc + "'.", e);
        }
    }

    private void ensureDocumentBudgetAllowsAnother() throws WSDLException {
        if (documentNestingDepth >= MAX_DOCUMENT_NESTING_DEPTH) {
            throw new WSDLException(
                WSDLException.OTHER_ERROR,
                "Maximum WSDL/schema nesting depth of " + MAX_DOCUMENT_NESTING_DEPTH + " exceeded."
            );
        }
        if (fetchedDocumentCount >= MAX_FETCHED_DOCUMENTS) {
            throw new WSDLException(
                WSDLException.OTHER_ERROR,
                "Maximum number of fetched WSDL/schema documents (" + MAX_FETCHED_DOCUMENTS + ") exceeded."
            );
        }
    }

    private void enterNestedDocument() throws WSDLException {
        ensureDocumentBudgetAllowsAnother();
        documentNestingDepth++;
        fetchedDocumentCount++;
    }

    private void leaveNestedDocument() {
        documentNestingDepth--;
    }

    private static void registerNSDeclarations(NamedNodeMap attrs, Definition def) {
        int size = attrs.getLength();

        for (int i = 0; i < size; i++) {
            Attr attr = (Attr) attrs.item(i);
            String namespaceURI = attr.getNamespaceURI();
            String localPart = attr.getLocalName();
            String value = attr.getValue();

            if (namespaceURI != null && namespaceURI.equals(Constants.NS_URI_XMLNS)) {
                if (localPart != null && !localPart.equals(Constants.ATTR_XMLNS)) {
                    DOMUtils.registerUniquePrefix(localPart, value, def);
                } else {
                    DOMUtils.registerUniquePrefix(null, value, def);
                }
            }
        }
    }

    /**
     * Read the WSDL document accessible via the specified
     * URI into a WSDL definition.
     *
     * @param wsdlURI a URI (can be a filename or URL) pointing to a
     * WSDL XML definition.
     * @return the definition.
     */
    public Definition readWSDL(String wsdlURI) throws WSDLException {
        return readWSDL(null, wsdlURI);
    }

    /**
     * Read the WSDL document accessible via the specified
     * URI into a WSDL definition.
     *
     * @param contextURI the context in which to resolve the
     * wsdlURI, if the wsdlURI is relative. Can be null, in which
     * case it will be ignored.
     * @param wsdlURI a URI (can be a filename or URL) pointing to a
     * WSDL XML definition.
     * @return the definition.
     */
    public Definition readWSDL(String contextURI, String wsdlURI) throws WSDLException {
        try {
            if (verbose) {
                System.out.println(
                    "Retrieving document at '" + wsdlURI + "'" + (contextURI == null ? "." : ", relative to '" + contextURI + "'.")
                );
            }

            URL url = SecureWSDLLocator.resolve(contextURI, wsdlURI);
            InputStream inputStream = StringUtils.getContentAsInputStream(url);
            InputSource inputSource = new InputSource(inputStream);
            inputSource.setSystemId(url.toString());
            Document doc = getDocument(inputSource, url.toString());

            inputStream.close();

            Definition def = readWSDL(url.toString(), doc);

            return def;
        } catch (WSDLException e) {
            throw e;
        } catch (WsdlImportResolutionException e) {
            throw new WSDLException(
                WSDLException.OTHER_ERROR,
                "Unable to resolve imported document at '" + wsdlURI + (contextURI == null ? "'." : "', relative to '" + contextURI + "'."),
                e
            );
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WSDLException(
                WSDLException.OTHER_ERROR,
                "Unable to resolve imported document at '" + wsdlURI + (contextURI == null ? "'." : "', relative to '" + contextURI + "'."),
                e
            );
        }
    }

    /**
     * Read the specified &lt;wsdl:definitions&gt; element into a WSDL
     * definition.
     *
     * @param documentBaseURI the document base URI of the WSDL definition
     * described by the element. Will be set as the documentBaseURI
     * of the returned Definition. Can be null, in which case it
     * will be ignored.
     * @param definitionsElement the &lt;wsdl:definitions&gt; element
     * @return the definition described by the element.
     */
    public Definition readWSDL(String documentBaseURI, Element definitionsElement) throws WSDLException {
        return readWSDL(documentBaseURI, definitionsElement, null);
    }

    /**
     * Read the specified &lt;wsdl:definitions&gt; element into a WSDL
     * definition. The WSDLLocator is used to provide the document
     * base URIs. The InputSource of the WSDLLocator is ignored, instead
     * the WSDL is parsed from the given Element.
     *
     * @param locator A WSDLLocator object used to provide
     * the document base URI of the WSDL definition described by the
     * element.
     * @param definitionsElement the &lt;wsdl:definitions&gt; element
     * @return the definition described by the element.
     */
    public Definition readWSDL(WSDLLocator locator, Element definitionsElement) throws WSDLException {
        try {
            this.loc = locator;
            return readWSDL(locator.getBaseURI(), definitionsElement, null);
        } finally {
            locator.close();
            this.loc = null;
        }
    }

    protected Definition readWSDL(String documentBaseURI, Element definitionsElement, Map importedDefs) throws WSDLException {
        enterNestedDocument();
        try {
            return parseDefinitions(documentBaseURI, definitionsElement, importedDefs);
        } finally {
            leaveNestedDocument();
        }
    }

    /**
     * Read the specified WSDL document into a WSDL definition.
     *
     * @param documentBaseURI the document base URI of the WSDL definition
     * described by the document. Will be set as the documentBaseURI
     * of the returned Definition. Can be null, in which case it
     * will be ignored.
     * @param wsdlDocument the WSDL document, an XML
     * document obeying the WSDL schema.
     * @return the definition described in the document.
     */
    public Definition readWSDL(String documentBaseURI, Document wsdlDocument) throws WSDLException {
        return readWSDL(documentBaseURI, wsdlDocument.getDocumentElement());
    }

    /**
     * Read a WSDL document into a WSDL definition.
     *
     * @param documentBaseURI the document base URI of the WSDL definition
     * described by the document. Will be set as the documentBaseURI
     * of the returned Definition. Can be null, in which case it
     * will be ignored.
     * @param inputSource an InputSource pointing to the
     * WSDL document, an XML document obeying the WSDL schema.
     * @return the definition described in the document pointed to
     * by the InputSource.
     */
    public Definition readWSDL(String documentBaseURI, InputSource inputSource) throws WSDLException {
        String location = (inputSource.getSystemId() != null ? inputSource.getSystemId() : "- WSDL Document -");

        return readWSDL(documentBaseURI, getDocument(inputSource, location));
    }

    /**
     * Read a WSDL document into a WSDL definition.
     *
     * @param locator A WSDLLocator object used to provide InputSources
     * pointing to the wsdl file.
     * @return the definition described in the document
     */
    public Definition readWSDL(WSDLLocator locator) throws WSDLException {
        InputSource is = locator.getBaseInputSource();
        String base = locator.getBaseURI();

        if (is == null) {
            throw new WSDLException(WSDLException.OTHER_ERROR, "Unable to locate document at '" + base + "'.");
        }
        is.setSystemId(base);

        this.loc = locator;

        if (verbose) {
            System.out.println("Retrieving document at '" + base + "'.");
        }

        try {
            return readWSDL(base, is);
        } finally {
            this.loc.close();
            this.loc = null;
        }
    }
}
