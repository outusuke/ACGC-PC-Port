package io.github.outusuke.acgcpcport;

import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.ViewGroup;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GameActivity extends SDLActivity {
    private static final String TAG = "ACGC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        copyShaders();
        super.onCreate(savedInstanceState);

        if (!hasGamepad()) {
            mLayout.addView(new TouchControlsView(this),
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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
