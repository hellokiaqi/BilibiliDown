package nicelee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import nicelee.bilibili.INeedAV;
import nicelee.bilibili.model.FavList;
import nicelee.bilibili.util.Logger;
import nicelee.ui.item.JOptionPane;
import nicelee.ui.item.MJButton;
import nicelee.ui.item.MJButton.Style;
import nicelee.ui.item.MJTabVideo;
import nicelee.ui.item.MJTextField;
import nicelee.ui.thread.GetVideoDetailThread;
import nicelee.ui.thread.LoginThread;

public class TabIndex extends JPanel implements ActionListener, MouseListener, ItemListener {

	private static final long serialVersionUID = -5829023045158490349L;

	private static final String FAV_PLACEHOLDER = "---我的收藏夹---";

	public ImageIcon imgIconHeaderDefault = new ImageIcon(this.getClass().getResource("/resources/header.png"));
	public JLabel jlHeader;
	public JComboBox<Object> cmbFavList = new JComboBox<>();
	public JComboBox<Integer> cmbPageSize = new JComboBox<>();
	String placeHolder = "请输入视频/UP/收藏夹链接，或 BV/av/ep/ss/md/ml 号";
	JTextField txtSearch = new MJTextField(placeHolder);
	JButton btnSearch = new MJButton("查找", Style.PRIMARY);
	JButton btnSearchNextPage = new MJButton("下一页");
	JButton btnQuickDownload = new MJButton("UP 全部视频");

	JTabbedPane jTabbedpane;

	public TabIndex(JTabbedPane jTabbedpane) {
		this.jTabbedpane = jTabbedpane;
		init();
	}

	public void init() {
		this.setOpaque(false);
		this.setLayout(new BorderLayout());
		this.setBorder(Theme.padding(16, 18, 8, 18));

		JPanel content = new JPanel(new GridBagLayout());
		content.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 0;
		content.add(buildHeaderCard(), c);
		c.gridy = 1;
		c.insets = new Insets(14, 0, 0, 0);
		content.add(buildSearchCard(), c);
		c.gridy = 2;
		c.insets = new Insets(12, 0, 0, 0);
		content.add(buildHintCard(), c);
		c.gridy = 3;
		c.weighty = 1;
		content.add(Box.createGlue(), c);

		this.add(content, BorderLayout.CENTER);
		installTabShortcuts();
	}

	/** 顶部信息区: 头像 + 名称 + 登录提示 */
	private JPanel buildHeaderCard() {
		Theme.CardPanel card = new Theme.CardPanel(new BorderLayout(18, 0));
		card.setBorder(Theme.padding(16, 20, 16, 20));

		imgIconHeaderDefault = Theme.avatarPlaceholder(64);
		jlHeader = new JLabel(imgIconHeaderDefault);
		jlHeader.setPreferredSize(new Dimension(64, 64));
		jlHeader.setHorizontalAlignment(SwingConstants.CENTER);
		jlHeader.setToolTipText("点击登录");
		jlHeader.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		jlHeader.addMouseListener(this);

		JPanel brand = new JPanel(new GridBagLayout());
		brand.setOpaque(false);
		GridBagConstraints bc = new GridBagConstraints();
		bc.gridx = 0;
		bc.weightx = 1;
		bc.anchor = GridBagConstraints.WEST;
		bc.fill = GridBagConstraints.NONE;

		JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		nameRow.setOpaque(false);
		JLabel name = Theme.title("BilibiliDown", 24);
		nameRow.add(name);
		JLabel version = new JLabel(Global.version);
		version.setFont(Theme.font(Font.PLAIN, 11));
		version.setForeground(Theme.TEXT_MUTED);
		version.setBorder(Theme.padding(10, 0, 0, 0));
		nameRow.add(version);
		bc.gridy = 0;
		brand.add(nameRow, bc);

		JLabel subtitle = new JLabel("B 站视频解析与批量下载 · 支持视频 / 番剧 / UP 全部投稿 / 收藏夹");
		subtitle.setFont(Theme.font(Font.PLAIN, 13));
		subtitle.setForeground(Theme.TEXT_SECOND);
		subtitle.setBorder(Theme.padding(6, 0, 0, 0));
		bc.gridy = 1;
		brand.add(subtitle, bc);

		JLabel loginHint = new JLabel("未登录时清晰度与列表可能受限，点击左侧头像登录");
		loginHint.setFont(Theme.font(Font.PLAIN, 12));
		loginHint.setForeground(Theme.TEXT_MUTED);
		loginHint.setBorder(Theme.padding(6, 0, 0, 0));
		bc.gridy = 2;
		brand.add(loginHint, bc);

		card.add(jlHeader, BorderLayout.WEST);
		card.add(brand, BorderLayout.CENTER);
		return card;
	}

