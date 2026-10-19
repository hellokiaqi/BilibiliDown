package nicelee.ui.item;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicComboBoxUI;

import nicelee.ui.Theme;

/**
 * 扁平下拉框: 白底 + 细边框 + 手绘箭头，替换默认的金属质感样式。
 */
public class FlatComboBoxUI extends BasicComboBoxUI {

	@Override
	protected JButton createArrowButton() {
		JButton button = new JButton() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				try {
					Theme.enableAA(g2);
					g2.setColor(Theme.TEXT_MUTED);
					g2.setStroke(new java.awt.BasicStroke(1.6f, java.awt.BasicStroke.CAP_ROUND,
							java.awt.BasicStroke.JOIN_ROUND));
					int cx = getWidth() / 2;
					int cy = getHeight() / 2;
					g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
					g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
				} finally {
					g2.dispose();
				}
			}
		};
		button.setBorder(BorderFactory.createEmptyBorder());
		button.setContentAreaFilled(false);
		button.setFocusable(false);
		button.setOpaque(false);
		button.setPreferredSize(new Dimension(22, 22));
		return button;
	}

	@Override
	public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
		g.setColor(Color.WHITE);
		g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
	}

	@Override
	protected Dimension getDefaultSize() {
		return new Dimension(96, 30);
	}
}
