package com.rogaiopytov;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

@Mojo(name = "obfuscate")
public class ObfuscateMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	private final Random random = new Random();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File baseDir = project.getBasedir();
		File srcDir = new File(baseDir, "src");
		File obfuscatedDir = new File(baseDir, "obfs");

		if (!srcDir.exists()) {
			throw new MojoExecutionException("Source directory 'src' does not exist at " + srcDir.getAbsolutePath());
		}

		try {
			FileUtils.copyDirectoryStructure(srcDir, obfuscatedDir);
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to copy 'src' directory", e);
		}

		Map<String, String> fqMappings = generateMappings(obfuscatedDir);
		logMappings(fqMappings);

		// Build a simple name mapping
		Map<String, String> simpleMapping = new HashMap<>();
		// Assuming class names are unique across packages. If not, you'll have to
		// decide how to handle collisions.
		for (Map.Entry<String, String> entry : fqMappings.entrySet()) {
			String simpleName = entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1);
			simpleMapping.put(simpleName, entry.getValue());
		}

		// Now scan EVERY file in obfuscatedDir and replace occurrences of the class
		// names.
		try {
			Collection<File> allFiles = FileUtils.getFiles(obfuscatedDir, "**/*", null);
			for (File file : allFiles) {
				replaceIdentifiersInFile(file, simpleMapping);
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Error scanning files for replacement", e);
		}
	}

	private Map<String, String> generateMappings(File sourceDir) throws MojoExecutionException {
		Map<String, String> mappings = new HashMap<>();

		Pattern packagePattern = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;", Pattern.MULTILINE);
		Pattern classPattern = Pattern.compile("\\bclass\\s+([A-Za-z][A-Za-z0-9_]*)\\b");

		Collection<File> javaFiles;
		try {
			javaFiles = FileUtils.getFiles(sourceDir, "**/*.java", null);
		} catch (IOException e) {
			throw new MojoExecutionException("Error reading Java source files", e);
		}

		for (File javaFile : javaFiles) {
			String fileContents;
			try {
				fileContents = FileUtils.fileRead(javaFile, StandardCharsets.UTF_8.name());
			} catch (IOException e) {
				getLog().error("Error reading file: " + javaFile.getAbsolutePath(), e);
				continue;
			}

			String packageName = "";
			Matcher packageMatcher = packagePattern.matcher(fileContents);
			if (packageMatcher.find()) {
				packageName = packageMatcher.group(1);
			}

			Matcher classMatcher = classPattern.matcher(fileContents);
			while (classMatcher.find()) {
				String className = classMatcher.group(1);
				String fullyQualifiedName = packageName.isEmpty() ? className : packageName + "." + className;

				String obfuscatedName = generateObfuscatedName(className.length());
				mappings.put(fullyQualifiedName, obfuscatedName);
			}
		}

		return mappings;
	}

	private void logMappings(Map<String, String> mappings) {
		for (Map.Entry<String, String> entry : mappings.entrySet()) {
			getLog().info(entry.getKey() + " : " + entry.getValue());
		}
	}

	private void replaceIdentifiersInFile(File file, Map<String, String> simpleMapping) {
		String fileContents;
		try {
			fileContents = FileUtils.fileRead(file, StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			getLog().error("Error reading file for replacement: " + file.getAbsolutePath(), e);
			return;
		}

		// For each simple name that needs replacement, catch whole word occurrence
		// ignoring case.
		for (Map.Entry<String, String> entry : simpleMapping.entrySet()) {
			// (?<![A-Za-z0-9]) - negative lookbehind: previous char is not alphanumeric
			// (?i) - case-insensitive flag for this part of the pattern
			// (original) - match the original simple name
			// (?![A-Za-z0-9]) - negative lookahead: next char is not alphanumeric
			// Note: Java doesn't allow inline (?i) in the middle of a pattern if using
			// lookahead/lookbehind,
			// so we compile a case-insensitive pattern.
			String original = entry.getKey();
			String obfuscated = entry.getValue();
			Pattern pattern = Pattern.compile("(?<![A-Za-z0-9])(" + Pattern.quote(original) + ")(?![A-Za-z0-9])", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(fileContents);
			// We use StringBuffer with matcher.appendReplacement since replacement can
			// repeat.
			StringBuffer sb = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(sb, obfuscated);
			}
			matcher.appendTail(sb);
			fileContents = sb.toString();
		}

		try {
			FileUtils.fileWrite(file.getAbsolutePath(), StandardCharsets.UTF_8.name(), fileContents);
		} catch (IOException e) {
			getLog().error("Error writing file after replacement: " + file.getAbsolutePath(), e);
		}
	}

	private String generateObfuscatedName(int length) {
		if (length < 1) {
			throw new IllegalArgumentException("Length must be at least 1.");
		}
		StringBuilder sb = new StringBuilder(length);
		char firstChar = randomLetter();
		sb.append(firstChar);
		for (int i = 1; i < length; i++) {
			sb.append(randomAlphaNumeric());
		}
		return sb.toString();
	}

	private char randomLetter() {
		boolean upper = random.nextBoolean();
		if (upper) {
			return (char) ('A' + random.nextInt(26));
		} else {
			return (char) ('a' + random.nextInt(26));
		}
	}

	private char randomAlphaNumeric() {
		int choice = random.nextInt(36);
		if (choice < 26) {
			return random.nextBoolean() ? (char) ('A' + choice) : (char) ('a' + choice);
		} else {
			return (char) ('0' + (choice - 26));
		}
	}
}
