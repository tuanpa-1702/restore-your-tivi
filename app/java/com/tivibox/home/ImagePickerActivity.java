package com.tivibox.home;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Photo picker for the TV remote: one grid with every photo found on internal storage and USB drives (scanned
 * folder by folder, newest first), so there is nothing to browse. Mode "wallpaper": OK on a photo uses it as
 * the home wallpaper. Mode "slides": OK ticks photos (up to 30); "Xong" saves them as the screensaver slideshow.
 */
public class ImagePickerActivity extends Activity {
    static final String EXTRA_MODE = "mode";
    static final String MODE_WALLPAPER = "wallpaper", MODE_SLIDES = "slides";
    private static final int MAX_SLIDES = 30, REQ_PERMISSION = 7;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newFixedThreadPool(2);
    private final LruCache<String, Bitmap> thumbs = new LruCache<>(80);
    private final List<File> photos = new ArrayList<>();
    private final Map<String, File> picked = new LinkedHashMap<>();
    private List<Photos.Volume> volumes = new ArrayList<>();
    private boolean slides;
    private GridView grid;
    private TextView status, done;
    private Adapter adapter;
    private float dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dp = getResources().getDisplayMetrics().density;
        slides = MODE_SLIDES.equals(getIntent().getStringExtra(EXTRA_MODE));

