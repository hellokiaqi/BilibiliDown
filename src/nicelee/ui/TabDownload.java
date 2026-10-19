package nicelee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import nicelee.ui.item.DownloadInfoPanel;
import nicelee.ui.item.MJButton;
import nicelee.ui.item.MJButton.Style;
import nicelee.ui.thread.DownloadExecutors;

public class TabDownload extends JPanel implements ActionListener {

	private static final long serialVersionUID = 8714599826187286737L;
	private static boolean stopAll = false;

	JPanel jpContent;
	JScrollPane jpScorll;
	JLabel lbStatus;
	JLabel lbEmptyHint;
	java.awt.CardLayout bodyLayout;
	JPanel body;
	JButton btnContinue, btnStop, btnDeleteAll, btnDeleteDown;

	public TabDownload() {
		initUI();
	}

	public int activeTask;

	public void refreshStatus(int totalTask, int activeTask, int pauseTask, int doneTask, int queuingTask) {
		this.activeTask = activeTask;
		String txt = String.format("总计 %d　·　下载中 %d　·　暂停 %d　·　已完成 %d　·　队列中 %d", totalTask, activeTask,
				pauseTask, doneTask, queuingTask);
		if (lbStatus != null) {
			lbStatus.setText(txt);
		}
		if (bodyLayout != null && body != null) {
			bodyLayout.show(body, totalTask > 0 ? "tasks" : "empty");
		}
	}

	public void initUI() {
		setOpaque(false);
		setLayout(new BorderLayout(0, 12));
		setBorder(new EmptyBorder(16, 18, 10, 18));

		// 顶部: 状态 + 操作按钮
		Theme.CardPanel top = new Theme.CardPanel(new BorderLayout(12, 0));
		top.setBorder(new EmptyBorder(12, 16, 12, 16));

		lbStatus = new JLabel("总计 0　·　下载中 0　·　暂停 0　·　已完成 0　·　队列中 0");
		lbStatus.setFont(Theme.font(Font.PLAIN, 13));
		lbStatus.setForeground(Theme.TEXT_SECOND);
		top.add(lbStatus, BorderLayout.WEST);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		actions.setOpaque(false);
		btnContinue = new MJButton("全部继续", Style.PRIMARY);
		btnStop = new MJButton("全部暂停");
		btnDeleteAll = new MJButton("全部删除");
		btnDeleteDown = new MJButton("删除已完成");
		Dimension size = new Dimension(104, 32);
		btnContinue.setPreferredSize(size);
		btnStop.setPreferredSize(size);
		btnDeleteAll.setPreferredSize(size);
		btnDeleteDown.setPreferredSize(new Dimension(112, 32));
		btnContinue.addActionListener(this);
		btnStop.addActionListener(this);
		btnDeleteAll.addActionListener(this);
		btnDeleteDown.addActionListener(this);
		actions.add(btnContinue);
		actions.add(btnStop);
		actions.add(btnDeleteAll);
		actions.add(btnDeleteDown);
		top.add(actions, BorderLayout.EAST);
		add(top, BorderLayout.NORTH);

		// 任务列表
		Theme.CardPanel bodyCard = new Theme.CardPanel(new java.awt.CardLayout());
		bodyCard.setBorder(new EmptyBorder(10, 10, 10, 10));
		body = bodyCard;
		bodyLayout = (java.awt.CardLayout) bodyCard.getLayout();

		jpContent = new JPanel();
		jpContent.setOpaque(false);
		jpContent.setLayout(new BoxLayout(jpContent, BoxLayout.Y_AXIS));
		jpContent.setBackground(Color.WHITE);

		jpScorll = new JScrollPane(jpContent);
		jpScorll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		jpScorll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		jpScorll.setBorder(null);
		jpScorll.setOpaque(false);
		jpScorll.getViewport().setOpaque(false);
		bodyCard.add(jpScorll, "tasks");

		bodyCard.add(buildEmptyPanel(), "empty");
		bodyLayout.show(bodyCard, "empty");

		add(bodyCard, BorderLayout.CENTER);
	}

	/** 无任务时的空状态提示 */
	private JPanel buildEmptyPanel() {
		JPanel empty = new JPanel(new java.awt.GridBagLayout());
		empty.setOpaque(false);
		javax.swing.JPanel box = new javax.swing.JPanel();
		box.setOpaque(false);
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

		JLabel title = new JLabel("暂无下载任务");
		title.setFont(Theme.font(Font.BOLD, 15));
		title.setForeground(Theme.TEXT_SECOND);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);

		lbEmptyHint = new JLabel("在主页解析视频后，点击清晰度按钮即可开始下载");
		lbEmptyHint.setFont(Theme.font(Font.PLAIN, 12));
		lbEmptyHint.setForeground(Theme.TEXT_MUTED);
		lbEmptyHint.setAlignmentX(Component.CENTER_ALIGNMENT);
		lbEmptyHint.setBorder(new EmptyBorder(8, 0, 0, 0));

		box.add(title);
		box.add(lbEmptyHint);
		empty.add(box);
		return empty;
	}

	@Override
	public void paintComponent(Graphics g) {
		Theme.paintAppBackground(g, this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnContinue) {
			stopAll = false;
			for (int i = 0; i < jpContent.getComponentCount(); i++) {
				Component comp = jpContent.getComponent(i);
				if (comp instanceof DownloadInfoPanel) {
					((DownloadInfoPanel) comp).setFailCnt(0);
					((DownloadInfoPanel) comp).continueTask();
				}
			}
		} else if (e.getSource() == btnStop) {
			// 约3s后置false
			stopAll = true;
			btnContinue.setEnabled(false);
			btnStop.setEnabled(false);
			btnDeleteAll.setEnabled(false);
			// 先shutdown, 队列里的线程无需再执行
			Global.downLoadThreadPool.shutdownNow();
			for (DownloadInfoPanel dp : Global.downloadTaskList.keySet()) {
				dp.stopTask();
			}
			// 停止进程需要时间, 期间最好不进行其他操作
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
					}
					// 双保险
					for (DownloadInfoPanel dp : Global.downloadTaskList.keySet()) {
						dp.stopTask();
					}
					int fixPool = Global.downloadPoolSize;
					Global.downLoadThreadPool = DownloadExecutors.newPriorityFixedThreadPool(fixPool);
					btnContinue.setEnabled(true);
					btnStop.setEnabled(true);
					btnDeleteAll.setEnabled(true);
					stopAll = false;
				}
			}).start();
		} else if (e.getSource() == btnDeleteAll) {
			for (DownloadInfoPanel dp : Global.downloadTaskList.keySet()) {
				dp.removeTask(true);
			}
		} else if (e.getSource() == btnDeleteDown) {
			for (DownloadInfoPanel dp : Global.downloadTaskList.keySet()) {
				dp.removeTask(false);
			}
		}
	}

	public JPanel getJpContent() {
		return jpContent;
	}

	public void setJpContent(JPanel jpContent) {
		this.jpContent = jpContent;
	}

	public static boolean isStopAll() {
		return stopAll;
	}
}
