package com.rogaiopytov;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "diff", threadSafe = true)
public class DiffMojo extends AbstractMojo {

    private final String logPrefix = "Diff Goal";

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

    @Parameter(property = "configFile", defaultValue = "${project.basedir}/.gitnotice")
    private String configFileParam;

    @Override
    public void execute() throws MojoExecutionException {
        logInfoMessage("Report goal started");

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

            logInfoMessage("Parsing config file");

            Path configFile = getAbsolutePath(configFileParam, project.getBasedir().getAbsolutePath());

            if (!Files.exists(configFile) || !Files.isReadable(configFile)) {
                throw new MojoExecutionException(String.format("Config file %s does not exist or can't be read", configFile));
            }

            Set<Path> configLines = new HashSet<>();
            try (Stream<String> lines = Files.lines(configFile)) {
                lines.forEach(line -> configLines.add(getAbsolutePath(line, project.getBasedir().getAbsolutePath())));
            }

            // clear staging area before plugin operations, preserving changes (git restore --staged)
            git.reset().setMode(ResetCommand.ResetType.MIXED).call();

            Status status = git.status().call();

            Set<String> filesSet = new HashSet<>();
            filesSet.addAll(status.getUncommittedChanges());

            Set<Path> filteredFilesSet = filterPaths(configLines, filesSet);

            if (filteredFilesSet.isEmpty()) {
                logInfoMessage("No changes in specified files detected, stopping");
                return;
            }

            AddCommand addCommand = git.add();
            // now it's impossible to track untracked and deleted files at the same time, I decided to track deleted
            addCommand.setUpdate(true);
            for (Path p : filteredFilesSet) {
                logInfoMessage("Add file: " + p);
                addCommand.addFilepattern(project.getBasedir().toPath().relativize(p).toString());
            }
            addCommand.call();



            CommitCommand commitCommand = git.commit();
            commitCommand.setAuthor("com.rogaikopytov", "rogaikopytov@rogaikopytov.com");
            commitCommand.setCommitter("com.rogaikopytov", "rogaikopytov@rogaikopytov.com");
            commitCommand.setMessage("Diff: automatical diff commit by RogaIKopytov plugin");
            commitCommand.call();

            logInfoMessage("Diff commited");
        } catch (GitAPIException | IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        logInfoMessage("Diff goal finished");
    }

    private Path getAbsolutePath(String pathStr, String base) {
        Path path = Path.of(pathStr);
        if (path.isAbsolute()) {
            return path;
        } else {
            return Paths.get(base, pathStr);
        }
    }

    private Set<Path> filterPaths(Set<Path> filters, Set<String> pathsStr) {
        Set<Path> paths = pathsStr.stream()
                .map(line -> getAbsolutePath(line, project.getBasedir().getAbsolutePath()))
                .collect(Collectors.toSet());

        Set<Path> normalizedFilters = new HashSet<>();
        for (Path filter : filters) {
            Path normalized = filter.toAbsolutePath().normalize();
            normalizedFilters.add(normalized);
        }

        Set<Path> result = new HashSet<>();
        for (Path path : paths) {
            Path normalizedPath = path.toAbsolutePath().normalize();

            if (isIncluded(normalizedPath, normalizedFilters)) {
                result.add(path);
            }
        }
        return result;
    }

    private boolean isIncluded(Path normalizedPath, Set<Path> normalizedFilters) {
        if (normalizedFilters.contains(normalizedPath)) {
            return true;
        }

        Path parent = normalizedPath.getParent();
        while (parent != null) {
            if (normalizedFilters.contains(parent)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private void logInfoMessage(String message) {
        getLog().info(String.format("%s: %s", logPrefix, message));
    }
}
