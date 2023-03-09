package com.example.application;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.LazyFuture;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

public class BuildImageTest {

	private static final Future<String> IMAGE_FUTURE = new LazyFuture<>() {
		@Override
		protected String resolve() {
			// Find project's root dir
			File cwd;
			cwd = new File(".");
			while (!new File(cwd, "mvnw").isFile()) {
				cwd = cwd.getParentFile();
			}

			var properties = new Properties();
			properties.put("skipTests", "true"); // Don't want to create a loop

			var request = new DefaultInvocationRequest().setPomFile(new File(cwd, "pom.xml"))
					.setGoals(List.of("spring-boot:build-image")).setMavenExecutable(new File(cwd, "mvnw"))
					.setProfiles(List.of("production", "build-image-test")).setProperties(properties);

			InvocationResult buildResult;
			try {
				buildResult = new DefaultInvoker().execute(request);
			}
			catch (MavenInvocationException e) {
				throw new RuntimeException(e);
			}

			if (buildResult.getExitCode() != 0) {
				throw new RuntimeException(buildResult.getExecutionException());
			}

			return "docker.io/dashaun/hilla-aot-demo:integration-native";
		}
	};

	@Container
	static final GenericContainer<?> APP = new GenericContainer<>(IMAGE_FUTURE).withExposedPorts(8080);

	@Test
	void containerStartupTest() {
		APP.start();
	}

}