package io.gravitee.management.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PluginEntity {

    @JsonProperty("class")
    private String className;

    private URL[] dependencies;

    private String path;

    private String type;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public URL[] getDependencies() {
        return dependencies;
    }

    public void setDependencies(URL[] dependencies) {
        this.dependencies = dependencies;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
