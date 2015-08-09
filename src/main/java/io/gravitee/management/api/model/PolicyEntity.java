package io.gravitee.management.api.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class PolicyEntity {

    /**
     * The policy identifier
     */
    private String id;

    /**
     * The policy name
     */
    private String name;

    /**
     * The policy description
     */
    private String description;

    /**
     * The policy version
     */
    private String version;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PolicyEntity{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyEntity that = (PolicyEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
