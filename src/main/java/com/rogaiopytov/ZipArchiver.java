package com.rogaiopytov;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipArchiver {

	public void archiveWarFiles(String directoryPath) throws IOException {
		if (directoryPath == null || directoryPath.trim().isEmpty()) {
			throw new IllegalArgumentException("Directory path must not be null or empty");
		}

		File dir = new File(directoryPath);

		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("The provided path is not a directory: " + directoryPath);
		}

		File[] warFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".war"));

		if (warFiles == null || warFiles.length == 0) {
			return;
		}

		File zipFile = new File(dir, "giga-archive.zip");

		try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {

			byte[] buffer = new byte[1024];
			for (File warFile : warFiles) {
				try (FileInputStream fis = new FileInputStream(warFile)) {
					ZipEntry zipEntry = new ZipEntry(warFile.getName());
					zos.putNextEntry(zipEntry);

					int len;
					while ((len = fis.read(buffer)) > 0) {
						zos.write(buffer, 0, len);
					}
					zos.closeEntry();
				}
			}
		}
	}
}
