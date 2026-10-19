package nicelee.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * 全局界面主题：统一配色、字体、圆角卡片与背景绘制。
 * 新增界面请优先复用这里的常量与工具方法，避免风格再次发散。
 */
public final class Theme {

	/** 主色(B站粉) */
	public static final Color PRIMARY = new Color(0xFB, 0x72, 0x99);
	public static final Color PRIMARY_HOVER = new Color(0xFC, 0x8F, 0xAC);
	public static final Color PRIMARY_PRESS = new Color(0xE0, 0x5F, 0x83);
	public static final Color PRIMARY_DISABLED = new Color(0xF8, 0xCE, 0xDA);

	public static final Color BG_TOP = new Color(0xFA, 0xFB, 0xFD);
	public static final Color BG_BOTTOM = new Color(0xEB, 0xEF, 0xF5);
	public static final Color CARD = Color.WHITE;
	public static final Color BORDER = new Color(0xE4, 0xE7, 0xEC);
	public static final Color DIVIDER = new Color(0xEF, 0xF1, 0xF4);
	public static final Color ROW_ALT = new Color(0xF8, 0xFA, 0xFC);

	public static final Color TEXT = new Color(0x1F, 0x23, 0x29);
	public static final Color TEXT_SECOND = new Color(0x64, 0x6A, 0x73);
	public static final Color TEXT_MUTED = new Color(0x8F, 0x95, 0x9E);

	public static final Color SUCCESS = new Color(0x2B, 0xAC, 0x76);
	public static final Color DANGER = new Color(0xE5, 0x48, 0x4D);
	public static final Color WARNING = new Color(0xC8, 0x85, 0x00);

	public static final int RADIUS = 10;

	private static String family;

	private Theme() {
	}

