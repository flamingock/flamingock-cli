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
        assertEquals(List.of(SkillsInstallationTarget.agents(tempDir.resolve(".agents/skills"))), targets);
    }

    @Test
    void resolveTargets_nullAgentResolvesToAgentsPath() {
        RecordingDirectoryResolver directoryResolver = new RecordingDirectoryResolver(tempDir.resolve(".agents/skills"));
        SkillsInstallationTargetResolver resolver = new SkillsInstallationTargetResolver(directoryResolver);

        List<SkillsInstallationTarget> targets = resolver.resolveTargets(tempDir, false, (String) null);

        assertEquals(1, targets.size());
        assertEquals("local:agents", targets.get(0).identifier());
        assertArrayEquals(new String[]{".agents", "skills"}, directoryResolver.segments);
        assertTrue(directoryResolver.called);
    }

    @Test
    void resolveTargets_agentsAgentThrowsWithSupportedValues() {
        RecordingDirectoryResolver directoryResolver = new RecordingDirectoryResolver(tempDir.resolve(".agents/skills"));
        SkillsInstallationTargetResolver resolver = new SkillsInstallationTargetResolver(directoryResolver);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> resolver.resolveTargets(tempDir, false, "agents"));

        assertFalse(directoryResolver.called);
        String message = exception.getMessage();
        assertTrue(message.contains("agents"));
        assertTrue(message.contains("claude"));
        assertTrue(message.contains("github"));
        assertTrue(message.contains("cursor"));
        assertTrue(message.contains("opencode"));
        assertTrue(message.contains("gemini"));
        assertTrue(message.contains("windsurf"));
        assertTrue(message.contains("pi"));
    }

    @Test
    void resolveTargets_claudeAgentResolvesToClaudePath() {
        RecordingDirectoryResolver directoryResolver = new RecordingDirectoryResolver(tempDir.resolve(".claude/skills"));
        SkillsInstallationTargetResolver resolver = new SkillsInstallationTargetResolver(directoryResolver);

        List<SkillsInstallationTarget> targets = resolver.resolveTargets(tempDir, false, "claude");

        assertEquals(1, targets.size());
        assertEquals("local:claude", targets.get(0).identifier());
        assertArrayEquals(new String[]{".claude", "skills"}, directoryResolver.segments);
        assertTrue(directoryResolver.called);
    }

    @Test
    void resolveTargets_geminiAgentResolvesToGeminiPath() {
        RecordingDirectoryResolver directoryResolver = new RecordingDirectoryResolver(tempDir.resolve(".gemini/skills"));
        SkillsInstallationTargetResolver resolver = new SkillsInstallationTargetResolver(directoryResolver);

        List<SkillsInstallationTarget> targets = resolver.resolveTargets(tempDir, false, "gemini");

        assertEquals(1, targets.size());
        assertEquals("local:gemini", targets.get(0).identifier());
        assertEquals(tempDir.resolve(".gemini/skills"), targets.get(0).destinationSkillsDir());
        assertArrayEquals(new String[]{".gemini", "skills"}, directoryResolver.segments);
        assertTrue(directoryResolver.called);
    }

    @Test
    void resolveTargets_githubAgentResolvesToGithubPath() {
        RecordingDirectoryResolver directoryResolver = new RecordingDirectoryResolver(tempDir.resolve(".github/skills"));
        SkillsInstallationTargetResolver resolver = new SkillsInstallationTargetResolver(directoryResolver);

        List<SkillsInstallationTarget> targets = resolver.resolveTargets(tempDir, false, "github");

        assertEquals(1, targets.size());
        assertEquals("local:github", targets.get(0).identifier());
        assertArrayEquals(new String[]{".github", "skills"}, directoryResolver.segments);
        assertTrue(directoryResolver.called);
    }

    @Test
    void resolveTargets_cursorAgentResolvesToCursorPath() {
        RecordingDirectoryResolver directoryResolver = new RecordingDirectoryResolver(tempDir.resolve(".cursor/skills"));
        SkillsInstallationTargetResolver resolver = new SkillsInstallationTargetResolver(directoryResolver);

        List<SkillsInstallationTarget> targets = resolver.resolveTargets(tempDir, false, "cursor");

        assertEquals(1, targets.size());
        assertEquals("local:cursor", targets.get(0).identifier());
        assertArrayEquals(new String[]{".cursor", "skills"}, directoryResolver.segments);
        assertTrue(directoryResolver.called);
    }

    @Test
    void resolveTargets_opencodeAgentResolvesToOpencodePath() {
        RecordingDirectoryResolver directoryResolver = new RecordingDirectoryResolver(tempDir.resolve(".opencode/skills"));
        SkillsInstallationTargetResolver resolver = new SkillsInstallationTargetResolver(directoryResolver);

        List<SkillsInstallationTarget> targets = resolver.resolveTargets(tempDir, false, "opencode");

        assertEquals(1, targets.size());
        assertEquals("local:opencode", targets.get(0).identifier());
        assertArrayEquals(new String[]{".opencode", "skills"}, directoryResolver.segments);
        assertTrue(directoryResolver.called);
    }

    @Test
    void resolveTargets_windsurfAgentResolvesToWindsurfPath() {
        RecordingDirectoryResolver directoryResolver = new RecordingDirectoryResolver(tempDir.resolve(".windsurf/skills"));
        SkillsInstallationTargetResolver resolver = new SkillsInstallationTargetResolver(directoryResolver);

        List<SkillsInstallationTarget> targets = resolver.resolveTargets(tempDir, false, "windsurf");

        assertEquals(1, targets.size());
        assertEquals("local:windsurf", targets.get(0).identifier());
        assertArrayEquals(new String[]{".windsurf", "skills"}, directoryResolver.segments);
        assertTrue(directoryResolver.called);
    }

    @Test
    void resolveTargets_piAgentResolvesToPiPath() {
        RecordingDirectoryResolver directoryResolver = new RecordingDirectoryResolver(tempDir.resolve(".pi/skills"));
        SkillsInstallationTargetResolver resolver = new SkillsInstallationTargetResolver(directoryResolver);

        List<SkillsInstallationTarget> targets = resolver.resolveTargets(tempDir, false, "pi");

        assertEquals(1, targets.size());
        assertEquals("local:pi", targets.get(0).identifier());
        assertArrayEquals(new String[]{".pi", "skills"}, directoryResolver.segments);
        assertTrue(directoryResolver.called);
    }

    @Test
    void resolveTargets_invalidAgentThrowsWithSupportedValues() {
        RecordingDirectoryResolver directoryResolver = new RecordingDirectoryResolver(tempDir.resolve(".agents/skills"));
        SkillsInstallationTargetResolver resolver = new SkillsInstallationTargetResolver(directoryResolver);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> resolver.resolveTargets(tempDir, false, "foo"));

        assertFalse(directoryResolver.called);
        String message = exception.getMessage();
        assertTrue(message.contains("foo"));
        assertTrue(message.contains("claude"));
        assertTrue(message.contains("github"));
        assertTrue(message.contains("cursor"));
        assertTrue(message.contains("opencode"));
        assertTrue(message.contains("gemini"));
        assertTrue(message.contains("windsurf"));
        assertTrue(message.contains("pi"));
    }

    @Test
    void resolveTargets_globalModeStillThrowsBeforeAgentEval() {
        RecordingDirectoryResolver directoryResolver = new RecordingDirectoryResolver(tempDir.resolve(".claude/skills"));
        SkillsInstallationTargetResolver resolver = new SkillsInstallationTargetResolver(directoryResolver);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> resolver.resolveTargets(tempDir, true, "claude"));

        assertFalse(directoryResolver.called);
        assertTrue(exception.getMessage().contains("not implemented yet"));
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
