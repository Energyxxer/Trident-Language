package com.energyxxer.trident.ui.audio;

import com.energyxxer.trident.ui.display.DisplayModule;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.swing.*;
import java.io.File;

public class AudioPlayer extends JPanel implements DisplayModule {

    private File file;

    private MediaPlayer player;

    public AudioPlayer(File file) {
        this.file = file;

        new JFXPanel();

        Media m = new Media(file.toURI().toString());
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
