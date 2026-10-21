package io.github.outusuke.acgcpcport;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GameActivity extends SDLActivity {
    private static final String TAG = "ACGC";
    private static final int[] RENDER_HEIGHTS = {480, 540, 720, 0};
    private static final int DEFAULT_RENDER_HEIGHT = 540;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        copyShaders();
        super.onCreate(savedInstanceState);
        limitRenderResolution();
        lockRefreshRate();

        if (!hasGamepad()) {
            mLayout.addView(new TouchControlsView(this),
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    static int renderHeight(Context ctx) {
        return ctx.getSharedPreferences("video", MODE_PRIVATE).getInt("render_height", DEFAULT_RENDER_HEIGHT);
    }

    static void setRenderHeight(Context ctx, int height) {
        ctx.getSharedPreferences("video", MODE_PRIVATE).edit().putInt("render_height", height).apply();
    }

    static int nextRenderHeight(int current) {
        for (int i = 0; i < RENDER_HEIGHTS.length; i++) {
            if (RENDER_HEIGHTS[i] == current) {
                return RENDER_HEIGHTS[(i + 1) % RENDER_HEIGHTS.length];
            }
        }
        return DEFAULT_RENDER_HEIGHT;
    }

    private void limitRenderResolution() {
        int target = renderHeight(this);
        if (target <= 0 || mSurface == null) {
            return;
        }

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        int shortSide = Math.min(dm.widthPixels, dm.heightPixels);
        int longSide = Math.max(dm.widthPixels, dm.heightPixels);
        if (shortSide <= target) {
            return;
        }

        int width = Math.round(longSide * (float) target / shortSide) & ~1;
        // SDL adds the surface as wrap_content, which would show the small buffer unscaled
        mSurface.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mSurface.getHolder().setFixedSize(width, target);
    }

    // The game is paced at 60 fps, so 90/120 Hz panels judder unless pinned to 60
    private void lockRefreshRate() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.preferredRefreshRate = 60f;
        getWindow().setAttributes(lp);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && mSurface != null) {
            mSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    holder.getSurface().setFrameRate(60f, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                }
            });
        }
    }

    private void copyShaders() {
        File dir = new File(getFilesDir(), "shaders");
        dir.mkdirs();
        try {
            for (String name : getAssets().list("shaders")) {
                try (InputStream in = getAssets().open("shaders/" + name);
                     OutputStream out = new FileOutputStream(new File(dir, name))) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "shader copy failed", e);
        }
    }

    private static boolean hasGamepad() {
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null && (device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
                return true;
            }
        }
        return false;
    }
}
