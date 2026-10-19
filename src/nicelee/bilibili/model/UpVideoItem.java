package nicelee.bilibili.model;

public class UpVideoItem {
	private final String bvid;
	private final String title;
	private final String pic;
	private final long created;
	private final int play;
	private final int comment;
	private final String length;

	public UpVideoItem(String bvid, String title, String pic, long created, int play, int comment, String length) {
		this.bvid = bvid;
		this.title = title;
		this.pic = pic;
		this.created = created;
		this.play = play;
		this.comment = comment;
		this.length = length;
	}

	public String getBvid() {
		return bvid;
	}

	public String getTitle() {
		return title;
	}

	public String getPic() {
		return pic;
	}

	public long getCreated() {
		return created;
	}

	public int getPlay() {
		return play;
	}

	public int getComment() {
		return comment;
	}

	public String getLength() {
		return length;
	}
}
