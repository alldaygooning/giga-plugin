package com.rogaiopytov;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "n2a")
public class AsciiMojo extends AbstractMojo {

	private final String logPrefix = "Native2Ascii Goal";

	@Component
	private MavenProject project;

	@Component
	private MavenSession session;

	@Component
	private BuildPluginManager pluginManager;

	@Parameter(property = "src", required = true, defaultValue = "src/main/resources")
	private String inputDir;

	@Parameter(property = "dst", required = true, defaultValue = "target/locale")
	private String targetDir;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		this.inputDir = String.format("%s/%s", this.project.getBasedir().toString(), this.inputDir);
		this.targetDir = String.format("%s/%s", this.project.getBasedir().toString(), this.targetDir);

		File targetDirectoryFile = new File(targetDir);
		if (!targetDirectoryFile.exists() && !targetDirectoryFile.mkdirs()) {
			throw new MojoExecutionException(String.format("%s: Could not create target directory: %s", logPrefix, targetDir));
		}

		File inputDirectoryFile = new File(inputDir);
		if (!inputDirectoryFile.exists() || !inputDirectoryFile.isDirectory()) {
			throw new MojoExecutionException(
					String.format("%s: Input directory does not exist or is not a directory: %s", logPrefix, inputDir));
		}
		try {
			Files.walk(Paths.get(inputDir)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".properties"))
					.forEach(this::processFile);
		} catch (IOException e) {
			throw new MojoExecutionException(String.format("%s: Error walking through input directory: %s", logPrefix, inputDir), e);
		}
	}

	private void processFile(Path sourcePath) {
		Path relativePath = Paths.get(inputDir).relativize(sourcePath);
		Path targetPath = Paths.get(targetDir).resolve(relativePath);

		File targetFileParent = targetPath.getParent().toFile();
		if (!targetFileParent.exists() && !targetFileParent.mkdirs()) {
			getLog().error(String.format("%s: Could not create directory: %s", logPrefix, targetFileParent.getAbsolutePath()));
			return;
		}

		getLog().info(String.format("%s: Processing file: %s", logPrefix, sourcePath.toString()));
		try (BufferedReader reader = new BufferedReader(new FileReader(sourcePath.toFile()));
				BufferedWriter writer = new BufferedWriter(new FileWriter(targetPath.toFile()))) {

			String line;
			while ((line = reader.readLine()) != null) {
				String convertedLine = toAsciiEscaped(line);
				writer.write(convertedLine);
				writer.newLine();
			}
		} catch (IOException e) {
			getLog().error(String.format("%s: Error processing file: %s", logPrefix, sourcePath.toString()), e);
		}
	}

	private String toAsciiEscaped(String input) {
		if (Objects.isNull(input)) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (char c : input.toCharArray()) {
			if (c > 127) {
				sb.append(String.format("\\u%04X", (int) c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
