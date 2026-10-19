package nicelee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import nicelee.ui.item.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import nicelee.bilibili.INeedLogin;
import nicelee.bilibili.PackageScanLoader;
import nicelee.bilibili.util.CmdUtil;
import nicelee.bilibili.util.ConfigUtil;
import nicelee.bilibili.util.HttpCookies;
import nicelee.bilibili.util.RepoUtil;
import nicelee.bilibili.util.ResourcesUtil;
import nicelee.bilibili.util.SysUtil;
import nicelee.ui.item.FlatTabbedPaneUI;
import nicelee.ui.item.MJTitleBar;
import nicelee.ui.thread.BatchDownloadRbyRThread;
import nicelee.ui.thread.CookieRefreshThread;
import nicelee.ui.thread.LoginThread;
import nicelee.ui.thread.MonitoringThread;

public class FrameMain extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	JTabbedPane jTabbedpane;// 存放选项卡的组件
	MJTitleBar titleBar;// 标题栏组件

	public static void main(String[] args) {
		System.out.println();
		// System.getProperties().setProperty("file.encoding", "utf-8");
		boolean isFFmpegSupported = SysUtil.surportFFmpegOfficially();
		System.out.println("Java version:" + System.getProperty("java.specification.version"));
		System.out.println(ResourcesUtil.baseDirectory());
		// 读取配置文件
		ConfigUtil.initConfigs();
		// -v 打印版本，然后退出
		if(args.length == 1 && "-v".equalsIgnoreCase(args[0])) {
			System.out.println(Global.version);
			System.exit(0);
		}
		// 初始化 - 检查对数据文件夹是否有“写”的权限
		InitCheck.checkFileAccess();
		// 显示过渡动画
		Global.frWaiting = new FrameWaiting();
		Global.frWaiting.start();

		if (Global.lockCheck) {
			if (ConfigUtil.isRunning()) {
				Global.frWaiting.stop();
				JOptionPane.showMessageDialog(null, "程序已经在运行!", "请注意!!", JOptionPane.WARNING_MESSAGE);
				return;
			}
			ConfigUtil.createLock();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				ConfigUtil.deleteLock();
			}));
		}
		
		nicelee.bilibili.util.custom.System.init(Global.syncServerTime);
//		// 如果存在hosts文件，那么使之生效
//		if (HostSetUtil.readHostsFromFile("config/hosts.config")) {
//			HostSetUtil.injectHosts();
//		}
		// 初始化主题
		initUITheme();
		// 初始化UI
		FrameMain main = new FrameMain();
		main.InitUI();

		// 初始化监控线程，用于刷新下载面板
		MonitoringThread th = new MonitoringThread();
		th.start();

		// 尝试刷新cookie
		INeedLogin inl = new INeedLogin();
		String cookiesStr = inl.readCookies();
		if (cookiesStr != null) {
			Global.needToLogin = true;
			if(Global.tryRefreshCookieOnStartup && !Global.runWASMinBrowser) {
				HttpCookies.setGlobalCookies(HttpCookies.convertCookies(cookiesStr));
				CookieRefreshThread.showTips = false;
				CookieRefreshThread thCR = CookieRefreshThread.newInstance();
				thCR.start();
				try {
					thCR.join();
				} catch (InterruptedException e1) {
				}
				CookieRefreshThread.showTips = true;
			}
		}
		// 初始化 - 登录
		LoginThread loginTh = new LoginThread();
		loginTh.start();

		// 初始化 - ffmpeg环境判断
		InitCheck.checkFFmpeg(isFFmpegSupported);

		//
		if (Global.saveToRepo) {
			RepoUtil.init(false);
		}
