package nicelee.ui.item;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.plaf.basic.BasicTabbedPaneUI;

import nicelee.ui.Theme;

/**
 * 扁平化标签栏: 选中项白底 + 主色下划线，未选中项半透明白底。
 * 只重写单个标签的绘制，保留 Basic 的滚动/多行布局逻辑。
 */
public class FlatTabbedPaneUI extends BasicTabbedPaneUI {

	@Override
	protected void installDefaults() {
		super.installDefaults();
		tabInsets = new Insets(7, 18, 7, 18);
		selectedTabPadInsets = new Insets(0, 0, 0, 0);
		tabAreaInsets = new Insets(8, 12, 0, 12);
		contentBorderInsets = new Insets(0, 0, 0, 0);
		tabRunOverlay = 0;
	}

	@Override
	protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
		return Math.max(34, super.calculateTabHeight(tabPlacement, tabIndex, fontHeight));
	}

	@Override
	protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect,
			Rectangle textRect) {
		Rectangle rect = rects[tabIndex];
		if (rect == null || rect.width <= 0 || rect.height <= 0) {
			return;
		}
		boolean selected = tabPane.getSelectedIndex() == tabIndex;
		paintTabBackground(g, tabPlacement, tabIndex, rect.x, rect.y, rect.width, rect.height, selected);
		if (tabPane.getTabComponentAt(tabIndex) == null) {
			paintTabTitle(g, tabIndex, rect, selected);
		}
	}

	private void paintTabTitle(Graphics g, int tabIndex, Rectangle rect, boolean selected) {
		String title = tabPane.getTitleAt(tabIndex);
		if (title == null || title.isEmpty()) {
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			Theme.enableAA(g2);
			g2.setFont(Theme.font(selected ? Font.BOLD : Font.PLAIN, 13));
			g2.setColor(selected ? Theme.TEXT : Theme.TEXT_SECOND);
			FontMetrics fm = g2.getFontMetrics();
			int textX = rect.x + (rect.width - fm.stringWidth(title)) / 2;
			int textY = rect.y + (rect.height - fm.getHeight()) / 2 + fm.getAscent();
			g2.drawString(title, textX, textY);
		} finally {
			g2.dispose();
		}
	}

	@Override
	protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h,
			boolean isSelected) {
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			Theme.enableAA(g2);
			if (isSelected) {
				g2.setColor(Theme.CARD);
				g2.fillRoundRect(x, y, w, h + 8, 10, 10);
				g2.setColor(Theme.PRIMARY);
				g2.fillRoundRect(x + 10, y + h - 4, Math.max(12, w - 20), 3, 3, 3);
			} else {
				g2.setColor(new Color(255, 255, 255, 90));
				g2.fillRoundRect(x, y + 3, w, h - 3, 10, 10);
			}
		} finally {
			g2.dispose();
		}
	}

	@Override
	protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h,
			boolean isSelected) {
		// 无边框
	}

	@Override
	protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
		// 内容区不画边框，交给各页面自己的卡片
	}

	@Override
	protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex,
			Rectangle iconRect, Rectangle textRect, boolean isSelected) {
		// 不画焦点框
	}
}