        FrameLayout root = new FrameLayout(this);
        MeshBackgroundView bg = new MeshBackgroundView(this, null);
        bg.setTheme(new Prefs(this).theme());
        root.addView(bg, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(40), px(28), px(40), 0);
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        col.addView(head, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        titles.addView(text(slides ? "Chọn ảnh cho màn hình chờ" : "Chọn ảnh nền", 26, 0xFFFFFFFF, true));
        status = text("", 12, 0xA6FFFFFF, false);
        titles.addView(status);
        if (slides) {
            done = text("Xong", 16, 0xFFFFFFFF, true);
            done.setGravity(Gravity.CENTER);
            done.setBackgroundResource(R.drawable.setting_row_bg);
            done.setFocusable(true);
            done.setPadding(px(22), px(10), px(22), px(10));
            done.setOnClickListener(v -> saveSlides());
            head.addView(done);
        }

        grid = new GridView(this);
        grid.setNumColumns(5);
        grid.setHorizontalSpacing(px(12));
        grid.setVerticalSpacing(px(12));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(R.drawable.app_selector);
        grid.setDrawSelectorOnTop(true);
        grid.setClipToPadding(false);
        grid.setPadding(0, px(8), 0, px(28));
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(-1, 0, 1);
        glp.topMargin = px(12);
        col.addView(grid, glp);
        adapter = new Adapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((p, v, pos, id) -> onPick(photos.get(pos)));
        setContentView(root);

        if (hasPermission()) scan();
        else requestPermissions(new String[]{permission()}, REQ_PERMISSION);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    private static String permission() {
        return Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private boolean hasPermission() {
        return checkSelfPermission(permission()) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode != REQ_PERMISSION) return;
        if (hasPermission()) scan();
        else status.setText("Chưa được cấp quyền đọc ảnh. Cấp lại trong Cài đặt → Ứng dụng → Tivi Home → Quyền, "
                + "hoặc qua ADB: pm grant com.tivibox.home " + permission());
    }

    private void scan() {
        status.setText("Đang quét ảnh trong bộ nhớ trong và USB…");
        io.execute(() -> {
            List<Photos.Volume> vols = Photos.volumes(this);
            List<File> found = Photos.scanAll(this);
            handler.post(() -> {
                if (isDestroyed()) return;
                volumes = vols;
                photos.clear();
                photos.addAll(found);
                adapter.notifyDataSetChanged();
                if (found.isEmpty()) {
                    status.setText("Không tìm thấy ảnh nào. Chép ảnh vào TV (ví dụ gửi bằng LocalSend vào thư mục "
                            + "Download) hoặc cắm USB có ảnh, rồi mở lại.");
                    return;
                }
                status.setText(found.size() + " ảnh từ " + sourcesText() + ", mới nhất trước · "
                        + (slides ? "OK để chọn / bỏ chọn (tối đa " + MAX_SLIDES + "), xong bấm Xong" : "OK để chọn")
                        + " · BACK để thoát");
                grid.requestFocus();
            });
        });
    }

    private String sourcesText() {
        List<String> names = new ArrayList<>();
        for (Photos.Volume v : volumes) names.add(v.name);
        return TextUtils.join(", ", names);
    }

    /** "Bộ nhớ trong · Pictures" for the folder holding `f`. */
    private String where(File f) {
        File parent = f.getParentFile();
        for (Photos.Volume v : volumes) {
            String root = v.dir.getPath();
            if (f.getPath().startsWith(root + "/")) {
                String dir = parent == null || parent.getPath().equals(root) ? "" : " · " + parent.getName();
                return v.name + dir;
            }
        }
        return parent == null ? "" : parent.getName();
    }

    private void onPick(File f) {
        if (!slides) {
            saveWallpaper(Uri.fromFile(f));
            return;
        }
        String k = f.getPath();
        if (picked.containsKey(k)) picked.remove(k);
        else if (picked.size() < MAX_SLIDES) picked.put(k, f);
        else Toast.makeText(this, "Tối đa " + MAX_SLIDES + " ảnh", Toast.LENGTH_SHORT).show();
        done.setText(picked.isEmpty() ? "Xong" : "Xong (" + picked.size() + ")");
        adapter.notifyDataSetChanged();
    }

    private void saveWallpaper(Uri uri) {
        status.setText("Đang lưu ảnh…");
        io.execute(() -> {
            String err = null;
            try {
                Photos.save(this, uri, Photos.wallpaperFile(this));
            } catch (Exception e) {
                err = e.getMessage();
            }
            String error = err;
            handler.post(() -> {
                if (isDestroyed()) return;
                if (error != null) {
                    status.setText("Không dùng được ảnh này: " + error);
                    return;
                }
                Toast.makeText(this, "Đã đặt ảnh nền", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    private void saveSlides() {
        if (picked.isEmpty()) {
            Toast.makeText(this, "Chưa chọn ảnh nào", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Uri> uris = new ArrayList<>();
        for (File f : picked.values()) uris.add(Uri.fromFile(f));
        status.setText("Đang lưu " + uris.size() + " ảnh…");
        io.execute(() -> {
            String err = null;
            try {
                Photos.saveSlides(this, uris);
            } catch (Exception e) {
                err = e.getMessage();
            }
            String error = err;
            handler.post(() -> {
                if (isDestroyed()) return;
                if (error != null) {
                    status.setText("Lưu ảnh không thành công: " + error);
                    return;
                }
                Toast.makeText(this, "Đã lưu " + uris.size() + " ảnh cho màn hình chờ", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    private TextView text(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return t;
    }

    private int px(float v) {
        return Math.round(v * dp);
    }

    private final class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return photos.size();
        }

        @Override
        public Object getItem(int position) {
            return photos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout cell;
            if (convertView == null) {
                Context c = ImagePickerActivity.this;
                cell = new LinearLayout(c);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setPadding(px(4), px(4), px(4), px(6));
                FrameLayout frame = new FrameLayout(c);
                cell.addView(frame, new LinearLayout.LayoutParams(-1, px(92)));
                ImageView img = new ImageView(c);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                img.setClipToOutline(true);
                img.setBackgroundResource(R.drawable.app_icon_clip);
                frame.addView(img, new FrameLayout.LayoutParams(-1, -1));
                TextView badge = text("", 13, 0xFF15151C, true);
                badge.setGravity(Gravity.CENTER);
                badge.setBackgroundResource(R.drawable.pick_badge);
                FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(px(26), px(26), Gravity.TOP | Gravity.END);
                blp.setMargins(0, px(6), px(6), 0);
                frame.addView(badge, blp);
                TextView label = text("", 12, 0xE6FFFFFF, false);
                label.setSingleLine(true);
                label.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                label.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-1, -2);
                llp.topMargin = px(6);
                cell.addView(label, llp);
                TextView place = text("", 10, 0x8CFFFFFF, false);
                place.setSingleLine(true);
                place.setEllipsize(TextUtils.TruncateAt.END);
                place.setGravity(Gravity.CENTER);
                cell.addView(place, new LinearLayout.LayoutParams(-1, -2));
                cell.setTag(new Object[]{img, badge, label, place});
            } else {
                cell = (LinearLayout) convertView;
            }
            Object[] tag = (Object[]) cell.getTag();
            ImageView img = (ImageView) tag[0];
            TextView badge = (TextView) tag[1];
            File f = photos.get(position);
            ((TextView) tag[2]).setText(f.getName());
            ((TextView) tag[3]).setText(where(f));
            int order = orderOf(f);
            badge.setVisibility(order >= 0 ? View.VISIBLE : View.GONE);
            badge.setText(String.valueOf(order + 1));
            cell.setBackgroundResource(order >= 0 ? R.drawable.app_item_moving : 0);
            String key = f.getPath();
            img.setTag(key);
            Bitmap b = thumbs.get(key);
            img.setImageBitmap(b);
            if (b == null) {
                io.execute(() -> {
                    Bitmap t = Photos.fileThumbnail(f);
                    if (t == null) return;
                    handler.post(() -> {
                        thumbs.put(key, t);
                        if (key.equals(img.getTag())) img.setImageBitmap(t);
                    });
                });
            }
            return cell;
        }

        private int orderOf(File f) {
            int i = 0;
            for (String k : picked.keySet()) {
                if (k.equals(f.getPath())) return i;
                i++;
            }
            return -1;
        }
    }

    static Intent open(Context c, String mode) {
        return new Intent(c, ImagePickerActivity.class).putExtra(EXTRA_MODE, mode);
    }
}
