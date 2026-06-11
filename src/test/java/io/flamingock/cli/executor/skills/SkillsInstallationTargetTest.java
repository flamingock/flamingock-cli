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

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SkillsInstallationTargetTest {

    @Test
    void agentsFactoryRejectsNullPath() {
        assertThrows(NullPointerException.class,
                () -> SkillsInstallationTarget.agents(null),
                "destinationSkillsDir must not be null");
    }

    @Test
    void claudeFactoryRejectsNullPath() {
        assertThrows(NullPointerException.class,
                () -> SkillsInstallationTarget.claude(null),
                "destinationSkillsDir must not be null");
    }

    @Test
    void agentsFactoryCreatesTargetWithAgentsIdentifier() {
        Path dest = Path.of("/some/path/.agents/skills");
        SkillsInstallationTarget target = SkillsInstallationTarget.agents(dest);

        assertEquals("local:agents", target.identifier());
        assertEquals(dest, target.destinationSkillsDir());
    }

    @Test
    void claudeFactoryCreatesTargetWithClaudeIdentifier() {
        Path dest = Path.of("/some/path/.claude/skills");
        SkillsInstallationTarget target = SkillsInstallationTarget.claude(dest);

        assertEquals("local:claude", target.identifier());
        assertEquals(dest, target.destinationSkillsDir());
    }

    @Test
    void githubFactoryCreatesTargetWithGithubIdentifier() {
        Path dest = Path.of("/some/path/.github/skills");
        SkillsInstallationTarget target = SkillsInstallationTarget.github(dest);

        assertEquals("local:github", target.identifier());
        assertEquals(dest, target.destinationSkillsDir());
    }

    @Test
    void githubFactoryRejectsNullPath() {
        assertThrows(NullPointerException.class,
                () -> SkillsInstallationTarget.github(null),
                "destinationSkillsDir must not be null");
    }

    @Test
    void cursorFactoryCreatesTargetWithCursorIdentifier() {
        Path dest = Path.of("/some/path/.cursor/skills");
        SkillsInstallationTarget target = SkillsInstallationTarget.cursor(dest);

        assertEquals("local:cursor", target.identifier());
        assertEquals(dest, target.destinationSkillsDir());
    }

    @Test
    void cursorFactoryRejectsNullPath() {
        assertThrows(NullPointerException.class,
                () -> SkillsInstallationTarget.cursor(null),
                "destinationSkillsDir must not be null");
    }

    @Test
    void opencodeFactoryCreatesTargetWithOpencodeIdentifier() {
        Path dest = Path.of("/some/path/.opencode/skills");
        SkillsInstallationTarget target = SkillsInstallationTarget.opencode(dest);

        assertEquals("local:opencode", target.identifier());
        assertEquals(dest, target.destinationSkillsDir());
    }

    @Test
    void opencodeFactoryRejectsNullPath() {
        assertThrows(NullPointerException.class,
                () -> SkillsInstallationTarget.opencode(null),
                "destinationSkillsDir must not be null");
    }

    @Test
    void geminiFactoryCreatesTargetWithGeminiIdentifier() {
        Path dest = Path.of("/some/path/.gemini/skills");
        SkillsInstallationTarget target = SkillsInstallationTarget.gemini(dest);

        assertEquals("local:gemini", target.identifier());
        assertEquals(dest, target.destinationSkillsDir());
    }

    @Test
    void geminiFactoryRejectsNullPath() {
        assertThrows(NullPointerException.class,
                () -> SkillsInstallationTarget.gemini(null),
                "destinationSkillsDir must not be null");
    }

    @Test
    void windsurfFactoryCreatesTargetWithWindsurfIdentifier() {
        Path dest = Path.of("/some/path/.windsurf/skills");
        SkillsInstallationTarget target = SkillsInstallationTarget.windsurf(dest);

        assertEquals("local:windsurf", target.identifier());
        assertEquals(dest, target.destinationSkillsDir());
    }

    @Test
    void windsurfFactoryRejectsNullPath() {
        assertThrows(NullPointerException.class,
            () -> SkillsInstallationTarget.windsurf(null),
            "destinationSkillsDir must not be null");
    }

    @Test
    void piFactoryCreatesTargetWithPiIdentifier() {
        Path dest = Path.of("/some/path/.pi/skills");
        SkillsInstallationTarget target = SkillsInstallationTarget.pi(dest);

        assertEquals("local:pi", target.identifier());
        assertEquals(dest, target.destinationSkillsDir());
    }

    @Test
    void piFactoryRejectsNullPath() {
        assertThrows(NullPointerException.class,
            () -> SkillsInstallationTarget.pi(null),
            "destinationSkillsDir must not be null");
    }
}