	/** 搜索区: 第一行输入与主操作，第二行分页/收藏夹与 UP 全部视频 */
	private JPanel buildSearchCard() {
		Theme.CardPanel card = new Theme.CardPanel(new BorderLayout(0, 12));
		card.setBorder(Theme.padding(16, 20, 16, 20));

		// 第一行: 输入框 + 查找 + 下一页
		JPanel row1 = new JPanel(new BorderLayout(10, 0));
		row1.setOpaque(false);

		JLabel inputLabel = new JLabel("链接或 ID");
		inputLabel.setFont(Theme.font(Font.PLAIN, 12));
		inputLabel.setForeground(Theme.TEXT_SECOND);
		inputLabel.setPreferredSize(new Dimension(66, 36));
		row1.add(inputLabel, BorderLayout.WEST);

		txtSearch.setPreferredSize(new Dimension(560, 36));
		Theme.styleField(txtSearch);
		row1.add(txtSearch, BorderLayout.CENTER);

		JPanel primaryActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		primaryActions.setOpaque(false);
		btnSearch.setPreferredSize(new Dimension(92, 36));
		btnSearch.addActionListener(this);
		primaryActions.add(btnSearch);
		btnSearchNextPage.setPreferredSize(new Dimension(92, 36));
		btnSearchNextPage.setToolTipText("在输入内容后追加分页参数，用于翻页");
		btnSearchNextPage.addActionListener(this);
		primaryActions.add(btnSearchNextPage);
		row1.add(primaryActions, BorderLayout.EAST);

		// 第二行: 每页数量 + 收藏夹 + UP 全部视频
		JPanel row2 = new JPanel(new BorderLayout(12, 0));
		row2.setOpaque(false);

		JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		filters.setOpaque(false);

		JLabel pageSizeLabel = new JLabel("每页数量");
		pageSizeLabel.setFont(Theme.font(Font.PLAIN, 12));
		pageSizeLabel.setForeground(Theme.TEXT_SECOND);
		filters.add(pageSizeLabel);

		cmbPageSize.setPreferredSize(new Dimension(96, 32));
		Theme.styleCombo(cmbPageSize);
		initPageSizeCombo();
		filters.add(cmbPageSize);

		JLabel favLabel = new JLabel("收藏夹");
		favLabel.setFont(Theme.font(Font.PLAIN, 12));
		favLabel.setForeground(Theme.TEXT_SECOND);
		favLabel.setBorder(Theme.padding(0, 12, 0, 0));
		filters.add(favLabel);

		cmbFavList.addItem(FAV_PLACEHOLDER);
		cmbFavList.setPreferredSize(new Dimension(200, 32));
		cmbFavList.setToolTipText("登录后这里会列出你的收藏夹与稍后再看");
		Theme.styleCombo(cmbFavList);
		cmbFavList.addItemListener(this);
		filters.add(cmbFavList);
		row2.add(filters, BorderLayout.WEST);

		JPanel quick = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		quick.setOpaque(false);
		btnQuickDownload.setPreferredSize(new Dimension(132, 32));
		btnQuickDownload.setToolTipText("加载该 UP 的全部投稿，按年份筛选后批量下载");
		btnQuickDownload.addActionListener(this);
		quick.add(btnQuickDownload);
		row2.add(quick, BorderLayout.EAST);

		card.add(row1, BorderLayout.NORTH);
		card.add(row2, BorderLayout.SOUTH);
		return card;
	}

