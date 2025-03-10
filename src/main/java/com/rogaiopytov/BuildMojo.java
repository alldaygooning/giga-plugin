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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BuildMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

	@Parameter(property = "src", defaultValue = "src")
	private String src;

	private final String logPrefix = "Build Goal";

    @Override
    public void execute() throws MojoExecutionException {
		this.src = String.format("%s/%s", this.project.getBasedir().toString(), this.src);
        getLog().info(String.format("%s: Using src directory: %s", logPrefix, src));

        executeMojo(
            plugin(
                groupId("com.RogaIKopytov"),
                artifactId("demo-plugin-eclipse"),
                version("1.0")
            ),
            goal("compile"),
            configuration(
                element("src", src)
            ),
            executionEnvironment(project, session, pluginManager)
        );

        executeMojo(
            plugin(
                groupId("org.apache.maven.plugins"),
                artifactId("maven-war-plugin"),
                version("3.4.0")
            ),
            goal("war"),
				configuration(element(name("warSourceDirectory"),
						String.format("%s/main/webapp", this.src))
            ),
            executionEnvironment(project, session, pluginManager)
        );
    }
}
