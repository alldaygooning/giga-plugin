package com.rogaiopytov;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

@Mojo(name = "obfuscate")
@Execute(phase = LifecyclePhase.PROCESS_RESOURCES)
public class ObfuscateMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File baseDir = project.getBasedir();
		File srcDir = new File(baseDir, "src");
		File destDir = new File(baseDir, "src-obfuscated");

		if (!srcDir.exists() || !srcDir.isDirectory()) {
			throw new MojoExecutionException("Source directory does not exist: " + srcDir.getAbsolutePath());
		}

		try {
			FileUtils.copyDirectory(srcDir, destDir);
			getLog().info("Directory copied successfully.");
		} catch (IOException e) {
			throw new MojoExecutionException("Error copying directory", e);
		}
	}
}
