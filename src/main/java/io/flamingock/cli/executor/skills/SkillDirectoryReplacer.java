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
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Replaces one installed skill directory with a fresh copy from the archive.
 */
public class SkillDirectoryReplacer {

    /**
     * Deletes the existing destination skill tree and copies the new one in its place.
     *
     * @param sourceSkillDir source skill directory from the extracted archive
     * @param destinationSkillsDir destination root containing installed skills
     * @throws IOException if replacement fails
     */
    public void replaceSkill(Path sourceSkillDir, Path destinationSkillsDir) throws IOException {
        Path destinationSkillDir = destinationSkillsDir.resolve(sourceSkillDir.getFileName().toString());
        SkillsFileUtils.deleteRecursively(destinationSkillDir);
        Files.createDirectories(destinationSkillDir);

        try (Stream<Path> sourceTree = Files.walk(sourceSkillDir)) {
            for (Path sourcePath : sourceTree.sorted(Comparator.naturalOrder()).toList()) {
                Path relative = sourceSkillDir.relativize(sourcePath);
                Path target = destinationSkillDir.resolve(relative.toString());
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(sourcePath, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
