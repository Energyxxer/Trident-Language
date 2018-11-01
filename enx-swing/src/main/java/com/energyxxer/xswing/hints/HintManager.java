package com.energyxxer.xswing.hints;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class HintManager {
    private final JFrame owner;
    private ArrayList<Hint> hints = new ArrayList<>();
    private Timer timer = new Timer();

    private static final int FADE_DISTANCE = 30;
    private static final int FORCE_HIDE_DISTANCE = 200;

    public HintManager(JFrame owner) {
        this.owner = owner;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for(int i = 0; i < hints.size(); i++) {
                    Hint hint = hints.get(i);
                    if(hint.disposed) {
                        hints.remove(i);
                        i--;
                        continue;
                    }
                    if(hint.timer < 0) {
                        hint.timer++;
                        if(!hint.shouldContinueShowing()) {
                            hint.timer = 0;
                            continue;
                        }
                        if(hint.timer == 0) {
                            hint.forceShow();
                        }
                    } else if(hint.timer == 0) {
                        if(hint.isShowing()) {
                            double distance = (hint.isInteractive()) ? hint.getDistanceFromPoint(MouseInfo.getPointerInfo().getLocation()) : 0;
                            if(!hint.isInteractive() || distance >= FADE_DISTANCE) {
                                if(!hint.shouldContinueShowing()) {
                                    hint.timer = hint.outDelay;
                                }
                            }
                        }
                    } else {
                        double distance = hint.getDistanceFromPoint(MouseInfo.getPointerInfo().getLocation());
                        if(distance >= FORCE_HIDE_DISTANCE) {
                            hint.timer = 0;
                            hint.dismiss();
                            continue;
                        }
                        if(hint.shouldContinueShowing()) {
                            hint.timer = 0;
                            continue;
                        }
                        hint.timer--;
                        if(hint.timer == 0) {
                            hint.dismiss();
                        }
                    }
                }
            }
        }, 0, 10);
    }

    public TextHint createTextHint(String text) {
        TextHint newHint = new TextHint(owner, text);
        this.hints.add(newHint);
        return newHint;
    }
}
