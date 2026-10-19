package nicelee.ui.item;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;

import nicelee.ui.Global;
import nicelee.ui.Theme;

/**
 * 统一风格的按钮：圆角、扁平、带 hover/pressed 状态。
 * PRIMARY 用于主操作(查找/下载)，SECONDARY 用于普通操作。
 */
public class MJButton extends JButton {

	private static final long serialVersionUID = -2872601692608872176L;

	public enum Style {
		PRIMARY, SECONDARY, GHOST
	}

	private Style style = Style.SECONDARY;
	private final boolean flat = Global.btnStyle;

	public MJButton() {
		super();
		init();
	}

	public MJButton(String text) {
		super(text);
		init();
	}

	public MJButton(String text, Style style) {
		super(text);
		this.style = style;
		init();
	}

	public MJButton(Icon icon) {
		super(icon);
		init();
	}

	private void init() {
		if (!flat) {
			return;
		}
		setUI(new BasicButtonUI());
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setOpaque(false);
		setRolloverEnabled(true);
		setFont(Theme.font(Font.PLAIN, 13));
		setBorder(new EmptyBorder(6, 14, 6, 14));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	@Override
	public Color getForeground() {
		if (!flat) {
			return super.getForeground();
		}
		if (!isEnabled()) {
			return Theme.TEXT_MUTED;
		}
		return style == Style.PRIMARY ? Color.WHITE : Theme.TEXT;
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		if (!flat) {
			super.paintComponent(graphics);
			return;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			Theme.enableAA(g);
			int w = getWidth();
			int h = getHeight();
			int radius = Math.min(8, Math.max(4, h / 3));
			boolean pressed = getModel().isPressed();
			boolean rollover = getModel().isRollover();
			Color background;
			Color border;
			if (style == Style.PRIMARY) {
				if (!isEnabled()) {
					background = Theme.PRIMARY_DISABLED;
				} else if (pressed) {
					background = Theme.PRIMARY_PRESS;
				} else if (rollover) {
					background = Theme.PRIMARY_HOVER;
				} else {
					background = Theme.PRIMARY;
				}
				border = background;
			} else if (style == Style.GHOST) {
				background = rollover ? new Color(0x00, 0x00, 0x00, 0x0A) : new Color(0, 0, 0, 0);
				border = new Color(0, 0, 0, 0);
			} else {
				if (!isEnabled()) {
					background = new Color(0xF3, 0xF4, 0xF6);
				} else if (pressed) {
					background = new Color(0xEC, 0xEE, 0xF1);
				} else if (rollover) {
					background = new Color(0xF7, 0xF8, 0xFA);
				} else {
					background = Color.WHITE;
				}
				border = isEnabled() ? Theme.BORDER : new Color(0xF0, 0xF1, 0xF3);
			}
			g.setColor(background);
			g.fillRoundRect(0, 0, w, h, radius, radius);
			if (border.getAlpha() > 0) {
				g.setColor(border);
				g.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
			}
		} finally {
			g.dispose();
		}
		super.paintComponent(graphics);
	}

	@Override
	public Insets getInsets() {
		Insets insets = super.getInsets();
		if (insets == null || (!flat)) {
			return insets;
		}
		return insets;
	}

	public Style getStyle() {
		return style;
	}

	public void setStyle(Style style) {
		this.style = style;
		repaint();
	}
}
