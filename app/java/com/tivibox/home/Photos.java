package com.tivibox.home;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Size;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * User photos: listing images the TV can see (internal storage, Download, USB drives through MediaStore), copying a
 * chosen one into the app (so it survives unplugging the USB drive) and preparing the home wallpaper.
 * Stored files: files/wallpaper.jpg and files/slides/NN.jpg, at most 1920x1080.
 */
final class Photos {
    static final class Item {
        final Uri uri;
        final String name;
        final long id;

        Item(Uri uri, String name, long id) {
            this.uri = uri;
            this.name = name;
            this.id = id;
        }
    }

    private static Bitmap wallpaperCache;
    private static String wallpaperKey = "";

    private Photos() {}

    /** Newest first, from every storage volume Android has indexed. */
    static List<Item> list(Context c) {
        List<Item> out = new ArrayList<>();
        ContentResolver cr = c.getContentResolver();
        List<Uri> volumes = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 29) {
            Set<String> names = MediaStore.getExternalVolumeNames(c);
            for (String n : names) volumes.add(MediaStore.Images.Media.getContentUri(n));
        } else {
            volumes.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        String[] cols = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_MODIFIED};
        for (Uri volume : volumes) {
            try (Cursor cur = cr.query(volume, cols, null, null, MediaStore.Images.Media.DATE_MODIFIED + " DESC")) {
                if (cur == null) continue;
                while (cur.moveToNext()) {
                    long id = cur.getLong(0);
                    out.add(new Item(Uri.withAppendedPath(volume, String.valueOf(id)), cur.getString(1), id));
                }
            } catch (RuntimeException ignored) {
                // A volume being unmounted mid-query; skip it.
            }
        }
        return out;
    }

    /** A storage root to browse: internal shared storage and each USB drive / SD card. */
    static final class Volume {
        final String name;
        final File dir;

        Volume(String name, File dir) {
            this.name = name;
            this.dir = dir;
        }
    }

    static List<Volume> volumes(Context c) {
        List<Volume> out = new ArrayList<>();
        android.os.storage.StorageManager sm = c.getSystemService(android.os.storage.StorageManager.class);
        if (Build.VERSION.SDK_INT >= 30 && sm != null) {
            for (android.os.storage.StorageVolume v : sm.getStorageVolumes()) {
                File dir = v.getDirectory();
                if (dir == null || !dir.canRead()) continue;
                String name = v.isPrimary() ? "Bộ nhớ trong" : v.getDescription(c);
                out.add(new Volume(v.isRemovable() && !v.isPrimary() ? "USB · " + name : name, dir));
            }
        }
        if (out.isEmpty()) {
            out.add(new Volume("Bộ nhớ trong", android.os.Environment.getExternalStorageDirectory()));
            File storage = new File("/storage");
            File[] others = storage.listFiles();
            if (others != null) {
                for (File f : others) {
                    if (f.isDirectory() && f.canRead() && !f.getName().equals("emulated") && !f.getName().equals("self")) {
                        out.add(new Volume("USB · " + f.getName(), f));
                    }
                }
            }
        }
        return out;
    }

    private static final String[] IMAGE_EXT = {".jpg", ".jpeg", ".png", ".webp", ".bmp", ".heic", ".heif", ".gif"};

    static boolean isImage(File f) {
        String n = f.getName().toLowerCase(java.util.Locale.ROOT);
        for (String e : IMAGE_EXT) if (n.endsWith(e)) return true;
        return false;
    }

    /**
     * Every photo on internal storage and all USB drives, newest first: a direct walk of the folders (so photos the
     * media library has not indexed are found too), skipping hidden folders and Android/. Bounded so a huge drive
     * cannot stall the TV.
     */
    static List<File> scanAll(Context c) {
        List<File> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Volume v : volumes(c)) walk(v.dir, 0, out, seen);
        out.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return out;
    }

    private static void walk(File dir, int depth, List<File> out, java.util.Set<String> seen) {
        if (depth > 8 || out.size() >= 3000) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith(".")) continue;
            if (f.isDirectory()) {
                if (!(depth == 0 && name.equals("Android"))) walk(f, depth + 1, out, seen);
            } else if (isImage(f) && f.length() > 10_000 && seen.add(f.getAbsolutePath())) {
                out.add(f);
            }
        }
    }

    /** Sub-folders (not hidden, not Android/) then images, each sorted by name. */
    static void listFolder(File dir, List<File> folders, List<File> images) {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File f : files) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) {
                if (!f.getName().equals("Android")) folders.add(f);
            } else if (isImage(f)) {
                images.add(f);
            }
        }
    }

    static Bitmap fileThumbnail(File f) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        if (o.outWidth <= 0) return null;
        int sample = 1;
        while (o.outWidth / (sample * 2) >= 320 && o.outHeight / (sample * 2) >= 180) sample *= 2;
        BitmapFactory.Options d = new BitmapFactory.Options();
        d.inSampleSize = sample;
        return BitmapFactory.decodeFile(f.getPath(), d);
    }

    static Bitmap thumbnail(Context c, Item item) {
        try {
            if (Build.VERSION.SDK_INT >= 29) return c.getContentResolver().loadThumbnail(item.uri, new Size(320, 180), null);
            return decode(c, item.uri, 320, 180);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Decodes, rotates per EXIF and scales down to fit maxW x maxH. */
    static Bitmap decode(Context c, Uri uri, int maxW, int maxH) throws IOException {
        ContentResolver cr = c.getContentResolver();
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = cr.openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, o);
        }
        if (o.outWidth <= 0) throw new IOException("Không đọc được ảnh");
        int sample = 1;
        while (o.outWidth / (sample * 2) >= maxW && o.outHeight / (sample * 2) >= maxH) sample *= 2;
        BitmapFactory.Options d = new BitmapFactory.Options();
        d.inSampleSize = sample;
        Bitmap b;
        try (InputStream in = cr.openInputStream(uri)) {
            b = BitmapFactory.decodeStream(in, null, d);
        }
        if (b == null) throw new IOException("Không đọc được ảnh");
        int rotate = 0;
        try (InputStream in = cr.openInputStream(uri)) {
            if (in != null) {
                int orientation = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                rotate = orientation == 6 ? 90 : orientation == 3 ? 180 : orientation == 8 ? 270 : 0;
            }
        } catch (IOException | RuntimeException ignored) {
            // No EXIF: keep as is.
        }
        float scale = Math.min(1f, Math.min(maxW / (float) b.getWidth(), maxH / (float) b.getHeight()));
        if (rotate != 0) scale = Math.min(1f, Math.min(maxW / (float) b.getHeight(), maxH / (float) b.getWidth()));
        if (scale < 1f || rotate != 0) {
            Matrix m = new Matrix();
            m.postScale(scale, scale);
            m.postRotate(rotate);
            Bitmap r = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (r != b) b.recycle();
            b = r;
        }
        return b;
    }

    static File wallpaperFile(Context c) {
        return new File(c.getFilesDir(), "wallpaper.jpg");
    }

    static File slidesDir(Context c) {
        File d = new File(c.getFilesDir(), "slides");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    static List<File> slides(Context c) {
        File[] files = slidesDir(c).listFiles((dir, name) -> name.endsWith(".jpg"));
        List<File> out = new ArrayList<>();
        if (files != null) {
            Arrays.sort(files);
            out.addAll(Arrays.asList(files));
        }
        return out;
    }

    /** Copies (decoded and resized) a picked photo to `dest` as JPEG. */
    static void save(Context c, Uri from, File dest) throws IOException {
        Bitmap b = decode(c, from, 1920, 1080);
        File tmp = new File(dest.getPath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            b.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } finally {
            b.recycle();
        }
        if (!tmp.renameTo(dest)) throw new IOException("Không lưu được ảnh");
    }

    /** Replaces all slideshow photos with the picked ones. */
    static void saveSlides(Context c, List<Uri> picked) throws IOException {
        for (File f : slides(c)) f.delete();
        for (int i = 0; i < picked.size(); i++) save(c, picked.get(i), new File(slidesDir(c), String.format("%02d.jpg", i)));
    }

    static void removeWallpaper(Context c) {
        wallpaperFile(c).delete();
        wallpaperCache = null;
        wallpaperKey = "";
    }

    /**
     * The home wallpaper ready to draw behind the glass at w x h (the caller scales the result to the screen).
     * fit=false fills the screen and crops the edges; fit=true shows the whole photo over a blurred copy of itself.
     * Blur is a real box blur (3 passes ≈ Gaussian) on a quarter-size copy, so it stays smooth and cheap on
     * 1 GB boxes; a sharp wallpaper keeps full resolution. Darkened so white text stays readable.
     * Cached until the file, blur, layout or size changes.
     */
    static Bitmap wallpaper(Context c, int w, int h, int blur, boolean fit) {
        File f = wallpaperFile(c);
        if (!f.exists() || w <= 0 || h <= 0) return null;
        String key = f.lastModified() + "/" + blur + "/" + fit + "/" + w + "x" + h;
        if (key.equals(wallpaperKey) && wallpaperCache != null) return wallpaperCache;
        Bitmap src = BitmapFactory.decodeFile(f.getPath());
        if (src == null) return null;
        int scale = blur > 0 ? 4 : 1;
        int ow = Math.max(1, w / scale), oh = Math.max(1, h / scale);
        Bitmap out = compose(src, ow, oh, fit);
        src.recycle();
        if (blur > 0) boxBlur(out, blur == 1 ? 3 : 9);
        new Canvas(out).drawColor(blur == 0 ? 0x66000000 : 0x4D000000);
        wallpaperCache = out; // the old one may still be on screen elsewhere; let GC reclaim it
        wallpaperKey = key;
        return out;
    }

    /** src at w x h: cropped to fill, or fitted whole over a blurred fill of itself. */
    private static Bitmap compose(Bitmap src, int w, int h, boolean fit) {
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        Rect full = new Rect(0, 0, w, h);
        if (!fit) {
            canvas.drawBitmap(src, centerCrop(src.getWidth(), src.getHeight(), w, h), full, p);
            return out;
        }
        Bitmap back = Bitmap.createBitmap(Math.max(1, w / 6), Math.max(1, h / 6), Bitmap.Config.ARGB_8888);
        new Canvas(back).drawBitmap(src, centerCrop(src.getWidth(), src.getHeight(), w, h),
                new Rect(0, 0, back.getWidth(), back.getHeight()), p);
        boxBlur(back, 6);
        canvas.drawBitmap(back, null, full, p);
        back.recycle();
        canvas.drawColor(0x33000000);
        float s = Math.min(w / (float) src.getWidth(), h / (float) src.getHeight());
        int dw = Math.round(src.getWidth() * s), dh = Math.round(src.getHeight() * s);
        int x = (w - dw) / 2, y = (h - dh) / 2;
        canvas.drawBitmap(src, null, new Rect(x, y, x + dw, y + dh), p);
        return out;
    }

    /** In-place box blur, three horizontal + vertical passes (close to a Gaussian of sigma ≈ radius). */
    static void boxBlur(Bitmap b, int radius) {
        int w = b.getWidth(), h = b.getHeight();
        if (radius < 1 || w < 2 || h < 2) return;
        int[] px = new int[w * h], tmp = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        for (int pass = 0; pass < 3; pass++) {
            blurLine(px, tmp, w, h, radius, true);
            blurLine(tmp, px, w, h, radius, false);
        }
        b.setPixels(px, 0, w, 0, 0, w, h);
    }

    /** One box pass along rows (horizontal) or columns, clamping at the edges. */
    private static void blurLine(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int lines = horizontal ? h : w, len = horizontal ? w : h, div = 2 * r + 1;
        for (int line = 0; line < lines; line++) {
            int base = horizontal ? line * w : line, step = horizontal ? 1 : w;
            int sa = 0, sr = 0, sg = 0, sb = 0;
            for (int i = -r; i <= r; i++) {
                int p = in[base + clamp(i, len) * step];
                sa += p >>> 24; sr += (p >> 16) & 255; sg += (p >> 8) & 255; sb += p & 255;
            }
            for (int i = 0; i < len; i++) {
                out[base + i * step] = (sa / div) << 24 | (sr / div) << 16 | (sg / div) << 8 | (sb / div);
                int add = in[base + clamp(i + r + 1, len) * step], sub = in[base + clamp(i - r, len) * step];
                sa += (add >>> 24) - (sub >>> 24);
                sr += ((add >> 16) & 255) - ((sub >> 16) & 255);
                sg += ((add >> 8) & 255) - ((sub >> 8) & 255);
                sb += (add & 255) - (sub & 255);
            }
        }
    }

    private static int clamp(int i, int len) {
        return i < 0 ? 0 : i >= len ? len - 1 : i;
    }

    /** Unblurred photo for the screensaver, filled (cropped) or fitted whole over a blurred copy. */
    static Bitmap sharp(File f, int w, int h, boolean fit) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        int sample = 1;
        while (o.outWidth / (sample * 2) >= w && o.outHeight / (sample * 2) >= h) sample *= 2;
        BitmapFactory.Options d = new BitmapFactory.Options();
        d.inSampleSize = sample;
        Bitmap src = BitmapFactory.decodeFile(f.getPath(), d);
        if (src == null) return null;
        Bitmap out = compose(src, w, h, fit);
        src.recycle();
        return out;
    }

    private static Rect centerCrop(int sw, int sh, int w, int h) {
        float target = w / (float) h;
        if (sw / (float) sh > target) {
            int cw = Math.round(sh * target);
            int x = (sw - cw) / 2;
            return new Rect(x, 0, x + cw, sh);
        }
        int ch = Math.round(sw / target);
        int y = (sh - ch) / 2;
        return new Rect(0, y, sw, y + ch);
    }
}
