package nicelee.ui.item;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import nicelee.ui.Global;
import nicelee.ui.TabDownload;
import nicelee.ui.Theme;
import nicelee.ui.thread.DownloadRunnableInternal;

import nicelee.bilibili.INeedAV;
import nicelee.bilibili.downloaders.Downloader;
import nicelee.bilibili.enums.StatusEnum;
import nicelee.bilibili.model.ClipInfo;
import nicelee.bilibili.util.Logger;
import nicelee.bilibili.util.custom.System;

public class DownloadInfoPanel extends JPanel implements ActionListener {

	ClipInfo clipInfo;
	String avTitle; // 原始av标题
	String clipTitle; // 原始clip标题
	String avid;
	String cid;
	int page;
	int remark;
	int qn;
	int realqn;

	// 下载相关
	public INeedAV iNeedAV;
	public String url;
	public String avid_qn;
	public String formattedTitle;
	public boolean stopOnQueue = false;
	int failCnt = 0;

	public int getFailCnt() {
		return failCnt;
	}

	public void setFailCnt(int failCnt) {
		this.failCnt = failCnt;
	}

	long lastCntTime = 0L;
	long lastCnt = 0L;
	/**
	 * 
	 */
	private static final long serialVersionUID = -752743062676819402L;
	String path;
	String fileName;
	long totalSize;
	long currentDown;
	boolean isdownloading = true;

	JButton btnRemove;
	JButton btnOpen;
	JButton btnOpenFolder;
	JButton btnControl;
	JLabel lbCurrentStatus;
	JLabel lbDownFile;
	JLabel lbFileName;
	JLabel lbavName;

	public DownloadInfoPanel(ClipInfo clip, int qn) {
		this.clipInfo = clip;
		this.avTitle = clip.getAvTitle();
		this.clipTitle = clip.getAvTitle();
		this.avid = clip.getAvId();
		this.cid = Long.toString(clip.getcId());
		this.page = clip.getPage();
		this.remark = clip.getRemark();
		this.qn = qn;
		path = "D:\\bilibiliDown\\";
		fileName = "timg.gif";
		totalSize = 0L;
		currentDown = 0L;
		initUI(this);
	}

	void initUI(DownloadInfoPanel dp) {
		this.setOpaque(false);
		this.setLayout(new BorderLayout(0, 6));
		this.setPreferredSize(new Dimension(1080, 104));
		this.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
		this.setBorder(new EmptyBorder(10, 14, 10, 14));

		// 第一行: 文件名 + 状态 + 操作按钮
		JPanel header = new JPanel(new BorderLayout(12, 0));
		header.setOpaque(false);

		lbFileName = new JLabel("尚未生成");
		lbFileName.setFont(Theme.font(Font.BOLD, 13));
		lbFileName.setForeground(Theme.TEXT);
		lbFileName.setPreferredSize(new Dimension(420, 24));
		lbFileName.setToolTipText("尚未生成");
		header.add(lbFileName, BorderLayout.WEST);

		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		right.setOpaque(false);

		lbCurrentStatus = new JLabel("正在下载...");
		lbCurrentStatus.setFont(Theme.font(Font.PLAIN, 12));
		lbCurrentStatus.setForeground(Theme.TEXT_SECOND);
		lbCurrentStatus.setHorizontalAlignment(SwingConstants.RIGHT);
		right.add(lbCurrentStatus);

		btnControl = new MJButton("暂停");
		btnControl.setPreferredSize(new Dimension(92, 30));
		btnControl.addActionListener(this);
		right.add(btnControl);

		btnOpen = new MJButton("打开文件");
		btnOpen.setPreferredSize(new Dimension(92, 30));
		btnOpen.addActionListener(this);
		right.add(btnOpen);

		btnOpenFolder = new MJButton("打开文件夹");
		btnOpenFolder.setPreferredSize(new Dimension(104, 30));
		btnOpenFolder.addActionListener(this);
		right.add(btnOpenFolder);

		btnRemove = new MJButton("删除任务");
		btnRemove.setPreferredSize(new Dimension(92, 30));
		btnRemove.addActionListener(this);
		right.add(btnRemove);

		header.add(right, BorderLayout.EAST);

		// 第二行: 视频标题 + 进度
		JPanel footer = new JPanel(new BorderLayout(12, 0));
		footer.setOpaque(false);

		lbavName = new JLabel(avTitle);
		lbavName.setFont(Theme.font(Font.PLAIN, 12));
		lbavName.setForeground(Theme.TEXT_MUTED);
		lbavName.setToolTipText(avTitle);
		footer.add(lbavName, BorderLayout.CENTER);

		lbDownFile = new JLabel(currentDown + "/" + totalSize);
		lbDownFile.setFont(Theme.font(Font.PLAIN, 12));
		lbDownFile.setForeground(Theme.TEXT_MUTED);
		lbDownFile.setHorizontalAlignment(SwingConstants.RIGHT);
		footer.add(lbDownFile, BorderLayout.EAST);

		add(header, BorderLayout.NORTH);
		add(footer, BorderLayout.CENTER);
	}

