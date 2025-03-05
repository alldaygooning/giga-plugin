package com.rogaiopytov;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "music", threadSafe = true)
@Execute(phase = LifecyclePhase.VERIFY)
public class MusicMojo extends AbstractMojo {

	@Parameter(property = "soundFile", required = true)
	private String soundFile;

	@Component
	private MavenProject project;

	@Component
	private MavenSession session;

	@Component
	private BuildPluginManager pluginManager;

	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Starting the 'music' goal...");

		playSoundFile(soundFile);

		getLog().info("Invoking the maven-war-plugin to build the WAR archive...");
		getLog().info("WAR archive built successfully!");
	}

	private void playSoundFile(String filePath) throws MojoExecutionException {
		getLog().info("Attempting to play sound file: " + filePath);
		File file = new File(filePath);

		if (!file.exists()) {
			throw new MojoExecutionException("Sound file does not exist: " + filePath);
		}

		String lowerPath = filePath.toLowerCase(Locale.ENGLISH);
		if (lowerPath.endsWith(".wav")) {
			playWavFile(file);
		} else if (lowerPath.endsWith(".mp3")) {
//			playMp3File(file);
			getLog().info("jopa");
		} else {
			throw new MojoExecutionException("Unsupported audio file format. Only .wav and .mp3 are supported.");
		}
	}

	private void playWavFile(File wavFile) throws MojoExecutionException {
		try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(wavFile)) {
			Clip clip = AudioSystem.getClip();
			clip.open(audioStream);
			clip.start();
			getLog().info("WAV playback started.");

			long clipLength = clip.getMicrosecondLength() / 1000;
			Thread.sleep(clipLength);

			clip.close();
			getLog().info("WAV playback finished.");
		} catch (UnsupportedAudioFileException e) {
			throw new MojoExecutionException("The provided file is not a supported audio file.", e);
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to load the audio file.", e);
		} catch (LineUnavailableException e) {
			throw new MojoExecutionException("Audio line for playback is unavailable.", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MojoExecutionException("Playback was interrupted", e);
		}
	}

	/**
	 * Plays an MP3 file using JavaFX's Media and MediaPlayer. This method
	 * initializes JavaFX (if not already done) via a JFXPanel.
	 *
	 * @param mp3File the File object for the MP3 file.
	 */
//	private void playMp3File(File mp3File) throws MojoExecutionException {
//		// Initialize JavaFX runtime if needed.
//		new JFXPanel();
//
//		final CountDownLatch playbackLatch = new CountDownLatch(1);
//
//		// Convert file to URI string (required by JavaFX Media API).
//		String mediaUrl = mp3File.toURI().toString();
//
//		// Ensure JavaFX operations run on the JavaFX Application Thread.
//		Platform.runLater(() -> {
//			try {
//				Media media = new Media(mediaUrl);
//				MediaPlayer mediaPlayer = new MediaPlayer(media);
//
//				// Add handler to signal when playback is complete.
//				mediaPlayer.setOnEndOfMedia(() -> {
//					getLog().info("MP3 playback finished.");
//					mediaPlayer.dispose();
//					playbackLatch.countDown();
//				});
//
//				// Also handle errors.
//				mediaPlayer.setOnError(() -> {
//					getLog().error("Error encountered during MP3 playback: " + mediaPlayer.getError());
//					playbackLatch.countDown();
//				});
//
//				getLog().info("MP3 playback started.");
//				mediaPlayer.play();
//			} catch (Exception e) {
//				getLog().error("Error initializing MP3 playback", e);
//				playbackLatch.countDown();
//			}
//		});
//
//		try {
//			// Wait for the playback to finish; set an upper bound if needed (for example, 5
//			// minutes).
//			if (!playbackLatch.await(5, TimeUnit.MINUTES)) {
//				getLog().warn("Timeout waiting for MP3 playback to finish.");
//			}
//		} catch (InterruptedException e) {
//			Thread.currentThread().interrupt();
//			throw new MojoExecutionException("MP3 playback was interrupted", e);
//		}

}
