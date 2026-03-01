# How to Rebuild the Backend (Gateway + REST API)

> **TL;DR**: You can't do `mvn clean install` from root — Lombok annotation processing is broken on upstream modules (`gravitee-apim-definition-model`, `rest-api-model`, etc.). Use the javac patch approach below, then bounce with `exec:java`.

## The Problem

Multiple upstream modules use Lombok (`@Getter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`) but annotation processing doesn't fire correctly during a clean Maven build. This causes 100s of `cannot find symbol` errors for generated methods like `getId()`, `builder()`, `log`.

These modules were originally built by CI/Artifactory and the JARs live in `~/.m2` from remote snapshots. **We never compiled them locally.** Any `mvn clean` destroys the working state and we can't rebuild.

## What Works

### 1. The "Happy Path" — When `.m2` JARs Are Fresh

If you haven't run `mvn clean` on upstream modules, this command handles everything:

```bash
mvn clean install -DskipTests -Dskip.validation -T 2C \
  -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service,gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution \
  -am
```

### 2. The "javac Patch" — When Lombok Is Broken

When the Maven reactor won't compile upstream modules, manually compile just our zee files and patch the service JAR.

#### Step A: Assemble the classpath from `.m2`

```bash
SERVICE_JAR=~/.m2/repository/io/gravitee/apim/rest/api/gravitee-apim-rest-api-service/4.11.0-SNAPSHOT/gravitee-apim-rest-api-service-4.11.0-SNAPSHOT.jar
V4_JAR=~/.m2/repository/io/gravitee/apim/definition/llm/v4/v4/0.1.0-ALPHA/v4-0.1.0-ALPHA.jar
V2_JAR=~/.m2/repository/io/gravitee/apim/definition/llm/v2/v2/0.1.0-ALPHA/v2-0.1.0-ALPHA.jar
ENGINE_JAR=~/.m2/repository/com/jsonschema/llm/json-schema-llm-engine/0.1.0-ALPHA/json-schema-llm-engine-0.1.0-ALPHA.jar
SPRING_CTX=$(find ~/.m2/repository/org/springframework/spring-context -name "spring-context-6*.jar" -not -name "*javadoc*" -not -name "*sources*" | sort | tail -1)
SPRING_BEANS=$(find ~/.m2/repository/org/springframework/spring-beans -name "spring-beans-6*.jar" -not -name "*javadoc*" -not -name "*sources*" | sort | tail -1)
SLF4J=$(find ~/.m2/repository/org/slf4j/slf4j-api -name "slf4j-api-2*.jar" -not -name "*sources*" | sort | tail -1)
JACKSON=$(find ~/.m2/repository/com/fasterxml/jackson/core/jackson-databind -name "jackson-databind-2.1*.jar" -not -name "*sources*" -not -name "*javadoc*" | sort | tail -1)
JACKSON_CORE=$(find ~/.m2/repository/com/fasterxml/jackson/core/jackson-core -name "jackson-core-2.1*.jar" -not -name "*sources*" -not -name "*javadoc*" | sort | tail -1)
JACKSON_ANN=$(find ~/.m2/repository/com/fasterxml/jackson/core/jackson-annotations -name "jackson-annotations-2.1*.jar" -not -name "*sources*" -not -name "*javadoc*" | sort | tail -1)
```

#### Step B: Compile changed files

```bash
SRC=gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java
mkdir -p /tmp/zee-build/classes

javac -d /tmp/zee-build/classes \
  -cp "$SERVICE_JAR:$V4_JAR:$V2_JAR:$ENGINE_JAR:$SPRING_CTX:$SPRING_BEANS:$SLF4J:$JACKSON:$JACKSON_CORE:$JACKSON_ANN" \
  $SRC/io/gravitee/apim/infra/zee/LlmEngineServiceImpl.java \
  $SRC/io/gravitee/apim/infra/zee/ZeeConfiguration.java \
  $SRC/io/gravitee/apim/core/zee/domain_service/LlmEngineService.java
```

Add any other changed `.java` files to the javac command as needed.

#### Step C: Patch the service JAR

```bash
# Update the .m2 JAR with new classes
cp $SERVICE_JAR /tmp/zee-build/service.jar
cd /tmp/zee-build/classes
jar uf /tmp/zee-build/service.jar $(find . -name "*.class" | sed 's|^\./||')
cp /tmp/zee-build/service.jar $SERVICE_JAR

# Update distribution
DIST_LIB=gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution/lib
cp /tmp/zee-build/service.jar $DIST_LIB/gravitee-apim-rest-api-service-4.11.0-SNAPSHOT.jar
```

#### Step D: Swap SDK JARs (if SDK version changed)

> ⚠️ **CRITICAL**: SDK JARs go in `lib/ext/` NOT `lib/`. The `lib/ext/` directory uses a **parent classloader** that takes priority. Putting JARs in `lib/` while stale copies exist in `lib/ext/` causes `NoSuchMethodError` at runtime.

