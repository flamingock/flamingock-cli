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
package io.flamingock.cli.executor.skills;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Resolved destination for a skills installation run.
 *
 * @param identifier stable identifier for the target
 * @param destinationSkillsDir destination directory that will receive the skills
 */
public record SkillsInstallationTarget(String identifier, Path destinationSkillsDir) {

    public SkillsInstallationTarget {
        identifier = Objects.requireNonNull(identifier, "identifier must not be null");
        destinationSkillsDir = Objects.requireNonNull(destinationSkillsDir, "destinationSkillsDir must not be null");
    }

    /**
     * Creates the current project-local installation target.
     *
     * @param destinationSkillsDir local destination directory
     * @return local installation target
     */
    public static SkillsInstallationTarget local(Path destinationSkillsDir) {
        return new SkillsInstallationTarget("local", destinationSkillsDir);
    }

    /**
     * Creates an agent-specific installation target for the <code>agents</code> assistant.
     *
     * @param destinationSkillsDir agent destination directory
     * @return agents installation target
     */
    public static SkillsInstallationTarget agents(Path destinationSkillsDir) {
        return new SkillsInstallationTarget("local:agents", destinationSkillsDir);
    }

    /**
     * Creates an agent-specific installation target for the <code>claude</code> assistant.
     *
     * @param destinationSkillsDir claude destination directory
     * @return claude installation target
     */
    public static SkillsInstallationTarget claude(Path destinationSkillsDir) {
        return new SkillsInstallationTarget("local:claude", destinationSkillsDir);
    }

    /**
     * Creates an agent-specific installation target for the <code>github</code> assistant.
     *
     * @param destinationSkillsDir github destination directory
     * @return github installation target
     */
    public static SkillsInstallationTarget github(Path destinationSkillsDir) {
        return new SkillsInstallationTarget("local:github", destinationSkillsDir);
    }

    /**
     * Creates an agent-specific installation target for the <code>cursor</code> assistant.
     *
     * @param destinationSkillsDir cursor destination directory
     * @return cursor installation target
     */
    public static SkillsInstallationTarget cursor(Path destinationSkillsDir) {
        return new SkillsInstallationTarget("local:cursor", destinationSkillsDir);
    }

    /**
     * Creates an agent-specific installation target for the <code>opencode</code> assistant.
     *
     * @param destinationSkillsDir opencode destination directory
     * @return opencode installation target
     */
    public static SkillsInstallationTarget opencode(Path destinationSkillsDir) {
        return new SkillsInstallationTarget("local:opencode", destinationSkillsDir);
    }

    /**
     * Creates an agent-specific installation target for the <code>gemini</code> assistant.
     *
     * @param destinationSkillsDir gemini destination directory
     * @return gemini installation target
     */
    public static SkillsInstallationTarget gemini(Path destinationSkillsDir) {
        return new SkillsInstallationTarget("local:gemini", destinationSkillsDir);
    }

    /**
     * Creates an agent-specific installation target for the <code>windsurf</code> assistant.
     *
     * @param destinationSkillsDir windsurf destination directory
     * @return windsurf installation target
     */
    public static SkillsInstallationTarget windsurf(Path destinationSkillsDir) {
        return new SkillsInstallationTarget("local:windsurf", destinationSkillsDir);
    }

    /**
     * Creates an agent-specific installation target for the <code>pi agent</code> assistant.
     *
     * @param destinationSkillsDir pi agent destination directory
     * @return pi agent installation target
     */
    public static SkillsInstallationTarget pi(Path destinationSkillsDir) {
        return new SkillsInstallationTarget("local:pi", destinationSkillsDir);
    }
}
