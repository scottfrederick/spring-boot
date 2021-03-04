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

package org.springframework.boot.image.paketo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.dockerjava.api.model.ContainerConfig;
import org.assertj.core.api.Condition;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.type.ImageName;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.image.assertions.ImageAssertions;
import org.springframework.boot.image.gradle.testkit.GradleBuild;
import org.springframework.boot.image.gradle.testkit.GradleTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Paketo builder and buildpacks.
 *
 * See
 * https://paketo.io/docs/buildpacks/language-family-buildpacks/java/#additional-metadata
 *
 * @author Scott Frederick
 */
@GradleTest(bootVersion = PaketoBuilderTests.SPRING_BOOT_VERSION)
class PaketoBuilderTests {

	static final String SPRING_BOOT_VERSION = "2.5.0-SNAPSHOT";

	private static final String PROJECT_VERSION = "1.0.0";

	private static final String PROJECT_DESCRIPTION = "Paketo Test";

	GradleBuild gradleBuild;

	@Test
	void executableJarApp() throws IOException {
		writeMainClass();
		String imageName = "paketo-integration/" + this.gradleBuild.getProjectDir().getName();
		ImageReference imageReference = ImageReference.of(ImageName.of(imageName));
		BuildResult result = this.gradleBuild.buildImage(imageName);
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (GenericContainer<?> container = new GenericContainer<>(imageName).withExposedPorts(8080)) {
			container.waitingFor(Wait.forHttp("/test")).start();
			ContainerConfig config = container.getContainerInfo().getConfig();
			assertSpringBootLabels(config);
			ImageAssertions.assertThat(config).buildMetadata().buildpacks().containsExactly(
					"paketo-buildpacks/ca-certificates", "paketo-buildpacks/bellsoft-liberica",
					"paketo-buildpacks/executable-jar", "paketo-buildpacks/dist-zip", "paketo-buildpacks/spring-boot");
			ImageAssertions.assertThat(config).buildMetadata().bomDependencies().contains("spring-beans", "spring-boot",
					"spring-boot-autoconfigure", "spring-boot-jarmode-layertools", "spring-context", "spring-core",
					"spring-web");
			ImageAssertions.assertThat(config).buildMetadata().bomJavaVersion("jre").startsWith("8.0");
			ImageAssertions.assertThat(config).buildMetadata().processOfType("web").extracting("command", "args")
					.containsExactly("java", Collections.singletonList("org.springframework.boot.loader.JarLauncher"));
			ImageAssertions.assertThat(config).buildMetadata().processOfType("executable-jar")
					.extracting("command", "args")
					.containsExactly("java", Collections.singletonList("org.springframework.boot.loader.JarLauncher"));
			assertImageLayersMatchLayersIndex(imageReference, config);
		}
		finally {
			removeImage(imageReference);
		}
	}

	@Test
	void executableWarApp() throws IOException {
		writeMainClass();
		writeServletInitializerClass();
		String imageName = "paketo-integration/" + this.gradleBuild.getProjectDir().getName();
		ImageReference imageReference = ImageReference.of(ImageName.of(imageName));
		BuildResult result = this.gradleBuild.buildImage(imageName);
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (GenericContainer<?> container = new GenericContainer<>(imageName).withExposedPorts(8080)) {
			container.waitingFor(Wait.forHttp("/test")).start();
			ContainerConfig config = container.getContainerInfo().getConfig();
			assertSpringBootLabels(config);
			ImageAssertions.assertThat(config).buildMetadata().buildpacks().containsExactly(
					"paketo-buildpacks/ca-certificates", "paketo-buildpacks/bellsoft-liberica",
					"paketo-buildpacks/executable-jar", "paketo-buildpacks/dist-zip", "paketo-buildpacks/spring-boot");
			ImageAssertions.assertThat(config).buildMetadata().bomDependencies().contains("spring-beans", "spring-boot",
					"spring-boot-autoconfigure", "spring-boot-jarmode-layertools", "spring-context", "spring-core",
					"spring-web");
			ImageAssertions.assertThat(config).buildMetadata().bomJavaVersion("jre").startsWith("8.0");
			ImageAssertions.assertThat(config).buildMetadata().processOfType("web").extracting("command", "args")
					.containsExactly("java", Collections.singletonList("org.springframework.boot.loader.WarLauncher"));
			ImageAssertions.assertThat(config).buildMetadata().processOfType("executable-jar")
					.extracting("command", "args")
					.containsExactly("java", Collections.singletonList("org.springframework.boot.loader.WarLauncher"));
			assertImageLayersMatchLayersIndex(imageReference, config);
		}
		finally {
			removeImage(imageReference);
		}
	}