	/** 底部提示: 支持的输入类型 + 操作提示(保持轻量，不要抢占视觉焦点) */
	private JPanel buildHintCard() {
		JPanel hint = new JPanel(new BorderLayout(0, 4));
		hint.setOpaque(false);
		hint.setBorder(Theme.padding(0, 8, 0, 8));

		JLabel line1 = new JLabel("支持 BV/av/ep/ss/md/ml 号、视频链接、UP 空间链接（或直接输入 mid）、收藏夹；按 Enter 直接搜索");
		line1.setFont(Theme.font(Font.PLAIN, 12));
		line1.setForeground(Theme.TEXT_MUTED);

		JLabel line2 = new JLabel("提示：双击标签页或鼠标中键点击标签页即可关闭");
		line2.setFont(Theme.font(Font.PLAIN, 12));
		line2.setForeground(new Color(0xA8, 0xAE, 0xB8));

		hint.add(line1, BorderLayout.NORTH);
		hint.add(line2, BorderLayout.CENTER);
		return hint;
	}

	private void initPageSizeCombo() {
		int[] sizes = new int[] { 10, 20, 30, 40, 60 };
		boolean hasDefault = false;
		for (int size : sizes) {
			cmbPageSize.addItem(size);
			if (size == Global.pageSize) {
				hasDefault = true;
			}
		}
		if (!hasDefault) {
			cmbPageSize.addItem(Global.pageSize);
		}
		cmbPageSize.setSelectedItem(Global.pageSize);
		cmbPageSize.addItemListener(this);
	}

