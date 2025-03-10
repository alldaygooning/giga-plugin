package com.rogaiopytov;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BuildMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

	@Parameter(property = "src", defaultValue = "src")
	private String src;

	@Parameter
	private List<ManifestEntry> manifestEntries;

	private final String logPrefix = "Build Goal";


	@Override
    public void execute() throws MojoExecutionException {
		this.src = String.format("%s/%s", this.project.getBasedir().toString(), this.src);
        getLog().info(String.format("%s: Using src directory: %s", logPrefix, src));

        executeMojo(
            plugin(
                groupId("com.RogaIKopytov"),
                artifactId("demo-plugin-eclipse"),
                version("1.0")
            ),
            goal("compile"),
            configuration(
                element("src", src)
            ),
            executionEnvironment(project, session, pluginManager)
        );

		Element manifestConfig = null;
		if (manifestEntries != null && !manifestEntries.isEmpty()) {
			List<Element> entryElements = new ArrayList<>();
			for (ManifestEntry me : manifestEntries) {
				entryElements.add(element(name(me.getName()), me.getValue()));
			}
			manifestConfig = element(name("manifestEntries"), entryElements.toArray(new Element[0]));
		}

		Element archiveConfig = null;
		if (manifestConfig != null) {
			archiveConfig = element("archive", manifestConfig);
		}

		List<Element> configElements = new ArrayList<>();
		configElements.add(element("warSourceDirectory", String.format("%s/main/webapp", this.src)));
		if (archiveConfig != null) {
			configElements.add(archiveConfig);
		}

		List<Element> webResources = new ArrayList<Element>();
		
		String buildDir = project.getBuild().getDirectory();
		File apidocsDir = new File(buildDir, "reports/apidocs");
		if (apidocsDir.exists() && apidocsDir.isDirectory()) {
			webResources.add(
					element(name("resource"), 
							element(name("directory"), apidocsDir.getAbsolutePath()),
							element(name("targetPath"), "javadoc")));
		}
		
		File localizationDir = new File(this.src, "main/resources");
        webResources.add(
            element(name("resource"),
                element(name("directory"), localizationDir.getAbsolutePath()),
                element(name("targetPath"), "WEB-INF/classes"),
                element(name("includes"),
                    element(name("include"), "**/*.properties")
                )
            )
        );

		Element webResourcesElement = element(name("webResources"), webResources.toArray(new Element[0]));
		configElements.add(webResourcesElement);

        executeMojo(
            plugin(
                groupId("org.apache.maven.plugins"),
                artifactId("maven-war-plugin"),
                version("3.4.0")
            ),
            goal("war"),
				configuration(configElements.toArray(new Element[0])),
            executionEnvironment(project, session, pluginManager)
        );
    }
}
