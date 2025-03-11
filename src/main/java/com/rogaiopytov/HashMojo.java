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
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

@Mojo(name = "hash", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class HashMojo extends AbstractMojo {

	@Component
	private MavenProject project;

	@Component
	private MavenSession session;

	@Component
	private BuildPluginManager pluginManager;

	@Parameter(property = "src", defaultValue = "src")
	private String src;

	private String providedSrc;

	private Map<String, String> md5Hashes = new HashMap<>();
	private Map<String, String> sha1Hashes = new HashMap<>();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		this.providedSrc = this.src;
		this.src = String.format("%s/%s", this.project.getBasedir().toString(), this.src);
		File directory = new File(src);
		if (!directory.exists() || !directory.isDirectory()) {
			throw new MojoExecutionException("Provided src directory does not exist or is not a directory: " + src);
		}

		try {
			processDirectory(directory);
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new MojoExecutionException("Error processing files", e);
		}
		
		List<ManifestEntry> manifestEntries = new ArrayList<>();

        for (Map.Entry<String, String> entry : md5Hashes.entrySet()) {
            File file = new File(entry.getKey());
			String fileName = this.getFileName(file);
            String entryName = "X-MD5-" + fileName;
            ManifestEntry manifestEntry = new ManifestEntry(entryName, entry.getValue());
            manifestEntries.add(manifestEntry);
        }

        for (Map.Entry<String, String> entry : sha1Hashes.entrySet()) {
            File file = new File(entry.getKey());
			String fileName = this.getFileName(file);
            String entryName = "X-SHA-1-" + fileName;
            ManifestEntry manifestEntry = new ManifestEntry(entryName, entry.getValue());
            manifestEntries.add(manifestEntry);
        }

		System.out.println(manifestEntries);

        List<Element> configElements = new ArrayList<>();
		configElements.add(element("src", this.providedSrc));
		
		if (!manifestEntries.isEmpty()) {
	        List<Element> entryElements = new ArrayList<>();
	        for (ManifestEntry me : manifestEntries) {
	            entryElements.add(
	                element("manifestEntry",
								element("name", me.getName()), element("value", me.getValue())
	                )
	            );
	        }
	        configElements.add(element("manifestEntries", entryElements.toArray(new Element[0])));
	    }

		executeMojo(
	            plugin(
	                groupId("com.RogaIKopytov"),
	                artifactId("rik-maven-plugin"),
	                version("1.0")
	            ),
				goal("build"),
				configuration(configElements.toArray(new Element[0])),
	            executionEnvironment(project, session, pluginManager)
	        );
	}

	private void processDirectory(File file) throws IOException, NoSuchAlgorithmException {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File child : files) {
					processDirectory(child);
				}
			}
		} else if (file.isFile() && file.getName().endsWith(".java")) {
			String md5 = computeChecksum(file, "MD5");
			String sha1 = computeChecksum(file, "SHA-1");
			md5Hashes.put(file.getAbsolutePath(), md5);
			sha1Hashes.put(file.getAbsolutePath(), sha1);
		}
	}

	private String getFileName(File file) {
		String fileName = file.getName();
		if (fileName == null || fileName.isEmpty()) {
			return fileName;
		}
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex <= 0) {
			return fileName;
		}
		return fileName.substring(0, dotIndex);
	}

	private String computeChecksum(File file, String algorithm) throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance(algorithm);
		try (FileInputStream fis = new FileInputStream(file)) {
			byte[] byteBuffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = fis.read(byteBuffer)) != -1) {
				digest.update(byteBuffer, 0, bytesRead);
			}
		}
		byte[] hashBytes = digest.digest();
		return bytesToHex(hashBytes);
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : bytes) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}
}
