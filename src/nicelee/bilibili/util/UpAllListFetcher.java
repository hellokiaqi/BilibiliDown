package nicelee.bilibili.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import nicelee.bilibili.API;
import nicelee.bilibili.model.UpVideoItem;
import nicelee.ui.Global;

/**
 * 拉取 UP 主的投稿列表（数据源: x/space/wbi/arc/search）。
 *
 * 该接口对请求频率非常敏感，短时间内连续请求会返回 -799 / HTTP 412 等风控错误。
 * 因此这里做了四件事：
 * 1. 分页之间保持最小间隔，并遵守 bilibili.download.period.between.query 配置；
 * 2. 命中风控时按 1s/2s/3s 退避重试，仍失败则回退到非 wbi 旧接口再试一次；
 * 3. 失败时返回“已经拿到的部分数据 + 错误原因 + 下一页页码”，而不是静默返回空列表，
 *    界面可以据此提示用户，并在稍后从断点继续拉取；
 * 4. 常见风控错误翻译成更易理解的中文提示。
 */
public class UpAllListFetcher {

	/** 接口单页上限 */
	private static final int PAGE_SIZE = 50;
	/** 安全上限: 最多翻 200 页(1W 条)，避免异常情况下无限请求 */
	private static final int MAX_PAGE = 200;
	/** 分页之间的最小间隔，降低触发风控的概率 */
	private static final long MIN_INTERVAL_MS = 300L;
	/** 单页最多重试次数 */
	private static final int MAX_RETRY = 3;

	private static final String URL_WBI = "https://api.bilibili.com/x/space/wbi/arc/search?mid=%s&ps=%d&tid=0&pn=%d&keyword=&order=pubdate&platform=web";
	private static final String URL_PLAIN = "https://api.bilibili.com/x/space/arc/search?mid=%s&ps=%d&tid=0&pn=%d&keyword=&order=pubdate&jsonp=jsonp";

	/** 进度回调，在抓取线程中执行，不要直接操作 UI */
	public interface Listener {
		/**
		 * @param page      正在请求的页码(从 1 开始)
		 * @param total     接口给出的总数，未知时为 -1
		 * @param collected 本批次已成功获取的条数
		 */
		void onProgress(int page, int total, int collected);
	}

	/** 抓取结果: 同时携带数据与状态，避免“失败被当成空列表” */
	public static final class Result {
		private final List<UpVideoItem> items;
		private final int total;
		private final boolean complete;
		private final String error;
		private final int nextPage;

		Result(List<UpVideoItem> items, int total, boolean complete, String error, int nextPage) {
			this.items = items;
			this.total = total;
			this.complete = complete;
			this.error = error;
			this.nextPage = nextPage;
		}

		public List<UpVideoItem> getItems() {
			return items;
		}

		/** 接口给出的总数，未知时为 -1 */
		public int getTotal() {
			return total;
		}

		/** 是否完整拉取(未因风控/异常中断) */
		public boolean isComplete() {
			return complete;
		}

		/** 失败原因，成功时为 null */
		public String getError() {
			return error;
		}

		public boolean hasError() {
			return error != null;
		}

		/** 失败中断时，下一页应从第几页继续；无更多数据时为 0 */
		public int getNextPage() {
			return nextPage;
		}
	}

	private static final class Resp {
		JSONObject data;
		String error;
		boolean retryable;
	}

	private static final class Page {
		JSONObject data;
		String error;
		boolean retryable;
	}

	/** 从第 1 页开始拉取 */
	public static Result fetchAll(String mid) {
		return fetchFrom(new HttpRequestUtilEx(), mid, 1, null);
	}

	/** 从第 1 页开始拉取，带进度回调 */
	public static Result fetchAll(HttpRequestUtil util, String mid, Listener listener) {
		return fetchFrom(util, mid, 1, listener);
	}