	@Test
	void traditionalWarApp() throws IOException {
		writeMainClass();
		writeServletInitializerClass();
		String imageName = "paketo-integration/" + this.gradleBuild.getProjectDir().getName();
		ImageReference imageReference = ImageReference.of(ImageName.of(imageName));
		BuildResult result = this.gradleBuild.buildImage(imageName);
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (GenericContainer<?> container = new GenericContainer<>(imageName).withExposedPorts(8080)) {
			container.waitingFor(Wait.forHttp("/test")).start();
			ContainerConfig config = container.getContainerInfo().getConfig();
			ImageAssertions.assertThat(config).buildMetadata().buildpacks().containsExactly(
					"paketo-buildpacks/ca-certificates", "paketo-buildpacks/bellsoft-liberica",
					"paketo-buildpacks/apache-tomcat", "paketo-buildpacks/dist-zip", "paketo-buildpacks/spring-boot");
			ImageAssertions.assertThat(config).buildMetadata().bomJavaVersion("jre").startsWith("8.0");
			ImageAssertions.assertThat(config).buildMetadata().processOfType("web").extracting("command", "args")
					.containsExactly("catalina.sh", Collections.singletonList("run"));
			ImageAssertions.assertThat(config).buildMetadata().processOfType("tomcat").extracting("command", "args")
					.containsExactly("catalina.sh", Collections.singletonList("run"));
			DigestCapturingCondition digests = new DigestCapturingCondition();
			ImageAssertions.assertThat(config).lifecycleMetadata().appLayerShas().haveExactly(1, digests);
			ImageAssertions.assertThat(imageReference).hasLayer(digests.getDigest(0)).entries()
					.contains("WEB-INF/classes/example/ExampleApplication.class",
							"WEB-INF/classes/example/HelloController.class", "META-INF/MANIFEST.MF")
					.anyMatch((s) -> s.startsWith("WEB-INF/lib/spring-boot-"))
					.anyMatch((s) -> s.startsWith("WEB-INF/lib/spring-core-"))
					.anyMatch((s) -> s.startsWith("WEB-INF/lib/spring-web-"));
		}
		finally {
			removeImage(imageReference);
		}
	}

	@Test
	void nativeApp() throws IOException {
		writeMainClass();
		String imageName = "paketo-integration/" + this.gradleBuild.getProjectDir().getName();
		ImageReference imageReference = ImageReference.of(ImageName.of(imageName));
		BuildResult result = this.gradleBuild.buildImage(imageName);
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (GenericContainer<?> container = new GenericContainer<>(imageName).withExposedPorts(8080)) {
			container.waitingFor(Wait.forHttp("/test")).start();
			ContainerConfig config = container.getContainerInfo().getConfig();
			assertSpringBootLabels(config);
			ImageAssertions.assertThat(config).buildMetadata().buildpacks().containsExactly("paketo-buildpacks/graalvm",
					"paketo-buildpacks/executable-jar", "paketo-buildpacks/spring-boot",
					"paketo-buildpacks/spring-boot-native-image");
			ImageAssertions.assertThat(config).buildMetadata().bomDependencies().contains("spring-beans", "spring-boot",
					"spring-boot-autoconfigure", "spring-boot-jarmode-layertools", "spring-context", "spring-core",
					"spring-graalvm-native", "spring-web");
			ImageAssertions.assertThat(config).buildMetadata().processOfType("web").extracting("command", "args")
					.containsExactly("/workspace/example.ExampleApplication", Collections.emptyList());
			ImageAssertions.assertThat(config).buildMetadata().processOfType("native-image")
					.extracting("command", "args")
					.containsExactly("/workspace/example.ExampleApplication", Collections.emptyList());
			DigestCapturingCondition digests = new DigestCapturingCondition();
			ImageAssertions.assertThat(config).lifecycleMetadata().appLayerShas().haveExactly(1, digests);
			ImageAssertions.assertThat(imageReference).hasLayer(digests.getDigest(0)).entries()
					.containsExactly("example.ExampleApplication");
		}
		finally {
			removeImage(imageReference);
		}
	}