	/** 选择一个系统里确实存在的中文字体，找不到就退回 Dialog */
	public static synchronized String fontFamily() {
		if (family != null) {
			return family;
		}
		Set<String> available = new HashSet<>();
		try {
			for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
				available.add(name);
			}
		} catch (Exception e) {
			// 极端环境(无图形环境)忽略
		}
		String[] preferred = { "Microsoft YaHei UI", "Microsoft YaHei", "PingFang SC", "Noto Sans CJK SC",
				"Source Han Sans CN", "WenQuanYi Micro Hei", "Segoe UI", "Dialog" };
		for (String name : preferred) {
			if (available.isEmpty() || available.contains(name)) {
				family = name;
				return family;
			}
		}
		family = Font.DIALOG;
		return family;
	}

	public static Font font(int style, int size) {
		return new Font(fontFamily(), style, size);
	}

	public static Border padding(int top, int left, int bottom, int right) {
		return new EmptyBorder(top, left, bottom, right);
	}

	/** 应用整体背景: 柔和渐变 + 用户自定义背景图(低透明度叠加，避免影响文字可读性) */
	public static void paintAppBackground(Graphics graphics, JComponent component) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			enableAA(g);
			int w = component.getWidth();
			int h = component.getHeight();
			g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
			g.fillRect(0, 0, w, h);
			ImageIcon background = Global.customBackground ? Global.backgroundImg : null;
			if (background != null && background.getImage() != null) {
				Image image = background.getImage();
				int iw = image.getWidth(component);
				int ih = image.getHeight(component);
				if (iw > 0 && ih > 0 && w > 0 && h > 0) {
					double scale = Math.max(w / (double) iw, h / (double) ih);
					int dw = (int) Math.ceil(iw * scale);
					int dh = (int) Math.ceil(ih * scale);
					Composite old = g.getComposite();
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.07f));
					g.drawImage(image, (w - dw) / 2, (h - dh) / 2, dw, dh, component);
					g.setComposite(old);
				}
			}
		} finally {
			g.dispose();
		}
	}

	/** 圆角卡片背景(白色填充 + 淡边框) */
	public static void paintCard(Graphics graphics, JComponent component, int radius, boolean shadow) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			enableAA(g);
			int w = component.getWidth();
			int h = component.getHeight();
			if (w <= 0 || h <= 0) {
				return;
			}
			if (shadow) {
				for (int i = 3; i >= 1; i--) {
					g.setColor(new Color(0, 0, 0, 6));
					g.fillRoundRect(i, i + 1, w - i * 2, h - i * 2, radius + 2, radius + 2);
				}
			}
			g.setColor(CARD);
			g.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);
			g.setColor(BORDER);
			g.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
		} finally {
			g.dispose();
		}
	}

	/** 浅色圆角底(用于状态条、提示块) */
	public static void paintPill(Graphics graphics, JComponent component, Color background, Color border, int radius) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			enableAA(g);
			int w = component.getWidth();
			int h = component.getHeight();
			g.setColor(background);
			g.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);
			if (border != null) {
				g.setColor(border);
				g.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
			}
		} finally {
			g.dispose();
		}
	}

	public static void enableAA(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}

	// ---------------------------------------------------------------- 组件工厂

	/** 圆角白卡片 */
	public static class CardPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private final int radius;
		private final boolean shadow;

		public CardPanel(LayoutManager layout) {
			this(layout, RADIUS, true);
		}

		public CardPanel(LayoutManager layout, int radius, boolean shadow) {
			super(layout);
			this.radius = radius;
			this.shadow = shadow;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			paintCard(g, this, radius, shadow);
		}
	}

	/** 状态/提示条：浅底圆角 */
	public static class PillPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private final Color background;
		private final Color border;

		public PillPanel(LayoutManager layout, Color background, Color border) {
			super(layout);
			this.background = background;
			this.border = border;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			paintPill(g, this, background, border, 999);
		}
	}

	/** 标题文字 */
	public static JLabel title(String text, int size) {
		JLabel label = new JLabel(text);
		label.setFont(font(Font.BOLD, size));
		label.setForeground(TEXT);
		return label;
	}

	/** 次要说明文字 */
	public static JLabel muted(String text, int size) {
		JLabel label = new JLabel(text);
		label.setFont(font(Font.PLAIN, size));
		label.setForeground(TEXT_MUTED);
		return label;
	}

	public static void styleField(JTextField field) {
		field.setFont(font(Font.PLAIN, 13));
		field.setForeground(TEXT);
		field.setBackground(Color.WHITE);
		field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
				padding(4, 10, 4, 10)));
	}

	public static void styleCombo(JComboBox<?> combo) {
		combo.setFont(font(Font.PLAIN, 12));
		combo.setForeground(TEXT);
		combo.setBackground(Color.WHITE);
		combo.setOpaque(true);
		combo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
				padding(2, 8, 2, 0)));
		try {
			combo.setUI(new nicelee.ui.item.FlatComboBoxUI());
		} catch (Exception e) {
			// 极端环境退回默认 UI
		}
		combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				java.awt.Component c = super.getListCellRendererComponent(list, value, index, isSelected,
						cellHasFocus);
				setBorder(padding(4, 10, 4, 10));
				if (!isSelected) {
					setBackground(Color.WHITE);
					setForeground(TEXT);
				}
				return c;
			}
		});
	}

	/** 表格统一外观 */
	public static void styleTable(JTable table) {
		table.setFont(font(Font.PLAIN, 12));
		table.setForeground(TEXT);
		table.setBackground(Color.WHITE);
		table.setGridColor(DIVIDER);
		table.setSelectionBackground(new Color(0xFF, 0xF0, 0xF4));
		table.setSelectionForeground(TEXT);
		table.setRowHeight(28);
		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(false);
		table.getTableHeader().setFont(font(Font.BOLD, 12));
		table.getTableHeader().setForeground(TEXT_SECOND);
		table.getTableHeader().setBackground(new Color(0xFA, 0xFB, 0xFC));
		table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
	}

	public static void styleTabbedPane(JTabbedPane tabs) {
		tabs.setOpaque(false);
		tabs.setFont(font(Font.PLAIN, 13));
		tabs.setForeground(TEXT_SECOND);
	}

	/** 生成占位图片(纯色) */
	public static BufferedImage solid(int width, int height, Color color) {
		BufferedImage image = new BufferedImage(Math.max(1, width), Math.max(1, height),
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(color);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.dispose();
		return image;
	}

	/** 默认头像占位图(灰色圆形 + 人形剪影)，登录成功后被真实头像替换 */
	public static ImageIcon avatarPlaceholder(int size) {
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try {
			enableAA(g);
			g.setColor(new Color(0xF1, 0xF3, 0xF6));
			g.fillOval(0, 0, size - 1, size - 1);
			g.setColor(new Color(0xC4, 0xCA, 0xD4));
			int head = Math.max(6, size * 3 / 10);
			int headX = (size - head) / 2;
			int headY = size / 5;
			g.fillOval(headX, headY, head, head);
			int bodyW = Math.max(10, size * 6 / 10);
			int bodyH = Math.max(8, size * 3 / 10);
			int bodyX = (size - bodyW) / 2;
			int bodyY = headY + head + Math.max(2, size / 14);
			g.fillRoundRect(bodyX, bodyY, bodyW, bodyH, bodyW / 2, bodyW / 2);
			g.setColor(BORDER);
			g.drawOval(0, 0, size - 1, size - 1);
		} finally {
			g.dispose();
		}
		return new ImageIcon(image);
	}
}
