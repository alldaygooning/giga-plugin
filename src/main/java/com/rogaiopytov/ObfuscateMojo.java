package com.rogaiopytov;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

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
			this.copy(srcDir, destDir);
		} catch (IOException e) {
			throw new MojoExecutionException(String.format("I/O Exception occured creating %s.", destDir.toURI()));
		}

	}

	private void copy(File src, File dst) throws IOException {
		Path source = src.toPath();
		Path destination = dst.toPath();
		Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Files.createDirectories(destination.resolve(source.relativize(dir).toString()));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.copy(file, destination.resolve(destination.relativize(file).toString()));
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
