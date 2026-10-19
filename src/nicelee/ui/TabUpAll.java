package nicelee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;

import nicelee.bilibili.INeedAV;
import nicelee.bilibili.enums.VideoQualityEnum;
import nicelee.bilibili.model.ClipInfo;
import nicelee.bilibili.model.UpVideoItem;
import nicelee.bilibili.model.VideoInfo;
import nicelee.bilibili.util.HttpRequestUtilEx;
import nicelee.bilibili.util.Logger;
import nicelee.bilibili.util.UpAllListFetcher;
import nicelee.ui.item.MJButton;
import nicelee.ui.Theme;
import nicelee.ui.thread.DownloadRunnable;

/**
 * UP 主全部投稿的浏览与批量下载。
 *
 * 列表用 JTable 承载: UP 主动辄上千条投稿，逐个 JPanel 会明显卡顿；
 * JTable 支持排序、按年份筛选，勾选状态独立保存，切换筛选不会丢失。
 */
public class TabUpAll extends JPanel {

	private static final long serialVersionUID = 6337826020936151565L;

	private static final String YEAR_ALL = "全部年份";
	private static final String YEAR_UNKNOWN = "年份未知";

	private final String mid;
	private final UpTableModel model = new UpTableModel();
	private final Set<String> checkedIds = new HashSet<>();
	private final Set<String> queuedIds = new HashSet<>();

	private final JTable table = new JTable(model);
	private final TableRowSorter<UpTableModel> sorter = new TableRowSorter<>(model);
	private final JComboBox<String> cmbYear = new JComboBox<>();
	private final JComboBox<String> cmbQn = new JComboBox<>();
	private final MJButton btnSelectAll = new MJButton("全选");
	private final MJButton btnClear = new MJButton("清空");
	private final MJButton btnDownloadSelected = new MJButton("下载所选");
	private final MJButton btnReload = new MJButton("刷新");
	private final MJButton btnResume = new MJButton("继续加载");
	private final JLabel lbStatus = new JLabel("准备加载...");

	private boolean reloadingYearCombo = false;
	private volatile boolean loading = false;
	private volatile boolean queueing = false;
	private String lastError = null;
	private int nextPage = 1;

	public TabUpAll(String mid) {
		this.mid = mid;
		initUI();
		loadAll();
	}

	private void initUI() {
		setLayout(new BorderLayout(0, 10));
		setBorder(new EmptyBorder(16, 18, 10, 18));
		setOpaque(false);

		add(buildTopBar(), BorderLayout.NORTH);
		add(buildTable(), BorderLayout.CENTER);
		add(buildStatusBar(), BorderLayout.SOUTH);
		updateStatus();
	}

	@Override
	public void paintComponent(Graphics g) {
		Theme.paintAppBackground(g, this);
	}

