package com.lu7idan;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

import com.cocosw.bottomsheet.BottomSheet;
import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.util.Random;


/**
 * Created by a on 2016/01/10.
 */
public class ImagePicker {
    public static final String TAG = ImagePicker.class.getSimpleName();
    private Fragment mFragment;
    private Activity mActivity;

    private int chooserType;
    private ImageChooserManager imageChooserManager;
    private String filePath;

    private String originalFilePath;
    ImagePicker.Callback mCallback;

    boolean isActivity;

    public ImagePicker(Activity activity) {
        mActivity = activity;
        isActivity = true;
    }

    public ImagePicker(Fragment fragment) {
        mFragment = fragment;
    }


    private void chooseImage() {
        chooserType = ChooserType.REQUEST_PICK_PICTURE;

        if (isActivity) {
            imageChooserManager = new ImageChooserManager(mActivity,
                    ChooserType.REQUEST_PICK_PICTURE, true);
        } else {
            imageChooserManager = new ImageChooserManager(mFragment,
                    ChooserType.REQUEST_PICK_PICTURE, true);
        }
        imageChooserManager.setImageChooserListener(mImageChosenListener);
        imageChooserManager.clearOldFiles();
        try {
            filePath = imageChooserManager.choose();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ImageChooserListener mImageChosenListener = new ImageChooserListener() {
        @Override
        public void onImageChosen(final ChosenImage image) {
            if (mActivity == null && mFragment == null)
                return;

            final Activity activity = mActivity == null ? mFragment.getActivity() : mActivity;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    originalFilePath = image.getFilePathOriginal();
                    Uri source = Uri.fromFile(new File(originalFilePath));  //fromFile(new File(getCacheDir(), "cropped"+new Random().nextInt()));
                    Uri destination = Uri.fromFile(new File(activity.getCacheDir(), "cropped" + new Random().nextInt()));
                    Crop.of(source, destination).asSquare().start(activity);
                }
            });
        }

        @Override
        public void onError(final String s) {
            if (mActivity == null && mFragment == null)
                return;
            final Activity activity = mActivity == null ? mFragment.getActivity() : mActivity;

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null)
                        mCallback.onError(s);
                    //showErrorMsg(reason);
                }
            });
        }
    };

    private void takePicture() {
        chooserType = ChooserType.REQUEST_CAPTURE_PICTURE;

        if (mActivity != null) {
            imageChooserManager = new ImageChooserManager(mActivity,
                    ChooserType.REQUEST_CAPTURE_PICTURE, true);
        } else {
            imageChooserManager = new ImageChooserManager(mFragment,
                    ChooserType.REQUEST_CAPTURE_PICTURE, true);
        }
        imageChooserManager.setImageChooserListener(mImageChosenListener);
        try {
            filePath = imageChooserManager.choose();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK
                && (requestCode == ChooserType.REQUEST_PICK_PICTURE || requestCode == ChooserType.REQUEST_CAPTURE_PICTURE)) {
            if (imageChooserManager == null) {
                reinitializeImageChooser();
            }
            imageChooserManager.submit(requestCode, data);
        } else {
        }

        if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, data);
        }
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == Activity.RESULT_OK) {
            String path = Crop.getOutput(result).getPath();
            originalFilePath = path;
            if (mCallback != null)
                mCallback.onSuccess(path);
        } else if (resultCode == Crop.RESULT_ERROR) {
            if (mCallback != null)
                mCallback.onError(Crop.getError(result).getMessage());
        }
    }


    // Should be called if for some reason the ImageChooserManager is null (Due
    // to destroying of activity for low memory situations)
    private void reinitializeImageChooser() {

        if (mActivity != null) {
            imageChooserManager = new ImageChooserManager(mActivity,
                    chooserType, true);
        } else {
            imageChooserManager = new ImageChooserManager(mFragment,
                    chooserType, true);
        }

        imageChooserManager.setImageChooserListener(mImageChosenListener);
        imageChooserManager.reinitialize(filePath);
    }


    public void addImage(Callback callback) {
        if (callback != null)
            this.mCallback = callback;

        Activity activity = mActivity != null ? mActivity : mFragment.getActivity();

        new BottomSheet.Builder(activity).title(R.string.choose_image_picking_source).sheet(R.menu.image_attachment_menu).listener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == R.id.gallery)
                    chooseImage();
                else if (which == R.id.camera)
                    takePicture();
            }
        }).show();
    }


    public interface Callback {
        void onSuccess(String imagePath);

        void onError(String errorMsg);
    }

}

