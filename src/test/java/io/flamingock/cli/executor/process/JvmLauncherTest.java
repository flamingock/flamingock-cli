/*
 * Copyright 2025 Flamingock (https://www.flamingock.io)
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
package io.flamingock.cli.executor.process;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for JvmLauncher command building.
 */
class JvmLauncherTest {

    // ================== Spring Boot Command Tests ==================

    @Test
    void buildSpringBootCommand_shouldContainJarFlag() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", null, null, null);

        assertTrue(command.contains("-jar"));
        assertTrue(command.contains("/path/to/app.jar"));
    }

    @Test
    void buildSpringBootCommand_shouldContainSpringWebDisabled() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", null, null, null);

        assertTrue(command.contains("--spring.main.web-application-type=none"));
    }

    @Test
    void buildSpringBootCommand_shouldContainCliProfile() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", null, null, null);

        assertTrue(command.contains("--spring.profiles.include=flamingock-cli"));
    }

    @Test
    void buildSpringBootCommand_shouldContainCliModeFlag() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", null, null, null);

        assertTrue(command.contains("--flamingock.cli.mode=true"));
    }

    @Test
    void buildSpringBootCommand_shouldDisableBanner() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", null, null, null);

        assertTrue(command.contains("--spring.main.banner-mode=off"));
    }

    @Test
    void buildSpringBootCommand_shouldHaveCorrectFlagCountWithoutOperation() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", null, null, null);

        // java -jar <jar> + 4 flags = 7 elements
        assertEquals(7, command.size());
    }

    @Test
    void buildSpringBootCommand_shouldIncludeOperationWhenProvided() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", "EXECUTE", null, null);

        assertTrue(command.contains("--flamingock.operation=EXECUTE"));
        // java -jar <jar> + 4 flags + operation = 8 elements
        assertEquals(8, command.size());
    }

    @Test
    void buildSpringBootCommand_shouldIncludeListOperation() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", "LIST", null, null);

        assertTrue(command.contains("--flamingock.operation=LIST"));
    }

    @Test
    void buildSpringBootCommand_shouldNotIncludeOperationWhenEmpty() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", "", null, null);

        // Should not contain any operation flag
        for (String arg : command) {
            assertFalse(arg.startsWith("--flamingock.operation="));
        }
        assertEquals(7, command.size());
    }

    @Test
    void buildSpringBootCommand_shouldIncludeLogLevelWhenProvided() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", "EXECUTE", null, "debug");

        assertTrue(command.contains("--logging.level.root=DEBUG"));
    }

    @Test
    void buildSpringBootCommand_shouldUppercaseLogLevel() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", null, null, "info");

        assertTrue(command.contains("--logging.level.root=INFO"));
    }

    @Test
    void buildSpringBootCommand_shouldNotIncludeLogLevelWhenNull() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", null, null, null);

        for (String arg : command) {
            assertFalse(arg.startsWith("--logging.level.root="));
        }
    }

    @Test
    void buildSpringBootCommand_shouldNotIncludeLogLevelWhenEmpty() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", null, null, "");

        for (String arg : command) {
            assertFalse(arg.startsWith("--logging.level.root="));
        }
    }

    @Test
    void buildSpringBootCommand_shouldIncludeOutputFileWhenProvided() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildSpringBootCommand("/path/to/app.jar", null, "/tmp/output.json", null);

        assertTrue(command.contains("--flamingock.output-file=/tmp/output.json"));
    }

    // ================== Plain Uber JAR Command Tests ==================

    @Test
    void buildPlainUberCommand_usesClasspath() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildPlainUberCommand("/path/to/app.jar", null, null, null);

        assertTrue(command.contains("-cp"));
        assertTrue(command.contains("/path/to/app.jar"));
        assertFalse(command.contains("-jar"));
    }

    @Test
    void buildPlainUberCommand_usesEntryPoint() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildPlainUberCommand("/path/to/app.jar", null, null, null);

        assertTrue(command.contains(JvmLauncher.FLAMINGOCK_CLI_ENTRY_POINT));
    }

    @Test
    void buildPlainUberCommand_noSpringFlags() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildPlainUberCommand("/path/to/app.jar", null, null, null);

        for (String arg : command) {
            assertFalse(arg.startsWith("--spring."), "Should not contain Spring flags: " + arg);
        }
    }

    @Test
    void buildPlainUberCommand_includesFlamingockFlags() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildPlainUberCommand("/path/to/app.jar", "EXECUTE", "/tmp/output.json", null);

        assertTrue(command.contains("--flamingock.cli.mode=true"));
        assertTrue(command.contains("--flamingock.operation=EXECUTE"));
        assertTrue(command.contains("--flamingock.output-file=/tmp/output.json"));
    }

    @Test
    void buildPlainUberCommand_usesFlamingockLogLevel() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildPlainUberCommand("/path/to/app.jar", null, null, "debug");

        assertTrue(command.contains("--flamingock.log.level=DEBUG"));
        // Should not use Spring-style logging
        for (String arg : command) {
            assertFalse(arg.startsWith("--logging.level.root="));
        }
    }

    @Test
    void buildPlainUberCommand_shouldHaveCorrectFlagCount() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildPlainUberCommand("/path/to/app.jar", null, null, null);

        // java -cp <jar> <entrypoint> + cli.mode = 5 elements
        assertEquals(5, command.size());
    }

    @Test
    void buildPlainUberCommand_shouldIncludeOperationWhenProvided() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildPlainUberCommand("/path/to/app.jar", "LIST", null, null);

        assertTrue(command.contains("--flamingock.operation=LIST"));
        // java -cp <jar> <entrypoint> + cli.mode + operation = 6 elements
        assertEquals(6, command.size());
    }

    @Test
    void buildPlainUberCommand_shouldNotIncludeOperationWhenEmpty() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildPlainUberCommand("/path/to/app.jar", "", null, null);

        for (String arg : command) {
            assertFalse(arg.startsWith("--flamingock.operation="));
        }
    }

    @Test
    void buildPlainUberCommand_shouldNotIncludeLogLevelWhenNull() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildPlainUberCommand("/path/to/app.jar", null, null, null);

        for (String arg : command) {
            assertFalse(arg.startsWith("--flamingock.log.level="));
        }
    }

    // ================== JAR Type Routing Tests ==================

    @Test
    void buildCommand_routesToSpringBootForSpringBootJar() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildCommand("/path/to/app.jar", null, null, null, JarType.SPRING_BOOT);

        assertTrue(command.contains("-jar"));
        assertTrue(command.contains("--spring.main.web-application-type=none"));
    }

    @Test
    void buildCommand_routesToPlainUberForPlainUberJar() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> command = launcher.buildCommand("/path/to/app.jar", null, null, null, JarType.PLAIN_UBER);

        assertTrue(command.contains("-cp"));
        assertTrue(command.contains(JvmLauncher.FLAMINGOCK_CLI_ENTRY_POINT));
    }

    // ================== Passthrough Args Tests (Spring Boot) ==================

    @Test
    void buildSpringBootCommand_jvmArgsPlacedBeforeJar() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> jvmArgs = Arrays.asList("-Xmx512m", "-Xms256m");
        List<String> command = launcher.buildSpringBootCommand(
                "/path/to/app.jar", "EXECUTE", null, null,
                Collections.emptyMap(), jvmArgs, Collections.emptyList());

        int xmxIndex = command.indexOf("-Xmx512m");
        int xmsIndex = command.indexOf("-Xms256m");
        int jarFlagIndex = command.indexOf("-jar");

        assertTrue(xmxIndex > 0, "JVM arg -Xmx512m should be present");
        assertTrue(xmsIndex > 0, "JVM arg -Xms256m should be present");
        assertTrue(xmxIndex < jarFlagIndex, "-Xmx512m should appear before -jar");
        assertTrue(xmsIndex < jarFlagIndex, "-Xms256m should appear before -jar");
    }

    @Test
    void buildSpringBootCommand_appArgsPlacedAtEnd() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> appArgs = Arrays.asList("--spring.profiles.active=prod", "--spring.datasource.url=jdbc:mysql://host/db");
        List<String> command = launcher.buildSpringBootCommand(
                "/path/to/app.jar", "EXECUTE", null, null,
                Collections.emptyMap(), Collections.emptyList(), appArgs);

        int lastIndex = command.size() - 1;
        assertEquals("--spring.datasource.url=jdbc:mysql://host/db", command.get(lastIndex));
        assertEquals("--spring.profiles.active=prod", command.get(lastIndex - 1));
    }

    @Test
    void buildSpringBootCommand_combinedJvmAndAppArgs() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> jvmArgs = Arrays.asList("-Xmx512m");
        List<String> appArgs = Arrays.asList("--spring.profiles.active=prod");
        List<String> command = launcher.buildSpringBootCommand(
                "/path/to/app.jar", "EXECUTE", null, null,
                Collections.emptyMap(), jvmArgs, appArgs);

        int xmxIndex = command.indexOf("-Xmx512m");
        int jarFlagIndex = command.indexOf("-jar");
        int profileIndex = command.indexOf("--spring.profiles.active=prod");

        assertTrue(xmxIndex < jarFlagIndex, "JVM arg should be before -jar");
        assertEquals(command.size() - 1, profileIndex, "App arg should be at the end");
    }

    @Test
    void buildSpringBootCommand_appArgsAfterOperationArgs() {
        JvmLauncher launcher = new JvmLauncher();
        Map<String, String> operationArgs = new HashMap<>();
        operationArgs.put("flamingock.change-id", "my-change");
        List<String> appArgs = Arrays.asList("--my.custom.prop=value");
        List<String> command = launcher.buildSpringBootCommand(
                "/path/to/app.jar", "EXECUTE", null, null,
                operationArgs, Collections.emptyList(), appArgs);

        int operationArgIndex = command.indexOf("--flamingock.change-id=my-change");
        int appArgIndex = command.indexOf("--my.custom.prop=value");

        assertTrue(operationArgIndex > 0, "Operation arg should be present");
        assertTrue(appArgIndex > operationArgIndex, "App arg should come after operation arg");
        assertEquals(command.size() - 1, appArgIndex, "App arg should be at the end");
    }

    @Test
    void buildSpringBootCommand_emptyPassthroughProducesSameResult() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> withoutPassthrough = launcher.buildSpringBootCommand(
                "/path/to/app.jar", "EXECUTE", null, null);
        List<String> withEmptyPassthrough = launcher.buildSpringBootCommand(
                "/path/to/app.jar", "EXECUTE", null, null,
                Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());

        assertEquals(withoutPassthrough.size(), withEmptyPassthrough.size());
        assertEquals(withoutPassthrough, withEmptyPassthrough);
    }

    @Test
    void buildSpringBootCommand_argsWithSpecialChars() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> appArgs = Arrays.asList("--spring.datasource.url=jdbc:mysql://host:3306/db?useSSL=true&serverTimezone=UTC");
        List<String> command = launcher.buildSpringBootCommand(
                "/path/to/app.jar", null, null, null,
                Collections.emptyMap(), Collections.emptyList(), appArgs);

        assertTrue(command.contains("--spring.datasource.url=jdbc:mysql://host:3306/db?useSSL=true&serverTimezone=UTC"));
    }

    // ================== Passthrough Args Tests (Plain Uber) ==================

    @Test
    void buildPlainUberCommand_jvmArgsPlacedBeforeCp() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> jvmArgs = Arrays.asList("-Xmx1g");
        List<String> command = launcher.buildPlainUberCommand(
                "/path/to/app.jar", "EXECUTE", null, null,
                Collections.emptyMap(), jvmArgs, Collections.emptyList());

        int xmxIndex = command.indexOf("-Xmx1g");
        int cpIndex = command.indexOf("-cp");

        assertTrue(xmxIndex > 0, "JVM arg should be present");
        assertTrue(xmxIndex < cpIndex, "-Xmx1g should appear before -cp");
    }

    @Test
    void buildPlainUberCommand_appArgsPlacedAtEnd() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> appArgs = Arrays.asList("--my.custom.arg=value");
        List<String> command = launcher.buildPlainUberCommand(
                "/path/to/app.jar", "EXECUTE", null, null,
                Collections.emptyMap(), Collections.emptyList(), appArgs);

        assertEquals("--my.custom.arg=value", command.get(command.size() - 1));
    }

    @Test
    void buildPlainUberCommand_emptyPassthroughProducesSameResult() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> withoutPassthrough = launcher.buildPlainUberCommand(
                "/path/to/app.jar", "EXECUTE", null, null);
        List<String> withEmptyPassthrough = launcher.buildPlainUberCommand(
                "/path/to/app.jar", "EXECUTE", null, null,
                Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());

        assertEquals(withoutPassthrough.size(), withEmptyPassthrough.size());
        assertEquals(withoutPassthrough, withEmptyPassthrough);
    }

    @Test
    void buildPlainUberCommand_combinedJvmAndAppArgs() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> jvmArgs = Arrays.asList("-Xmx256m");
        List<String> appArgs = Arrays.asList("--custom.prop=test");
        List<String> command = launcher.buildPlainUberCommand(
                "/path/to/app.jar", null, null, null,
                Collections.emptyMap(), jvmArgs, appArgs);

        int xmxIndex = command.indexOf("-Xmx256m");
        int cpIndex = command.indexOf("-cp");
        int customIndex = command.indexOf("--custom.prop=test");

        assertTrue(xmxIndex < cpIndex, "JVM arg should be before -cp");
        assertEquals(command.size() - 1, customIndex, "App arg should be at the end");
    }

    // ================== buildCommand routing with passthrough ==================

    @Test
    void buildCommand_springBoot_passesJvmAndAppArgs() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> jvmArgs = Arrays.asList("-Xmx512m");
        List<String> appArgs = Arrays.asList("--spring.profiles.active=prod");
        List<String> command = launcher.buildCommand(
                "/path/to/app.jar", "EXECUTE", null, null,
                JarType.SPRING_BOOT, Collections.emptyMap(), jvmArgs, appArgs);

        assertTrue(command.contains("-Xmx512m"));
        assertTrue(command.contains("--spring.profiles.active=prod"));
        assertTrue(command.contains("-jar"));
    }

    @Test
    void buildCommand_plainUber_passesJvmAndAppArgs() {
        JvmLauncher launcher = new JvmLauncher();
        List<String> jvmArgs = Arrays.asList("-Xmx512m");
        List<String> appArgs = Arrays.asList("--custom.arg=value");
        List<String> command = launcher.buildCommand(
                "/path/to/app.jar", "EXECUTE", null, null,
                JarType.PLAIN_UBER, Collections.emptyMap(), jvmArgs, appArgs);

        assertTrue(command.contains("-Xmx512m"));
        assertTrue(command.contains("--custom.arg=value"));
        assertTrue(command.contains("-cp"));
    }

    // ================== General Tests ==================

    @Test
    void getJavaExecutable_shouldReturnNonEmpty() {
        JvmLauncher launcher = new JvmLauncher();
        String javaExecutable = launcher.getJavaExecutable();

        assertTrue(javaExecutable != null && !javaExecutable.isEmpty());
    }
}
