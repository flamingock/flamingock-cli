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
public record InstallationTarget(String identifier, Path destinationSkillsDir) {

    public InstallationTarget {
        identifier = Objects.requireNonNull(identifier, "identifier must not be null");
        destinationSkillsDir = Objects.requireNonNull(destinationSkillsDir, "destinationSkillsDir must not be null");
    }

    /**
     * Creates the current project-local installation target.
     *
     * @param destinationSkillsDir local destination directory
     * @return local installation target
     */
    public static InstallationTarget local(Path destinationSkillsDir) {
        return new InstallationTarget("local", destinationSkillsDir);
    }
}
