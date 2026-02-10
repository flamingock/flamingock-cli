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
package io.flamingock.cli.executor.command;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for PassthroughArgsMixin validation logic.
 */
class PassthroughArgsMixinTest {

    // ================== Reserved Arg Rejection Tests ==================

    @Test
    void validate_rejectsFlamingockOperation() {
        PassthroughArgsMixin mixin = createMixinWithAppArgs(
                Arrays.asList("--flamingock.operation=EXECUTE"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, mixin::validate);
        assertTrue(ex.getMessage().contains("Reserved argument cannot be passed after '--'"));
        assertTrue(ex.getMessage().contains("--flamingock.operation=EXECUTE"));
        assertTrue(ex.getMessage().contains("--flamingock."));
    }

    @Test
    void validate_rejectsFlamingockCliMode() {
        PassthroughArgsMixin mixin = createMixinWithAppArgs(
                Arrays.asList("--flamingock.cli.mode=false"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, mixin::validate);
        assertTrue(ex.getMessage().contains("--flamingock.cli.mode=false"));
    }

    @Test
    void validate_rejectsFlamingockOutputFile() {
        PassthroughArgsMixin mixin = createMixinWithAppArgs(
                Arrays.asList("--flamingock.output-file=/tmp/hack.json"));

        assertThrows(IllegalArgumentException.class, mixin::validate);
    }

    @Test
    void validate_rejectsSpringWebApplicationType() {
        PassthroughArgsMixin mixin = createMixinWithAppArgs(
                Arrays.asList("--spring.main.web-application-type=servlet"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, mixin::validate);
        assertTrue(ex.getMessage().contains("safety-critical flag"));
    }

    @Test
    void validate_rejectsSpringBannerMode() {
        PassthroughArgsMixin mixin = createMixinWithAppArgs(
                Arrays.asList("--spring.main.banner-mode=console"));

        assertThrows(IllegalArgumentException.class, mixin::validate);
    }

    // ================== Allowed Args Tests ==================

    @Test
    void validate_allowsSpringProfilesActive() {
        PassthroughArgsMixin mixin = createMixinWithAppArgs(
                Arrays.asList("--spring.profiles.active=prod"));

        assertDoesNotThrow(mixin::validate);
    }

    @Test
    void validate_allowsSpringDatasourceUrl() {
        PassthroughArgsMixin mixin = createMixinWithAppArgs(
                Arrays.asList("--spring.datasource.url=jdbc:mysql://prod/db"));

        assertDoesNotThrow(mixin::validate);
    }

    @Test
    void validate_allowsCustomArgs() {
        PassthroughArgsMixin mixin = createMixinWithAppArgs(
                Arrays.asList("--my.custom.property=value", "--another.prop=123"));

        assertDoesNotThrow(mixin::validate);
    }

    @Test
    void validate_allowsSpringProfilesInclude() {
        PassthroughArgsMixin mixin = createMixinWithAppArgs(
                Arrays.asList("--spring.profiles.include=extra-profile"));

        assertDoesNotThrow(mixin::validate);
    }

    // ================== Empty/Null Tests ==================

    @Test
    void validate_passesWithEmptyAppArgs() {
        PassthroughArgsMixin mixin = createMixinWithAppArgs(Collections.emptyList());
        assertDoesNotThrow(mixin::validate);
    }

    @Test
    void validate_passesWithNullAppArgs() {
        PassthroughArgsMixin mixin = new PassthroughArgsMixin();
        assertDoesNotThrow(mixin::validate);
    }

    // ================== Getter Tests ==================

    @Test
    void getJvmArgs_returnsEmptyListWhenNull() {
        PassthroughArgsMixin mixin = new PassthroughArgsMixin();
        List<String> result = mixin.getJvmArgs();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAppArgs_returnsEmptyListWhenNull() {
        PassthroughArgsMixin mixin = new PassthroughArgsMixin();
        List<String> result = mixin.getAppArgs();

        assertTrue(result.isEmpty());
    }

    @Test
    void getJvmArgs_returnsUnmodifiableList() {
        PassthroughArgsMixin mixin = createMixinWithJvmArgs(Arrays.asList("-Xmx512m"));
        List<String> result = mixin.getJvmArgs();

        assertEquals(1, result.size());
        assertEquals("-Xmx512m", result.get(0));
        assertThrows(UnsupportedOperationException.class, () -> result.add("-Xms256m"));
    }

    @Test
    void getAppArgs_returnsUnmodifiableList() {
        PassthroughArgsMixin mixin = createMixinWithAppArgs(Arrays.asList("--spring.profiles.active=prod"));
        List<String> result = mixin.getAppArgs();

        assertEquals(1, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.add("--extra"));
    }

    // ================== JVM args not validated ==================

    @Test
    void validate_doesNotCheckJvmArgs() {
        // JVM args should not be validated against reserved prefixes
        PassthroughArgsMixin mixin = createMixinWithBoth(
                Arrays.asList("-Dflamingock.operation=EXECUTE"),
                Collections.emptyList());

        assertDoesNotThrow(mixin::validate);
    }

    // ================== Helpers ==================

    private PassthroughArgsMixin createMixinWithAppArgs(List<String> appArgs) {
        PassthroughArgsMixin mixin = new PassthroughArgsMixin();
        setField(mixin, "appArgs", appArgs);
        return mixin;
    }

    private PassthroughArgsMixin createMixinWithJvmArgs(List<String> jvmArgs) {
        PassthroughArgsMixin mixin = new PassthroughArgsMixin();
        setField(mixin, "jvmArgs", jvmArgs);
        return mixin;
    }

    private PassthroughArgsMixin createMixinWithBoth(List<String> jvmArgs, List<String> appArgs) {
        PassthroughArgsMixin mixin = new PassthroughArgsMixin();
        setField(mixin, "jvmArgs", jvmArgs);
        setField(mixin, "appArgs", appArgs);
        return mixin;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}
