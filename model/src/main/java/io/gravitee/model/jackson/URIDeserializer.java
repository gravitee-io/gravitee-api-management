package io.gravitee.model.jackson;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;

import java.net.URI;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class URIDeserializer extends FromStringDeserializer<URI> {
    public static final URIDeserializer instance = new URIDeserializer();

    public URIDeserializer() {
        super(URI.class);
    }

    protected URI _deserialize(String var1, DeserializationContext var2) throws IllegalArgumentException {
        return URI.create(var1);
    }

    protected URI _deserializeFromEmptyString() {
        return URI.create("");
    }
}