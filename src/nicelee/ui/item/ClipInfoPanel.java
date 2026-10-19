package nicelee.ui.item;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import nicelee.ui.Theme;

import nicelee.bilibili.enums.AudioQualityEnum;
import nicelee.bilibili.enums.VideoQualityEnum;
import nicelee.bilibili.model.ClipInfo;
import nicelee.bilibili.model.VideoInfo;
import nicelee.bilibili.util.Logger;
import nicelee.bilibili.util.ResourcesUtil;
import nicelee.bilibili.util.custom.System;
import nicelee.ui.Global;
import nicelee.ui.TabVideo;
import nicelee.ui.thread.DownloadRunnable;

public class ClipInfoPanel extends JPanel implements MouseListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -752743062676819403L;
	String avTitle;
	VideoInfo video;
	ClipInfo clip;

	private JLabel labelTitle;
	private long lastMousePressed;
	Dimension btnSize = new Dimension(84, 26);
	public ClipInfoPanel(VideoInfo video, ClipInfo clip) {
		this.video = video;
		this.clip = clip;
		this.avTitle = clip.getAvTitle();
		initUI();
	}

	void initUI() {
		this.setOpaque(false);
		this.setLayout(new BorderLayout(0, 6));
		this.setBorder(new EmptyBorder(10, 12, 10, 12));
		this.setPreferredSize(new Dimension(344, 136));
		this.setMaximumSize(new Dimension(Integer.MAX_VALUE, 136));

		// 分情况显示
		boolean isPic = ResourcesUtil.isPicture(clip);
		if (clip.getListName() != null || isPic) {
			labelTitle = new JLabel(shortenTitle(clip.getRemark() + " - " + clip.getAvTitle() + " " + clip.getTitle()));
		} else {
			labelTitle = new JLabel(shortenTitle(clip.getRemark() + " - " + clip.getTitle()));
		}
		labelTitle.setFont(Theme.font(Font.PLAIN, 12));
		labelTitle.setForeground(Theme.TEXT);
		labelTitle.setVerticalAlignment(SwingConstants.TOP);
		labelTitle.addMouseListener(this);
		labelTitle.setToolTipText("<html><body style='width:320px'>" + escape(clip.getAvTitle())
				+ escape(clip.getTitle()) + "</body></html>");
		this.add(labelTitle, BorderLayout.NORTH);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
		buttons.setOpaque(false);
		this.add(buttons, BorderLayout.CENTER);

		if (!isPic) {
			JButton btnDanmuku = new MJButton("弹幕");
			btnDanmuku.setPreferredSize(new Dimension(64, 26));
			btnDanmuku.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					DownloadRunnable downThread = new DownloadRunnable(video, clip, 801);
					Global.queryThreadPool.execute(downThread);
				}
			});
			buttons.add(btnDanmuku);
		}

		for (final int qn : clip.getLinks().keySet()) {
			if (qn >= 800)
				continue;
			String qnName = VideoQualityEnum.getQualityDescript(qn);
			if (qnName == null)
				qnName = AudioQualityEnum.getQualityDescript(qn);
			String label = (qnName != null && !isPic) ? compactQuality(qnName) : ("清晰度 " + qn);
			JButton btn = new MJButton(label);
			btn.setToolTipText(qnName != null ? qnName : ("清晰度 " + qn));
			addQnButton(buttons, qn, btn);
		}
		if (!isPic) {
			JButton btn = new MJButton("字幕");
			addQnButton(buttons, 800, btn);
		}
	}

	/** 把"高清1080P60"这类长描述压缩成按钮上更易读的简短文本 */
	private static String compactQuality(String text) {
		String value = text.replace(" ", "");
		String[] prefixes = { "超高清", "高清", "超清", "清晰", "流畅", "真彩" };
		for (String prefix : prefixes) {
			if (value.startsWith(prefix)) {
				value = value.substring(prefix.length());
				break;
			}
		}
		return value;
	}

	private static String shortenTitle(String text) {
		if (text == null) {
			return "";
		}
		return text.length() <= 46 ? text : text.substring(0, 45) + "…";
	}

	private static String escape(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	@Override
	protected void paintComponent(Graphics g) {
		Theme.paintCard(g, this, 8, false);
	}

	/**
	 * @param qn
	 * @param btn
	 */
	private void initQnBtn(final int qn, JButton btn) {
		addQnButton(this, qn, btn);
	}

	private void addQnButton(JPanel container, final int qn, JButton btn) {
		btn.setPreferredSize(btnSize);
		btn.setFont(Theme.font(Font.PLAIN, 12));
		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DownloadRunnable downThread = new DownloadRunnable(video, clip, qn);
				Global.queryThreadPool.execute(downThread);
			}
		});
		container.add(btn);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		String txtToCopy = null;
		if (e.getClickCount() == 1) {
			//txtToCopy = clip.getAvTitle() + clip.getTitle();
		} else {
			txtToCopy = clip.getAvTitle() + clip.getTitle() + " " +clip.getAvId();
		}
		// 获取系统剪贴板
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		// 封装文本内容
		Transferable trans = new StringSelection(txtToCopy);
		// 把文本内容设置到系统剪贴板
		clipboard.setContents(trans, null);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		lastMousePressed = System.currentTimeMillis();
		labelTitle.setBorder(BorderFactory.createLineBorder(Theme.PRIMARY, 1));
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		labelTitle.setBorder(null);
		long timeTouched = System.currentTimeMillis() - lastMousePressed;
		Logger.println("长按了" + timeTouched +"ms");
		if(timeTouched >= 500) {
			try {
				//获取父对象
				TabVideo tVideo = (TabVideo)this.getParent().getParent().getParent().getParent();
				//设置更换预览图片
				String toDisplay = clip.getPicPreview();
				if(toDisplay != null && !toDisplay.equals(tVideo.getCurrentDisplayPic())) {
					URL fileURL = new URL(toDisplay);
					ImageIcon imag1 = new ImageIcon(fileURL);
					imag1 = new ImageIcon(imag1.getImage().getScaledInstance(700, 460, Image.SCALE_SMOOTH) );
					tVideo.getLbAvPrivew().setText("");
					tVideo.getLbAvPrivew().setIcon(imag1);
					tVideo.setCurrentDisplayPic(toDisplay);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}
}
