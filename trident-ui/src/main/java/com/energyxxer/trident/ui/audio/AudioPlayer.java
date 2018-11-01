package com.energyxxer.trident.ui.audio;

import com.energyxxer.trident.ui.Tab;
import com.energyxxer.trident.ui.display.DisplayModule;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.swing.*;
import java.io.File;

public class AudioPlayer extends JPanel implements DisplayModule {

    private Tab tab;

    private MediaPlayer player;

    public AudioPlayer(Tab tab) {
        this.tab = tab;

        new JFXPanel();

        Media m = new Media(new File(tab.path).toURI().toString());
        player = new MediaPlayer(m);
        player.play();

        //TODO
    }

    @Override
    public void displayCaretInfo() {

    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public boolean canSave() {
        return false;
    }

    @Override
    public Object save() {
        return null;
    }

    @Override
    public void focus() {
        this.requestFocus();
    }
}