	private JPanel buildTopBar() {
		Theme.CardPanel top = new Theme.CardPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
		top.setBorder(Theme.padding(2, 6, 2, 6));

		JLabel title = new JLabel("UP " + mid);
		title.setFont(Theme.font(Font.BOLD, 15));
		title.setForeground(Theme.TEXT);
		title.setBorder(Theme.padding(0, 0, 0, 8));
		top.add(title);

		cmbYear.setPreferredSize(new Dimension(124, 30));
		Theme.styleCombo(cmbYear);
		cmbYear.addItemListener(e -> {
			if (!reloadingYearCombo && e.getStateChange() == ItemEvent.SELECTED) {
				applyYearFilter();
			}
		});
		top.add(label("年份"));
		top.add(cmbYear);

		for (VideoQualityEnum item : VideoQualityEnum.values()) {
			cmbQn.addItem(item.getQuality());
		}
		cmbQn.setSelectedItem(Global.menu_qn);
		if (cmbQn.getSelectedItem() == null) {
			cmbQn.setSelectedIndex(0);
		}
		cmbQn.setPreferredSize(new Dimension(100, 30));
		Theme.styleCombo(cmbQn);
		JLabel qnLabel = label("清晰度");
		qnLabel.setBorder(Theme.padding(0, 8, 0, 0));
		top.add(qnLabel);
		top.add(cmbQn);

		Dimension btnSize = new Dimension(76, 30);
		btnSelectAll.setPreferredSize(btnSize);
		btnClear.setPreferredSize(btnSize);
		btnDownloadSelected.setPreferredSize(new Dimension(108, 30));
		btnDownloadSelected.setStyle(MJButton.Style.PRIMARY);
		btnReload.setPreferredSize(btnSize);
		btnResume.setPreferredSize(new Dimension(100, 30));
		btnResume.setEnabled(false);
		btnSelectAll.addActionListener(e -> applySelection(true));
		btnClear.addActionListener(e -> applySelection(false));
		btnDownloadSelected.addActionListener(e -> downloadSelected());
		btnReload.addActionListener(e -> loadAll());
		btnResume.addActionListener(e -> loadMore());

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		buttons.setOpaque(false);
		buttons.add(btnSelectAll);
		buttons.add(btnClear);
		buttons.add(btnDownloadSelected);
		buttons.add(btnReload);
		buttons.add(btnResume);
		// 让按钮组靠右
		top.add(buttons);
		return top;
	}

	private static JLabel label(String text) {
		JLabel label = new JLabel(text);
		label.setFont(Theme.font(Font.PLAIN, 12));
		label.setForeground(Theme.TEXT_SECOND);
		return label;
	}

