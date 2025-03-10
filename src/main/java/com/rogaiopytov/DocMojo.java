package com.rogaiopytov;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "doc", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DocMojo extends AbstractMojo {

	@Component
	private MavenProject project;

	@Component
	private MavenSession session;

	@Component
	private BuildPluginManager pluginManager;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		executeMojo(
				plugin(
						groupId("org.apache.maven.plugins"), 
						artifactId("maven-javadoc-plugin"), 
						version("3.11.2")), 
				goal("javadoc"),
				configuration(), 
				executionEnvironment(project, session, pluginManager));

		executeMojo(
				plugin(
						groupId("com.RogaIKopytov"),
						artifactId("demo-plugin-eclipse"),
						version("1.0")
						),
				goal("build"),
				configuration(),
				executionEnvironment(project, session, pluginManager)
		);
	}
}
