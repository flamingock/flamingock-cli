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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillDirectoryReplacerTest {

    @TempDir
    Path tempDir;

    private final SkillDirectoryReplacer replacer = new SkillDirectoryReplacer();

    @Test
    void replaceSkill_deletesExistingSkillBeforeCopyingFreshTree() throws Exception {
        Path destinationSkillsDir = Files.createDirectories(tempDir.resolve(".agents").resolve("skills"));
        Path existingSkillDir = Files.createDirectories(destinationSkillsDir.resolve("flamingock-core"));
        Files.writeString(existingSkillDir.resolve("old-file.txt"), "old");
        Path sourceSkillDir = Files.createDirectories(tempDir.resolve("source").resolve("flamingock-core"));
        Files.writeString(sourceSkillDir.resolve("SKILL.md"), "new");

        replacer.replaceSkill(sourceSkillDir, destinationSkillsDir);

        assertFalse(Files.exists(destinationSkillsDir.resolve("flamingock-core").resolve("old-file.txt")));
        assertEquals("new", Files.readString(destinationSkillsDir.resolve("flamingock-core").resolve("SKILL.md")));
    }

    @Test
    void replaceSkill_preservesOtherUserCreatedFolders() throws Exception {
        Path destinationSkillsDir = Files.createDirectories(tempDir.resolve(".agents").resolve("skills"));
        Files.createDirectories(destinationSkillsDir.resolve("my-custom-skill"));
        Path sourceSkillDir = Files.createDirectories(tempDir.resolve("source").resolve("flamingock-java"));
        Files.writeString(sourceSkillDir.resolve("SKILL.md"), "java");

        replacer.replaceSkill(sourceSkillDir, destinationSkillsDir);

        assertTrue(Files.isDirectory(destinationSkillsDir.resolve("my-custom-skill")));
        assertTrue(Files.exists(destinationSkillsDir.resolve("flamingock-java").resolve("SKILL.md")));
    }
}
