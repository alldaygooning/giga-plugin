package com.rogaiopytov;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "scp", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ScpMojo extends AbstractMojo {

	private final String logPrefix = "SCP Goal";

	@Component
	private MavenProject project;

	@Component
	private MavenSession session;

	@Component
	private BuildPluginManager pluginManager;

	@Parameter(property = "server", required = true)
	private String server;

	@Parameter(property = "port", defaultValue = "2222")
	private int port;

	@Parameter(property = "user", required = true)
	private String user;

	@Parameter(property = "password")
	private String password;

	@Parameter(property = "dir", defaultValue = "~")
	private String dir;

	@Override
	public void execute() throws MojoExecutionException {
		executeMojo(plugin(groupId("com.RogaIKopytov"), artifactId("rik-maven-plugin"), version("1.0")), goal("build"), configuration(),
				executionEnvironment(project, session, pluginManager));
		File warArchive = resolveWarArchive();
		getLog().info(String.format("%s: Found WAR archive: %s", logPrefix, warArchive.getAbsolutePath()));
		String destination = buildDestination();
		String pwd = getPassword();
		executeScpTransfer(warArchive, destination, pwd);
	}

	private File resolveWarArchive() throws MojoExecutionException {
		String finalName = project.getBuild().getFinalName();
		String targetDir = project.getBuild().getDirectory();
		File warArchive = new File(targetDir, finalName + ".war");
		if (!warArchive.exists()) {
			throw new MojoExecutionException(String.format("%s: WAR archive not found: %s", logPrefix, warArchive.getAbsolutePath()));
		}
		return warArchive;
	}

	private String buildDestination() {
		String destination;
		if (dir != null && !dir.trim().isEmpty()) {
			String remoteDirectory = dir.endsWith("/") ? dir : dir + "/";
			destination = user + "@" + server + ":" + remoteDirectory;
		} else {
			destination = user + "@" + server + ":";
		}
		return destination;
	}

	private String getPassword() {
		String pwd = password;
		if (pwd == null || pwd.isEmpty()) {
			Console console = System.console();
			if (console != null) {
				char[] pwdChars = console.readPassword("Enter password for %s@%s: ", user, server);
				if (pwdChars != null) {
					pwd = new String(pwdChars);
				}
			} else {
				getLog().warn(String.format("%s: No console available to prompt for password. Proceeding without password.", logPrefix));
			}
		}
		return pwd;
	}

	private void executeScpTransfer(File warArchive, String destination, String pwd) throws MojoExecutionException {
		List<String> command = new ArrayList<>();
		if (pwd != null && !pwd.isEmpty()) {
			command.add("sshpass");
			command.add("-p");
			command.add(pwd);
		}
		command.add("scp");
		command.add("-P");
		command.add(String.valueOf(port));
		command.add(warArchive.getAbsolutePath());
		command.add(destination);

		getLog().info(String.format("%s: Transferring WAR archive using SCP to %s on port %s", logPrefix, destination, port));
		getLog().debug(String.format("%s: Executing command: %s", logPrefix, String.join(" ", command)));
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(true);
		try {
			Process process = pb.start();
			new Thread(() -> {
				try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						getLog().info(String.format("%s: %s", logPrefix, line));
					}
				} catch (IOException e) {
					getLog().error(String.format("%s: Error reading SCP output: %s", logPrefix, e.getMessage()));
				}
			}).start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new MojoExecutionException(String.format("%s: SCP command failed with exit code %s", logPrefix, exitCode));
			} else {
				getLog().info(String.format("%s: SCP transfer completed successfully.", logPrefix));
			}
		} catch (IOException | InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MojoExecutionException(String.format("%s: Error executing SCP command: %s", logPrefix, e.getMessage()), e);
		}
	}
}
