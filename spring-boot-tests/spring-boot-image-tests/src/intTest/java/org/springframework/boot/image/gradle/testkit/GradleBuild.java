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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonView;
import com.sun.jna.Platform;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

import org.springframework.asm.ClassVisitor;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

/**
 * A {@code GradleBuild} is used to run a Gradle build using {@link GradleRunner}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public class GradleBuild {

	private final Dsl dsl;

	private File projectDir;

	private String script;

	private String settings;

	private final Map<String, String> scriptProperties = new HashMap<>();

	private String bootVersion;

	public GradleBuild() {
		this(Dsl.GROOVY);
	}

	public GradleBuild(Dsl dsl) {
		this.dsl = dsl;
	}

	void before() throws IOException {
		this.projectDir = Files.createTempDirectory("gradle-").toFile();
	}

	void after() {
		this.script = null;
		FileSystemUtils.deleteRecursively(this.projectDir);
	}

	public void bootVersion(String bootVersion) {
		this.bootVersion = bootVersion;
		this.scriptProperties.put("bootVersion", getBootVersion());
	}

	private List<File> pluginClasspath() {
		return Arrays.asList(new File("bin/main"), new File("build/classes/java/main"),
				new File("build/resources/main"), new File(pathOfJarContaining(ClassVisitor.class)),
				new File(pathOfJarContaining(ArchiveEntry.class)), new File(pathOfJarContaining(JsonView.class)),
				new File(pathOfJarContaining(Platform.class)));
	}

	private String pathOfJarContaining(Class<?> type) {
		return type.getProtectionDomain().getCodeSource().getLocation().getPath();
	}

	public void script(String script) {
		this.script = script.endsWith(this.dsl.getExtension()) ? script : script + this.dsl.getExtension();
	}

	public void settings(String settings) {
		this.settings = settings;
	}

	public BuildResult buildImage(String imageName) {
		return build("bootBuildImage", "--imageName=" + imageName, "--pullPolicy=IF_NOT_PRESENT");
	}

	public BuildResult build(String... arguments) {
		try {
			return prepareRunner(arguments).build();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public BuildResult buildAndFail(String... arguments) {
		try {
			return prepareRunner(arguments).buildAndFail();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public GradleRunner prepareRunner(String... arguments) throws IOException {
		String scriptContent = FileCopyUtils.copyToString(new FileReader(this.script));
		for (Entry<String, String> property : this.scriptProperties.entrySet()) {
			scriptContent = scriptContent.replace("{" + property.getKey() + "}", property.getValue());
		}
		FileCopyUtils.copy(scriptContent, new FileWriter(new File(this.projectDir, "build" + this.dsl.getExtension())));
		FileCopyUtils.copy(new FileReader(this.settings), new FileWriter(new File(this.projectDir, "settings.gradle")));
		GradleRunner gradleRunner = GradleRunner.create().withProjectDir(this.projectDir)
				.withPluginClasspath(pluginClasspath());
		if (this.dsl != Dsl.KOTLIN) {
			// see https://github.com/gradle/gradle/issues/6862
			gradleRunner.withDebug(true);
		}
		List<String> allArguments = new ArrayList<>();
		allArguments.add("-PbootVersion=" + getBootVersion());
		allArguments.add("--stacktrace");
		allArguments.addAll(Arrays.asList(arguments));
		allArguments.add("--warning-mode");
		allArguments.add("all");
		return gradleRunner.withArguments(allArguments);
	}

	public File getProjectDir() {
		return this.projectDir;
	}

	private String getBootVersion() {
		return this.bootVersion;
	}

}
