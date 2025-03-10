package com.rogaiopytov;

public class ManifestEntry {
	private String name;
	private String value;

	public ManifestEntry() {
	}

	public ManifestEntry(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("%s : %s", this.name, this.value);
	}
}
