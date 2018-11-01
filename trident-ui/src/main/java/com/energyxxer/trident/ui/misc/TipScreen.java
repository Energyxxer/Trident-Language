package com.energyxxer.trident.ui.misc;

import com.energyxxer.trident.global.Resources;

import javax.swing.JComponent;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class TipScreen extends JComponent {
    private Timer timer;
    private int milliseconds = 0;
    private int seconds = 0;

    private static final int TICK_RATE = 50;
    private static final int TIP_PERIOD = 15;

    private Random random = new Random();
    private ArrayList<String> tipList = new ArrayList<>();
    private int currentTipIndex = -1;
    private String currentTip = "";

    private TimerTask task;

    public TipScreen() {
        this.setOpaque(false);

        tipList.addAll(Resources.tips);
        shuffleList();

        timer = new Timer();

        this.setPreferredSize(new Dimension(800, 100));
    }

    public void start() {
        start(0);
    }

    public void start(int delay) {
        if(currentTipIndex < 0) {
            showNext();
        }
        task = new TimerTask() {
            @Override
            public void run() {
                tick();
            }
        };
        timer.scheduleAtFixedRate(task, delay, 1000/ TICK_RATE);
    }

    public void pause() {
        if(task != null) {
            task.cancel();
            timer.purge();
        }
    }

    private void tick() {
        milliseconds += 1000/ TICK_RATE;
        if(milliseconds >= 1000) {
            milliseconds = 0;
            if(++seconds >= TIP_PERIOD) {
                this.showNext();
                seconds = 0;
            }
        }
        this.repaint();
    }

    private void showNext() {
        if(++currentTipIndex >= tipList.size()) {
            shuffleList();
            currentTipIndex = 0;
        }
        currentTip = tipList.get(currentTipIndex);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = ((Graphics2D) g);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if(seconds < 1 || seconds >= TIP_PERIOD-1) {
            float transitionTime = milliseconds;
            if(seconds < 1) transitionTime += 1000;
            transitionTime /= 2000;
            transitionTime *= 2 * Math.PI;
            transitionTime -= Math.PI;

            float opacity = (float) (1-((Math.cos(transitionTime)+1)/2));

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        }

        g.setColor(this.getForeground());
        g.setFont(this.getFont());
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(currentTip, (this.getWidth()-metrics.stringWidth(currentTip))/2, (this.getHeight()-metrics.getHeight())/2);

        g.dispose();
    }

    private void shuffleList() {
        for(int i = 0; i < tipList.size(); i++) {
            int index = i+random.nextInt(tipList.size()-i-((i == 0) ? 1 : 0));
            String tip = tipList.get(index);
            tipList.remove(index);
            tipList.add(i, tip);
        }
    }
}
