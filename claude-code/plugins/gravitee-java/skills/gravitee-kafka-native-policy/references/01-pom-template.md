# pom.xml Template

## Template

Replace `<POLICY_ID>`, `<POLICY_NAME>`, `<POLICY_DESCRIPTION>`, `<GROUP_ID>`, and `<PUBLISH_PATH>` with actual values.

- **Community policies**: `groupId=io.gravitee.policy`, `publish-folder-path=graviteeio-apim/plugins/policies`
- **Enterprise policies**: `groupId=com.graviteesource.policy`, `publish-folder-path=graviteeio-ee/apim/plugins/policies`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId><GROUP_ID></groupId>
    <artifactId>gravitee-policy-kafka-<POLICY_ID></artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <name>Gravitee.io APIM - Policy Kafka - <POLICY_NAME></name>
    <description><POLICY_DESCRIPTION></description>

    <parent>
        <groupId>io.gravitee</groupId>
        <artifactId>gravitee-parent</artifactId>
        <version>22.5.1</version>
    </parent>

    <properties>
        <!-- Gravitee dependencies for APIM -->
        <gravitee-apim.version>4.10.4</gravitee-apim.version>

        <!-- Maven plugins -->
        <maven-plugin-assembly.version>3.8.0</maven-plugin-assembly.version>
        <maven-plugin-properties.version>1.3.0</maven-plugin-properties.version>

        <!-- Property used by the publication job in CI-->
        <publish-folder-path><PUBLISH_PATH></publish-folder-path>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Import bom to properly inherit all dependencies -->
            <dependency>
                <groupId>io.gravitee.apim</groupId>
                <artifactId>gravitee-apim-bom</artifactId>
                <version>${gravitee-apim.version}</version>
                <scope>import</scope>
                <type>pom</type>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Provided scope (available at runtime via the gateway) -->
        <dependency>
            <groupId>io.gravitee.gateway</groupId>
            <artifactId>gravitee-gateway-api</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>io.gravitee.policy</groupId>
            <artifactId>gravitee-policy-api</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.kafka</groupId>
            <artifactId>kafka-clients</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Test scope -->
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.gravitee.json</groupId>
            <artifactId>gravitee-json-validation</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-params</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>net.javacrumbs.json-unit</groupId>
            <artifactId>json-unit-assertj</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
        <plugins>
            <plugin>
                <groupId>com.mycila</groupId>
                <artifactId>license-maven-plugin</artifactId>
                <version>${license-maven-plugin.version}</version>
                <configuration>
                    <skip>true</skip>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>properties-maven-plugin</artifactId>
                <version>${maven-plugin-properties.version}</version>
                <executions>
                    <execution>
                        <phase>initialize</phase>
                        <id>load-plugin-properties</id>
                        <goals>
                            <goal>read-project-properties</goal>
                        </goals>
                        <configuration>
                            <files>
                                <file>${project.basedir}/src/main/resources/plugin.properties</file>
                            </files>
                            <quiet>false</quiet>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>${maven-plugin-assembly.version}</version>
                <configuration>
                    <appendAssemblyId>false</appendAssemblyId>
                    <descriptors>
                        <descriptor>src/assembly/policy-assembly.xml</descriptor>
                    </descriptors>
                </configuration>
                <executions>
                    <execution>
                        <id>make-policy-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>com.hubspot.maven.plugins</groupId>
                <artifactId>prettier-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

## Notes

- The BOM (`gravitee-apim-bom`) manages all dependency versions. Do not specify versions for managed dependencies.
- All gateway-provided dependencies (`gravitee-gateway-api`, `kafka-clients`, `slf4j-api`) must be `scope=provided`.
- Additional runtime dependencies (not provided by the gateway) will be packaged in `lib/` of the ZIP assembly.
- Use Lombok (`provided` scope) for boilerplate reduction - it's included in the BOM.
