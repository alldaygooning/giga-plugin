package com.rogaiopytov;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

	private final String logPrefix = "Obfuscate Goal";

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	private final Random random = new Random();

	// Fully-Qualified Mappings.
	// Мап, хранящий полные названия классов и переменных.
	// Классы: package.Class;
	// Поля: package.Class.field;
	private Map<String, String> fqMappings = new HashMap<>();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File baseDir = project.getBasedir();
		File srcDir = new File(baseDir, "src");
		File obfuscatedDir = new File(baseDir, "obfs");

		if (!srcDir.exists()) {
			throw new MojoExecutionException(
					String.format("%s: Source directory 'src' does not exist at %s.", logPrefix, srcDir.getAbsolutePath()));
		}

		try {
			FileUtils.copyDirectoryStructure(srcDir, obfuscatedDir);
		} catch (IOException e) {
			throw new MojoExecutionException(String.format("%s: Failed to copy 'src' directory", logPrefix), e);
		}

		fqMappings = generateMappings(obfuscatedDir);
		logMappings(fqMappings, new File(baseDir, "mappings.txt"));

		/*
		 * Разделение маппингов названий классов от названий переменных. При замене
		 * классов и полей логика немного отличается.
		 */
		Map<String, String> simpleMappingClass = new HashMap<>();
		Map<String, String> simpleMappingField = new HashMap<>();
		for (Map.Entry<String, String> entry : fqMappings.entrySet()) {
			String fullyQualified = entry.getKey();
			String simpleName = fullyQualified.substring(fullyQualified.lastIndexOf('.') + 1);

			// Регулярка поиска полей по ошибке захватывает значения true/false, поэтому их
			// просто пропускаю
			if ("true".equalsIgnoreCase(simpleName) || "false".equalsIgnoreCase(simpleName)) {
				continue;
			}

			String[] tokens = fullyQualified.split("\\.");
			if (tokens.length >= 2) {
				String beforeLast = tokens[tokens.length - 2];
				if (!beforeLast.isEmpty() && Character.isUpperCase(beforeLast.charAt(0))) {
					simpleMappingField.put(simpleName, entry.getValue());
				} else {
					simpleMappingClass.put(simpleName, entry.getValue());
				}
			} else {
				simpleMappingClass.put(simpleName, entry.getValue());
			}
		}

		try {
			Collection<File> allFiles = new ArrayList<>(FileUtils.getFiles(obfuscatedDir, "**/*.java", null));
			Collection<File> replaceClassFiles = new ArrayList<>();

			File metaInfDir = new File(obfuscatedDir, "main/resources/META-INF");
			File webAppDir = new File(obfuscatedDir, "main/webapp");
//			File webInfDir = new File(obfuscatedDir, "/WEB-INF");

			if (metaInfDir.exists()) {
				allFiles.addAll(FileUtils.getFiles(metaInfDir, "**/*", null));
			}
			if (webAppDir.exists()) {
				replaceClassFiles.addAll(FileUtils.getFiles(webAppDir, "**/*.xhtml", null));
				File webInfDir = new File(webAppDir, "WEB-INF");
				if (webInfDir.exists()) {
					allFiles.addAll(FileUtils.getFiles(webInfDir, "**/*", null));
				}
			}

			for (File file : replaceClassFiles) {
				replaceIdentifiersInFile(file, simpleMappingClass, false);
			}

			for (File file : allFiles) {
				replaceIdentifiersInFile(file, simpleMappingClass, false);
				replaceIdentifiersInFile(file, simpleMappingField, true);
			}
		} catch (IOException e) {
			throw new MojoExecutionException(String.format("%s: Error scanning files for replacement", logPrefix), e);
		}

		renameMatchingFiles(obfuscatedDir, simpleMappingClass, false);
	}

	// Сканирует все .java классы, чтобы найти все названия классов и переменных.
	private Map<String, String> generateMappings(File sourceDir) throws MojoExecutionException {
		Map<String, String> mappings = new HashMap<>();

		// Регулярка, чтобы найти полное название пакета
		Pattern packagePattern = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;", Pattern.MULTILINE);

		// Регулярка, чтобы найти название класса (не работает с вложенными классами!)
		Pattern classPattern = Pattern.compile("\\bclass\\s+([A-Za-z][A-Za-z0-9_]*)\\b");

		// Регулярка, чтобы найти название класса (не работает с множественным
		// декларированием в одну строку)
		Pattern fieldPattern = Pattern.compile("(?:\\b(public|protected|private|static|final|transient|volatile)\\s+)*"
				+ "[A-Za-z0-9_<>\\$\\$]+\\s+([A-Za-z][A-Za-z0-9_]*)(\\s*(=|;))");

		Collection<File> javaFiles;
		try {
			javaFiles = FileUtils.getFiles(sourceDir, "**/*.java", null);
		} catch (IOException e) {
			throw new MojoExecutionException(String.format("%s: Error reading Java source files", logPrefix), e);
		}

		for (File javaFile : javaFiles) {
			String fileContents;
			try {
				fileContents = FileUtils.fileRead(javaFile, StandardCharsets.UTF_8.name());
			} catch (IOException e) {
				getLog().error(String.format("%s: Error reading file: %s", logPrefix, javaFile.getAbsolutePath()), e);
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
				String fullyQualifiedClass = packageName.isEmpty() ? className : packageName + "." + className;
				String obfuscatedClassName = generateObfuscatedName(className.length());
				mappings.put(fullyQualifiedClass, obfuscatedClassName);

				Matcher fieldMatcher = fieldPattern.matcher(fileContents);
				while (fieldMatcher.find()) {
					String fieldName = fieldMatcher.group(2);
					String fullyQualifiedField = fullyQualifiedClass + "." + fieldName;
					String obfuscatedFieldName = generateObfuscatedName(fieldName.length());
					mappings.put(fullyQualifiedField, obfuscatedFieldName);
				}
			}
		}
		return mappings;
	}

	private void logMappings(Map<String, String> mappings, File mappingFile) {
		StringBuilder sb = new StringBuilder();
		sb.append("Mappings:\n");
		for (Map.Entry<String, String> entry : mappings.entrySet()) {
			sb.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
		}

		try {
			FileUtils.fileWrite(mappingFile.getAbsolutePath(), StandardCharsets.UTF_8.name(), sb.toString());
			getLog().info(String.format("%s: Mappings written to file: %s", logPrefix, mappingFile.getAbsolutePath()));
		} catch (IOException e) {
			getLog().error(String.format("%s: Error writing mappings to file: %s", logPrefix, mappingFile.getAbsolutePath()), e);
		}
	}

	// Заменяет все найденные классы и поля на их обфусцированные версии
	private void replaceIdentifiersInFile(File file, Map<String, String> simpleMapping, boolean caseSensitive) {
		String fileContents;
		try {
			fileContents = FileUtils.fileRead(file, StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			getLog().error(String.format("%s: Error reading file for replacement: %s", logPrefix, file.getAbsolutePath()), e);
			return;
		}

		for (Map.Entry<String, String> entry : simpleMapping.entrySet()) {
			String original = entry.getKey();
			if ("true".equalsIgnoreCase(original) || "false".equalsIgnoreCase(original)) {
				continue;
			}
			String obfuscated = entry.getValue();
			int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
			Pattern pattern = Pattern.compile("(?<![@A-Za-z0-9_])(" + Pattern.quote(original) + ")(?![A-Za-z0-9_])", flags);
			Matcher matcher = pattern.matcher(fileContents);

			StringBuffer sb = new StringBuffer();
			while (matcher.find()) {
				String matchedText = matcher.group(1);
				String replacement = adjustCase(obfuscated, matchedText);
				matcher.appendReplacement(sb, replacement);
			}
			matcher.appendTail(sb);
			fileContents = sb.toString();
		}

		try {
			FileUtils.fileWrite(file.getAbsolutePath(), StandardCharsets.UTF_8.name(), fileContents);
		} catch (IOException e) {
			getLog().error(String.format("%s: Error writing file after replacement: %s", logPrefix, file.getAbsolutePath()), e);
		}
	}

	// Переименовывает файлы в соответсвии с обфусцированными названиями классов.
	private void renameMatchingFiles(File file, Map<String, String> simpleMapping, boolean caseSensitive) {
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					renameMatchingFiles(child, simpleMapping, caseSensitive);
				}
			}
		}

		String originalName = file.getName();
		String newName = originalName;

		for (Map.Entry<String, String> entry : simpleMapping.entrySet()) {
			String key = entry.getKey();
			if ("true".equalsIgnoreCase(key) || "false".equalsIgnoreCase(key)) {
				continue;
			}
			String obfuscated = entry.getValue();
			int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
			Pattern pattern = Pattern.compile("(?<![@A-Za-z0-9_])(" + Pattern.quote(key) + ")(?![A-Za-z0-9_])", flags);
			Matcher matcher = pattern.matcher(newName);
			StringBuffer sb = new StringBuffer();
			boolean found = false;
			while (matcher.find()) {
				found = true;
				String matchedText = matcher.group(1);
				String replacement = adjustCase(obfuscated, matchedText);
				matcher.appendReplacement(sb, replacement);
			}
			if (found) {
				matcher.appendTail(sb);
				newName = sb.toString();
			}
		}

		if (!newName.equals(originalName)) {
			File newFile = new File(file.getParent(), newName);
			boolean renamed = file.renameTo(newFile);
			if (renamed) {
				getLog().info(String.format("%s: Renamed: %s -> %s", logPrefix, file.getAbsolutePath(), newFile.getAbsolutePath()));
			} else {
				getLog().error(String.format("%s: Failed to rename: %s", logPrefix, file.getAbsolutePath()));
			}
		}
	}

	// Делает так, что обфусцированное название классов всегда начинается с большой
	// буквы, а переменных - с маленькой.
	private String adjustCase(String obfuscated, String original) {
		if (original.equals(original.toUpperCase())) {
			return obfuscated.toUpperCase();
		} else if (original.equals(original.toLowerCase())) {
			return obfuscated.toLowerCase();
		} else if (Character.isUpperCase(original.charAt(0))) {
			if (!obfuscated.isEmpty()) {
				return Character.toUpperCase(obfuscated.charAt(0)) + obfuscated.substring(1);
			}
		} else if (Character.isLowerCase(original.charAt(0))) {
			if (!obfuscated.isEmpty()) {
				return Character.toLowerCase(obfuscated.charAt(0)) + obfuscated.substring(1);
			}
		}
		return obfuscated;
	}

	// Генерирует обфусцированные названия.
	// Названия классов и переменных всегда начинаются с буквы.
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
