package dev.local.ringermodetile;
final class Modes {
    static final int SILENT = 0, VIBRATE = 1, NORMAL = 2;
    static int next(int mode, boolean vibrator) {
        if (mode == NORMAL) return vibrator ? VIBRATE : SILENT;
        if (mode == VIBRATE) return SILENT;
        return NORMAL;
    }
    static int icon(int mode) {
        return mode == SILENT ? R.drawable.ic_silent : mode == VIBRATE ? R.drawable.ic_vibrate : R.drawable.ic_ring;
    }
    static int label(int mode) {
        return mode == SILENT ? R.string.silent : mode == VIBRATE ? R.string.vibrate : R.string.normal;
    }
}
