package com.rogaiopytov;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
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

	private final String logPrefix = "Build Goal";

	@Override
	public void execute() throws MojoExecutionException {
		Path baseDir = project.getBasedir().toPath();
		Path obfsDir = baseDir.resolve("obfs");

		if (Files.exists(obfsDir) && Files.isDirectory(obfsDir)) {
			try {
				deleteDirectoryRecursively(obfsDir);
			} catch (IOException e) {
				throw new MojoExecutionException("Failed to delete 'obfs' directory.", e);
			}
		}

		getLog().info(String.format("%s: Cleaned successfully.", logPrefix));
	}

	private void deleteDirectoryRecursively(Path path) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
