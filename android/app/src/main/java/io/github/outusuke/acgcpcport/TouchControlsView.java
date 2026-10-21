package io.github.outusuke.acgcpcport;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class TouchControlsView extends View {
    private static final int Z = 0x0010;
    private static final int R = 0x0020;
    private static final int L = 0x0040;
    private static final int A = 0x0100;
    private static final int B = 0x0200;
    private static final int X = 0x0400;
    private static final int Y = 0x0800;
    private static final int START = 0x1000;

    private static final float DEADZONE = 0.12f;

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
            float pad = bounds.width() * 0.15f;
            return x >= bounds.left - pad && x <= bounds.right + pad && y >= bounds.top - pad && y <= bounds.bottom + pad;
        }
    }

    private final Button[] buttons = {
        new Button("A", A, true), new Button("B", B, true), new Button("X", X, true), new Button("Y", Y, true),
        new Button("L", L, false), new Button("R", R, false), new Button("Z", Z, false), new Button("START", START, false),
    };
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float leftX, leftY, leftR;
    private float rightX, rightY, rightR;
    private float leftDx, leftDy, rightDx, rightDy;
    private int leftPointer = -1;
    private int rightPointer = -1;
    private int pressed;

    public TouchControlsView(Context context) {
        super(context);
        text.setColor(0xFFFFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float u = Math.min(w, h);

        leftR = u * 0.25f;
        leftX = u * 0.55f;
        leftY = h - u * 0.45f;
        rightR = u * 0.14f;
        rightX = leftX + u * 0.75f;
        rightY = h - u * 0.25f;

        float ax = w - u * 0.40f;
        float ay = h - u * 0.40f;
        place(buttons[0], ax, ay, u * 0.13f);
        place(buttons[1], ax - u * 0.25f, ay + u * 0.08f, u * 0.09f);
        place(buttons[2], ax + u * 0.23f, ay - u * 0.08f, u * 0.09f);
        place(buttons[3], ax - u * 0.02f, ay - u * 0.27f, u * 0.09f);

        buttons[4].bounds.set(u * 0.10f, u * 0.08f, u * 0.42f, u * 0.24f);
        buttons[5].bounds.set(w - u * 0.42f, u * 0.08f, w - u * 0.10f, u * 0.24f);
        buttons[6].bounds.set(w - u * 0.42f, u * 0.30f, w - u * 0.10f, u * 0.44f);
        buttons[7].bounds.set(w / 2f - u * 0.14f, u * 0.08f, w / 2f + u * 0.14f, u * 0.20f);
        text.setTextSize(u * 0.06f);
    }

    private static void place(Button b, float cx, float cy, float r) {
        b.bounds.set(cx - r, cy - r, cx + r, cy + r);
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

            if (leftPointer < 0 && Math.hypot(x - leftX, y - leftY) < leftR * 1.4) {
                leftPointer = id;
            } else if (rightPointer < 0 && Math.hypot(x - rightX, y - rightY) < rightR * 1.6) {
                rightPointer = id;
            }
        } else if (lifting) {
            int id = e.getPointerId(index);

            if (id == leftPointer) {
                leftPointer = -1;
            }
            if (id == rightPointer) {
                rightPointer = -1;
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            leftPointer = -1;
            rightPointer = -1;
        }

        int mask = 0;
        leftDx = leftDy = rightDx = rightDy = 0;
        if (action != MotionEvent.ACTION_CANCEL) {
            for (int i = 0; i < e.getPointerCount(); i++) {
                if (lifting && i == index) {
                    continue;
                }
                int id = e.getPointerId(i);
                float x = e.getX(i);
                float y = e.getY(i);

                if (id == leftPointer) {
                    leftDx = clamp((x - leftX) / leftR);
                    leftDy = clamp((y - leftY) / leftR);
                } else if (id == rightPointer) {
                    rightDx = clamp((x - rightX) / rightR);
                    rightDy = clamp((y - rightY) / rightR);
                } else {
                    for (Button b : buttons) {
                        if (b.hit(x, y)) {
                            mask |= b.mask;
                        }
                    }
                }
            }
        }

        pressed = mask;
        nativeSetPad(mask, axis(leftDx), axis(-leftDy), axis(rightDx), axis(-rightDy));
        invalidate();
        return true;
    }

    private static float clamp(float v) {
        float len = Math.abs(v);
        return len > 1f ? Math.signum(v) : v;
    }

    private static int axis(float v) {
        if (Math.abs(v) < DEADZONE) {
            return 0;
        }
        return Math.round(Math.max(-1f, Math.min(1f, v)) * 127f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0x33FFFFFF);
        canvas.drawCircle(leftX, leftY, leftR, fill);
        canvas.drawCircle(rightX, rightY, rightR, fill);

        fill.setColor(0x88FFFFFF);
        canvas.drawCircle(leftX + leftDx * leftR, leftY + leftDy * leftR, leftR * 0.4f, fill);
        canvas.drawCircle(rightX + rightDx * rightR, rightY + rightDy * rightR, rightR * 0.4f, fill);

        for (Button b : buttons) {
            fill.setColor((pressed & b.mask) != 0 ? 0x88FFFFFF : 0x33FFFFFF);
            if (b.round) {
                canvas.drawCircle(b.bounds.centerX(), b.bounds.centerY(), b.bounds.width() / 2f, fill);
            } else {
                canvas.drawRoundRect(b.bounds, 24f, 24f, fill);
            }
            canvas.drawText(b.label, b.bounds.centerX(), b.bounds.centerY() + text.getTextSize() / 3f, text);
        }
    }

    private static native void nativeSetPad(int buttons, int leftX, int leftY, int rightX, int rightY);
}