	@Override
	protected void paintComponent(Graphics g) {
		Theme.paintCard(g, this, 8, false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnOpenFolder) {
			File file = new File(lbFileName.getText());
			String os = System.getProperty("os.name");
			try {
				if (file.exists() && os.toLowerCase().startsWith("win")) {
					// 打开并选中
					String cmd[] = { "explorer", "/e,/select,", file.getAbsolutePath() };
					Runtime.getRuntime().exec(cmd);
				} else if (file.exists()) {
					Desktop desktop = Desktop.getDesktop();
					desktop.open(file);
				} else {
					Desktop desktop = Desktop.getDesktop();
					desktop.open(file.getParentFile());
				}
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(null, "打开文件夹失败!", "失败", JOptionPane.INFORMATION_MESSAGE);
			}
		} else if (e.getSource() == btnOpen) {
			File file = new File(lbFileName.getText());
			try {
				Desktop.getDesktop().open(file);
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(null, "打开文件失败!", "失败", JOptionPane.INFORMATION_MESSAGE);
			}
		} else if (e.getSource() == btnRemove) {
			if (TabDownload.isStopAll()) {
				Logger.println("停止任务中，请误操作");
				return;
			}
			removeTask(true);
		} else if (e.getSource() == btnControl) {
			if (TabDownload.isStopAll()) {
				Logger.println("停止任务中，请误操作");
				return;
			}
			StatusEnum status = iNeedAV.getDownloader().currentStatus();
			if (status == StatusEnum.DOWNLOADING) {
				stopTask();
			} else {
				setFailCnt(0);
				continueTask();
			}
		}
	}

	/**
	 * 下载前的初始化工作
	 */
	public void initDownloadParams(INeedAV iNeedAV, String url, String avid_qn, String formattedTitle, int realqn) {
		this.iNeedAV = iNeedAV;
		this.avid_qn = avid_qn;
		this.formattedTitle = formattedTitle;
		this.url = url;
		this.realqn = realqn;
		this.lbavName.setText(formattedTitle);
		this.lbavName.setToolTipText(formattedTitle);
		this.stopOnQueue = false;
	}

	/**
	 * 停止任务(方法内包含状态判断)
	 */
	public void stopTask() {
		Downloader downloader = iNeedAV.getDownloader();
		downloader.stopTask();
		stopOnQueue = true;
	}

	/**
	 * 继续任务(方法内包含状态判断)
	 */
	public void continueTask() {
		stopOnQueue = false;
		Downloader downloader = iNeedAV.getDownloader();
		// 如果正在下载 或 下载完毕，则不需要下载
		StatusEnum status = downloader.currentStatus();
		if (status != StatusEnum.DOWNLOADING && status != StatusEnum.SUCCESS && status != StatusEnum.PROCESSING) {
			downloader.startTask();
			if (!Global.downLoadThreadPool.isShutdown()) {
				Global.downLoadThreadPool.execute(
						new DownloadRunnableInternal(this, System.currentTimeMillis(), true, failCnt));
			}
		}
	}

	/**
	 * 删除任务
	 */
	public void removeTask(boolean deleteAll) {
		// 删除所有 或 删除已完成的任务
		// 0 正在下载; 1 下载完毕; -1 出现错误; -2 人工停止;-3队列中
		if (deleteAll || iNeedAV.getDownloader().currentStatus() == StatusEnum.SUCCESS) {
			this.stopOnQueue = true;
			// 停止下载
			Global.downloadTaskList.get(this).stopTask();
			// 全局监控撤销
			Global.downloadTaskList.remove(this);
			// 当前页面控件删除
			Global.downloadTab.getJpContent().remove(this);
			// 大小重新适配
			Global.downloadTab.getJpContent()
					.setPreferredSize(new Dimension(1100, 112 * Global.downloadTaskList.size()));
			Global.downloadTab.getJpContent().updateUI();
			Global.downloadTab.getJpContent().repaint();
			// 删除未完成的下载文件
			File file = new File(lbFileName.getText() + ".part");
			if (file.exists()) {
				file.delete();
			}
		}
	}

	public JLabel getLbCurrentStatus() {
		return lbCurrentStatus;
	}

	public void setLbCurrentStatus(JLabel lbCurrentStatus) {
		this.lbCurrentStatus = lbCurrentStatus;
	}

	public JLabel getLbDownFile() {
		return lbDownFile;
	}

	public void setLbDownFile(JLabel lbDownFile) {
		this.lbDownFile = lbDownFile;
	}

	public JLabel getLbFileName() {
		return lbFileName;
	}

	public void setLbFileName(JLabel lbFileName) {
		this.lbFileName = lbFileName;
	}

	public JButton getBtnControl() {
		return btnControl;
	}

	public void setBtnControl(JButton btnControl) {
		this.btnControl = btnControl;
	}

	@Override
	public int hashCode() {
		return (avid + page).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DownloadInfoPanel) {
			DownloadInfoPanel down = (DownloadInfoPanel) obj;
			return (avid.equals(down.avid) && page == down.page);
		}
		return false;
	}

	public long getLastCntTime() {
		return lastCntTime;
	}

	public void setLastCntTime(long lastCntTime) {
		this.lastCntTime = lastCntTime;
	}

	public long getLastCnt() {
		return lastCnt;
	}

	public void setLastCnt(long lastCnt) {
		this.lastCnt = lastCnt;
	}

	public String getAvid() {
		return avid;
	}

	public void setAvid(String avid) {
		this.avid = avid;
	}

	public String getCid() {
		return cid;
	}

	public ClipInfo getClipInfo() {
		return clipInfo;
	}

	public int getQn() {
		return qn;
	}

	public int getRealqn() {
		return realqn;
	}

	public void setRealqn(int realqn) {
		this.realqn = realqn;
	}
}
