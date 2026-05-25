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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallDestinationResolverTest {

    @TempDir
    Path tempDir;

    private final InstallDestinationResolver resolver = new InstallDestinationResolver();

    @Test
    void resolveLocal_createsMissingAgentsSkillsTree() throws Exception {
        Path destination = resolver.resolveLocal(tempDir);

        assertEquals(tempDir.resolve(".agents").resolve("skills"), destination);
        assertTrue(Files.isDirectory(tempDir.resolve(".agents")));
        assertTrue(Files.isDirectory(destination));
    }

    @Test
    void resolveLocal_failsWhenAgentsPathIsAFile() throws Exception {
        Path agentsFile = Files.writeString(tempDir.resolve(".agents"), "not a directory");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> resolver.resolveLocal(tempDir));

        assertTrue(exception.getMessage().contains(agentsFile.toString()));
        assertTrue(exception.getMessage().contains("Delete or rename"));
    }

    @Test
    void resolveLocal_failsWhenSkillsPathIsAFile() throws Exception {
        Files.createDirectories(tempDir.resolve(".agents"));
        Path skillsFile = Files.writeString(tempDir.resolve(".agents").resolve("skills"), "not a directory");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> resolver.resolveLocal(tempDir));

        assertTrue(exception.getMessage().contains(skillsFile.toString()));
        assertTrue(exception.getMessage().contains("Delete or rename"));
    }
}
