package nicelee.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import nicelee.bilibili.enums.VideoQualityEnum;
import nicelee.bilibili.model.ClipInfo;
import nicelee.bilibili.model.VideoInfo;
import nicelee.ui.item.MJButton;
import nicelee.ui.item.MJButton.Style;
import nicelee.ui.thread.DownloadRunnable;

public class TabVideo extends JPanel implements ActionListener, MouseListener {

	private static final long serialVersionUID = -5829023045158490350L;

	VideoInfo avInfo;// 保存当前Tab 的视频信息

	JLabel lbTabTitle;
	JLabel lbVideoTitle = new JLabel("标题加载中...");
	JLabel lbAvID = new JLabel(" ");
	JLabel lbBreif = new JLabel("简介加载中...");
	JLabel lbAvPrivew;
	JPanel jpContent;
	JScrollPane jpScorll;
	JComboBox<String> cbQn; // 清晰度
	JButton btnDownAll; // 批量下载
	JButton btnDownCC; // 批量下载CC字幕
	String currentDisplayPic; // 当前预览图片路径
	JPanel nextPagePanel; // 下一页面板
	JLabel jlNextPageTips; // 下一页文字提示
	protected JButton btnNextPage; // 下一页

	public TabVideo(JLabel lbTabTitle) {
		this.lbTabTitle = lbTabTitle;
		init();
	}