	private Component buildTable() {
		table.setRowSorter(sorter);
		table.setIntercellSpacing(new Dimension(0, 0));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setFillsViewportHeight(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		table.getTableHeader().setReorderingAllowed(false);
		Theme.styleTable(table);
		table.setDefaultRenderer(Object.class, new StripeRenderer());
		table.setDefaultRenderer(Boolean.class, new CheckBoxRenderer());

		int[] widths = { 52, 520, 100, 68, 92, 132 };
		for (int i = 0; i < widths.length; i++) {
			table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
		}
		// 勾选列表头居中
		final TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
		table.getColumnModel().getColumn(0).setHeaderRenderer(new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				Component c = headerRenderer.getTableCellRendererComponent(t, value, isSelected, hasFocus, row,
						column);
				if (c instanceof JLabel) {
					((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
				}
				return c;
			}
		});
		// 时长/播放量为格式化文本，排序无意义，直接禁用
		sorter.setSortable(3, false);
		sorter.setSortable(4, false);
		sorter.setSortKeys(java.util.Collections.singletonList(new javax.swing.RowSorter.SortKey(2,
				javax.swing.SortOrder.DESCENDING)));

		// 单击勾选列即可切换，避免依赖默认编辑器的双击行为
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				int col = table.columnAtPoint(e.getPoint());
				if (row >= 0 && col == 0) {
					model.toggleChecked(table.convertRowIndexToModel(row));
					updateStatus();
				}
			}
		});

		JScrollPane scroll = new JScrollPane(table);
		scroll.getViewport().setBackground(Color.WHITE);
		scroll.setBorder(null);

		Theme.CardPanel card = new Theme.CardPanel(new BorderLayout());
		card.setBorder(Theme.padding(8, 8, 8, 8));
		card.add(scroll, BorderLayout.CENTER);
		return card;
	}

	/** 斑马纹渲染: 长列表更易读 */
	private class StripeRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (!isSelected) {
				c.setBackground(row % 2 == 0 ? Color.WHITE : Theme.ROW_ALT);
			}
			setBorder(Theme.padding(0, 8, 0, 8));
			setForeground(column == 5 ? Theme.TEXT_MUTED : Theme.TEXT);
			return c;
		}
	}

	/** 勾选列居中显示 */
	private class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {
		private static final long serialVersionUID = 1L;

		CheckBoxRenderer() {
			setOpaque(true);
			setHorizontalAlignment(SwingConstants.CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			setSelected(Boolean.TRUE.equals(value));
			setBackground(isSelected ? table.getSelectionBackground()
					: (row % 2 == 0 ? Color.WHITE : Theme.ROW_ALT));
			return this;
		}
	}

	private Component buildStatusBar() {
		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		bottom.setOpaque(false);
		lbStatus.setFont(Theme.font(Font.PLAIN, 12));
		lbStatus.setForeground(Theme.TEXT_SECOND);
		bottom.add(lbStatus);
		return bottom;
	}

	private void loadAll() {
		startFetch(1, false);
	}

	private void loadMore() {
		if (nextPage <= 0) {
			return;
		}
		startFetch(nextPage, true);
	}

	private void startFetch(final int startPage, final boolean append) {
		if (loading) {
			return;
		}
		loading = true;
		lastError = null;
		if (!append) {
			queuedIds.clear();
		}
		setControlsEnabled(false);
		setStatus(append ? String.format("正在从第 %d 页继续加载...", startPage)
				: "正在加载 UP " + mid + " 的投稿列表...", false);
		new Thread(() -> {
			UpAllListFetcher.Result result = UpAllListFetcher.fetchFrom(new HttpRequestUtilEx(), mid, startPage,
					(page, total, collected) -> {
						String text = total > 0
								? String.format("正在加载第 %d 页... 已获取 %d/%d 条", page, collected, total)
								: String.format("正在加载第 %d 页... 已获取 %d 条", page, collected);
						SwingUtilities.invokeLater(() -> setStatus(text, false));
					});
			SwingUtilities.invokeLater(() -> applyResult(result, append));
		}, "UpAllListFetcher").start();
	}

	private void applyResult(UpAllListFetcher.Result result, boolean append) {
		loading = false;
		setControlsEnabled(true);
		if (append) {
			model.mergeItems(result.getItems());
		} else {
			model.setItems(result.getItems());
		}

		// 只保留仍然存在的勾选项
		Set<String> present = new HashSet<>();
		for (UpVideoItem item : model.getItems()) {
			present.add(item.getBvid());
		}
		checkedIds.retainAll(present);

		rebuildYearCombo();
		nextPage = result.getNextPage();
		lastError = result.hasError() ? result.getError() : null;
		applyYearFilter();
		refreshResumeButton();
		if (model.getRowCount() == 0 && lastError == null) {
			setStatus("没有加载到视频，请确认该 UP 是否有公开投稿", true);
		}
	}

	private void rebuildYearCombo() {
		Set<Integer> years = new TreeSet<>(Comparator.reverseOrder());
		for (int row = 0; row < model.getRowCount(); row++) {
			years.add(model.getYear(row));
		}
		reloadingYearCombo = true;
		try {
			cmbYear.removeAllItems();
			cmbYear.addItem(YEAR_ALL);
			for (Integer year : years) {
				cmbYear.addItem(year <= 0 ? YEAR_UNKNOWN : String.valueOf(year));
			}
			cmbYear.setSelectedIndex(0);
		} finally {
			reloadingYearCombo = false;
		}
	}

	private void applyYearFilter() {
		String year = (String) cmbYear.getSelectedItem();
		if (year == null || YEAR_ALL.equals(year)) {
			sorter.setRowFilter(null);
		} else {
			final int target = YEAR_UNKNOWN.equals(year) ? 0 : Integer.parseInt(year);
			sorter.setRowFilter(new RowFilter<UpTableModel, Integer>() {
				@Override
				public boolean include(Entry<? extends UpTableModel, ? extends Integer> entry) {
					return entry.getModel().getYear(entry.getIdentifier()) == target;
				}
			});
		}
		updateStatus();
	}

	private static int yearOf(UpVideoItem item) {
		long time = item.getCreated();
		if (time <= 0) {
			return 0;
		}
		java.util.Calendar calendar = java.util.Calendar.getInstance();
		calendar.setTimeInMillis(time);
		return calendar.get(java.util.Calendar.YEAR);
	}

	private void applySelection(boolean selected) {
		List<Integer> rows = new ArrayList<>(table.getRowCount());
		for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
			rows.add(table.convertRowIndexToModel(viewRow));
		}
		model.setChecked(rows, selected);
		updateStatus();
	}

	private void downloadSelected() {
		if (queueing) {
			return;
		}
		final List<UpVideoItem> targets = new ArrayList<>();
		for (UpVideoItem item : model.getItems()) {
			String bvid = item.getBvid();
			if (checkedIds.contains(bvid) && !queuedIds.contains(bvid)) {
				targets.add(item);
			}
		}
		if (targets.isEmpty()) {
			setStatus(checkedIds.isEmpty() ? "请先勾选要下载的视频" : "已勾选的视频都在下载队列中了", true);
			return;
		}
		queueing = true;
		setControlsEnabled(false);
		int selectedQn = VideoQualityEnum.getQN(String.valueOf(cmbQn.getSelectedItem()));
		final int qn = selectedQn > 0 ? selectedQn : VideoQualityEnum.Q1080P.getQn();
		new Thread(() -> {
			INeedAV ina = new INeedAV();
			final long interval = Math.max(200L, Global.sleepAfterDownloadQuery);
			int success = 0;
			int failed = 0;
			for (int i = 0; i < targets.size(); i++) {
				UpVideoItem item = targets.get(i);
				final String text = String.format("正在加入下载队列 %d/%d: %s", i + 1, targets.size(), item.getBvid());
				SwingUtilities.invokeLater(() -> setStatus(text, false));
				try {
					VideoInfo avInfo = ina.getVideoDetail(item.getBvid(), Global.downloadFormat, false);
					if (avInfo != null && avInfo.getClips() != null && !avInfo.getClips().isEmpty()) {
						for (ClipInfo clip : avInfo.getClips().values()) {
							Global.queryThreadPool.execute(new DownloadRunnable(avInfo, clip, qn));
						}
						queuedIds.add(item.getBvid());
						success++;
					} else {
						failed++;
						Logger.println("解析失败(无分P): " + item.getBvid());
					}
				} catch (Exception e) {
					failed++;
					Logger.println("解析失败: " + item.getBvid() + " " + e);
				}
				if (i + 1 < targets.size()) {
					sleep(interval);
				}
			}
			final int successCount = success;
			final int failedCount = failed;
			SwingUtilities.invokeLater(() -> {
				queueing = false;
				setControlsEnabled(true);
				refreshResumeButton();
				if (failedCount == 0) {
					setStatus(String.format("已加入下载队列 %d 个视频", successCount), false);
				} else {
					setStatus(String.format("已加入下载队列 %d 个视频，失败 %d 个", successCount, failedCount), true);
				}
				switchToDownloadTab();
			});
		}, "UpAll-Download").start();
	}

	private void switchToDownloadTab() {
		try {
			if (Global.tabs != null && Global.downloadTab != null) {
				Global.tabs.setSelectedComponent(Global.downloadTab);
			}
		} catch (Exception e) {
			// 忽略: 下载页不可用时不影响任务已入队
		}
	}

	private void setControlsEnabled(boolean enabled) {
		btnSelectAll.setEnabled(enabled);
		btnClear.setEnabled(enabled);
		btnDownloadSelected.setEnabled(enabled);
		btnReload.setEnabled(enabled);
		cmbYear.setEnabled(enabled);
		cmbQn.setEnabled(enabled);
		if (!enabled) {
			btnResume.setEnabled(false);
		}
	}

	/** 只有上次加载中断且还有后续页时，「继续加载」才可用 */
	private void refreshResumeButton() {
		btnResume.setEnabled(!loading && !queueing && lastError != null && nextPage > 0);
	}

	private void updateStatus() {
		String counts = String.format("共 %d 条  |  当前显示 %d 条  |  已选 %d 条", model.getRowCount(), table.getRowCount(),
				checkedIds.size());
		if (loading || queueing) {
			return; // 加载/入队过程中保留进度文案
		}
		if (lastError != null) {
			lbStatus.setText(lastError + "  |  " + counts);
			lbStatus.setForeground(Theme.DANGER);
		} else {
			lbStatus.setText(counts);
			lbStatus.setForeground(Theme.TEXT_SECOND);
		}
	}

	private void setStatus(String text, boolean error) {
		lbStatus.setText(text);
		lbStatus.setForeground(error ? Theme.DANGER : Theme.TEXT_SECOND);
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

	/** 表格模型: 勾选状态由外部 checkedIds 统一保存 */
	private final class UpTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private final String[] columns = { "选择", "标题", "发布日期", "时长", "播放量", "BV号" };
		private final List<UpVideoItem> items = new ArrayList<>();
		private int[] years = new int[0];

		void setItems(List<UpVideoItem> newItems) {
			items.clear();
			items.addAll(newItems);
			rebuildYears();
			fireTableDataChanged();
		}

		/** 断点续拉: 追加数据并按 BV 号去重 */
		void mergeItems(List<UpVideoItem> newItems) {
			Set<String> existing = new HashSet<>();
			for (UpVideoItem item : items) {
				existing.add(item.getBvid());
			}
			boolean changed = false;
			for (UpVideoItem item : newItems) {
				if (existing.add(item.getBvid())) {
					items.add(item);
					changed = true;
				}
			}
			if (changed) {
				rebuildYears();
				fireTableDataChanged();
			}
		}

		List<UpVideoItem> getItems() {
			return items;
		}

		UpVideoItem getItem(int row) {
			return items.get(row);
		}

		/** 投稿年份，预计算避免每次筛选都解析日期 */
		int getYear(int row) {
			return years[row];
		}

		void toggleChecked(int row) {
			UpVideoItem item = items.get(row);
			if (!checkedIds.remove(item.getBvid())) {
				checkedIds.add(item.getBvid());
			}
			fireTableCellUpdated(row, 0);
		}

		/** 批量设置勾选，只触发一次重绘(上千条时差异明显) */
		void setChecked(List<Integer> rows, boolean checked) {
			for (Integer row : rows) {
				UpVideoItem item = items.get(row);
				if (checked) {
					checkedIds.add(item.getBvid());
				} else {
					checkedIds.remove(item.getBvid());
				}
			}
			fireTableDataChanged();
		}

		private void rebuildYears() {
			years = new int[items.size()];
			for (int i = 0; i < items.size(); i++) {
				years[i] = yearOf(items.get(i));
			}
		}

		@Override
		public int getRowCount() {
			return items.size();
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		@Override
		public String getColumnName(int column) {
			return columns[column];
		}

		@Override
		public Class<?> getColumnClass(int column) {
			return column == 0 ? Boolean.class : String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		@Override
		public Object getValueAt(int row, int column) {
			UpVideoItem item = items.get(row);
			switch (column) {
			case 0:
				return checkedIds.contains(item.getBvid());
			case 1:
				return item.getTitle();
			case 2:
				return formatDate(item.getCreated());
			case 3:
				return item.getLength() == null ? "" : item.getLength();
			case 4:
				return formatCount(item.getPlay());
			case 5:
				return item.getBvid();
			default:
				return "";
			}
		}

		private String formatDate(long millis) {
			if (millis <= 0) {
				return "";
			}
			return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(millis));
		}

		private String formatCount(int count) {
			if (count >= 100000000) {
				return String.format("%.1f亿", count / 100000000.0);
			}
			if (count >= 10000) {
				return String.format("%.1f万", count / 10000.0);
			}
			return String.valueOf(count);
		}
	}
}