```bash
DIST_EXT=$DIST_LIB/ext

# Copy ALL SDK-related JARs to lib/ext/
cp $V4_JAR $DIST_EXT/
cp $V2_JAR $DIST_EXT/
cp $ENGINE_JAR $DIST_EXT/
cp ~/.m2/repository/com/jsonschema/llm/json-schema-llm-java/0.1.0-ALPHA/json-schema-llm-java-0.1.0-ALPHA.jar $DIST_EXT/

# Also update transitive WASM deps if they changed
for name in wasm wasi log; do
  SRC=$(find ~/.m2/repository/com/jsonschema/llm/$name -name "$name-*.jar" ! -name "*sources*" ! -name "*javadoc*" 2>/dev/null | head -1)
  [ -n "$SRC" ] && cp "$SRC" "$DIST_EXT/"
done
```

### 3. Starting the Services

Kill the old process first, then:

```bash
# REST API
cd gravitee-apim-rest-api && mvn compile exec:java \
  -Pdev \
  -pl gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-container

# Gateway
cd gravitee-apim-gateway && mvn compile exec:java \
  -Pdev \
  -pl gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-container
```

The `exec:java` command resolves classpath from `.m2` + `distribution/lib/`. It does NOT recompile upstream modules — it just compiles the standalone container (which has minimal code) and runs the main class.

## Key Gotchas

| Trap                                  | What Happens                                                                      | Fix                                             |
| ------------------------------------- | --------------------------------------------------------------------------------- | ----------------------------------------------- |
| Running `mvn clean` from root         | Destroys all compiled classes across all modules                                  | Never `mvn clean` from root. Use targeted `-pl` |
| Running `mvn clean install` from root | Tries to recompile all modules, hits Lombok failures                              | Use the javac patch approach above              |
| `-am` flag with `-pl`                 | Pulls in upstream modules and recompiles them                                     | Skip `-am` if `.m2` JARs are fresh              |
| `exec:java` says "Nothing to compile" | Good! It's using pre-compiled classes from `.m2`                                  | Expected behavior                               |
| New file added without license header | License plugin blocks even with `-Dskip.validation`                               | Run `mvn license:format` first                  |
| **SDK JARs in `lib/` not `lib/ext/`** | **`lib/ext/` has parent classloader priority — stale copies there shadow `lib/`** | **ALWAYS put SDK JARs in `lib/ext/` only**      |
| Same-name JAR in both dirs            | `NoSuchMethodError` at runtime — old class wins                                   | Remove from `lib/`, keep only in `lib/ext/`     |

## Why This Happens

The `gravitee-api-management` monorepo has 100+ modules. Many use Lombok for code generation. The Lombok annotation processor requires proper configuration that seems to only work when:

1. JARs are pre-built by CI and pulled from Artifactory into `.m2`, OR
2. A very specific build order is maintained (which `mvn clean install` from root _should_ do but doesn't)

The root cause is likely a version mismatch (`maven-lombok.version=1.18.36` in parent POM vs `lombok:1.18.42` resolved dependency) or a missing annotation processor path configuration.

## SDK Versions

| Artifact             | GroupId                              | Current Version | `.m2` Source                                                              | Runtime Location |
| -------------------- | ------------------------------------ | --------------- | ------------------------------------------------------------------------- | ---------------- |
| v4 SDK               | `io.gravitee.apim.definition.llm.v4` | `0.1.0-ALPHA`   | `~/.m2/repository/io/gravitee/apim/definition/llm/v4/v4/0.1.0-ALPHA/`     | `lib/ext/`       |
| v2 SDK               | `io.gravitee.apim.definition.llm.v2` | `0.1.0-ALPHA`   | `~/.m2/repository/io/gravitee/apim/definition/llm/v2/v2/0.1.0-ALPHA/`     | `lib/ext/`       |
| Engine               | `com.jsonschema.llm`                 | `0.1.0-ALPHA`   | `~/.m2/repository/com/jsonschema/llm/json-schema-llm-engine/0.1.0-ALPHA/` | `lib/ext/`       |
| json-schema-llm-java | `com.jsonschema.llm`                 | `0.1.0-ALPHA`   | `~/.m2/repository/com/jsonschema/llm/json-schema-llm-java/0.1.0-ALPHA/`   | `lib/ext/`       |

SDKs are generated from `~/workspace/Gravitee/gravitee-api-definition-spec` → `sdks/java/{v4,v2}/` → `mvn install` → `.m2`.

## Classloader Architecture

The Gravitee Bootstrap creates TWO classloaders:

1. **Extension classloader** (PARENT): loads `lib/ext/*.jar` — takes priority
2. **Gravitee classloader** (CHILD): loads `lib/*.jar` — loses to parent

**Rule**: SDK JARs MUST go in `lib/ext/` only. Never put them in `lib/` — if a stale copy exists in `lib/ext/`, the parent classloader wins and you get `NoSuchMethodError`.
