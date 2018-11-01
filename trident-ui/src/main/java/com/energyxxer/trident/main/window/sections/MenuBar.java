package com.energyxxer.trident.main.window.sections;

import com.energyxxer.trident.global.*;
import com.energyxxer.trident.global.temp.projects.Project;
import com.energyxxer.trident.main.Trident;
import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.dialogs.ProjectProperties;
import com.energyxxer.trident.ui.dialogs.settings.Settings;
import com.energyxxer.trident.ui.styledcomponents.StyledMenu;
import com.energyxxer.trident.ui.styledcomponents.StyledMenuItem;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.trident.util.FileUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by User on 12/15/2016.
 */
public class MenuBar extends JMenuBar {

    private ThemeListenerManager tlm = new ThemeListenerManager();

    {
        tlm.addThemeChangeListener(t -> {
            this.setBackground(t.getColor(new Color(215, 215, 215), "MenuBar.background"));
            this.setBorder(BorderFactory.createMatteBorder(0, 0, Math.max(t.getInteger(1,"MenuBar.border.thickness"),0), 0, t.getColor(new Color(150, 150, 150), "MenuBar.border.color")));
        });

        this.setPreferredSize(new Dimension(0, 20));

        {
            StyledMenu menu = new StyledMenu(" File ");

            menu.setMnemonic(KeyEvent.VK_F);

            // --------------------------------------------------


            //StyledMenu newMenu = MenuItems.newMenu("New                    ");
            //menu.add(newMenu);

            // --------------------------------------------------

            menu.addSeparator();

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Save", "save");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
                menu.add(item);
            }

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Save As", "save_as");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 3));
                menu.add(item);
            }

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Save All", "save_all");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 10));
                menu.add(item);
            }

            // --------------------------------------------------

            menu.addSeparator();

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Close");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
                menu.add(item);
            }

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Close All");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, 3));
                menu.add(item);
            }


            // --------------------------------------------------

            menu.addSeparator();

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Move");
                menu.add(item);
            }

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Rename", "rename");
                menu.add(item);
            }

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Refresh", "reload");
                item.addActionListener(e -> TridentWindow.projectExplorer.refresh());
                menu.add(item);
            }

            // --------------------------------------------------

            menu.addSeparator();

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Exit");
                item.addActionListener(e -> TridentWindow.close());
                menu.add(item);
            }

            // --------------------------------------------------

            this.add(menu);
        }

        {
            StyledMenu menu = new StyledMenu(" Edit ");
            menu.setMnemonic(KeyEvent.VK_E);

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Undo", "undo");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
                menu.add(item);
            }

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Redo", "redo");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
                menu.add(item);
            }

            // --------------------------------------------------

            menu.addSeparator();

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Copy");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
                menu.add(item);
            }

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Cut");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
                menu.add(item);
            }

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Paste");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
                menu.add(item);
            }

            // --------------------------------------------------

            menu.addSeparator();

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Delete");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
                menu.add(item);
            }

            // --------------------------------------------------

            this.add(menu);
        }

        {
            StyledMenu menu = new StyledMenu(" Project ");
            menu.setMnemonic(KeyEvent.VK_P);

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Generate", "export");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, 9));
                menu.add(item);
            }

            // --------------------------------------------------

            menu.addSeparator();

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Properties");
                item.addActionListener(e -> {
                    Project selectedProject = Commons.getActiveProject();
                    if(selectedProject != null) ProjectProperties.show(selectedProject);
                });
                menu.add(item);
            }

            // --------------------------------------------------

            this.add(menu);
        }

        {
            StyledMenu menu = new StyledMenu(" Debug ");
            menu.setMnemonic(KeyEvent.VK_D);

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Reset Preferences", "warn");
                item.addActionListener(e -> {
                    int confirmation = JOptionPane.showConfirmDialog(null,
                            "        Are you sure you want to reset all saved settings?        ",
                            "Reset Preferences? ", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirmation == JOptionPane.YES_OPTION) {
                        Preferences.reset();
                    }
                });
                menu.add(item);
            }

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Edit Electron Theme");
                item.addActionListener(e -> {
                    TabManager.openTab(System.getProperty("user.home") + File.separator + "Trident" + File.separator + "resources" + File.separator + "themes" + File.separator + "gui" + File.separator + "Electron Dark.properties");
                });
                menu.add(item);
            }

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Refresh Native Library");
                item.addActionListener(e -> {
                    //Resources.nativeLib.refresh();
                });
                menu.add(item);
            }

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Extract Native Library");
                item.addActionListener(e -> {
                    try {
                        URL url = Trident.class.getResource("/natives/");

                        File extractedNatives = new File(System.getProperty("user.home") + File.separator + "Trident" + File.separator + "natives" + File.separator);
                        FileUtil.deleteFolder(extractedNatives);
                        extractedNatives.mkdir();

                        String protocol = url.getProtocol();
                        if (protocol.equals("file")) {
                            File packagedNatives = new File(url.getFile());

                            Files.walkFileTree(packagedNatives.toPath(), new FileVisitor<Path>() {
                                @Override
                                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                                    return CONTINUE;
                                }

                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                    File newFile = new File(System.getProperty("user.home") + File.separator + "Trident" + File.separator + "natives" + File.separator + (packagedNatives.toPath().relativize(file)));
                                    newFile.mkdirs();
                                    Files.copy(file, newFile.toPath(), REPLACE_EXISTING);
                                    TridentWindow.setStatus("Created file '" + newFile + "'");
                                    return CONTINUE;
                                }

                                @Override
                                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                                    return CONTINUE;
                                }

                                @Override
                                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                                    return CONTINUE;
                                }
                            });
                        } else if (protocol.equals("jar")) {
                            String file = url.getFile();
                            int bangIndex = file.indexOf('!');
                            file = new URL(file.substring(0, bangIndex)).getFile();
                            ZipFile zip = new ZipFile(file);
                            Enumeration<? extends ZipEntry> entries = zip.entries();
                            while(entries.hasMoreElements()) {
                                ZipEntry entry = entries.nextElement();
                                if(entry.getName().startsWith("natives/") && !entry.isDirectory()) {
                                    File newFile = new File(extractedNatives.getPath() + entry.getName().substring(7));
                                    newFile.mkdirs();
                                    Files.copy(zip.getInputStream(entry), newFile.toPath(), REPLACE_EXISTING);
                                    TridentWindow.setStatus("Created file '" + newFile + "'");
                                }
                            }
                        }
                        TridentWindow.setStatus("Native Library extraction completed successfully at '" + extractedNatives.getPath() + "'");
                    } catch (IOException x) {
                        TridentWindow.setStatus(new Status(Status.ERROR, "An error occurred during extraction: " + x.getMessage()));
                        x.printStackTrace();
                    }
                });
                menu.add(item);
            }

            // --------------------------------------------------

            this.add(menu);
        }

        {
            StyledMenu menu = new StyledMenu(" Window ");
            menu.setMnemonic(KeyEvent.VK_W);

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("Settings", "cog");

                item.addActionListener(e -> Settings.show());
                menu.add(item);
            }

            this.add(menu);
        }

        {
            StyledMenu menu = new StyledMenu("Help");
            menu.setMnemonic(KeyEvent.VK_H);

            // --------------------------------------------------

            {
                StyledMenuItem item = new StyledMenuItem("About");

                item.addActionListener(e -> AboutPane.INSTANCE.setVisible(true));
                menu.add(item);
            }

            this.add(menu);
        }
    }
}
