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
package io.gravitee.apim.infra.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import freemarker.core.TemplateClassResolver;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import io.gravitee.apim.core.template.TemplateProcessorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Ensures that FreeMarker templates written by users (notification templates, documentation pages, metadata, portal pages...)
 * cannot escape the sandbox.
 */
class TemplateSandboxTest {

    @TempDir
    Path tempDir;

    private final FreemarkerTemplateProcessor processor = new FreemarkerTemplateProcessor();

    public static class Context {

        private final Map<String, Object> attributes = new HashMap<>();

        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }

    /** Overrides a blacklisted method: an exact-method blacklist alone would not catch it. */
    public static class CustomClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return super.loadClass(name);
        }
    }

    public static class LeakyBean {

        public Class<?> type = ProcessBuilder.class;

        public String getName() {
            return "leaky";
        }

        public Object getSomething() {
            return ProcessBuilder.class;
        }

        public Class<?> getType() {
            return ProcessBuilder.class;
        }

        public ClassLoader getLoader() {
            return LeakyBean.class.getClassLoader();
        }
    }

    /** Mimics a static setter reachable from a model object, e.g. a definition class holding a process-wide default. */
    public static class Settings {

        private static int defaultStatus = 400;

        private int status = 400;

        public static void setDefaultStatus(int status) {
            defaultStatus = status;
        }

        public static int defaultStatus() {
            return defaultStatus;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }
    }

    public enum Visibility {
        PUBLIC,
        PRIVATE,
    }

    public static class Owner {

        public String getDisplayName() {
            return "John Doe";
        }
    }

    public static class Api {

        private final Settings settings = new Settings();

        public String getName() {
            return "My API";
        }

        public String getVersion() {
            return "1.0";
        }

        public Visibility getVisibility() {
            return Visibility.PUBLIC;
        }

        public Owner getPrimaryOwner() {
            return new Owner();
        }

        public Map<String, String> getMetadata() {
            return Map.of("email-support", "support@example.com");
        }

        public List<String> getEntrypoints() {
            return List.of("https://api.example.com/my-api");
        }

        public Date getDeployedAt() {
            return new Date(0);
        }

        public Settings getSettings() {
            return settings;
        }

        @Override
        public String toString() {
            return "Api{name=My API}";
        }
    }

    private Map<String, Object> model() {
        Context context = new Context();
        context.attributes.put("my-attribute", "my-value");
        // Models may contain arbitrary objects: dangerous types must never be usable from templates
        context.attributes.put("pb", new ProcessBuilder("id"));
        context.attributes.put("runtime", Runtime.getRuntime());
        context.attributes.put("file", tempDir.resolve("victim").toFile());
        context.attributes.put("path", Path.of("/etc/passwd"));
        context.attributes.put("clazz", String.class);
        Map<String, Object> model = new HashMap<>();
        model.put("context", context);
        model.put("api", new Api());
        model.put("items", List.of("a", "b", "c"));
        model.put("headers", Map.of("X-Foo", List.of("bar")));
        model.put("thread", Thread.currentThread());
        model.put("loader", getClass().getClassLoader());
        model.put("anEnum", TimeUnit.SECONDS);
        model.put("customLoader", new CustomClassLoader());
        model.put("bean", new LeakyBean());
        return model;
    }

    private String render(String template) throws TemplateProcessorException {
        return processor.processInlineTemplate(template, model());
    }

    @Test
    void should_block_unsafe_process_builder_rce() {
        Path marker = tempDir.resolve("pwned");
        String payload = """
            <#assign C = context.getClass()>
            <#assign MOD = C.getClass().getModule()>
            <#assign LAYER = MOD.getLayer()>
            <#list LAYER.modules() as m>
              <#if m.getName() == "jdk.unsupported">
                <#assign UC = C.forName(m, "sun.misc.Unsafe")>
              </#if>
            </#list>
            <#assign UF = UC.getDeclaredField("theUnsafe")>
            <#assign a = UF.trySetAccessible()>
            <#assign U = UF.get(null)>
            <#assign PB = C.forName(MOD, "java.lang.ProcessBuilder")>
            <#assign pb = U.allocateInstance(PB)>
            <#assign ign = pb.command("/bin/sh","-c","touch %s")>
            <#assign p = pb.start()>
            <#assign ignored = p.waitFor()>
            """.formatted(marker.toAbsolutePath());

        assertThatThrownBy(() -> render(payload)).isInstanceOf(TemplateProcessorException.class);
        assertThat(Files.exists(marker)).isFalse();
    }

    @Test
    void should_block_method_invoke_chain_from_a_reachable_class() {
        Path marker = tempDir.resolve("pwned");
        String payload = """
            <#assign c = context.attributes['clazz']>
            <#list c.getMethods() as m><#if m.getName() == 'getClass'><#assign gc = m></#if></#list>
            <#assign CLS = gc.invoke(c)>
            <#list CLS.getMethods() as m><#if m.getName() == 'forName' && m.getParameterCount() == 1><#assign fn = m></#if></#list>
            <#assign RT = fn.invoke(CLS, 'java.lang.Runtime')>
            <#list RT.getMethods() as m><#if m.getName() == 'getRuntime'><#assign gr = m></#if></#list>
            <#assign p = gr.invoke(RT).exec(['/bin/sh','-c','touch %s'])>
            <#assign ignored = p.waitFor()>
            """.formatted(marker.toAbsolutePath());

        assertThatThrownBy(() -> render(payload)).isInstanceOf(TemplateProcessorException.class);
        assertThat(Files.exists(marker)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            // Entry point: obtaining a java.lang.Class from any model object
            "${context.getClass()}",
            "${context.class}",
            "${api.getClass()}",
            "${items.getClass()}",
            "${anEnum.getDeclaringClass()}",
            // Anything on java.lang.Class (reflection, modules, class loading)
            "${context.attributes['clazz'].getName()}",
            "${context.attributes['clazz'].getModule()}",
            "${context.attributes['clazz'].forName('java.lang.Runtime')}",
            "${context.attributes['clazz'].getMethods()?size}",
            "${context.attributes['clazz'].getDeclaredFields()?size}",
            "${context.attributes['clazz'].getInterfaces()?size}",
            // Class loaders
            "${loader.loadClass('java.lang.Runtime')}",
            "${loader.getParent()}",
            "${customLoader.loadClass('java.lang.Runtime')}",
            // Threads
            "${thread.getContextClassLoader()}",
            "${thread.getStackTrace()?size}",
            // Dangerous objects reachable through the model
            "${context.attributes['pb'].start()}",
            "${context.attributes['pb'].command('id')}",
            "${context.attributes['runtime'].exec('id')}",
            "${context.attributes['runtime'].availableProcessors()}",
            "${context.attributes['file'].delete()?c}",
            "${context.attributes['file'].getName()}",
            "${context.attributes['path'].toFile()}",
            // Leaks through getters / fields typed with (or returning at runtime) a dangerous type
            "${bean.type.getName()}",
            "${bean.getType().getName()}",
            "${bean.something.getName()}",
            "${bean.getLoader().loadClass('java.lang.Runtime')}",
        }
    )
    void should_block_reflection_gadgets(String template) {
        assertThatThrownBy(() -> render(template)).isInstanceOf(TemplateProcessorException.class);
    }

    @Test
    void should_not_allow_templates_to_change_process_wide_state_through_static_methods() {
        assertThatThrownBy(() -> render("<#assign x = api.settings.setDefaultStatus(299)>")).isInstanceOf(TemplateProcessorException.class);
        assertThat(Settings.defaultStatus()).isEqualTo(400);
    }

    @Test
    void should_not_allow_templates_to_modify_model_objects() throws Exception {
        var model = model();
        var api = (Api) model.get("api");

        assertThatThrownBy(() -> processor.processInlineTemplate("<#assign x = api.settings.setStatus(299)>", model)).isInstanceOf(
            TemplateProcessorException.class
        );
        assertThat(api.getSettings().getStatus()).isEqualTo(400);
        assertThat(processor.processInlineTemplate("${api.settings.status}", model)).isEqualTo("400");
    }

    @Test
    void should_not_delete_files_reachable_from_the_model() throws IOException {
        File victim = tempDir.resolve("victim").toFile();
        Files.writeString(victim.toPath(), "content");

        assertThatThrownBy(() -> render("${context.attributes['file'].delete()?c}")).isInstanceOf(TemplateProcessorException.class);
        assertThat(victim).exists();
    }

    @Test
    void should_block_api_and_new_builtins() {
        assertThatThrownBy(() -> render("${context?api.getClass()}")).isInstanceOf(TemplateProcessorException.class);
        assertThatThrownBy(() -> render("<#assign ex='freemarker.template.utility.Execute'?new()>${ex('id')}")).isInstanceOf(
            TemplateProcessorException.class
        );
        assertThatThrownBy(() ->
            render("<#assign ob='freemarker.template.utility.ObjectConstructor'?new()>${ob('java.lang.ProcessBuilder','id')}")
        ).isInstanceOf(TemplateProcessorException.class);
        assertThatThrownBy(() -> render("<#assign s='freemarker.template.SimpleScalar'?new('x')>${s}"))
            .isInstanceOf(TemplateProcessorException.class)
            .hasMessageContaining("not allowed");
    }

    /**
     * The whole defense against <code>?new</code> / <code>?api</code> based payloads (e.g. CVE-2025-70830) relies on these settings.
     */
    @Test
    void should_configure_freemarker_restrictively() {
        Configuration configuration = FreemarkerConfigurationFactory.createSecureConfiguration();

        assertThat(configuration.getNewBuiltinClassResolver()).isSameAs(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        assertThat(configuration.isAPIBuiltinEnabled()).isFalse();
        assertThat(configuration.getObjectWrapper()).isInstanceOf(BeansWrapper.class);
        assertThat(((BeansWrapper) configuration.getObjectWrapper()).getMemberAccessPolicy()).isSameAs(SecureMemberAccessPolicy.INSTANCE);
    }

    /**
     * CVE-2025-70830 (Datart): FreeMarker SSTI → RCE through <code>ObjectConstructor?new()</code>.
     */
    @Test
    void should_block_cve_2025_70830_object_constructor_payloads() {
        Path marker = tempDir.resolve("pwned");
        String processBuilder = """
            <#assign ob="freemarker.template.utility.ObjectConstructor"?new()>
            <#assign pb=ob("java.lang.ProcessBuilder",["/bin/sh","-c","touch %s"])>
            ${pb.start()}
            """.formatted(marker.toAbsolutePath());
        String scriptEngine = """
            <#assign ob='freemarker.template.utility.ObjectConstructor'?new()>
            <#assign sem=ob('javax.script.ScriptEngineManager')>
            ${sem.getEngineByName('js').eval('1+1')}
            """;

        assertThatThrownBy(() -> render(processBuilder))
            .isInstanceOf(TemplateProcessorException.class)
            .hasMessageContaining("ObjectConstructor")
            .hasMessageContaining("not allowed");
        assertThatThrownBy(() -> render(scriptEngine))
            .isInstanceOf(TemplateProcessorException.class)
            .hasMessageContaining("ObjectConstructor")
            .hasMessageContaining("not allowed");
        assertThat(Files.exists(marker)).isFalse();
    }

    /**
     * Templates must not be able to re-enable what the configuration disables (<code>?new</code>, <code>?api</code>, object wrapper).
     */
    @ParameterizedTest
    @ValueSource(
        strings = {
            "<#setting new_builtin_class_resolver='unrestricted'><#assign ob='freemarker.template.utility.ObjectConstructor'?new()>${ob('java.lang.ProcessBuilder','touch','%s').start()}",
            "<#setting newBuiltinClassResolver='unrestricted'><#assign ex='freemarker.template.utility.Execute'?new()>${ex('touch %s')}",
            "<#setting new_builtin_class_resolver='allows_nothing, freemarker.template.utility.Execute'><#assign ex='freemarker.template.utility.Execute'?new()>${ex('touch %s')}",
            "<#setting api_builtin_enabled=true>${context?api.getClass().forName('java.lang.ProcessBuilder')} %s",
            "<#setting object_wrapper='DefaultObjectWrapper(2.3.30)'>${context.getClass().forName('java.lang.ProcessBuilder')} %s",
            "<#setting template_loader='freemarker.cache.FileTemplateLoader'>%s",
            // Same payloads, evaluated dynamically
            "<#assign t=r\"<#assign ex='freemarker.template.utility.Execute'?new()>${ex('touch %s')}\"?interpret><@t/>",
            "${\"'freemarker.template.utility.Execute'?new()('touch %s')\"?eval}",
        }
    )
    void should_not_allow_templates_to_relax_the_sandbox(String template) {
        Path marker = tempDir.resolve("pwned");

        assertThatThrownBy(() -> render(template.formatted(marker.toAbsolutePath()))).isInstanceOf(TemplateProcessorException.class);
        assertThat(Files.exists(marker)).isFalse();
    }

    @Test
    void should_still_render_legitimate_templates() throws Exception {
        String template = """
            attr=${context.attributes['my-attribute']}
            missing=${context.attributes['unknown']!'none'}
            <#list items as i>${i?upper_case}</#list>
            header=${headers['X-Foo'][0]}
            size=${items?size}
            enum=${anEnum.name()}
            str=${"hello"?cap_first}
            bean=${bean.name}
            api=${api.name} ${api.getVersion()} ${api.visibility} ${api.primaryOwner.displayName}
            metadata=${api.metadata['email-support']}
            entrypoint=<#if api.entrypoints?has_content>${api.entrypoints[0]}</#if>
            deployed=${api.deployedAt?date?iso_utc}
            toString=${api.toString()}
            """;

        String result = render(template);

        assertThat(result)
            .contains("attr=my-value")
            .contains("missing=none")
            .contains("ABC")
            .contains("header=bar")
            .contains("size=3")
            .contains("enum=SECONDS")
            .contains("str=Hello")
            .contains("bean=leaky")
            .contains("api=My API 1.0 PUBLIC John Doe")
            .contains("metadata=support@example.com")
            .contains("entrypoint=https://api.example.com/my-api")
            .contains("deployed=1970-01-01")
            .contains("toString=Api{name=My API}");
    }
}
