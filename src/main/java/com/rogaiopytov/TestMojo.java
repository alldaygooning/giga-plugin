package com.rogaiopytov;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.PROCESS_TEST_CLASSES)
public class TestMojo extends AbstractMojo {

	@Component
	private MavenProject project;

	@Component
	private MavenSession session;

	@Component
	private BuildPluginManager pluginManager;


	private final String logPrefix = "Test Goal: ";

	@Override
	public void execute() throws MojoExecutionException {
		executeMojo(
				plugin(
						groupId("com.RogaIKopytov"),
						artifactId("demo-plugin-eclipse"),
						version("1.0")
				),
				goal("build"),
				configuration(
				),
				executionEnvironment(project, session, pluginManager)
		);

		getLog().info(logPrefix + "Test goal started");

		executeMojo(
				plugin(
						groupId("org.apache.maven.plugins"),
						artifactId("maven-surefire-plugin"),
						version("3.2.2")
				),
				goal("test"),
				configuration(
						element(name("useSystemClassLoader"), "false"),
						element(name("useManifestOnlyJar"), "false")
				),
				executionEnvironment(project, session, pluginManager)
		);

		getLog().info(logPrefix + "Tests finished");
	}

}
