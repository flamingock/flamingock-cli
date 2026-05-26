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
package io.flamingock.cli.executor.filesystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryReplacerTest {

    @TempDir
    Path tempDir;

    private final DirectoryReplacer replacer = new DirectoryReplacer();

    @Test
    void replaceDirectory_deletesExistingDestinationBeforeCopyingFreshTree() throws Exception {
        Path destinationDirectory = Files.createDirectories(tempDir.resolve(".agents").resolve("skills").resolve("flamingock-core"));
        Files.writeString(destinationDirectory.resolve("old-file.txt"), "old");
        Path sourceDirectory = Files.createDirectories(tempDir.resolve("source").resolve("flamingock-core"));
        Files.writeString(sourceDirectory.resolve("SKILL.md"), "new");

        replacer.replaceDirectory(sourceDirectory, destinationDirectory);

        assertFalse(Files.exists(destinationDirectory.resolve("old-file.txt")));
        assertEquals("new", Files.readString(destinationDirectory.resolve("SKILL.md")));
    }

    @Test
    void replaceDirectory_preservesSiblingFoldersOutsideDestination() throws Exception {
        Path skillsRoot = Files.createDirectories(tempDir.resolve(".agents").resolve("skills"));
        Files.createDirectories(skillsRoot.resolve("my-custom-skill"));
        Path sourceDirectory = Files.createDirectories(tempDir.resolve("source").resolve("flamingock-java"));
        Files.writeString(sourceDirectory.resolve("SKILL.md"), "java");

        replacer.replaceDirectory(sourceDirectory, skillsRoot.resolve("flamingock-java"));

        assertTrue(Files.isDirectory(skillsRoot.resolve("my-custom-skill")));
        assertTrue(Files.exists(skillsRoot.resolve("flamingock-java").resolve("SKILL.md")));
    }
}
