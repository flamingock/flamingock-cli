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

import io.flamingock.cli.executor.util.filesystem.DirectoryResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillsInstallationTargetResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveTargets_localModeResolvesSingleProjectTarget() {
        RecordingDirectoryResolver directoryResolver = new RecordingDirectoryResolver(tempDir.resolve(".agents/skills"));
        SkillsInstallationTargetResolver resolver = new SkillsInstallationTargetResolver(directoryResolver);

        List<SkillsInstallationTarget> targets = resolver.resolveTargets(tempDir, false);

        assertTrue(directoryResolver.called);
        assertEquals(tempDir, directoryResolver.workingDirectory);
        assertArrayEquals(new String[]{".agents", "skills"}, directoryResolver.segments);
        assertEquals(List.of(SkillsInstallationTarget.local(tempDir.resolve(".agents/skills"))), targets);
    }

    @Test
    void resolveTargets_globalModeFailsWithoutCreatingDirectories() {
        RecordingDirectoryResolver directoryResolver = new RecordingDirectoryResolver(tempDir.resolve(".agents/skills"));
        SkillsInstallationTargetResolver resolver = new SkillsInstallationTargetResolver(directoryResolver);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> resolver.resolveTargets(tempDir, true));

        assertFalse(directoryResolver.called);
        assertFalse(Files.exists(tempDir.resolve(".agents")));
        assertTrue(exception.getMessage().contains("not implemented yet"));
    }

    private static final class RecordingDirectoryResolver extends DirectoryResolver {

        private final Path destination;
        private boolean called;
        private Path workingDirectory;
        private String[] segments;

        private RecordingDirectoryResolver(Path destination) {
            this.destination = destination;
        }

        @Override
        public Path resolveDirectory(Path workingDirectory, String... segments) {
            this.called = true;
            this.workingDirectory = workingDirectory;
            this.segments = segments;
            return destination;
        }
    }
}
