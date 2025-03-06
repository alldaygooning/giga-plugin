package com.rogaiopytov;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

@Mojo(name = "resolve")
@Execute(phase = LifecyclePhase.VALIDATE)
public class ResolveMojo extends AbstractMojo {

	@Component
	private MavenProject project;

	@Component
	private MavenSession session;

	@Component
	private BuildPluginManager pluginManager;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// Designate output file location
		String outputFilePath = project.getBuild().getDirectory() + File.separator + "classpath.txt";
		getLog().info("Building classpath and writing to: " + outputFilePath);

		// Execute the maven-dependency-plugin:build-classpath goal
		executeMojo(plugin(groupId("org.apache.maven.plugins"), artifactId("maven-dependency-plugin"), version("3.3.0")),
				goal("build-classpath"), configuration(element(name("outputFile"), outputFilePath),
						// Optionally, you can include other configuration elements
						element(name("includeTypes"), "jar")),
				executionEnvironment(project, session, pluginManager));

		// Optionally, read the file and log the classpath
		Path path = new File(outputFilePath).toPath();
		try {
			List<String> lines = Files.readAllLines(path);
			if (!lines.isEmpty()) {
				String classpath = lines.get(0);
				getLog().info("Resolved Classpath:");
				getLog().info(classpath);
			} else {
				getLog().warn("Classpath file is empty.");
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Error reading the classpath file: " + outputFilePath, e);
		}
	}
}
