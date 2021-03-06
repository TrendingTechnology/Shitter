package org.nuclearfog.twidda.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.MessageUpdater;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.holder.MessageHolder;
import org.nuclearfog.twidda.backend.utils.DialogBuilder;
import org.nuclearfog.twidda.backend.utils.DialogBuilder.OnDialogClick;
import org.nuclearfog.twidda.backend.utils.ErrorHandler;
import org.nuclearfog.twidda.backend.utils.FontTool;
import org.nuclearfog.twidda.database.GlobalSettings;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.Intent.ACTION_PICK;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.AsyncTask.Status.RUNNING;
import static android.view.View.VISIBLE;
import static android.view.Window.FEATURE_NO_TITLE;
import static android.widget.Toast.LENGTH_SHORT;
import static org.nuclearfog.twidda.activity.MediaViewer.KEY_MEDIA_LINK;
import static org.nuclearfog.twidda.activity.MediaViewer.KEY_MEDIA_TYPE;
import static org.nuclearfog.twidda.activity.MediaViewer.MEDIAVIEWER_IMG_S;
import static org.nuclearfog.twidda.backend.utils.DialogBuilder.DialogType.MSG_POPUP_LEAVE;

/**
 * Direct message popup activity
 */
public class MessagePopup extends AppCompatActivity implements OnClickListener, OnDismissListener, OnDialogClick {

    /**
     * key for the screen name if any
     */
    public static final String KEY_DM_PREFIX = "dm_prefix";

    /**
     * permission request for the external storage
     */
    private static final String[] PERM_READ = {Manifest.permission.READ_EXTERNAL_STORAGE};

    /**
     * Cursor mode to get the full path to the image
     */
    private static final String[] PICK_IMAGE = {MediaStore.Images.Media.DATA};

    /**
     * mime type for image files with undefined extensions
     */
    private static final String TYPE_IMAGE = "image/*";

    /**
     * request code to access gallery images
     */
    private static final int REQ_MEDIA = 3;

    /**
     * request code to get read permission
     */
    private static final int REQ_PERM_READ = 4;

    private MessageUpdater messageAsync;

    private EditText receiver, message;
    private ImageButton media;
    private Dialog loadingCircle, leaveDialog;

    @Nullable
    private String mediaPath;


    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.popup_dm);
        View root = findViewById(R.id.dm_popup);
        View send = findViewById(R.id.dm_send);
        media = findViewById(R.id.dm_media);
        receiver = findViewById(R.id.dm_receiver);
        message = findViewById(R.id.dm_text);
        loadingCircle = new Dialog(this, R.style.LoadingDialog);
        View load = View.inflate(this, R.layout.item_load, null);
        View cancelButton = load.findViewById(R.id.kill_button);

        Bundle param = getIntent().getExtras();
        if (param != null && param.containsKey(KEY_DM_PREFIX)) {
            String prefix = param.getString(KEY_DM_PREFIX);
            receiver.append(prefix);
        }

        GlobalSettings settings = GlobalSettings.getInstance(this);
        root.setBackgroundColor(settings.getPopupColor());
        FontTool.setViewFontAndColor(settings, root);

        leaveDialog = DialogBuilder.create(this, MSG_POPUP_LEAVE, this);
        loadingCircle.requestWindowFeature(FEATURE_NO_TITLE);
        loadingCircle.setCanceledOnTouchOutside(false);
        loadingCircle.setContentView(load);
        cancelButton.setVisibility(VISIBLE);

        send.setOnClickListener(this);
        media.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        loadingCircle.setOnDismissListener(this);
    }


    @Override
    public void onBackPressed() {
        if (receiver.getText().length() == 0 && message.getText().length() == 0 && mediaPath == null) {
            super.onBackPressed();
        } else if (!leaveDialog.isShowing()) {
            leaveDialog.show();
        }
    }


    @Override
    protected void onDestroy() {
        if (messageAsync != null && messageAsync.getStatus() == RUNNING)
            messageAsync.cancel(true);
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int reqCode, int returnCode, @Nullable Intent intent) {
        super.onActivityResult(reqCode, returnCode, intent);
        if (reqCode == REQ_MEDIA && returnCode == RESULT_OK) {
            if (intent != null && intent.getData() != null) {
                Cursor c = getContentResolver().query(intent.getData(), PICK_IMAGE, null, null, null);
                if (c != null) {
                    if (c.moveToFirst()) {
                        int index = c.getColumnIndex(PICK_IMAGE[0]);
                        mediaPath = c.getString(index);
                        media.setImageResource(R.drawable.image);
                    }
                    c.close();
                }
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_PERM_READ && grantResults[0] == PERMISSION_GRANTED) {
            getMedia();
        }
    }


    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        // send direct message
        if (viewId == R.id.dm_send) {
            String username = receiver.getText().toString();
            String message = this.message.getText().toString();
            if (!username.trim().isEmpty() && (!message.trim().isEmpty() || mediaPath != null)) {
                MessageHolder messageHolder = new MessageHolder(username, message, mediaPath);
                messageAsync = new MessageUpdater(this, messageHolder);
                messageAsync.execute();
            } else {
                Toast.makeText(this, R.string.error_dm, LENGTH_SHORT).show();
            }
        }
        // open media
        else if (viewId == R.id.dm_media) {
            if (mediaPath == null)
                getMedia();
            else {
                Intent image = new Intent(this, MediaViewer.class);
                image.putExtra(KEY_MEDIA_LINK, new String[]{mediaPath});
                image.putExtra(KEY_MEDIA_TYPE, MEDIAVIEWER_IMG_S);
                startActivity(image);
            }
        }
        // stop updating
        else if (viewId == R.id.kill_button) {
            loadingCircle.dismiss();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (messageAsync != null && messageAsync.getStatus() == RUNNING) {
            messageAsync.cancel(true);
        }
    }

    @Override
    public void onConfirm(DialogBuilder.DialogType type) {
        if (type == MSG_POPUP_LEAVE) {
            finish();
        }
    }

    /**
     * enable or disable loading dialog
     *
     * @param enable true to enable dialog
     */
    public void setLoading(boolean enable) {
        if (enable) {
            loadingCircle.show();
        } else {
            loadingCircle.dismiss();
        }
    }

    /**
     * called when direct message is sent
     */
    public void onSuccess() {
        Toast.makeText(this, R.string.info_dm_send, Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * called when an error occurs
     *
     * @param error Engine Exception
     */
    public void onError(EngineException error) {
        ErrorHandler.handleFailure(this, error);
    }

    /**
     * access to storage to add an image to the direct message
     * or ask for permission
     */
    private void getMedia() {
        boolean accessGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int check = checkSelfPermission(READ_EXTERNAL_STORAGE);
            if (check == PERMISSION_DENIED) {
                requestPermissions(PERM_READ, REQ_PERM_READ);
                accessGranted = false;
            }
        }
        if (accessGranted) {
            Intent mediaSelect = new Intent(ACTION_PICK);
            mediaSelect.setType(TYPE_IMAGE);
            try {
                startActivityForResult(mediaSelect, REQ_MEDIA);
            } catch (ActivityNotFoundException err) {
                Toast.makeText(getApplicationContext(), R.string.error_no_media_app, LENGTH_SHORT).show();
            }
        }
    }
}