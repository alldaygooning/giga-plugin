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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "build", threadSafe = true)
@Execute(goal = "compile")
public class BuildMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

	@Parameter(property = "webapp", defaultValue = "src/main/webapp")
	private String sourceDirectory;

	private String logPrefix = "Build Goal";

    @Override
    public void execute() throws MojoExecutionException {
		getLog().info(String.format("%s: Using source directory: %s", logPrefix, sourceDirectory));

        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
						artifactId("maven-war-plugin"),
						version("3.3.1")
                ),
				goal("war"),
                configuration(
						element(name("warSourceDirectory"), sourceDirectory),
						element(name("outputDirectory"),
								"${project.build.directory}")
                ),
                executionEnvironment(project, session, pluginManager)
        );

		getLog().info(String.format("%s: Completed successfully", logPrefix));
    }
}
