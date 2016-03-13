package com.neusoft.fish.note;

public class NoteItem {
	private String time;
	private String title;
	private String content;

	private NoteItem(String time, String title, String content) {
		this.time = time;
		this.title = title;
		this.content = content;
	}

	public String getTime() {
		return time;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}

	public static class Builder {
		private String time;
		private String title;
		private String content;

		public Builder setTime(String time) {
			this.time = time;
			return this;
		}

		public Builder setTitle(String title) {
			this.title = title;
			return this;
		}

		public Builder setContent(String content) {
			this.content = content;
			return this;
		}

		public NoteItem build() {
			return new NoteItem(this.time, this.title, this.content);
		}
	}
}
