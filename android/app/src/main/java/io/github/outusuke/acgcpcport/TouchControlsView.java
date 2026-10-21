package io.github.outusuke.acgcpcport;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

public class TouchControlsView extends View {
    private static final int DPAD_LEFT = 0x0001;
    private static final int DPAD_RIGHT = 0x0002;
    private static final int DPAD_DOWN = 0x0004;
    private static final int DPAD_UP = 0x0008;
    private static final int Z = 0x0010;
    private static final int R = 0x0020;
    private static final int L = 0x0040;
    private static final int A = 0x0100;
    private static final int B = 0x0200;
    private static final int X = 0x0400;
    private static final int Y = 0x0800;
    private static final int START = 0x1000;

    private static final float DEADZONE = 0.12f;
    private static final float DPAD_DEADZONE = 0.25f;

    private static final int FILL_IDLE = 0x80FFFFFF;
    private static final int FILL_PRESSED = 0x80000000;
    private static final int ICON_IDLE = 0x80000000;
    private static final int ICON_PRESSED = 0x80FFFFFF;
    private static final int DIAL_BACKGROUND = 0x1AFFFFFF;
    private static final int DIAL_RIM = 0x33FFFFFF;

    private static class Button {
        final String label;
        final int mask;
        final boolean round;
        final RectF bounds = new RectF();

        Button(String label, int mask, boolean round) {
            this.label = label;
            this.mask = mask;
            this.round = round;
        }

        boolean hit(float x, float y) {
            if (round) {
                return Math.hypot(x - bounds.centerX(), y - bounds.centerY()) <= bounds.width() * 0.6f;
            }
            float pad = bounds.height() * 0.3f;
            return x >= bounds.left - pad && x <= bounds.right + pad && y >= bounds.top - pad && y <= bounds.bottom + pad;
        }
    }

    private static class Dial {
        final boolean dpad;
        final String label;
        float cx, cy, radius, background;
        float dx, dy;
        int pointer = -1;

        Dial(boolean dpad, String label) {
            this.dpad = dpad;
            this.label = label;
        }

        void place(float cx, float cy, float background) {
            this.cx = cx;
            this.cy = cy;
            this.background = background;
            this.radius = dpad ? background : background / 1.4f;
        }

        boolean grab(float x, float y) {
            return Math.hypot(x - cx, y - cy) < background * 1.2f;
        }

        void track(float x, float y) {
            dx = (x - cx) / radius;
            dy = (y - cy) / radius;
            float len = (float) Math.hypot(dx, dy);
            if (len > 1f) {
                dx /= len;
                dy /= len;
            }
        }

        int directions() {
            if (Math.hypot(dx, dy) < DPAD_DEADZONE) {
                return 0;
            }
            int mask = 0;
            double angle = Math.toDegrees(Math.atan2(-dy, dx));
            if (angle < 0) {
                angle += 360;
            }
            if (angle < 67.5 || angle >= 292.5) {
                mask |= DPAD_RIGHT;
            }
            if (angle >= 22.5 && angle < 157.5) {
                mask |= DPAD_UP;
            }
            if (angle >= 112.5 && angle < 247.5) {
                mask |= DPAD_LEFT;
            }
            if (angle >= 202.5 && angle < 337.5) {
                mask |= DPAD_DOWN;
            }
            return mask;
        }
    }

    private final Button[] buttons = {
        new Button("A", A, true), new Button("B", B, true), new Button("X", X, true), new Button("Y", Y, true),
        new Button("L", L, false), new Button("R", R, false), new Button("Z", Z, false), new Button("START", START, false),
    };
    private final Dial dpad = new Dial(true, null);
    private final Dial leftStick = new Dial(false, null);
    private final Dial cStick = new Dial(false, "C");
    private final Dial[] dials = {dpad, leftStick, cStick};

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrow = new Path();
    private int pressed;

    public TouchControlsView(Context context) {
        super(context);
        rim.setStyle(Paint.Style.STROKE);
        rim.setColor(DIAL_RIM);
        text.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float u = Math.min(h, w * 0.56f);
        float cy = h - u * 0.40f;
        float lx = u * 0.40f;
        float rx = w - u * 0.42f;

        rim.setStrokeWidth(Math.max(2f, u * 0.006f));
        leftStick.place(lx, cy, u * 0.19f);
        dpad.place(polarX(lx, -35f, u * 0.33f), polarY(cy, -35f, u * 0.33f), u * 0.08f);
        cStick.place(polarX(rx, 235f, u * 0.34f), polarY(cy, 235f, u * 0.34f), u * 0.085f);

        place(buttons[0], rx, cy, u * 0.095f);
        place(buttons[1], polarX(rx, 205f, u * 0.16f), polarY(cy, 205f, u * 0.16f), u * 0.06f);
        place(buttons[2], polarX(rx, 20f, u * 0.18f), polarY(cy, 20f, u * 0.18f), u * 0.06f);
        place(buttons[3], polarX(rx, 100f, u * 0.18f), polarY(cy, 100f, u * 0.18f), u * 0.06f);

        placeBar(buttons[4], polarX(lx, 115f, u * 0.33f), polarY(cy, 115f, u * 0.33f), u * 0.17f, u * 0.08f);
        placeBar(buttons[5], polarX(rx, 65f, u * 0.34f), polarY(cy, 65f, u * 0.34f), u * 0.17f, u * 0.08f);
        placeBar(buttons[6], polarX(rx, 105f, u * 0.34f), polarY(cy, 105f, u * 0.34f), u * 0.15f, u * 0.08f);
        placeBar(buttons[7], polarX(rx, 145f, u * 0.34f), polarY(cy, 145f, u * 0.34f), u * 0.15f, u * 0.07f);
    }