	public void init() {
		setOpaque(false);
		setLayout(new BorderLayout(0, 10));
		setBorder(Theme.padding(14, 16, 10, 16));

		// 顶部信息卡片: 标题/ID/简介 + 操作
		Theme.CardPanel header = new Theme.CardPanel(new BorderLayout(16, 8));
		header.setBorder(Theme.padding(12, 16, 12, 16));

		lbVideoTitle.setFont(Theme.font(Font.BOLD, 16));
		lbVideoTitle.setForeground(Theme.TEXT);
		lbVideoTitle.setToolTipText("单击复制标题");
		lbVideoTitle.addMouseListener(this);

		lbAvID.setFont(Theme.font(Font.PLAIN, 12));
		lbAvID.setForeground(Theme.PRIMARY_PRESS);
		lbAvID.setToolTipText("单击复制 ID");
		lbAvID.addMouseListener(this);

		lbBreif.setFont(Theme.font(Font.PLAIN, 12));
		lbBreif.setForeground(Theme.TEXT_SECOND);
		lbBreif.setToolTipText("单击复制简介");
		lbBreif.addMouseListener(this);

		JPanel info = new JPanel();
		info.setOpaque(false);
		info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
		info.add(lbVideoTitle);
		info.add(Box.createVerticalStrut(4));
		info.add(lbAvID);
		info.add(Box.createVerticalStrut(4));
		info.add(lbBreif);
		header.add(info, BorderLayout.CENTER);

		// 操作区
		JPanel ops = new JPanel();
		ops.setOpaque(false);
		ops.setLayout(new BoxLayout(ops, BoxLayout.Y_AXIS));

		JPanel qualityRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		qualityRow.setOpaque(false);
		JLabel qnLabel = new JLabel("优先清晰度");
		qnLabel.setFont(Theme.font(Font.PLAIN, 12));
		qnLabel.setForeground(Theme.TEXT_SECOND);
		qualityRow.add(qnLabel);

		cbQn = new JComboBox<>();
		for (VideoQualityEnum item : VideoQualityEnum.values()) {
			cbQn.addItem(item.getQuality());
		}
		cbQn.setSelectedItem(Global.tab_qn);
		cbQn.setPreferredSize(new Dimension(104, 30));
		Theme.styleCombo(cbQn);
		qualityRow.add(cbQn);

		btnDownAll = new MJButton("批量下载", Style.PRIMARY);
		btnDownAll.setPreferredSize(new Dimension(96, 30));
		btnDownAll.setToolTipText("下载当前视频的全部分P");
		btnDownAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				download(true, VideoQualityEnum.getQN(String.valueOf(cbQn.getSelectedItem())));
			}
		});
		qualityRow.add(btnDownAll);

		btnDownCC = new MJButton("字幕下载");
		btnDownCC.setPreferredSize(new Dimension(96, 30));
		btnDownCC.setToolTipText("下载该视频的 CC 字幕");
		btnDownCC.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				downloadCC();
			}
		});
		qualityRow.add(btnDownCC);
		ops.add(qualityRow);
		ops.add(Box.createVerticalStrut(8));

		nextPagePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		nextPagePanel.setOpaque(false);
		jlNextPageTips = new JLabel("内容不全？试试翻页");
		jlNextPageTips.setFont(Theme.font(Font.PLAIN, 12));
		jlNextPageTips.setForeground(Theme.TEXT_MUTED);
		btnNextPage = new MJButton("下一页");
		btnNextPage.setPreferredSize(new Dimension(88, 28));
		ops.add(nextPagePanel);
		header.add(ops, BorderLayout.EAST);

		add(header, BorderLayout.NORTH);

		// 主体: 左侧分P列表 + 右侧预览
		jpContent = new JPanel();
		jpContent.setOpaque(false);
		jpContent.setLayout(new BoxLayout(jpContent, BoxLayout.Y_AXIS));
		jpContent.setBorder(Theme.padding(4, 4, 4, 4));

		jpScorll = new JScrollPane(jpContent);
		jpScorll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		jpScorll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		jpScorll.setBorder(null);
		jpScorll.setOpaque(false);
		jpScorll.getViewport().setOpaque(false);

		Theme.CardPanel listCard = new Theme.CardPanel(new BorderLayout());
		listCard.setBorder(Theme.padding(10, 10, 10, 10));
		listCard.add(jpScorll, BorderLayout.CENTER);
		listCard.setPreferredSize(new Dimension(392, 0));

		lbAvPrivew = new JLabel("预览图加载中...", SwingConstants.CENTER);
		lbAvPrivew.setFont(Theme.font(Font.PLAIN, 16));
		lbAvPrivew.setForeground(Theme.TEXT_MUTED);
		lbAvPrivew.setToolTipText("单击复制图片链接");
		lbAvPrivew.addMouseListener(this);
		lbAvPrivew.setHorizontalAlignment(SwingConstants.CENTER);

		Theme.CardPanel previewCard = new Theme.CardPanel(new BorderLayout());
		previewCard.setBorder(Theme.padding(10, 10, 10, 10));
		previewCard.add(lbAvPrivew, BorderLayout.CENTER);

		JPanel body = new JPanel(new BorderLayout(12, 0));
		body.setOpaque(false);
		body.add(listCard, BorderLayout.WEST);
		body.add(previewCard, BorderLayout.CENTER);
		add(body, BorderLayout.CENTER);
	}

	public void displayNextPagePanel() {
		nextPagePanel.add(jlNextPageTips);
		nextPagePanel.add(btnNextPage);
		nextPagePanel.revalidate();
	}

	/**
	 * 用于批量下载视频
	 */
	public void download(boolean downAll, int qn) {
		if (avInfo == null || avInfo.getClips() == null) {
			return;
		}
		int total = avInfo.getClips().values().size();
		download(0, qn);
		if (downAll) {
			for (int i = 1; i < total; i++) {
				download(i, qn);
			}
		}
	}

	/**
	 * 用于下载字幕
	 */
	public void downloadCC() {
		if (avInfo == null || avInfo.getClips() == null) {
			return;
		}
		int total = avInfo.getClips().values().size();
		for (int i = 0; i < total; i++) {
			download(i, 800);
		}
	}

	/**
	 * 下载第i个视频
	 */
	private void download(int i, int qn) {
		try {
			ClipInfo clip = (ClipInfo) avInfo.getClips().values().toArray()[i];
			DownloadRunnable downThread = new DownloadRunnable(avInfo, clip, qn);
			Global.queryThreadPool.execute(downThread);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		Theme.paintAppBackground(g, this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

	}

	public JLabel getLbTabTitle() {
		return lbTabTitle;
	}

	public void setLbTabTitle(JLabel lbTabTitle) {
		this.lbTabTitle = lbTabTitle;
	}

	public JLabel getLbVideoTitle() {
		return lbVideoTitle;
	}

	public void setLbVideoTitle(JLabel lbVideoTitle) {
		this.lbVideoTitle = lbVideoTitle;
	}

	public JLabel getLbAvID() {
		return lbAvID;
	}

	public void setLbAvID(JLabel lbAvID) {
		this.lbAvID = lbAvID;
	}

	public JLabel getLbBreif() {
		return lbBreif;
	}

	public void setLbBreif(JLabel lbBreif) {
		this.lbBreif = lbBreif;
	}

	public JLabel getLbAvPrivew() {
		return lbAvPrivew;
	}

	public void setLbAvPrivew(JLabel lbAvPrivew) {
		this.lbAvPrivew = lbAvPrivew;
	}

	public JScrollPane getJpScorll() {
		return jpScorll;
	}

	public void setJpScorll(JScrollPane jpScorll) {
		this.jpScorll = jpScorll;
	}

	public JPanel getJpContent() {
		return jpContent;
	}

	public void setJpContent(JPanel jpContent) {
		this.jpContent = jpContent;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		JLabel label = (JLabel) e.getSource();
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		String txtToCopy = null;
		if (label == lbAvPrivew) {
			txtToCopy = currentDisplayPic != null ? currentDisplayPic : (avInfo != null ? avInfo.getVideoPreview() : null);
		} else {
			txtToCopy = label.getText();
		}
		if (txtToCopy != null) {
			Transferable trans = new StringSelection(txtToCopy);
			clipboard.setContents(trans, null);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		JLabel label = (JLabel) e.getSource();
		label.setBorder(BorderFactory.createLineBorder(Theme.PRIMARY, 1));
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		JLabel label = (JLabel) e.getSource();
		label.setBorder(null);
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	public VideoInfo getAvInfo() {
		return avInfo;
	}

	public void setAvInfo(VideoInfo avInfo) {
		this.avInfo = avInfo;
	}

	public String getCurrentDisplayPic() {
		return currentDisplayPic;
	}

	public void setCurrentDisplayPic(String currentDisplayPic) {
		this.currentDisplayPic = currentDisplayPic;
	}
}
