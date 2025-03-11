package com.rogaiopytov;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "obfuscate-build", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ObfuscateBuildMojo extends AbstractMojo{

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
	                groupId("com.RogaIKopytov"),
	                artifactId("rik-maven-plugin"),
	                version("1.0")
	            ),
				goal("obfuscate"),
	            configuration(
	            ),
	            executionEnvironment(project, session, pluginManager)
	        );
		executeMojo(
	            plugin(
	                groupId("com.RogaIKopytov"),
	                artifactId("rik-maven-plugin"),
	                version("1.0")
	            ),
				goal("build"),
	            configuration(
	            		element("src", "obfs")
	            ),
	            executionEnvironment(project, session, pluginManager)
	        );
	}
}
