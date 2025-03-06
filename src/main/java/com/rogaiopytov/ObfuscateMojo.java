package com.rogaiopytov;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "obfuscate", defaultPhase = LifecyclePhase.VERIFY)
public class ObfuscateMojo extends AbstractMojo {

	// Use project directories. The "src" remains at the project level.
	private final Path srcDir = Paths.get("src");

	// All generated files will be under "target" directory.
	private final Path targetDir = Paths.get("target");
	// Obfuscated source directory will now be "target/src-obfuscated"
	private final Path obfSrcDir = targetDir.resolve("src-obfuscated");
	// Mapping file in "target"
	private final Path mappingFile = targetDir.resolve("obfuscation-mapping.txt");

	// Mapping structures: keys are original names, values are obfuscated names.
	private final Map<String, String> classMapping = new LinkedHashMap<>();
	private final Map<String, String> variableMapping = new LinkedHashMap<>();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			// Prepare target directory
			if (!Files.exists(targetDir)) {
				Files.createDirectories(targetDir);
			}

			// Step 1: Duplicate directory structure from "src" to "target/src-obfuscated"
			if (!Files.exists(srcDir)) {
				throw new MojoExecutionException("The source directory 'src' does not exist.");
			}
			copyDirectory(srcDir, obfSrcDir);
			getLog().info("Copied src to " + obfSrcDir.toString());

			// Step 2: Process each Java file in src-obfuscated for obfuscation.
			List<Path> javaFiles = new ArrayList<>();
			Files.walk(obfSrcDir).filter(path -> path.toString().endsWith(".java")).forEach(javaFiles::add);
			getLog().info("Found " + javaFiles.size() + " Java files in " + obfSrcDir.toString());

			// Obfuscate the Java files.
			for (Path javaFile : javaFiles) {
				obfuscateFile(javaFile);
			}
			getLog().info("Obfuscation completed, writing mapping file.");

			// Write mapping file in the target directory.
			writeMappingFile();

			// Step 3: Package both directories into .war files.
			getLog().info("Packaging obfuscated-src into target/obfuscated.war");
			createWar(obfSrcDir, targetDir.resolve("obfuscated.war"));

			getLog().info("Packaging normal src into target/normal.war");
			createWar(srcDir, targetDir.resolve("normal.war"));

		} catch (Exception e) {
			throw new MojoExecutionException("Error during obfuscation: " + e.getMessage(), e);
		}
	}

	/**
	 * Copy the directory tree from source to target.
	 */
	private void copyDirectory(Path source, Path target) throws IOException {
		if (Files.exists(target)) {
			// Delete the target directory first.
			deleteDirectory(target);
		}
		Files.walk(source).forEach(sourcePath -> {
			try {
				Path targetPath = target.resolve(source.relativize(sourcePath));
				if (Files.isDirectory(sourcePath)) {
					Files.createDirectories(targetPath);
				} else {
					Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException ex) {
				throw new RuntimeException("Error copying file " + sourcePath + " to " + target, ex);
			}
		});
	}

	/**
	 * Recursively delete a directory.
	 */
	private void deleteDirectory(Path directory) throws IOException {
		if (Files.exists(directory)) {
			Files.walk(directory).sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					getLog().warn("Unable to delete " + path, e);
				}
			});
		}
	}

	/**
	 * Obfuscates the content of a Java file.
	 */
	private void obfuscateFile(Path javaFile) throws IOException {
		String content = new String(Files.readAllBytes(javaFile), StandardCharsets.UTF_8);

		// Simple regex patterns to match class names and variable names.
		Pattern classPattern = Pattern.compile("\\b(class|interface|enum)\\s+(\\w+)");
		Pattern varPattern = Pattern.compile("\\b(?:private|protected|public|final|static|volatile|transient)\\s+\\S+\\s+(\\w+)\\s*(=|;)");

		// Process class names
		Matcher m = classPattern.matcher(content);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String originalName = m.group(2);
			String obfName = classMapping.computeIfAbsent(originalName, k -> generateRandomName());
			m.appendReplacement(sb, m.group(1) + " " + obfName);
		}
		m.appendTail(sb);
		content = sb.toString();

		// Process variable names
		m = varPattern.matcher(content);
		sb = new StringBuffer();
		while (m.find()) {
			String originalName = m.group(1);
			String obfName = variableMapping.computeIfAbsent(originalName, k -> generateRandomName());
			// Replace while preserving the delimiter.
			m.appendReplacement(sb, m.group(0).replace(originalName, obfName));
		}
		m.appendTail(sb);
		content = sb.toString();

		Files.write(javaFile, content.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Generate a random obfuscated identifier that does not start with a number.
	 */
	private String generateRandomName() {
		String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		String alphanum = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		Random rnd = new Random();
		// First character must be a letter:
		StringBuilder sb = new StringBuilder();
		sb.append(letters.charAt(rnd.nextInt(letters.length())));
		// generate a name of length between 6 and 12 characters:
		int len = 6 + rnd.nextInt(7);
		for (int i = 1; i < len; i++) {
			sb.append(alphanum.charAt(rnd.nextInt(alphanum.length())));
		}
		return sb.toString();
	}

	/**
	 * Write the obfuscation mappings to a file.
	 */
	private void writeMappingFile() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(mappingFile, StandardCharsets.UTF_8)) {
			writer.write("Class mappings:\n");
			for (Map.Entry<String, String> entry : classMapping.entrySet()) {
				writer.write("Class [" + entry.getKey() + "] : " + entry.getValue() + "\n");
			}
			writer.write("\nVariable mappings:\n");
			for (Map.Entry<String, String> entry : variableMapping.entrySet()) {
				writer.write("Variable [" + entry.getKey() + "] : " + entry.getValue() + "\n");
			}
		}
		getLog().info("Mapping file written: " + mappingFile.toAbsolutePath());
	}

	/**
	 * Create a basic WAR file from a given source directory. This implementation
	 * packages the directory structure in a ZIP file with the .war extension.
	 */
	private void createWar(Path folder, Path warFile) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(warFile.toFile()); ZipOutputStream zos = new ZipOutputStream(fos)) {
			Files.walk(folder).filter(path -> !Files.isDirectory(path)).forEach(path -> {
				ZipEntry zipEntry = new ZipEntry(folder.relativize(path).toString().replace("\\", "/"));
				try {
					zos.putNextEntry(zipEntry);
					Files.copy(path, zos);
					zos.closeEntry();
				} catch (IOException e) {
					throw new RuntimeException("Error while creating war file", e);
				}
			});
		}
	}
}