    private static float polarX(float cx, float degrees, float dist) {
        return cx + (float) Math.cos(Math.toRadians(degrees)) * dist;
    }

    private static float polarY(float cy, float degrees, float dist) {
        return cy - (float) Math.sin(Math.toRadians(degrees)) * dist;
    }

    private static void place(Button b, float cx, float cy, float r) {
        b.bounds.set(cx - r, cy - r, cx + r, cy + r);
    }

    private static void placeBar(Button b, float cx, float cy, float w, float h) {
        b.bounds.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        int index = e.getActionIndex();
        boolean lifting = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP;

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int id = e.getPointerId(index);
            float x = e.getX(index);
            float y = e.getY(index);

            for (Dial d : dials) {
                if (d.pointer < 0 && d.grab(x, y)) {
                    d.pointer = id;
                    break;
                }
            }
        } else if (lifting) {
            int id = e.getPointerId(index);

            for (Dial d : dials) {
                if (d.pointer == id) {
                    d.pointer = -1;
                }
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            for (Dial d : dials) {
                d.pointer = -1;
            }
        }

        int mask = 0;
        for (Dial d : dials) {
            d.dx = d.dy = 0;
        }
        if (action != MotionEvent.ACTION_CANCEL) {
            for (int i = 0; i < e.getPointerCount(); i++) {
                if (lifting && i == index) {
                    continue;
                }
                int id = e.getPointerId(i);
                float x = e.getX(i);
                float y = e.getY(i);
                Dial owner = null;

                for (Dial d : dials) {
                    if (d.pointer == id) {
                        owner = d;
                    }
                }
                if (owner != null) {
                    owner.track(x, y);
                    continue;
                }
                for (Button b : buttons) {
                    if (b.hit(x, y)) {
                        mask |= b.mask;
                    }
                }
            }
        }

        mask |= dpad.directions();
        if ((mask & ~pressed) != 0) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
        pressed = mask;
        nativeSetPad(mask, axis(leftStick.dx), axis(-leftStick.dy), axis(cStick.dx), axis(-cStick.dy));
        invalidate();
        return true;
    }

    private static int axis(float v) {
        if (Math.abs(v) < DEADZONE) {
            return 0;
        }
        return Math.round(Math.max(-1f, Math.min(1f, v)) * 127f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawDpad(canvas);
        drawStick(canvas, leftStick);
        drawStick(canvas, cStick);

        for (Button b : buttons) {
            boolean down = (pressed & b.mask) != 0;
            float size = Math.min(b.bounds.width(), b.bounds.height());

            fill.setStyle(Paint.Style.FILL);
            fill.setColor(down ? FILL_PRESSED : FILL_IDLE);
            if (b.round) {
                canvas.drawCircle(b.bounds.centerX(), b.bounds.centerY(), b.bounds.width() / 2f, fill);
            } else {
                canvas.drawRoundRect(b.bounds, size / 2f, size / 2f, fill);
            }
            text.setColor(down ? ICON_PRESSED : ICON_IDLE);
            text.setTextSize(b.round ? size * 0.5f : size * 0.4f);
            canvas.drawText(b.label, b.bounds.centerX(), b.bounds.centerY() + text.getTextSize() / 3f, text);
        }
    }

    private void drawBackground(Canvas canvas, Dial d) {
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(DIAL_BACKGROUND);
        canvas.drawCircle(d.cx, d.cy, d.background, fill);
        canvas.drawCircle(d.cx, d.cy, d.background, rim);
    }

    private void drawStick(Canvas canvas, Dial d) {
        boolean active = d.pointer >= 0;
        float knob = d.radius * 0.4f;
        float kx = d.cx + d.dx * d.radius;
        float ky = d.cy + d.dy * d.radius;

        drawBackground(canvas, d);
        fill.setColor(active ? FILL_PRESSED : FILL_IDLE);
        canvas.drawCircle(kx, ky, knob, fill);
        if (d.label != null) {
            text.setColor(active ? ICON_PRESSED : ICON_IDLE);
            text.setTextSize(knob);
            canvas.drawText(d.label, kx, ky + knob / 3f, text);
        }
    }

    private void drawDpad(Canvas canvas) {
        int held = pressed & (DPAD_LEFT | DPAD_RIGHT | DPAD_DOWN | DPAD_UP);
        float reach = dpad.background * 0.62f;
        float half = dpad.background * 0.22f;

        drawBackground(canvas, dpad);
        drawArrow(canvas, 0, -1, reach, half, (held & DPAD_UP) != 0);
        drawArrow(canvas, 0, 1, reach, half, (held & DPAD_DOWN) != 0);
        drawArrow(canvas, -1, 0, reach, half, (held & DPAD_LEFT) != 0);
        drawArrow(canvas, 1, 0, reach, half, (held & DPAD_RIGHT) != 0);
    }

    private void drawArrow(Canvas canvas, int dirX, int dirY, float reach, float half, boolean down) {
        float tipX = dpad.cx + dirX * reach;
        float tipY = dpad.cy + dirY * reach;
        float baseX = dpad.cx + dirX * (reach - half * 1.6f);
        float baseY = dpad.cy + dirY * (reach - half * 1.6f);

        arrow.reset();
        arrow.moveTo(tipX, tipY);
        arrow.lineTo(baseX + dirY * half, baseY + dirX * half);
        arrow.lineTo(baseX - dirY * half, baseY - dirX * half);
        arrow.close();
        fill.setColor(down ? FILL_PRESSED : FILL_IDLE);
        canvas.drawPath(arrow, fill);
    }

    private static native void nativeSetPad(int buttons, int leftX, int leftY, int rightX, int rightY);
}
