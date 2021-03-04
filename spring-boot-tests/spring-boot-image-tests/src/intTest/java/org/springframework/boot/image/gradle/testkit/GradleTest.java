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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;

/**
 * {@link Extension} that runs tests against multiple with a pre-configured Gradle build.
 * Test classes using the extension must have a non-private and non-final
 * {@link GradleBuild} field named {@code gradleBuild}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(GradleBuildExtension.class)
public @interface GradleTest {

	/**
	 * The version of Spring Boot that test projects should be run with.
	 * @return the version
	 */
	String bootVersion();

}
