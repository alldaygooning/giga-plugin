package com.rogaiopytov;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "build", threadSafe = true)
@Execute(goal = "compile")
public class BuildMojo extends AbstractMojo {
    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("'build' goal started");

        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-jar-plugin"),
                        version("3.2.0")
                ),
                goal("jar"),
                configuration(
                        element(name("outputDirectory"), "${project.build.directory}")
                ),
                executionEnvironment(project, session, pluginManager)
        );

        getLog().info("'build' goal completed successfully");
    }
}
