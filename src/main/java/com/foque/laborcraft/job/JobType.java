package com.foque.laborcraft.job;

public record JobType(String id, String translationKey) {
	public String shortName() {
		int separator = id.indexOf(':');
		return separator < 0 ? id : id.substring(separator + 1);
	}
}
