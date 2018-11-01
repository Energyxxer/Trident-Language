package com.energyxxer.xswing.hints;

import com.energyxxer.util.Confirmation;
import com.energyxxer.util.Constant;
import com.energyxxer.util.Disposable;
import com.energyxxer.xswing.Padding;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Hint extends JDialog implements MouseListener, Disposable {
    private static final int MARGIN = 10;
    private static final int DISTANCE = 5;
    private static final int ARROW_HEIGHT = 10;
    private static final int ARROW_WIDTH = (int) (2*Math.ceil(Math.tan(Math.toRadians(30))*ARROW_HEIGHT));

    public static final Constant ABOVE = new Constant("ABOVE");
    public static final Constant BELOW = new Constant("BELOW");
    public static final Constant LEFT = new Constant("LEFT");
    public static final Constant RIGHT = new Constant("RIGHT");

    private Confirmation visibilityCheck = null;

    boolean disposed = false;

    int inDelay = 20;
    int outDelay = 60;

    int timer = 0;

    private int x,y;
    private Component content;
    private boolean rollover = false;
    private int arrowOffset = 0;

    private Constant preferredPos = BELOW;
    private Constant realPos = BELOW;
    private boolean interactive = false;

    private Color background = new Color(60, 63, 65);
    private Color border = new Color(91, 93, 95);

    private MouseAdapter contentAdapter;

    public Hint(JFrame owner) {
        super(owner);
        this.setUndecorated(true);
        this.setBackground(new Color(0,0,0,0));
        this.setContentPane(new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {

                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g.setColor(border);
                g.fillRect(MARGIN-1, MARGIN-1, this.getWidth()-2*MARGIN+2, this.getHeight()-2*MARGIN+2);
                g.setColor(background);
                g.fillRect(MARGIN, MARGIN, this.getWidth()-2*MARGIN, this.getHeight()-2*MARGIN);

                if(realPos == ABOVE) {
                    int[] xPoints = new int[] {this.getWidth()/2-ARROW_WIDTH/2+arrowOffset, this.getWidth()/2+arrowOffset, this.getWidth()/2+ARROW_WIDTH/2+arrowOffset};
                    int[] yPoints = new int[]{this.getHeight()-MARGIN, this.getHeight(), this.getHeight()-MARGIN};
                    g.setColor(border);
                    g.fillPolygon(xPoints, yPoints,3);
                    yPoints[0]--;
                    yPoints[1] -= 2;
                    yPoints[2]--;
                    g.setColor(background);
                    g.fillPolygon(xPoints, yPoints,3);
                } else if(realPos == BELOW) {
                    int[] xPoints = new int[] {this.getWidth()/2-ARROW_WIDTH/2+arrowOffset, this.getWidth()/2+arrowOffset, this.getWidth()/2+ARROW_WIDTH/2+arrowOffset};
                    int[] yPoints = new int[]{MARGIN, 0, MARGIN};
                    g.setColor(border);
                    g.fillPolygon(xPoints, yPoints,3);
                    yPoints[0]++;
                    yPoints[1] += 2;
                    yPoints[2]++;
                    g.setColor(background);
                    g.fillPolygon(xPoints, yPoints,3);
                } else if(realPos == LEFT) {
                    int[] xPoints = new int[] {this.getWidth()-MARGIN, this.getWidth(), this.getWidth()-MARGIN};
                    int[] yPoints = new int[]{this.getHeight()/2-ARROW_WIDTH/2+arrowOffset, this.getHeight()/2+arrowOffset, this.getHeight()/2+ARROW_WIDTH/2+arrowOffset};
                    g.setColor(border);
                    g.fillPolygon(xPoints, yPoints,3);
                    xPoints[0]--;
                    xPoints[1] -= 2;
                    xPoints[2]--;
                    g.setColor(background);
                    g.fillPolygon(xPoints, yPoints,3);
                } else if(realPos == RIGHT) {
                    int[] xPoints = new int[] {MARGIN, 0, MARGIN};
                    int[] yPoints = new int[]{this.getHeight()/2-ARROW_WIDTH/2+arrowOffset, this.getHeight()/2+arrowOffset, this.getHeight()/2+ARROW_WIDTH/2+arrowOffset};
                    g.setColor(border);
                    g.fillPolygon(xPoints, yPoints,3);
                    xPoints[0]++;
                    xPoints[1] += 2;
                    xPoints[2]++;
                    g.setColor(background);
                    g.fillPolygon(xPoints, yPoints,3);
                }

            }
        });
        this.getContentPane().add(new Padding(MARGIN),BorderLayout.NORTH);
        this.getContentPane().add(new Padding(MARGIN),BorderLayout.SOUTH);
        this.getContentPane().add(new Padding(MARGIN),BorderLayout.WEST);
        this.getContentPane().add(new Padding(MARGIN),BorderLayout.EAST);

        this.addMouseListener(this);
    }

    public Hint(JFrame owner, Component content) {
        this(owner);
        setContent(content);
    }

    public void update() {
        this.revalidate();
        SwingUtilities.invokeLater(this::pack);
        this.updateLocation();
        this.repaint();
    }

    private void updateLocation() {

        int w = this.getWidth()-2*MARGIN;
        int h = this.getHeight()-2*MARGIN;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Point screenSize = ge.getCenterPoint();
        screenSize.x *= 2;
        screenSize.y *= 2;

        Constant current = preferredPos;

        Rectangle proposedRect = new Rectangle(0,0,w,h);
        if(current == ABOVE) {
            proposedRect.x = this.x - w/2;
            proposedRect.y = this.y - DISTANCE - MARGIN - h;
        } else if(current == BELOW) {
            proposedRect.x = this.x - w/2;
            proposedRect.y = this.y + DISTANCE + MARGIN;
        } else if(current == LEFT) {
            proposedRect.x = this.x - DISTANCE - MARGIN - w;
            proposedRect.y = this.y - h/2;
        } else if(current == RIGHT) {
            proposedRect.x = this.x + DISTANCE + MARGIN;
            proposedRect.y = this.y - h/2;
        }

        boolean xFlipped = false;
        boolean yFlipped = false;

        if(current == ABOVE || current == BELOW) {
            if(proposedRect.y < 0 || proposedRect.y+proposedRect.height >= screenSize.y) {
                current = (current == ABOVE) ? BELOW : ABOVE;
                yFlipped = true;
            }
            if(proposedRect.x < 0) {
                arrowOffset = proposedRect.x;
                proposedRect.x = 0;
            } else if(proposedRect.x+proposedRect.width >= screenSize.x) {
                arrowOffset = proposedRect.x+proposedRect.width - screenSize.x;
                proposedRect.x = screenSize.x - proposedRect.width;
            } else arrowOffset = 0;
        } else if(current == LEFT || current == RIGHT) {
            if(proposedRect.x < 0 || proposedRect.x+proposedRect.width >= screenSize.x) {
                current = (current == LEFT) ? RIGHT : LEFT;
                xFlipped = true;
            }
            if(proposedRect.y < 0) {
                arrowOffset = proposedRect.y;
                proposedRect.y = 0;
            } else if(proposedRect.y+proposedRect.height >= screenSize.y) {
                arrowOffset = proposedRect.y+proposedRect.height - screenSize.y;
                proposedRect.y = screenSize.y - proposedRect.height;
            } else arrowOffset = 0;
        }

        realPos = current;

        if(xFlipped) {
            if(current == LEFT) {
                proposedRect.x = this.x - DISTANCE - MARGIN - w;
            } else {
                proposedRect.x = this.x + DISTANCE + MARGIN;
            }
        }
        if(yFlipped) {
            if(current == ABOVE) {
                proposedRect.y = this.y - DISTANCE - MARGIN - h;
            } else {
                proposedRect.y = this.y + DISTANCE + MARGIN;
            }
        }

        this.setLocation(proposedRect.x - MARGIN, proposedRect.y - MARGIN);
    }

    public void forceShow() {
        this.update();
        this.setFocusableWindowState(false);
        this.setVisible(true);
        this.setFocusableWindowState(interactive);
    }

    public void show(Point loc, Confirmation visibilityCheck) {
        this.x = loc.x;
        this.y = loc.y;
        this.visibilityCheck = visibilityCheck;
        if(timer >= 0) dismiss();
        timer = -inDelay;

    }

    public void updateLocation(Point loc) {
        this.x = loc.x;
        this.y = loc.y;
        updateLocation();
    }

    public void updateCondition(Confirmation visibilityCheck) {
        this.visibilityCheck = visibilityCheck;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    public void dismiss() {
        this.setVisible(false);
        this.rollover = false;
        this.timer = 0;
    }

    public void setContent(Component content) {
        if(this.content != null) {
            this.content.removeMouseListener(contentAdapter);
        }
        this.content = content;
        this.getContentPane().add(content, BorderLayout.CENTER);
        this.content.addMouseListener(contentAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                Hint.this.rollover = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                Hint.this.rollover = false;
            }
        });
        this.revalidate();
    }

    public Constant getPreferredPos() {
        return preferredPos;
    }

    public void setPreferredPos(Constant preferredPos) {
        this.preferredPos = preferredPos;
    }

    public int getInDelay() {
        return inDelay;
    }

    public void setInDelay(int inDelay) {
        if(inDelay <= 0) throw new IllegalArgumentException("Hint in-delay must be positive.");
        this.inDelay = inDelay;
    }

    public int getOutDelay() {
        return outDelay;
    }

    public void setOutDelay(int outDelay) {
        if(outDelay <= 0) throw new IllegalArgumentException("Hint out-delay must be positive.");
        this.outDelay = outDelay;
    }

    public boolean shouldContinueShowing() {
        return rollover || (this.visibilityCheck != null && this.visibilityCheck.confirm());
    }

    public Color getBackgroundColor() {
        return background;
    }

    public Color getBorderColor() {
        return border;
    }

    public void setBackgroundColor(Color background) {
        this.background = background;
    }

    public void setBorderColor(Color border) {
        this.border = border;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        this.rollover = true;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        this.rollover = false;
    }

    public double getDistanceFromPoint(Point p) {
        return Math.sqrt(Math.pow(Math.max(this.getX()+MARGIN,Math.min(this.getX()+this.getWidth()-MARGIN,p.x))-p.x,2)+Math.pow(this.getY()+this.getHeight()/2-p.y,2));
    }

    @Override
    public void dispose() {
        this.dismiss();
        this.visibilityCheck = null;
        this.disposed = true;
        super.dispose();
    }
}
