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
import java.util.List;

/**
 * Summary of a completed skills installation.
 *
 * @param destinationSkillsDir destination directory that received the installed skills
 * @param installedSkills installed official skill folder names
 */
public record SkillsInstallationResult(Path destinationSkillsDir, List<String> installedSkills) {
}
