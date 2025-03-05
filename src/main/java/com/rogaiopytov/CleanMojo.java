package com.rogaiopytov;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

@Mojo(name = "clean", threadSafe = true)
@Execute(phase = LifecyclePhase.POST_CLEAN)
public class CleanMojo extends AbstractMojo {
	@Component
	private MavenProject project;

	@Component
	private MavenSession session;

	@Component
	private BuildPluginManager pluginManager;

	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("'clean' goal completed successfully");
	}
}
