package io.gravitee.definition.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JacksonSerializerTest {

    @Test
    public void definition_defaultHttpConfig() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaulthttpconfig.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_overridedHttpConfig() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-overridedhttpconfig.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_noProxyPart() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-noproxy-part.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_noPath() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-nopath.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_defaultPath() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_multiplePath() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-multiplepath.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_pathwithmethods() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_pathwithoutmethods() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-path-nohttpmethod.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_pathwithpolicies() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_pathwithpolicies_disabled() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_pathwithoutpolicy() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-path-withoutpolicy.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    private Api getDefinition(String resource) throws Exception {
        URL jsonFile = JacksonDeserializerTest.class.getResource(resource);
        return objectMapper().readValue(jsonFile, Api.class);
    }

    private ObjectMapper objectMapper() {
        return new GraviteeMapper();
    }
}