	/** 标签页操作增强: 鼠标中键关闭非主页标签 */
	private void installTabShortcuts() {
		jTabbedpane.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON2) {
					int index = jTabbedpane.indexAtLocation(e.getX(), e.getY());
					if (index >= 2) {
						jTabbedpane.removeTabAt(index);
					}
				}
			}
		});
	}

	@Override
	public void paintComponent(Graphics g) {
		Theme.paintAppBackground(g, this);
	}

	public void closeAllVideoTabs() {
		System.out.println("当前Tab数量: " + (jTabbedpane.getTabCount() - 2));
		System.out.println("正在关闭视频Tab标签页...");
		for (int i = jTabbedpane.getTabCount() - 1; i >= 2; i--) {
			jTabbedpane.removeTabAt(i);
		}
		System.out.println("当前Tab数量: " + (jTabbedpane.getTabCount() - 2));
	}

	public void downVideoTabs(boolean downAll, int qn) {
		for (int i = 0; i < jTabbedpane.getTabCount(); i++) {
			System.out.printf("共 %d 个Tab，当前处理第 %d 个\r\n", jTabbedpane.getTabCount(), i);
			Component comp = jTabbedpane.getComponentAt(i);
			if (comp instanceof TabVideo) {
				TabVideo tabVideo = (TabVideo) comp;
				tabVideo.download(downAll, qn);
			}
		}
	}

	static Pattern paramPattern = Pattern.compile("(.*)p=([0-9]+)$");
	static Pattern spaceMidPattern = Pattern.compile("space\\.bilibili\\.com/([0-9]+)");
	static Pattern pureMidPattern = Pattern.compile("^\\d{3,}$");

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnSearch) {
			search();
		} else if (e.getSource() == btnSearchNextPage) {
			String origin = txtSearch.getText();
			int page = 1;
			String modified = null;
			Matcher matcher = paramPattern.matcher(origin);
			if (matcher.find()) {
				page = Integer.parseInt(matcher.group(2));
				modified = matcher.group(1) + "p=" + (page + 1);
			} else {
				modified = origin + " p=" + (page + 1);
			}
			txtSearch.setText(modified);
			search();
		} else if (e.getSource() == btnQuickDownload) {
			openUpLibrary();
		}
	}

	public void search() {
		String avId = txtSearch.getText();
		if (avId == null || avId.trim().isEmpty() || placeHolder.equals(avId)) {
			return;
		}
		avId = avId.trim();
		applyPageSizeFromUI();
		INeedAV iNeedAV = new INeedAV();
		avId = iNeedAV.getValidID(avId);
		Logger.println("Parsed id: " + avId);
		popVideoInfoTab(avId);
	}

	private void applyPageSizeFromUI() {
		Object sel = cmbPageSize.getSelectedItem();
		if (sel instanceof Integer) {
			Global.pageSize = (Integer) sel;
		}
	}

	private void openUpLibrary() {
		String input = txtSearch.getText().trim();
		String mid = extractMid(input);
		if (mid == null) {
			JOptionPane.showMessageDialog(this, "请输入 UP 主页链接或 mid 数字 ID", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		JLabel label = new JLabel("UP " + mid);
		label.setFont(Theme.font(Font.PLAIN, 13));
		label.setBorder(Theme.padding(0, 6, 0, 6));
		label.setToolTipText("双击关闭");
		final TabUpAll tab = new TabUpAll(mid);
		jTabbedpane.addTab("UP 视频列表", tab);
		jTabbedpane.setTabComponentAt(jTabbedpane.indexOfComponent(tab), label);
		jTabbedpane.setSelectedComponent(tab);
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2 && e.getButton() == MouseEvent.BUTTON1) {
					jTabbedpane.remove(tab);
				}
			}
		});
	}

	private String extractMid(String input) {
		if (input == null || input.isEmpty())
			return null;
		Matcher m = spaceMidPattern.matcher(input);
		if (m.find())
			return m.group(1);
		if (pureMidPattern.matcher(input).find())
			return input;
		return null;
	}

	private void popVideoInfoTab(String avId) {
		if ("".equals(avId)) {
			JOptionPane.showMessageDialog(this, "输入无效", "错误", JOptionPane.WARNING_MESSAGE);
			return;
		}
		JLabel label = new JLabel("加载中...");
		label.setFont(Theme.font(Font.PLAIN, 13));
		label.setBorder(Theme.padding(0, 6, 0, 6));
		final TabVideo tab = new MJTabVideo(jTabbedpane, label, avId);
		jTabbedpane.addTab("视频", tab);
		jTabbedpane.setTabComponentAt(jTabbedpane.indexOfComponent(tab), label);
		jTabbedpane.setSelectedComponent(tab);
		GetVideoDetailThread th = new GetVideoDetailThread(tab, avId);
		th.start();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == jlHeader) {
			Global.needToLogin = true;
			LoginThread loginTh = new LoginThread();
			loginTh.start();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getSource() == jlHeader) {
			jlHeader.setBorder(BorderFactory.createLineBorder(Theme.PRIMARY, 2));
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getSource() == jlHeader) {
			jlHeader.setBorder(null);
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() != ItemEvent.SELECTED) {
			return;
		}
		if (e.getSource() == cmbPageSize && e.getItem() instanceof Integer) {
			Global.pageSize = (Integer) e.getItem();
			return;
		}
		if (e.getSource() == cmbFavList) {
			if (e.getItem() instanceof FavList) {
				FavList fav = (FavList) e.getItem();
				String url = "https://space.bilibili.com/%s/favlist?fid=%s&ftype=create";
				url = String.format(url, fav.getOwnerId(), fav.getfId());
				txtSearch.setText(url);
				txtSearch.setForeground(Theme.TEXT);
				search();
				cmbFavList.setSelectedIndex(0);
			} else if ("稍后再看".equals(String.valueOf(e.getItem()))) {
				txtSearch.setText("https://www.bilibili.com/watchlater/#/list");
				txtSearch.setForeground(Theme.TEXT);
				search();
				cmbFavList.setSelectedIndex(0);
			}
		}
	}
}
