package com.rogaiopytov;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

@Mojo(name = "report", defaultPhase = LifecyclePhase.TEST, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(goal = "test")
public class ReportMojo extends AbstractMojo {

    private final String logPrefix = "Report Goal";

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

    @Parameter(property = "xmlSourceDirectory", defaultValue = "${project.build.directory}/surefire-reports")
    private String xmlSourceDirectoryParam;

    @Parameter(property = "reportsDirectory", defaultValue = "${project.basedir}/test-reports")
    private String reportsDirectoryParam;

    @Override
    public void execute() throws MojoExecutionException {
        logInfoMessage("Report goal started");

        Path xmlSourceDirectory;
        Path reportsDirectory;

        if (Path.of(xmlSourceDirectoryParam).isAbsolute()) {
            xmlSourceDirectory = Path.of(xmlSourceDirectoryParam);
        } else {
            xmlSourceDirectory = Paths.get(project.getBasedir().getAbsolutePath(), xmlSourceDirectoryParam);
        }
        logInfoMessage("XML source directory: " + xmlSourceDirectory);

        if (Path.of(reportsDirectoryParam).isAbsolute()) {
            reportsDirectory = Path.of(reportsDirectoryParam);
        } else {
            reportsDirectory = Paths.get(project.getBasedir().getAbsolutePath(), reportsDirectoryParam);
        }
        logInfoMessage("Reports directory: " + reportsDirectory);

        logInfoMessage("Loading git repository");
        Path repositoryPath = Paths.get(project.getBasedir().getAbsolutePath());
        if (!Files.exists(repositoryPath)) {
            throw new MojoExecutionException(String.format("Repository %s does not exist", repositoryPath));
        }
        if (!Files.isDirectory(repositoryPath)) {
            throw new MojoExecutionException(String.format("Repository %s is not a directory", repositoryPath));
        }

        try (Repository repository = Git.open(repositoryPath.toFile()).getRepository()) {
            Git git = new Git(repository);

			if (!Files.exists(xmlSourceDirectory) || !Files.isDirectory(xmlSourceDirectory)) {
				throw new MojoExecutionException(String.format("XML source directory %s is invalid or does not exist", xmlSourceDirectory));
			}

			File[] xmlFiles = xmlSourceDirectory.toFile().listFiles(((dir, name) -> name.endsWith(".xml")));

			if (xmlFiles == null || xmlFiles.length == 0) {
				logInfoMessage("No XML files found. Skipping report commit");
				return;
			}

            if (Files.exists(reportsDirectory) && Files.isDirectory(reportsDirectory)) {
                logInfoMessage("Found reports directory");
            } else {
                logInfoMessage("Creating reports directory");

                deleteIfExists(reportsDirectory);
                Files.createDirectory(reportsDirectory);
                logInfoMessage("Directory created: " + reportsDirectory);
            }

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String datePrefix = ZonedDateTime.now(ZoneOffset.UTC).format(dateTimeFormatter);
            Path reportDirectory = reportsDirectory.resolve(datePrefix);

            deleteIfExists(reportDirectory);
            Files.createDirectory(reportDirectory);
            logInfoMessage("Created report directory: " + reportDirectory);

            for (File xmlFile : xmlFiles) {
                Path sourcePath = xmlFile.toPath();
                Path targetPath = reportDirectory.resolve(xmlFile.getName());
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                logInfoMessage("Copied: " + sourcePath + " to " + targetPath);
            }

            AddCommand addCommand = git.add();
            addCommand.addFilepattern(project.getBasedir().toPath().relativize(reportsDirectory).toString());
            addCommand.call();

            Status status = git.status().call();
            logInfoMessage("Added to git: " + status.getAdded());

            CommitCommand commitCommand = git.commit();
            commitCommand.setAuthor("com.rogaikopytov", "rogaikopytov@rogaikopytov.com");
            commitCommand.setCommitter("com.rogaikopytov", "rogaikopytov@rogaikopytov.com");
            commitCommand.setMessage("Report: automatical test report by RogaIKopytov plugin");
            commitCommand.call();

            logInfoMessage("Report commited");
        } catch (GitAPIException | IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        logInfoMessage("Report goal finished");
    }

    private boolean deleteIfExists(Path path) {
        try {
            if (Files.exists(path)) {
                FileUtils.forceDelete(path.toFile());
                return true;
            }
        } catch (IOException e) {
            getLog().error("Deletion failed", e);
        }
        return false;
    }

    private void logInfoMessage(String message) {
        getLog().info(String.format("%s: %s", logPrefix, message));
    }
}
