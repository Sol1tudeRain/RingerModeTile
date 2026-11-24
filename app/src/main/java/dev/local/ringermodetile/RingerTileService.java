package dev.local.ringermodetile;

import android.content.*;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public final class RingerTileService extends TileService {
    static final String INTERNAL_CHANGED = "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION";
    private boolean listening;
    private final BroadcastReceiver changes = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { update(); }
    };
    @Override public void onStartListening() {
        super.onStartListening();
        if (!listening) {
            IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
            filter.addAction(INTERNAL_CHANGED);
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(changes, filter, Context.RECEIVER_EXPORTED);
            else registerReceiver(changes, filter);
            listening = true;
        }
        update();
    }
    @Override public void onStopListening() { stop(); super.onStopListening(); }
    @Override public void onDestroy() { stop(); super.onDestroy(); }
    private void stop() {
        if (listening) { unregisterReceiver(changes); listening = false; }
    }
    @Override public void onTileAdded() { super.onTileAdded(); update(); }
    // The privileged click is handled only in SystemUI. No root command or policy-access grant.
    @Override public void onClick() {
        android.util.Log.w("RingerModeTile", "SystemUI hook not active; no mode change");
        update();
    }
    private void update() {
        Tile tile = getQsTile();
        if (tile == null) return;
        AudioManager audio = getSystemService(AudioManager.class);
        if (audio == null) return;
        int mode = audio.getRingerMode();
        CharSequence label = getText(Modes.label(mode));
        tile.setLabel(label);
        tile.setContentDescription(label);
        tile.setIcon(Icon.createWithResource(this, Modes.icon(mode)));
        tile.setState(mode == Modes.SILENT ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.updateTile();
    }
}
