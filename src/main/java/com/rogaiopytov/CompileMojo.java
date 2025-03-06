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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

@Mojo(name = "compile", threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class CompileMojo extends AbstractMojo {

	@Component
	private MavenProject project;

	@Component
	private MavenSession session;

	@Component
	private BuildPluginManager pluginManager;

	@Parameter(property = "src", defaultValue = "${project.build.sourceDirectory}", required = true)
	private String src;

	@Override
	public void execute() throws MojoExecutionException {
		Set<String> compileSourceRoots = new HashSet<>();
		File srcDir = new File(src);
		if (!srcDir.exists() || !srcDir.isDirectory()) {
			throw new MojoExecutionException("The provided source directory does not exist or is not a directory: " + src);
		}
		findJavaSourceRoots(srcDir, compileSourceRoots);

		if (compileSourceRoots.isEmpty()) {
			getLog().warn("No Java source files found under directory: " + src);
		} else {
			getLog().info("Found Java source roots: " + compileSourceRoots);
		}

		List<Element> sourceRootElements = new ArrayList<>();
		for (String dir : compileSourceRoots) {
			sourceRootElements.add(element("compileSourceRoot", dir));
		}

		getLog().info("Compiling using these source roots: " + compileSourceRoots);

		executeMojo(plugin(groupId("org.apache.maven.plugins"), artifactId("maven-compiler-plugin"), version("3.14.0")), goal("compile"),
				configuration(element("compileSourceRoots", sourceRootElements.toArray(new Element[0]))),
				executionEnvironment(project, session, pluginManager));

		copyMetaInfResources(src);
	}

	private void findJavaSourceRoots(File dir, Set<String> roots) {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		boolean containsJavaFile = false;
		for (File file : files) {
			if (file.isFile() && file.getName().endsWith(".java")) {
				containsJavaFile = true;
				break;
			}
		}
		if (containsJavaFile) {
			try {
				roots.add(dir.getCanonicalPath());
			} catch (Exception ex) {
				getLog().warn("Failed to get canonical path for directory " + dir.getAbsolutePath(), ex);
				roots.add(dir.getAbsolutePath());
			}
		}
		for (File file : files) {
			if (file.isDirectory()) {
				findJavaSourceRoots(file, roots);
			}
		}
	}

	private void copyMetaInfResources(String src) {
		File sourceMetaInfDir = new File(project.getBasedir(), src + "/main/resources/META-INF");
		if (!sourceMetaInfDir.exists() || !sourceMetaInfDir.isDirectory()) {
			getLog().info("No META-INF resource directory found at: " + sourceMetaInfDir.getAbsolutePath());
			return;
		}

		String outputDirPath = project.getBuild().getOutputDirectory();
		File outputMetaInfDir = new File(outputDirPath, "META-INF");

		getLog().info(
				"Copying META-INF resources from " + sourceMetaInfDir.getAbsolutePath() + " to " + outputMetaInfDir.getAbsolutePath());
		try {
			FileUtils.copyDirectory(sourceMetaInfDir, outputMetaInfDir);
			getLog().info("META-INF resources successfully copied.");
		} catch (IOException e) {
			getLog().error("Failed to copy META-INF resources.", e);
		}
	}
}
