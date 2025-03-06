package com.rogaiopytov;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

@Mojo(name = "music", threadSafe = true)
@Execute(goal = "build")
public class MusicMojo extends AbstractMojo {

	@Parameter(property = "soundFile", required = true)
	private String soundFile;

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
			playMp3File(file);
		} else {
			throw new MojoExecutionException("Unsupported audio file format. Only .wav and .mp3 are supported.");
		}
	}

	private void playWavFile(File wavFile) throws MojoExecutionException {
		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(wavFile));
				AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bis)) {
			Clip clip = AudioSystem.getClip();
			clip.open(audioInputStream);
			getLog().info("Playing WAV file: " + wavFile.getAbsolutePath());
			clip.start();
			while (!clip.isRunning())
				Thread.sleep(10);
			while (clip.isRunning())
				Thread.sleep(10);
			clip.close();
		} catch (UnsupportedAudioFileException e) {
			throw new MojoExecutionException("The specified audio file is not supported: " + wavFile.getAbsolutePath(), e);
		} catch (LineUnavailableException e) {
			throw new MojoExecutionException("Audio line for playing back is unavailable.", e);
		} catch (IOException e) {
			throw new MojoExecutionException("I/O Error playing WAV file: " + wavFile.getAbsolutePath(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MojoExecutionException("Playback interrupted for WAV file: " + wavFile.getAbsolutePath(), e);
		}
	}

	private void playMp3File(File mp3File) throws MojoExecutionException {
		try (FileInputStream fis = new FileInputStream(mp3File); BufferedInputStream bis = new BufferedInputStream(fis)) {
			getLog().info("Playing MP3 file: " + mp3File.getAbsolutePath());
			Player player = new Player(bis);
			player.play();
		} catch (JavaLayerException e) {
			throw new MojoExecutionException("Error playing MP3 file: " + mp3File.getAbsolutePath(), e);
		} catch (Exception e) {
			throw new MojoExecutionException("Error reading MP3 file: " + mp3File.getAbsolutePath(), e);
		}
	}
}
