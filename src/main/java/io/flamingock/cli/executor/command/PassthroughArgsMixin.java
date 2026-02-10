/*
 * Copyright 2026 Flamingock (https://www.flamingock.io)
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
package io.flamingock.cli.executor.command;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Picocli mixin that captures passthrough arguments for the spawned JVM process.
 *
 * <p>Supports two categories of passthrough arguments:</p>
 * <ul>
 *   <li><b>JVM arguments</b> ({@code -J} / {@code --java-opt}): placed before {@code -jar}/{@code -cp}
 *       in the spawned command. Use for memory settings, system properties, GC options, etc.</li>
 *   <li><b>Application arguments</b> (after {@code --}): appended at the end of the spawned command,
 *       after all Flamingock flags. Use for Spring profiles, datasource overrides, custom properties, etc.</li>
 * </ul>
 *
 * <p>Reserved arguments (those controlled by the CLI) are validated and rejected with actionable
 * error messages to prevent users from accidentally overriding safety-critical flags.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 *   flamingock execute apply --jar app.jar -J -Xmx512m -- --spring.profiles.active=prod
 * </pre>
 */
public class PassthroughArgsMixin {

    /**
     * Reserved application argument prefixes that cannot be overridden by the user.
     * These are controlled by the CLI for correct operation.
     */
    static final String[] RESERVED_APP_ARG_PREFIXES = {
            "--flamingock.",
            "--spring.main.web-application-type",
            "--spring.main.banner-mode"
    };

    @Option(names = {"-J", "--java-opt"},
            paramLabel = "<jvm-arg>",
            description = "JVM argument passed to the spawned process (repeatable, placed before -jar/-cp). "
                    + "Example: -J -Xmx512m -J -Xms256m -J \"-Dmy.prop=value\"")
    private List<String> jvmArgs;

    @Parameters(paramLabel = "APP_ARGS",
            description = "Application arguments passed after '--' to the spawned process. "
                    + "Example: -- --spring.profiles.active=prod --spring.datasource.url=jdbc:mysql://host/db")
    private List<String> appArgs;

    /**
     * Returns the JVM arguments to pass before {@code -jar}/{@code -cp} in the spawned command.
     *
     * @return unmodifiable list of JVM arguments (never null)
     */
    public List<String> getJvmArgs() {
        return jvmArgs != null ? Collections.unmodifiableList(jvmArgs) : Collections.emptyList();
    }

    /**
     * Returns the application arguments to append at the end of the spawned command.
     *
     * @return unmodifiable list of application arguments (never null)
     */
    public List<String> getAppArgs() {
        return appArgs != null ? Collections.unmodifiableList(appArgs) : Collections.emptyList();
    }

    /**
     * Validates that application arguments do not contain reserved prefixes.
     *
     * <p>Reserved prefixes include {@code --flamingock.*} (entire Flamingock namespace),
     * {@code --spring.main.web-application-type} (safety flag), and
     * {@code --spring.main.banner-mode} (CLI-controlled).</p>
     *
     * <p>JVM arguments are not validated — they operate at the JVM level and do not
     * interfere with application-level Flamingock flags.</p>
     *
     * @throws IllegalArgumentException if a reserved argument is found, with an actionable error message
     */
    public void validate() {
        if (appArgs == null || appArgs.isEmpty()) {
            return;
        }
        for (String arg : appArgs) {
            for (String reserved : RESERVED_APP_ARG_PREFIXES) {
                if (arg.toLowerCase().startsWith(reserved.toLowerCase())) {
                    String category = reserved.startsWith("--flamingock.")
                            ? "Arguments starting with '--flamingock.' are controlled by the CLI and cannot be overridden."
                            : "The argument '" + reserved + "' is a safety-critical flag controlled by the CLI.";
                    throw new IllegalArgumentException(
                            "Reserved argument cannot be passed after '--': " + arg + "\n\n"
                                    + "  " + category + "\n\n"
                                    + "  For help: flamingock <command> --help"
                    );
                }
            }
        }
    }
}