	private void writeMainClass() throws IOException {
		writeProjectFile("src/main/java/example", "ExampleApplication.java", (writer) -> {
			writer.println("package example;");
			writer.println();
			writer.println("import org.springframework.boot.SpringApplication;");
			writer.println("import org.springframework.boot.autoconfigure.SpringBootApplication;");
			writer.println("import org.springframework.stereotype.Controller;");
			writer.println("import org.springframework.web.bind.annotation.RequestMapping;");
			writer.println("import org.springframework.web.bind.annotation.ResponseBody;");
			writer.println();
			writer.println("@SpringBootApplication");
			writer.println("public class ExampleApplication {");
			writer.println();
			writer.println("    public static void main(String[] args) {");
			writer.println("        SpringApplication.run(ExampleApplication.class, args);");
			writer.println("    }");
			writer.println();
			writer.println("}");
			writer.println();
			writer.println("@Controller");
			writer.println("class HelloController {");
			writer.println();
			writer.println("    @RequestMapping(\"/test\")");
			writer.println("    @ResponseBody");
			writer.println("    String home() {");
			writer.println("        return \"Hello, world!\";");
			writer.println("    }");
			writer.println();
			writer.println("}");
		});
	}

	private void writeServletInitializerClass() throws IOException {
		writeProjectFile("src/main/java/example", "ServletInitializer.java", (writer) -> {
			writer.println("package example;");
			writer.println();
			writer.println("import org.springframework.boot.builder.SpringApplicationBuilder;");
			writer.println("import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;");
			writer.println();
			writer.println("public class ServletInitializer extends SpringBootServletInitializer {");
			writer.println();
			writer.println("    @Override");
			writer.println("    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {");
			writer.println("        return application.sources(ExampleApplication.class);");
			writer.println("    }");
			writer.println();
			writer.println("}");
		});
	}

	private void writeProjectFile(String directory, String fileName, Consumer<PrintWriter> consumer)
			throws IOException {
		File examplePackage = new File(this.gradleBuild.getProjectDir(), directory);
		examplePackage.mkdirs();
		File main = new File(examplePackage, fileName);
		try (PrintWriter writer = new PrintWriter(new FileWriter(main))) {
			consumer.accept(writer);
		}
	}

	private void assertSpringBootLabels(ContainerConfig config) {
		ImageAssertions.assertThat(config).label("org.springframework.boot.version").isEqualTo(SPRING_BOOT_VERSION);
		ImageAssertions.assertThat(config).label("org.opencontainers.image.title").isEqualTo(PROJECT_DESCRIPTION);
		ImageAssertions.assertThat(config).label("org.opencontainers.image.version").isEqualTo(PROJECT_VERSION);
	}

	private void assertImageLayersMatchLayersIndex(ImageReference imageReference, ContainerConfig config)
			throws IOException {
		DigestCapturingCondition digests = new DigestCapturingCondition();
		ImageAssertions.assertThat(config).lifecycleMetadata().appLayerShas().haveExactly(5, digests);
		LayersIndex layersIndex = LayersIndex
				.fromArchiveFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0]);
		ImageAssertions.assertThat(imageReference).hasLayer(digests.getDigest(0)).entries()
				.containsExactlyElementsOf(layersIndex.getLayer("dependencies"));
		ImageAssertions.assertThat(imageReference).hasLayer(digests.getDigest(1)).entries()
				.allMatch((entry) -> startsWithOneOf(entry, layersIndex.getLayer("spring-boot-loader")));
		ImageAssertions.assertThat(imageReference).hasLayer(digests.getDigest(2)).entries()
				.containsExactlyElementsOf(layersIndex.getLayer("snapshot-dependencies"));
		ImageAssertions.assertThat(imageReference).hasLayer(digests.getDigest(3)).entries()
				.allMatch((entry) -> startsWithOneOf(entry, layersIndex.getLayer("application")));
		ImageAssertions.assertThat(imageReference).hasLayer(digests.getDigest(4)).entries()
				.allMatch((entry) -> entry.contains("lib/spring-cloud-bindings-"));
	}

	private boolean startsWithOneOf(String actual, List<String> expectedPrefix) {
		for (String prefix : expectedPrefix) {
			if (actual.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private void removeImage(ImageReference image) throws IOException {
		new DockerApi().image().remove(image, false);
	}

	private static class DigestCapturingCondition extends Condition<Object> {

		private static List<String> digests;

		DigestCapturingCondition() {
			super(predicate(), "a value starting with 'sha256:'");
		}

		private static Predicate<Object> predicate() {
			digests = new ArrayList<>();
			return (sha) -> {
				digests.add(sha.toString());
				return sha.toString().startsWith("sha256:");
			};
		}

		String getDigest(int index) {
			return digests.get(index);
		}

	}

}
