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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves and validates the local skills installation destination.
 */
public class InstallDestinationResolver {

    /**
     * Resolves the local destination under {@code ./.agents/skills}.
     *
     * @param workingDirectory current working directory
     * @return validated destination directory
     */
    public Path resolveLocal(Path workingDirectory) {
        Path agentsDir = workingDirectory.resolve(".agents");
        validateDirectoryPath(agentsDir, "'.agents' must be a directory before installing skills.");

        Path skillsDir = agentsDir.resolve("skills");
        validateDirectoryPath(skillsDir, "'.agents/skills' must be a directory before installing skills.");

        try {
            Files.createDirectories(skillsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create skills directory '" + skillsDir
                    + "'. Check filesystem permissions and try again.", e);
        }

        return skillsDir;
    }

    private void validateDirectoryPath(Path path, String guidance) {
        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalStateException("Invalid skills destination path: '" + path + "' is not a directory. "
                    + guidance + " Delete or rename that path and run the command again.");
        }
    }
}
