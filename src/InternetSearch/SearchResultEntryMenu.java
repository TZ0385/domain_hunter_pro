package InternetSearch;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.StringUtils;

import com.bit4woo.utilbox.utils.SystemUtils;
import com.bit4woo.utilbox.utils.TextUtils;

import GUI.GUIMain;
import burp.BurpExtender;
import config.ConfigManager;
import config.ConfigName;
import domain.target.AssetTrustLevel;
import utils.PortScanUtils;

public class SearchResultEntryMenu extends JPopupMenu {


	private static final long serialVersionUID = 1L;
	PrintWriter stdout = BurpExtender.getStdout();
	PrintWriter stderr = BurpExtender.getStderr();
	GUIMain guiMain;
	SearchTable searchTable;
	SearchTableModel searchTableModel;

	/**
	 * 这处理传入的行index数据是经过转换的 model中的index，不是原始的JTable中的index。
	 *
	 * @param lineTable
	 * @param modelRows
	 * @param columnIndex
	 */
	SearchResultEntryMenu(final GUIMain guiMain, SearchTable searchTable, final int[] modelRows, final int columnIndex,String sourceTabName) {
		this.guiMain = guiMain;
		this.searchTable = searchTable;
		this.searchTableModel = searchTable.getSearchTableModel();

		JMenuItem itemNumber = new JMenuItem(new AbstractAction(modelRows.length + " Items Selected") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
			}
		});


		JMenuItem copyUrlItem = new JMenuItem(new AbstractAction("Copy URL") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					java.util.List<String> urls = searchTableModel.getMultipleValue(modelRows, "URL");
					String textUrls = String.join(System.lineSeparator(), urls);
					SystemUtils.writeToClipboard(textUrls);
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem copyHostItem = new JMenuItem(new AbstractAction("Copy Host") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					java.util.List<String> urls = searchTableModel.getMultipleValue(modelRows, "Host");
					String textUrls = String.join(System.lineSeparator(), urls);
					SystemUtils.writeToClipboard(textUrls);
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem copyIPItem = new JMenuItem(new AbstractAction("Copy IP") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					java.util.List<String> ip_list = searchTableModel.getMultipleValue(modelRows, "IP");
					String textUrls = String.join(System.lineSeparator(), ip_list);
					SystemUtils.writeToClipboard(textUrls);
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem copyRootDomainItem = new JMenuItem(new AbstractAction("Copy Root Domain") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					java.util.List<String> rootDomains = searchTableModel.getMultipleValue(modelRows, SearchTableHead.RootDomain);
					rootDomains = TextUtils.deduplicate(rootDomains);
					String textUrls = String.join(System.lineSeparator(), rootDomains);
					SystemUtils.writeToClipboard(textUrls);
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}
		});

		
		JMenuItem copyRootDomainWithIpItem = new JMenuItem(new AbstractAction("Copy Root Domain With IP") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					java.util.List<java.util.List<String>> lines = searchTableModel.getMultipleValueOfMultipleColumn(modelRows,SearchTableHead.IP, SearchTableHead.RootDomain);
					
					Map<String, HashSet<String>> result = new HashMap<>();
					for (List<String> line : lines) {
					    String ip = line.get(0);
					    String rootDomain = line.get(1);

					    // 如果这个 IP 不存在，就初始化一个新集合
					    result.computeIfAbsent(ip, k -> new HashSet<>()).add(rootDomain);
					}
					
					String text ="";
					for (Map.Entry<String, HashSet<String>> entry : result.entrySet()) {
					    String ip = entry.getKey() + " --->"+System.lineSeparator();
					    HashSet<String> domains = entry.getValue();
					    
					    text = text + ip;
					    text = text + String.join(System.lineSeparator(), domains);
					}

					SystemUtils.writeToClipboard(text);
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}
		});
		
		JMenuItem genPortScanCmd = new JMenuItem(new AbstractAction("Copy Port Scan Cmd") {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					java.util.List<String> ip_list = searchTableModel.getMultipleValue(modelRows, "IP");
					String nmapPath = ConfigManager.getStringConfigByKey(ConfigName.PortScanCmd);
					PortScanUtils.genCmdAndCopy(nmapPath, new HashSet<>(ip_list));
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}
		});


		JMenuItem openURLwithBrowserItem = new JMenuItem(new AbstractAction("Open With Browser(double click url)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					java.util.List<String> urls = searchTableModel.getMultipleValue(modelRows, "URL");
					if (urls.size() >= 50) {//避免一次开太多网页导致系统卡死
						return;
					}
					for (String url : urls) {
						SystemUtils.browserOpen(url, ConfigManager.getStringConfigByKey(ConfigName.BrowserPath));
					}
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem addToTargetItem = new JMenuItem(new AbstractAction("Add Host/Domain To Target") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker() {
					@Override
					protected Object doInBackground() throws Exception {
						try {
							List<SearchResultEntry> entries = searchTableModel.getEntries(modelRows);
							for (SearchResultEntry entry : entries) {
								entry.AddToTarget(null, null);
							}
							guiMain.getDomainPanel().saveDomainDataToDB();
						} catch (Exception e1) {
							e1.printStackTrace(stderr);
						}
						return null;
					}
				}.execute();
			}
		});

		JMenuItem addToTargetConfirmItem = new JMenuItem(new AbstractAction("Add Host/Domain To Target (Confirm Level)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker() {
					@Override
					protected Object doInBackground() throws Exception {
						try {
							List<SearchResultEntry> entries = searchTableModel.getEntries(modelRows);
							for (SearchResultEntry entry : entries) {
								entry.AddToTarget(AssetTrustLevel.Confirm, null);
							}
							guiMain.getDomainPanel().saveDomainDataToDB();
						} catch (Exception e1) {
							e1.printStackTrace(stderr);
						}
						return null;
					}
				}.execute();
			}
		});

		JMenuItem addToTargetWithCommentItem = new JMenuItem(new AbstractAction("Add Host/Domain To Target With Comment") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker() {
					@Override
					protected Object doInBackground() throws Exception {
						try {
							String comment = JOptionPane.showInputDialog("Comment", "");
							if (StringUtils.isBlank(comment)) {
								return null;
							}
							List<SearchResultEntry> entries = searchTableModel.getEntries(modelRows);
							for (SearchResultEntry entry : entries) {
								entry.AddToTarget(null, comment);
							}
							guiMain.getDomainPanel().saveDomainDataToDB();
						} catch (Exception e1) {
							e1.printStackTrace(stderr);
						}
						return null;
					}
				}.execute();
			}
		});

		JMenuItem addToTargetnotTargetItem = new JMenuItem(new AbstractAction("Add Host/Domain To Target (Not Target)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker() {
					@Override
					protected Object doInBackground() throws Exception {
						try {
							List<SearchResultEntry> entries = searchTableModel.getEntries(modelRows);
							for (SearchResultEntry entry : entries) {
								entry.AddToTarget(AssetTrustLevel.NonTarget, null);
							}
							guiMain.getDomainPanel().saveDomainDataToDB();
						} catch (Exception e1) {
							e1.printStackTrace(stderr);
						}
						return null;
					}
				}.execute();
			}
		});

		JMenuItem deleteFromTarget = new JMenuItem(new AbstractAction("Delete From Target") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker() {
					@Override
					protected Object doInBackground() throws Exception {
						try {
							List<SearchResultEntry> entries = searchTableModel.getEntries(modelRows);
							for (SearchResultEntry entry : entries) {
								entry.RemoveFromTarget();
							}
							guiMain.getDomainPanel().saveDomainDataToDB();
						} catch (Exception e1) {
							e1.printStackTrace(stderr);
						}
						return null;
					}
				}.execute();
			}
		});


		JMenuItem addIPToBlackListItem = new JMenuItem(new AbstractAction("Add IP To Black List") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker() {
					@Override
					protected Object doInBackground() throws Exception {
						try {
							List<SearchResultEntry> entries = searchTableModel.getEntries(modelRows);
							Set<String> blackIPSet = new HashSet<String>();
							for (SearchResultEntry entry : entries) {
								blackIPSet.addAll(entry.getIPSet());
							}
							guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet().addAll(blackIPSet);
							guiMain.getDomainPanel().saveDomainDataToDB();
						} catch (Exception e1) {
							e1.printStackTrace(stderr);
						}
						return null;
					}
				}.execute();
			}
		});
		
		
		/**
		 * 单纯从title记录中删除,不做其他修改
		 */
		JMenuItem removeItem = new JMenuItem(new AbstractAction("Delete Entry") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker<Map,Map>(){
					@Override
					protected Map doInBackground() throws Exception {
						int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE these items ?");
						if (result == JOptionPane.YES_OPTION) {
							searchTableModel.removeRows(modelRows);
						}
						return null;
					}
				}.execute();
			}
		});
		removeItem.setToolTipText("Just Delete Entry In Title Panel");

		this.add(itemNumber);

		this.addSeparator();

		//常用多选操作
		this.add(addToTargetItem);
		this.add(addToTargetConfirmItem);
		this.add(addToTargetWithCommentItem);

		this.addSeparator();
		this.add(addToTargetnotTargetItem);
		this.add(deleteFromTarget);
		this.add(addIPToBlackListItem);
		this.add(removeItem);

		this.addSeparator();

		this.add(copyUrlItem);
		this.add(copyHostItem);
		this.add(copyIPItem);
		this.add(copyRootDomainItem);
		this.add(copyRootDomainWithIpItem);
		this.add(openURLwithBrowserItem);
		this.add(genPortScanCmd);

		//搜索
		this.addSeparator();
		SearchEngine.AddSearchMenuItems(this, searchTableModel, modelRows, columnIndex,sourceTabName);
		this.addSeparator();

	}
}
