/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.image.gradle.testkit;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.regex.Pattern;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

import org.springframework.util.ReflectionUtils;

/**
 * An {@link Extension} for managing the lifecycle of a {@link GradleBuild} stored in a
 * field named {@code gradleBuild}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public class GradleBuildExtension implements BeforeEachCallback, AfterEachCallback {

	private static final Pattern GRADLE_VERSION_PATTERN = Pattern.compile("\\[Gradle .+\\]");

	private final GradleBuild gradleBuild = new GradleBuild();

	private final Dsl dsl = Dsl.GROOVY;

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		URL scriptUrl = findDefaultScript(context);
		if (scriptUrl != null) {
			this.gradleBuild.script(scriptUrl.getFile());
		}
		URL settingsUrl = getSettings(context);
		if (settingsUrl != null) {
			this.gradleBuild.settings(settingsUrl.getFile());
		}
		this.gradleBuild.before();

		String bootVersion = AnnotationUtils.findAnnotation(context.getRequiredTestClass(), GradleTest.class).get()
				.bootVersion();
		this.gradleBuild.bootVersion(bootVersion);

		Field field = ReflectionUtils.findField(context.getRequiredTestClass(), "gradleBuild");
		field.setAccessible(true);
		field.set(context.getRequiredTestInstance(), this.gradleBuild);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		this.gradleBuild.after();
	}

	private URL findDefaultScript(ExtensionContext context) {
		URL scriptUrl = getScriptForTestMethod(context);
		if (scriptUrl != null) {
			return scriptUrl;
		}
		return getScriptForTestClass(context.getRequiredTestClass());
	}

	private URL getScriptForTestMethod(ExtensionContext context) {
		Class<?> testClass = context.getRequiredTestClass();
		String name = testClass.getSimpleName() + "-" + removeGradleVersion(context.getRequiredTestMethod().getName())
				+ this.dsl.getExtension();
		return testClass.getResource(name);
	}

	private URL getSettings(ExtensionContext context) {
		Class<?> testClass = context.getRequiredTestClass();
		return testClass.getResource("settings.gradle");
	}

	private URL getScriptForTestClass(Class<?> testClass) {
		return testClass.getResource(testClass.getSimpleName() + this.dsl.getExtension());
	}

	private String removeGradleVersion(String methodName) {
		return GRADLE_VERSION_PATTERN.matcher(methodName).replaceAll("").trim();
	}

}
