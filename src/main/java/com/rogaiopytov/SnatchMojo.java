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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

@Mojo(name = "snatch", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class SnatchMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

    @Override
    public void execute() throws MojoExecutionException {
        Repository repository = null;
        Git git = null;
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            repository = builder.findGitDir(new File("."))
                                .readEnvironment() 
                                .build();

            git = new Git(repository);

            ObjectId originalHead = repository.resolve("HEAD");
            if (originalHead == null) {
                throw new MojoExecutionException("Cannot resolve current HEAD commit.");
            }

            String[] commitRefs = {"HEAD~3", "HEAD~2", "HEAD~1", "HEAD"};

            String initialFinalName = project.getBuild().getFinalName();

            for (String ref : commitRefs) {
                getLog().info("Resetting repository to " + ref);
                ObjectId refObject = repository.resolve(ref);
                if (refObject == null) {
                    throw new MojoExecutionException("Cannot resolve reference: " + ref);
                }
                
                git.reset()
                   .setMode(ResetCommand.ResetType.HARD)
                   .setRef(ref)
                   .call();
                
                String modifiedFinalName = String.format("%s-%s", initialFinalName, ref.replace("~", ""));
                getLog().info("Executing build goal on " + ref + " with finalName: " + modifiedFinalName);
                executeMojo(
                        plugin(
                            groupId("com.RogaIKopytov"),
                            artifactId("rik-maven-plugin"),
                            version("1.0")
                        ),
                        goal("build"),
                        configuration(
                                element("finalName", modifiedFinalName)
                        ),
                        executionEnvironment(project, session, pluginManager)
                );
                
                project.getBuild().setFinalName(initialFinalName);
                
                getLog().info("Resetting repository back to original HEAD: " + originalHead.getName());
                git.reset()
                   .setMode(ResetCommand.ResetType.HARD)
                   .setRef(originalHead.getName())
                   .call();
            }

			ZipArchiver zipArchiver = new ZipArchiver();
			zipArchiver.archiveWarFiles(project.getBuild().getDirectory());
        } catch (Exception e) {
            throw new MojoExecutionException("Error during repository reset/build execution", e);
        } finally {
            if (git != null) {
                git.close();
            }
            if (repository != null) {
                repository.close();
            }
        }
    }
}
