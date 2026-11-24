package dev.local.ringermodetile;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Vibrator;
import android.service.quicksettings.Tile;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class SystemUiHook implements IXposedHookLoadPackage {
    private static final String PKG = "dev.local.ringermodetile";
    private static final String SERVICE = PKG + ".RingerTileService";
    private static final String TAG = "[RingerModeTile] ";
    private static final String CUSTOM = "com.android.systemui.qs.external.CustomTile";
    private static final String BASE = "com.android.systemui.qs.tileimpl.QSTileImpl";

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) {
        if (!"com.android.systemui".equals(p.packageName)
                || !"com.android.systemui".equals(p.processName)) return;
        try {
            Class<?> custom = XposedHelpers.findClass(CUSTOM, p.classLoader);
            Class<?> base = XposedHelpers.findClass(BASE, p.classLoader);
            int clicks = hookNamed(custom, "handleClick", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam h) {
                    if (!isOurs(h.thisObject)) return;
                    // Consume only our own tile, even if the operation fails.
                    h.setResult(null);
                    try {
                        Context context = context(h.thisObject);
                        AudioManager audio = context.getSystemService(AudioManager.class);
                        Vibrator vibrator = context.getSystemService(Vibrator.class);
                        int previous = mode(audio);
                        int next = Modes.next(previous, vibrator != null && vibrator.hasVibrator());
                        XposedHelpers.callMethod(audio, "setRingerModeInternal", next);
                        refresh(h.thisObject);
                        XposedBridge.log(TAG + "Mode " + previous + " -> " + mode(audio));
                    } catch (Throwable t) { log("Click failed", t); }
                }
            });
            XC_MethodHook longPress = new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam h) {
                    if (!isOurs(h.thisObject)) return;
                    h.setResult(null);
                    try {
                        AudioManager audio = context(h.thisObject).getSystemService(AudioManager.class);
                        // ADJUST_SAME never raises or lowers volume; SHOW_UI opens the native overlay.
                        audio.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
                        XposedBridge.log(TAG + "Native volume panel requested");
                    } catch (Throwable t) { log("Long press failed", t); }
                }
            };
            int longs = hookNamed(custom, "handleLongClick", longPress);
            longs += hookNamed(base, "handleLongClick", longPress);
            int updates = hookNamed(custom, "handleUpdateState", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam h) {
                    if (h.hasThrowable() || !isOurs(h.thisObject) || h.args.length == 0) return;
                    try { render(h.thisObject, h.args[0], p.classLoader); }
                    catch (Throwable t) { log("State render failed", t); }
                }
            });
            XposedBridge.log(TAG + "Installed hooks: click=" + clicks + " long=" + longs + " state=" + updates);
        } catch (Throwable t) { log("Installation failed", t); }
    }
    private static int hookNamed(Class<?> c, String name, XC_MethodHook hook) {
        int n = 0;
        for (Method m : c.getDeclaredMethods()) {
            if (!m.getName().equals(name) || m.isBridge() || m.isSynthetic()
                    || m.getReturnType() != void.class) continue;
            XposedBridge.hookMethod(m, hook);
            n++;
        }
        return n;
    }
    private static boolean isOurs(Object tile) {
        try {
            Object component = XposedHelpers.getObjectField(tile, "mComponent");
            if (component instanceof ComponentName) {
                ComponentName c = (ComponentName) component;
                return PKG.equals(c.getPackageName()) && SERVICE.equals(c.getClassName());
            }
        } catch (Throwable ignored) { }
        return false;
    }
    private static Context context(Object tile) {
        return (Context) XposedHelpers.getObjectField(tile, "mContext");
    }
    private static int mode(AudioManager audio) {
        return (Integer) XposedHelpers.callMethod(audio, "getRingerModeInternal");
    }
    private static void refresh(Object tile) {
        try { XposedHelpers.callMethod(tile, "refreshState"); }
        catch (Throwable t) { log("Refresh failed", t); }
    }
    private static void render(Object tile, Object state, ClassLoader loader) throws Exception {
        if (XposedHelpers.getIntField(state, "state") == Tile.STATE_UNAVAILABLE) return;
        Context ctx = context(tile);
        Context module = ctx.createPackageContext(PKG, 0);
        int mode = mode(ctx.getSystemService(AudioManager.class));
        int id = Modes.icon(mode);
        Drawable drawable = module.getDrawable(id);
        if (drawable == null) return;
        Object icon;
        try {
            Class<?> cls = XposedHelpers.findClass(BASE + "$DrawableIconWithRes", loader);
            icon = XposedHelpers.newInstance(cls, drawable, id);
        } catch (Throwable missing) {
            Class<?> cls = XposedHelpers.findClass(BASE + "$DrawableIcon", loader);
            icon = XposedHelpers.newInstance(cls, drawable);
        }
        final Object finalIcon = icon;
        // Android 15 variants use either icon or iconSupplier (or both).
        try { XposedHelpers.setObjectField(state, "icon", icon); } catch (Throwable ignored) { }
        try { XposedHelpers.setObjectField(state, "iconSupplier", (Supplier<Object>) () -> finalIcon); }
        catch (Throwable ignored) { }
        CharSequence label = module.getText(Modes.label(mode));
        XposedHelpers.setObjectField(state, "label", label);
        XposedHelpers.setObjectField(state, "contentDescription", label);
        XposedHelpers.setIntField(state, "state", mode == Modes.SILENT ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
    }
    private static void log(String message, Throwable t) {
        XposedBridge.log(TAG + message + ": " + t);
    }
}