	/**
	 * 从指定页开始拉取，便于风控中断后从断点继续。
	 *
	 * @param startPage 起始页码，从 1 开始
	 */
	public static Result fetchFrom(HttpRequestUtil util, String mid, int startPage, Listener listener) {
		final LinkedHashMap<String, UpVideoItem> map = new LinkedHashMap<>();
		final int fromPage = Math.max(1, startPage);
		int total = -1;
		int page = fromPage;
		int maxPage = MAX_PAGE;
		String error = null;
		boolean complete = false;
		boolean retryableError = false;
		final long interval = Math.max(MIN_INTERVAL_MS, Global.sleepAfterDownloadQuery);

		while (page <= maxPage) {
			Page p = requestPage(util, mid, page);
			if (p.error != null) {
				error = p.error;
				retryableError = p.retryable;
				break;
			}
			JSONObject data = p.data;
			if (data == null) {
				error = "接口返回数据为空";
				break;
			}
			JSONObject pageObj = data.optJSONObject("page");
			if (pageObj != null && total <= 0) {
				int count = pageObj.optInt("count", -1);
				if (count > 0) {
					total = count;
					maxPage = Math.min(MAX_PAGE, (total + PAGE_SIZE - 1) / PAGE_SIZE);
				}
			}
			JSONArray vlist = optVList(data);
			if (vlist == null || vlist.length() == 0) {
				// 没有更多数据了
				complete = true;
				break;
			}
			for (int i = 0; i < vlist.length(); i++) {
				JSONObject v = vlist.optJSONObject(i);
				if (v == null) {
					continue;
				}
				String bvid = v.optString("bvid", "");
				if (bvid.isEmpty() || map.containsKey(bvid)) {
					continue;
				}
				map.put(bvid, new UpVideoItem(bvid, v.optString("title", ""), v.optString("pic", ""),
						v.optLong("created", 0L) * 1000L, v.optInt("play", 0), v.optInt("comment", 0),
						v.optString("length", "")));
			}
			if (listener != null) {
				listener.onProgress(page, total, map.size());
			}
			// 已抓取条数(含起始页之前的满页)达到总数即完成
			if (total > 0 && (fromPage - 1) * PAGE_SIZE + map.size() >= total) {
				complete = true;
				break;
			}
			page++;
			if (page <= maxPage) {
				sleep(interval);
			}
		}

		if (error == null && !complete) {
			// 正常退出循环但没取完，说明触到了页数上限或总数与实际不一致
			error = "达到翻页上限";
		}
		int nextPage = 0;
		if (error != null && retryableError) {
			// 仅风控/网络类错误值得断点续拉，避免对"用户不存在"这类错误反复重试
			nextPage = page;
		}
		return new Result(new ArrayList<>(map.values()), total, complete && error == null, error,
				nextPage);
	}

	private static JSONArray optVList(JSONObject data) {
		JSONObject list = data.optJSONObject("list");
		if (list == null) {
			return null;
		}
		return list.optJSONArray("vlist");
	}

	private static Page requestPage(HttpRequestUtil util, String mid, int page) {
		Page result = new Page();
		String lastError = null;
		boolean lastRetryable = false;
		for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
			Resp resp = request(util, mid, page, true);
			if (resp.data != null) {
				result.data = resp.data;
				return result;
			}
			lastError = resp.error;
			lastRetryable = resp.retryable;
			if (!resp.retryable || attempt >= MAX_RETRY) {
				break;
			}
			// 风控/网络抖动: 退避后重试
			sleep(1000L * attempt);
		}
		// 回退到旧接口(无需 wbi 签名)再试一次
		Resp plain = request(util, mid, page, false);
		if (plain.data != null) {
			result.data = plain.data;
			return result;
		}
		result.error = friendly(plain.error != null ? plain.error : lastError);
		result.retryable = plain.retryable || lastRetryable;
		return result;
	}

	private static Resp request(HttpRequestUtil util, String mid, int page, boolean useWbi) {
		Resp resp = new Resp();
		try {
			String url = String.format(useWbi ? URL_WBI : URL_PLAIN, mid, PAGE_SIZE, page);
			if (useWbi) {
				url += API.genDmImgParams();
				url = API.encWbi(url);
			}
			HashMap<String, String> headers = new HttpHeaders().getCommonHeaders("api.bilibili.com");
			headers.put("Referer", "https://space.bilibili.com/");
			headers.put("Origin", "https://space.bilibili.com/");
			String json = util.getContent(url, headers, HttpCookies.globalCookiesWithFingerprint());
			Logger.println(url);
			Logger.println(json);
			JSONObject obj = new JSONObject(json);
			int code = obj.optInt("code", 0);
			if (code == 0) {
				resp.data = obj.optJSONObject("data");
				if (resp.data == null) {
					resp.error = "接口返回 data 为空";
					resp.retryable = true;
				}
				return resp;
			}
			String message = obj.optString("message", "");
			resp.error = message.isEmpty() ? String.valueOf(code) : code + " " + message;
			resp.retryable = isRiskControl(code);
			return resp;
		} catch (Exception e) {
			String message = e.getMessage();
			resp.error = message == null ? e.toString() : message;
			// 412/403 等 HTTP 错误也按可重试处理
			resp.retryable = true;
			return resp;
		}
	}

	/** 是否属于可以退避重试的风控/限流错误 */
	private static boolean isRiskControl(int code) {
		return code == -799 || code == -352 || code == -412 || code == -509 || code == -500 || code == -10403;
	}

	/** 把接口/网络错误翻译成更容易理解的提示 */
	private static String friendly(String error) {
		if (error == null || error.isEmpty()) {
			return "未知错误";
		}
		if (error.contains("412")) {
			return "触发 B 站风控(412)，请稍后点「继续加载」或登录后再试";
		}
		if (error.contains("-352")) {
			return "触发 B 站风控(-352)，请稍后点「继续加载」";
		}
		if (error.contains("-799")) {
			return "请求过于频繁(-799)，请稍后点「继续加载」";
		}
		if (error.contains("-509")) {
			return "请求超出限制(-509)，请稍后点「继续加载」";
		}
		return error;
	}

	private static void sleep(long ms) {
		if (ms <= 0) {
			return;
		}
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
