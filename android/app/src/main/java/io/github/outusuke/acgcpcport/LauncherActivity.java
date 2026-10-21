package io.github.outusuke.acgcpcport;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class LauncherActivity extends Activity {
    private static final int REQUEST_DISC = 1;

    private TextView status;
    private Button play;
    private Button select;
    private Button graphics;
    private Button log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);

        status = new TextView(this);
        status.setGravity(Gravity.CENTER);
        status.setTextSize(18f);

        select = new Button(this);
        select.setText("Select disc image");
        select.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_DISC);
        });

        graphics = new Button(this);
        graphics.setOnClickListener(v -> {
            GameActivity.setRenderHeight(this, GameActivity.nextRenderHeight(GameActivity.renderHeight(this)));
            refresh();
        });

        log = new Button(this);
        log.setOnClickListener(v -> {
            GameActivity.setLogEnabled(this, !GameActivity.logEnabled(this));
            refresh();
        });

        play = new Button(this);
        play.setText("Play");
        play.setOnClickListener(v -> startActivity(new Intent(this, GameActivity.class)));

        root.addView(status);
        root.addView(select);
        root.addView(graphics);
        root.addView(log);
        root.addView(play);
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private File romDir() {
        File dir = new File(GameActivity.dataDir(this), "rom");
        dir.mkdirs();
        return dir;
    }

    private static boolean isDisc(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".iso") || lower.endsWith(".gcm") || lower.endsWith(".ciso");
    }

    private void refresh() {
        String found = null;
        File[] files = romDir().listFiles();

        if (files != null) {
            for (File f : files) {
                if (isDisc(f.getName())) {
                    found = f.getName();
                }
            }
        }
        int height = GameActivity.renderHeight(this);
        graphics.setText(height > 0 ? "Render resolution: " + height + "p" : "Render resolution: native");
        log.setText(GameActivity.logEnabled(this) ? "Logging: on" : "Logging: off");
        play.setEnabled(found != null);
        status.setText(found != null ? "Disc image: " + found : "Select your Animal Crossing (GAFE01) disc image");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_DISC && resultCode == RESULT_OK && data != null && data.getData() != null) {
            importDisc(data.getData());
        }
    }

    private String displayName(Uri uri) {
        String name = "game.iso";

        try (Cursor c = getContentResolver().query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && c.getString(0) != null) {
                name = c.getString(0).replace('/', '_');
            }
        }
        return isDisc(name) ? name : name + ".iso";
    }

    private void importDisc(Uri uri) {
        select.setEnabled(false);
        play.setEnabled(false);
        status.setText("Copying disc image...");

        new Thread(() -> {
            File dest = new File(romDir(), displayName(uri));
            String result;

            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[1 << 20];
                long total = 0;
                int n;

                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    total += n;
                    long mb = total >> 20;
                    runOnUiThread(() -> status.setText("Copying disc image... " + mb + " MB"));
                }
                result = null;
            } catch (IOException | NullPointerException e) {
                dest.delete();
                result = "Copy failed: " + e.getMessage();
            }

            String message = result;
            runOnUiThread(() -> {
                select.setEnabled(true);
                refresh();
                if (message != null) {
                    status.setText(message);
                }
            });
        }).start();
    }
}
