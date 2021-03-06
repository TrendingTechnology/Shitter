package org.nuclearfog.twidda.activity;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.ImageAdapter;
import org.nuclearfog.twidda.adapter.ImageAdapter.OnImageClickListener;
import org.nuclearfog.twidda.backend.ImageLoader;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.holder.ImageHolder;
import org.nuclearfog.twidda.backend.utils.ErrorHandler;
import org.nuclearfog.zoomview.ZoomView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN;
import static android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END;
import static android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START;
import static android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START;
import static android.os.AsyncTask.Status.RUNNING;
import static android.os.Environment.DIRECTORY_PICTURES;
import static android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;

/**
 * Media viewer activity for images and videos
 */
public class MediaViewer extends AppCompatActivity implements OnImageClickListener,
        OnPreparedListener, OnInfoListener, OnErrorListener {

    /**
     * Key for the media URL, local or online, required
     */
    public static final String KEY_MEDIA_LINK = "media_link";

    /**
     * Key for the media type, required
     * {@link #MEDIAVIEWER_IMG_S}, {@link #MEDIAVIEWER_IMAGE}, {@link #MEDIAVIEWER_VIDEO} or {@link #MEDIAVIEWER_ANGIF}
     */
    public static final String KEY_MEDIA_TYPE = "media_type";

    /// Media Types
    private static final int MEDIAVIEWER_NONE = 0;  // Not Initialized
    public static final int MEDIAVIEWER_IMG_S = 1;  // Image from Storage
    public static final int MEDIAVIEWER_IMAGE = 2;  // Image from Twitter
    public static final int MEDIAVIEWER_VIDEO = 3;  // Video from Twitter
    public static final int MEDIAVIEWER_ANGIF = 4;  // GIF from Twitter

    /**
     * Quality of the saved jpeg images
     */
    private static final int JPEG_QUALITY = 90;

    /**
     * request write permission on API < 29
     */
    private static final String[] REQ_WRITE_SD = {WRITE_EXTERNAL_STORAGE};
    private static final int REQCODE_SD = 6;

    private ImageLoader imageAsync;
    private Thread imageSaveThread;

    private ProgressBar video_progress;
    private ProgressBar image_progress;
    private MediaController videoController;
    private View imageWindow, videoWindow;
    private RecyclerView imageList;
    private ImageAdapter adapter;
    private VideoView videoView;
    private ZoomView zoomImage;

    private String[] mediaLinks;
    private int type = MEDIAVIEWER_NONE;
    private int videoPos = 0;


    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_media);
        imageList = findViewById(R.id.image_list);
        imageWindow = findViewById(R.id.image_window);
        videoWindow = findViewById(R.id.video_window);
        image_progress = findViewById(R.id.image_load);
        video_progress = findViewById(R.id.video_load);
        zoomImage = findViewById(R.id.image_full);
        videoView = findViewById(R.id.video_view);
        videoController = new MediaController(this);
        adapter = new ImageAdapter(this);
        videoView.setZOrderOnTop(true);
        videoView.setOnPreparedListener(this);
        videoView.setOnErrorListener(this);

        Bundle param = getIntent().getExtras();
        if (param != null && param.containsKey(KEY_MEDIA_LINK)) {
            mediaLinks = param.getStringArray(KEY_MEDIA_LINK);
            type = param.getInt(KEY_MEDIA_TYPE, MEDIAVIEWER_NONE);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (imageWindow.getVisibility() != VISIBLE && videoWindow.getVisibility() != VISIBLE) {
            if (mediaLinks != null && mediaLinks.length > 0) {
                switch (type) {
                    case MEDIAVIEWER_IMG_S:
                        adapter.disableSaveButton();
                    case MEDIAVIEWER_IMAGE:
                        imageWindow.setVisibility(VISIBLE);
                        imageList.setLayoutManager(new LinearLayoutManager(this, HORIZONTAL, false));
                        imageList.setAdapter(adapter);
                        if (imageAsync == null) {
                            imageAsync = new ImageLoader(this);
                            imageAsync.execute(mediaLinks);
                        }
                        break;

                    case MEDIAVIEWER_VIDEO:
                        videoView.setMediaController(videoController);
                    case MEDIAVIEWER_ANGIF:
                        videoWindow.setVisibility(VISIBLE);
                        Uri video = Uri.parse(mediaLinks[0]);
                        videoView.setVideoURI(video);
                        break;
                }
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (type == MEDIAVIEWER_VIDEO) {
            videoPos = videoView.getCurrentPosition();
            videoView.pause();
        }
    }


    @Override
    protected void onDestroy() {
        if (imageAsync != null && imageAsync.getStatus() == RUNNING)
            imageAsync.cancel(true);
        super.onDestroy();
    }


    @Override
    public void onImageClick(Bitmap image) {
        zoomImage.reset();
        zoomImage.setImageBitmap(image);
    }


    @Override
    public void onImageSave(Bitmap image, int pos) {
        boolean accessGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            int check = checkSelfPermission(WRITE_EXTERNAL_STORAGE);
            if (check == PERMISSION_DENIED) {
                requestPermissions(REQ_WRITE_SD, REQCODE_SD);
                accessGranted = false;
            }
        }
        if (accessGranted) {
            storeImage(image, mediaLinks[pos]);
        }
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        if (type == MEDIAVIEWER_ANGIF) {
            mp.setLooping(true);
        } else {
            videoController.show(0);
            if (videoPos > 0) {
                mp.seekTo(videoPos);
            }
        }
        mp.setOnInfoListener(this);
        mp.start();
    }


    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MEDIA_INFO_BUFFERING_END:
            case MEDIA_INFO_VIDEO_RENDERING_START:
                video_progress.setVisibility(INVISIBLE);
                return true;

            case MEDIA_INFO_BUFFERING_START:
                video_progress.setVisibility(VISIBLE);
                return true;
        }
        return false;
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (what == MEDIA_ERROR_UNKNOWN) {
            Toast.makeText(this, R.string.error_cant_load_video, Toast.LENGTH_SHORT).show();
            finish();
            return true;
        }
        return false;
    }

    /**
     * Called from {@link ImageLoader} when all images are downloaded successfully
     */
    public void onSuccess() {
        adapter.disableLoading();
    }

    /**
     * Called from {@link ImageLoader} when an error occurs
     *
     * @param err Exception caught by {@link ImageLoader}
     */
    public void onError(EngineException err) {
        ErrorHandler.handleFailure(getApplicationContext(), err);
        finish();
    }

    /**
     * set downloaded image into preview list
     *
     * @param image Image container
     */
    public void setImage(ImageHolder image) {
        if (adapter.isEmpty()) {
            zoomImage.reset();
            zoomImage.setImageBitmap(image.getMiddleSize());
            image_progress.setVisibility(View.INVISIBLE);
        }
        adapter.addLast(image);
    }

    /**
     * called to save an image into storage
     *
     * @param image Image file
     * @param link  image link or path
     */
    private void storeImage(final Bitmap image, final String link) {
        if (imageSaveThread == null || !imageSaveThread.isAlive()) {
            imageSaveThread = new Thread() {
                @Override
                public void run() {
                    boolean imageSaved = false;
                    try {
                        String name = "shitter_" + link.substring(link.lastIndexOf('/') + 1);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // use scoped storage
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                            values.put(MediaStore.MediaColumns.RELATIVE_PATH, DIRECTORY_PICTURES);
                            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                            Uri imageUri = getContentResolver().insert(EXTERNAL_CONTENT_URI, values);
                            if (imageUri != null) {
                                OutputStream fileStream = getContentResolver().openOutputStream(imageUri);
                                if (fileStream != null) {
                                    imageSaved = image.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fileStream);
                                    fileStream.close();
                                }
                            }
                        } else {
                            // store images directly
                            File imageFile = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES), name + ".jpg");
                            OutputStream fileStream = new FileOutputStream(imageFile);
                            imageSaved = image.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fileStream);
                            fileStream.close();
                            // start media scanner to scan for new image
                            String[] fileName = {imageFile.toString()};
                            MediaScannerConnection.scanFile(getApplicationContext(), fileName, null, null);
                        }
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                    if (imageSaved) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), R.string.info_image_saved, Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), R.string.error_image_save, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            };
            imageSaveThread.start();
        }
    }
}