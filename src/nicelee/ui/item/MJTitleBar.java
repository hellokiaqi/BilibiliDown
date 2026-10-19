package nicelee.ui.item;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import nicelee.ui.FrameMain;
import nicelee.ui.Global;
import nicelee.ui.SysTray;
import nicelee.ui.Theme;

/**
 * 自绘窗口标题栏: 左侧图标+标题，右侧菜单栏与最小化/关闭按钮，支持拖动。
 */
public class MJTitleBar extends JPanel implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 7910189206713741389L;

	private final JFrame frame;
	private final boolean setTitle;
	private final boolean setMenuBar;
	private JButton btnMin;
	private JButton btnClose;
	private JLabel title;
	private Point pressedPoint;

	public MJTitleBar(JFrame frame) {
		this(frame, false, false);
	}

	public MJTitleBar(JFrame frame, boolean setTitle) {
		this(frame, setTitle, false);
	}

	public MJTitleBar(JFrame frame, boolean setTitle, boolean setMenuBar) {
		super();
		this.frame = frame;
		this.setTitle = setTitle;
		this.setMenuBar = setMenuBar;
		init();
	}

	private void init() {
		frame.setUndecorated(true);
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(frame.getWidth() - 4, 36));
		setOpaque(true);
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

		if (setTitle) {
			add(buildLeft(), BorderLayout.WEST);
		}

		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		right.setOpaque(false);
		right.setBorder(new EmptyBorder(0, 0, 0, 6));
		if (setMenuBar) {
			MJMenuBar menu = new MJMenuBar(frame);
			menu.setOpaque(false);
			right.add(menu);
		}
		btnMin = windowButton(false, false);
		btnClose = windowButton(true, true);
		right.add(btnMin);
		right.add(btnClose);
		add(right, BorderLayout.EAST);

		addMouseMotionListener(this);
		addMouseListener(this);
		btnMin.addMouseListener(this);
		btnClose.addMouseListener(this);
	}

	private JPanel buildLeft() {
		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		left.setOpaque(false);
		left.setBorder(new EmptyBorder(0, 12, 0, 0));
		left.addMouseMotionListener(this);
		left.addMouseListener(this);

		if (frame.getIconImage() != null) {
			ImageIcon iconImg = new ImageIcon(frame.getIconImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
			JLabel icon = new JLabel(iconImg);
			icon.addMouseMotionListener(this);
			icon.addMouseListener(this);
			left.add(icon);
		}

		title = new JLabel(formatTitle(frame.getTitle()));
		title.setFont(Theme.font(Font.BOLD, 12));
		title.setForeground(Theme.TEXT);
		title.addMouseMotionListener(this);
		title.addMouseListener(this);
		left.add(title);
		return left;
	}

	private static String formatTitle(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("~~", " ").replace("BiliBili Down", "BiliBiliDown").trim();
	}

	/** 自绘窗口按钮: 平时透明，hover 显示圆角底，关闭按钮 hover 变红 */
	private JButton windowButton(final boolean close, final boolean danger) {
		JButton button = new JButton() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				try {
					Theme.enableAA(g2);
					boolean pressed = getModel().isPressed();
					boolean hover = getModel().isRollover() || pressed;
					int w = getWidth();
					int h = getHeight();
					if (hover) {
						if (danger) {
							g2.setColor(pressed ? new Color(0xD9, 0x3E, 0x43) : new Color(0xF5, 0x6C, 0x71));
						} else {
							g2.setColor(new Color(0xEC, 0xEF, 0xF4));
						}
						g2.fillRoundRect(0, 0, w, h, 6, 6);
					}
					g2.setColor(danger && hover ? Color.WHITE : Theme.TEXT_SECOND);
					g2.setStroke(new java.awt.BasicStroke(1.4f, java.awt.BasicStroke.CAP_ROUND,
							java.awt.BasicStroke.JOIN_ROUND));
					int cx = w / 2;
					int cy = h / 2;
					int r = 5;
					if (close) {
						g2.drawLine(cx - r, cy - r, cx + r, cy + r);
						g2.drawLine(cx + r, cy - r, cx - r, cy + r);
					} else {
						g2.drawLine(cx - r, cy, cx + r, cy);
					}
				} finally {
					g2.dispose();
				}
			}
		};
		button.setPreferredSize(new Dimension(34, 26));
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);
		button.setFocusPainted(false);
		button.setOpaque(false);
		button.setRolloverEnabled(true);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setToolTipText(close ? "关闭" : "最小化");
		return button;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == btnClose) {
			if (frame instanceof FrameMain && SysTray.isSysTrayInitiated() && Global.closeToSystray) {
				frame.setExtendedState(JFrame.ICONIFIED);
				frame.setVisible(false);
			} else {
				// 有下载任务时二次确认
				if (frame instanceof FrameMain && Global.downloadTab != null && Global.downloadTab.activeTask > 0) {
					Object[] options = { "我要退出", "我再想想" };
					String msg = String.format("当前仍有 %d 个任务在下载/转码，退出后正在转码的文件可能丢失或异常，确定要退出吗？",
							Global.downloadTab.activeTask);
					int m = JOptionPane.showOptionDialog(null, msg, "警告", JOptionPane.YES_NO_OPTION,
							JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
					if (m != 0) {
						return;
					}
				}
				WindowEvent event = new WindowEvent(frame, WindowEvent.WINDOW_CLOSING);
				frame.dispatchEvent(event);
			}
		} else if (e.getSource() == btnMin) {
			frame.setExtendedState(JFrame.ICONIFIED);
			if (frame instanceof FrameMain && SysTray.isSysTrayInitiated() && Global.minimizeToSystray) {
				frame.setVisible(false);
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (!(e.getSource() instanceof JButton)) {
			pressedPoint = SwingUtilities.convertPoint((java.awt.Component) e.getSource(), e.getPoint(), frame);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		pressedPoint = null;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (pressedPoint == null || e.getSource() instanceof JButton) {
			return;
		}
		int newX = e.getXOnScreen() - pressedPoint.x;
		int newY = e.getYOnScreen() - pressedPoint.y;
		if (newX == frame.getX() && newY == frame.getY()) {
			return;
		}
		frame.setLocation(newX, newY);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	public void setTitle(String title) {
		if (this.title != null) {
			this.title.setText(formatTitle(title));
		}
	}
}