//		FrameQRCode qr = new FrameQRCode("https://www.bilibili.com/");
//		qr.initUI();
//		qr.dispose();
		// 预扫描加载类
		PackageScanLoader.validParserClasses.isEmpty();
		if(Global.batchDownloadRbyRRunOnStartup) {
			// 开始按计划周期性批量下载
			new Thread(new Runnable() {
				@Override
				public void run() {
					// 等待相关线程运行完毕
					try {
						loginTh.join();
					} catch (InterruptedException e) {}
					new BatchDownloadRbyRThread(Global.batchDownloadConfigName).start();
				}
			}).start();
		}
		System.out.println("如果过度界面显示时间过长，可双击跳过");
		try {
			while (Global.frWaiting.isVisible()) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			Global.frWaiting.stop();
		}
		Global.frWaiting = null;
		main.setVisible(true);
		main.setExtendedState(JFrame.NORMAL);
		main.toFront();
	}

	/**
	 * 
	 */
	static void initUITheme() {
		try {
			if (!Global.themeDefault) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				Font font = Theme.font(Font.PLAIN, 12);
				Enumeration<Object> keys = UIManager.getDefaults().keys();
				while (keys.hasMoreElements()) {
					Object key = keys.nextElement();
					Object value = UIManager.get(key);
					if (value instanceof javax.swing.plaf.FontUIResource) {
						UIManager.put(key, font);
					}
				}
			}
			// 统一各控件的默认字体，避免不同 L&F 下大小不一
			Font uiFont = Theme.font(Font.PLAIN, 12);
			String[] fontKeys = { "Label.font", "Button.font", "ComboBox.font", "TextField.font",
					"TextArea.font", "TabbedPane.font", "Table.font", "TableHeader.font", "OptionPane.messageFont",
					"OptionPane.buttonFont", "Menu.font", "MenuItem.font", "PopupMenu.font", "CheckBox.font",
					"RadioButton.font", "TitledBorder.font", "ToolTip.font" };
			for (String key : fontKeys) {
				UIManager.put(key, uiFont);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public void InitUI() {

		this.setTitle("BiliBili Down~~" + Global.version);
		this.setSize(1200, 745);
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setBackground(Color.WHITE);
		URL iconURL = this.getClass().getResource("/resources/favicon.png");
		ImageIcon icon = new ImageIcon(iconURL);
		this.setIconImage(icon.getImage());

		// pane 作为内容容器
		JPanel pane = new JPanel();
		pane.setBackground(Color.WHITE);
		pane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Theme.BORDER));
		// 添加标题栏
		titleBar = new MJTitleBar(this, true, true);
		pane.add(titleBar);

		jTabbedpane = new JTabbedPane();
		Global.tabs = jTabbedpane;
		jTabbedpane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		jTabbedpane.setPreferredSize(new Dimension(1194, 706));
		jTabbedpane.setBorder(null);
		jTabbedpane.setUI(new FlatTabbedPaneUI());
		Theme.styleTabbedPane(jTabbedpane);
		// Index Tab
		Global.index = new TabIndex(jTabbedpane);
		jTabbedpane.addTab("主页", Global.index);
		// 下载页
		Global.downloadTab = new TabDownload();
		jTabbedpane.addTab("下载页", Global.downloadTab);
		// 作品页
//		JLabel label = new JLabel("作品页");
//		TabVideo tab = new TabVideo(label);
//		jTabbedpane.addTab("作品页", tab);
//		jTabbedpane.setTabComponentAt(jTabbedpane.indexOfComponent(tab), label);
//		jTabbedpane.addTab("设置页", new TabSettings(jTabbedpane));
		
		pane.add(jTabbedpane);
		this.setContentPane(pane);

		// 选中标签页时加粗其标题，未选中恢复常规字重
		jTabbedpane.addChangeListener(e -> updateTabStyles());
		updateTabStyles();
		// 关闭窗口时
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				CmdUtil.deleteAllInactiveCmdTemp();
			}
		});
//		this.setVisible(true);
		SysTray.buildSysTray(this, icon.getImage());
	}

	/**
	 * 让标签页标题在选中/未选中时呈现不同字重与颜色(仅作用于自定义TabComponent)
	 */
	void updateTabStyles() {
		if (jTabbedpane == null) {
			return;
		}
		int selected = jTabbedpane.getSelectedIndex();
		for (int i = 0; i < jTabbedpane.getTabCount(); i++) {
			java.awt.Component comp = jTabbedpane.getTabComponentAt(i);
			if (comp instanceof JLabel) {
				JLabel label = (JLabel) comp;
				label.setFont(Theme.font(i == selected ? Font.BOLD : Font.PLAIN, 13));
				label.setForeground(i == selected ? Theme.TEXT : Theme.TEXT_SECOND);
			}
		}
	}

	@Override
	public void setTitle(String title) {
		super.setTitle(title);
		if (titleBar != null) {
			titleBar.setTitle(title);
		}
	}

}
