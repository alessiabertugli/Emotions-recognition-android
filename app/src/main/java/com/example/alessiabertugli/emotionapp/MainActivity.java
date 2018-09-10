package com.example.alessiabertugli.emotionapp;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.provider.BigImageCardProvider;
import com.dexafree.materialList.view.MaterialListView;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.PedestrianDet;
import com.tzutalin.dlib.VisionDetRet;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;
import hugo.weaving.DebugLog;
import timber.log.Timber;
import weka.classifiers.Classifier;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    public EmotionPredictionWeka em = new EmotionPredictionWeka();
    public EmotionsList cs = new EmotionsList();
    public String modelName = null;
    public Bundle bundle = new Bundle();
    private static final int RESULT_LOAD_IMG = 1;
    private static final int REQUEST_CODE_PERMISSION = 2;
    private static final String WEKA_TEST = "WekaTest";
    private static final String TAG = "MainActivity";
    private static final String tfModel = "fer2013_mini_XCEPTION.102-0.66.pb";
    private EmotionPredictionTFMobile mClassifier;
    String class_name_new = new String();

    // Storage Permissions
    private static String[] PERMISSIONS_REQ = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    protected String mTestImgPath;

    @ViewById(R.id.material_listview)
    protected MaterialListView mListView;
    @ViewById(R.id.fab)
    protected FloatingActionButton mFabActionBt;
    @ViewById(R.id.fab_cam)
    protected FloatingActionButton mFabCamActionBt;
    @ViewById(R.id.toolbar)
    protected Toolbar mToolbar;
    @ViewById(R.id.Model1)
    protected Button mButton1;
    @ViewById(R.id.Model2)
    protected Button mButton2;
    @ViewById(R.id.Model3)
    protected Button mButton3;
    @ViewById(R.id.TFModel)
    protected Button mButtonTF;

    private TextView mPred;

    FaceDet mFaceDet;
    PedestrianDet mPersonDet;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListView = findViewById(R.id.material_listview);
        setSupportActionBar(mToolbar);
        modelName = null;

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        // Just use hugo to print log
        isExternalStorageWritable();
        isExternalStorageReadable();

        // For API 23+ you need to request the read/write permissions even if they are already in your manifest.
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;

        if (currentapiVersion >= Build.VERSION_CODES.M) {
            verifyPermissions(this);
        }

        Log.d(WEKA_TEST, "onCreate() finished.");

    }

    @AfterViews
    protected void setupUI() {
        mToolbar.setTitle(getString(R.string.app_name));
        mToolbar.setTitleTextColor(Color.parseColor("#30DAC5"));
        Toast.makeText(MainActivity.this, getString(R.string.description_info), Toast.LENGTH_LONG).show();
    }

    @Click({R.id.fab})
    protected void launchGallery() {
        Toast.makeText(MainActivity.this, "Pick one image", Toast.LENGTH_SHORT).show();
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }
    @Click({R.id.fab_cam})
    protected void launchCameraPreview() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Click({R.id.Model1})
    protected  void loadModel1(){
        modelName = "logistic.model";
        saveModel();
        Toast.makeText(MainActivity.this,  "Model loaded: Logistic.", Toast.LENGTH_SHORT).show();
    }
    @Click({R.id.Model2})
    protected void loadModel2(){
        modelName = "sgd.model";
        saveModel();
        Toast.makeText(MainActivity.this,  "Model loaded: Sgd.", Toast.LENGTH_SHORT).show();
    }
    @Click({R.id.Model3})
    protected void loadModel3(){
        modelName = "naivebayes.model";
        saveModel();
        Toast.makeText(MainActivity.this,  "Model loaded: Naive Bayes.", Toast.LENGTH_SHORT).show();
    }
    @Click({R.id.TFModel})
    protected void loadTFModel(){
        modelName = tfModel;
        saveModel();
        Toast.makeText(MainActivity.this, "Model loaded: Cnn.", Toast.LENGTH_SHORT).show();
    }

    public void saveModel(){
        bundle.putString("modelString", modelName);
    }


    /**
     * Checks if the app has permission to write to device storage or open camera
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    @DebugLog
    private static boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int write_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_persmission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int camera_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (write_permission != PackageManager.PERMISSION_GRANTED ||
                read_persmission != PackageManager.PERMISSION_GRANTED ||
                camera_permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_REQ,
                    REQUEST_CODE_PERMISSION
            );
            return false;
        } else {
            return true;
        }
    }

    /* Checks if external storage is available for read and write */
    @DebugLog
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    @DebugLog
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    @DebugLog
    protected void demoStaticImage() {
        Timber.tag(WEKA_TEST).d("demoStaticImage() mTestImgPath is null, go to gallery");
        Toast.makeText(MainActivity.this, "Pick an image to run algorithms", Toast.LENGTH_SHORT).show();
        // Create intent to Open Image applications like Gallery, Google Photos
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            Toast.makeText(MainActivity.this, "Demo using static images", Toast.LENGTH_SHORT).show();
            demoStaticImage();
        }
    }

    //Load image from gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setContentView(R.layout.static_image_section);
        mPred = findViewById(R.id.predicted_emotion);

        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && null != data) {
                // Get the Image from data
                Uri selectedImage = data.getData();

                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                mTestImgPath = cursor.getString(columnIndex);
                cursor.close();
                if (mTestImgPath != null && !modelName.equals(tfModel)) {
                    runDetectAsync(mTestImgPath);
                }
                else if(mTestImgPath != null && modelName.equals(tfModel)){
                    runDetectAsyncTF(mTestImgPath);
                }
            } else {
                Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
        }

    }

    private ProgressDialog mDialog;

    //Load and run model to detect face
    @NonNull
    protected void runDetectAsync(@NonNull String imgPath) throws IOException {
        showDiaglog();

        final String targetPath = Constants.getFaceShapeModelPath();
        if (!new File(targetPath).exists()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Copy landmark model to " + targetPath, Toast.LENGTH_SHORT).show();
                }
            });
            FileUtils.copyFileFromRawToOthers(getApplicationContext(), R.raw.shape_predictor_68_face_landmarks, targetPath);
        }
        // Init
        if (mPersonDet == null) {
            mPersonDet = new PedestrianDet();
        }
        if (mFaceDet == null) {
            mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
        }

        Timber.tag(WEKA_TEST).d("Image path: " + imgPath);
        List<VisionDetRet> faceList = mFaceDet.detect(imgPath);

        if (faceList.size() > 0) {
            class_name_new = null;
            Card card = new Card.Builder(MainActivity.this)
                    .withProvider(BigImageCardProvider.class)
                    .setDrawable(drawRect(imgPath, faceList, Color.parseColor("#30DAC5")))
                    .endConfig()
                    .build();
            addCardView(card);
            setText(mPred, class_name_new);

        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "No face found", Toast.LENGTH_SHORT).show();
                }
            });
        }

        dismissDialog();
    }

    @NonNull
    protected void runDetectAsyncTF(@NonNull String imgPath) throws IOException {
        showDiaglog();

        class_name_new = null;
        Card card = new Card.Builder(MainActivity.this)
                .withProvider(BigImageCardProvider.class)
                .setDrawable(drawRectTF(imgPath, Color.parseColor("#30DAC5")))
                .endConfig()
                .build();
        addCardView(card);
        setText(mPred, class_name_new);

        dismissDialog();
    }

    private void setText(final TextView text,final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }

    @UiThread
    protected void addCardView(Card card) {
        mListView.add(card);
    }

    @UiThread
    protected void showDiaglog() {
        mDialog = ProgressDialog.show(MainActivity.this, "Wait", "Emotion detection", true);
    }

    @UiThread
    protected void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }


    //Draw rectangle and landmark on image
    @DebugLog
    public BitmapDrawable drawRect(String path, List<VisionDetRet> results, int color) throws IOException {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        Bitmap bm = BitmapFactory.decodeFile(path, options);

        android.graphics.Bitmap.Config bitmapConfig = bm.getConfig();
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bm = bm.copy(bitmapConfig, true);

        int width = bm.getWidth();
        int height = bm.getHeight();

        // By ratio scale
        float aspectRatio = bm.getWidth() / (float) bm.getHeight();
        final int MAX_SIZE = 512;
        int newWidth = MAX_SIZE;
        int newHeight = MAX_SIZE;
        float resizeRatio = 1;
        newHeight = Math.round(newWidth / aspectRatio);
        if (bm.getWidth() > MAX_SIZE && bm.getHeight() > MAX_SIZE) {
            Timber.tag(WEKA_TEST).d("Resize Bitmap");
            bm = getResizedBitmap(bm, newWidth, newHeight);
            resizeRatio = (float) bm.getWidth() / (float) width;
            Timber.tag(WEKA_TEST).d("resizeRatio " + resizeRatio);
        }
        // Create canvas to draw
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);

        float coeff = newWidth / 48;

        ArrayList<org.opencv.core.Point> points_coord = new ArrayList<>();
        String class_predicted = new String();
            // Loop result list
        for (VisionDetRet ret : results) {
            Rect bounds = new Rect();
            bounds.left = (int) (ret.getLeft() * resizeRatio);
            bounds.top = (int) (ret.getTop() * resizeRatio);
            bounds.right = (int) (ret.getRight() * resizeRatio);
            bounds.bottom = (int) (ret.getBottom() * resizeRatio);
            canvas.drawRect(bounds, paint);
            // Get landmark
            int cont = 0;
            ArrayList<android.graphics.Point> landmarks = ret.getFaceLandmarks();
            for (android.graphics.Point point : landmarks) {
                cont++;
                int pointX = (int) (point.x * resizeRatio);
                int pointY = (int) (point.y * resizeRatio);
                canvas.drawText(String.valueOf(cont), pointX, pointY, paint);

                double[] p = new double[2];
                //My model used 48x48 images, so I have to scale landmarks on a 48x48
                p[0] = (int) (pointX / coeff);
                p[1] = (int) (pointY / coeff);
                org.opencv.core.Point po = new org.opencv.core.Point();
                po.set(p);
                points_coord.add(po);
            }

            double[] current_features = em.extractFeatures(points_coord);
            double[] class_percentage = em.predictNewEmotion(current_features, loadModel());


            double max = 0.0;
            int index_class = 0;

            for (int i = 0; i < class_percentage.length; i++) {
                if (class_percentage[i] > max) {
                    max = class_percentage[i];
                    index_class = i;
                }
            }
            class_predicted = cs.GetClassLabel(index_class);
            }

        class_name_new = GetClassName(class_predicted);

        return new BitmapDrawable(getResources(), bm);
    }

    @DebugLog
    public BitmapDrawable drawRectTF(String path, int color) throws IOException {
        init();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        Bitmap bm = BitmapFactory.decodeFile(path, options);

        android.graphics.Bitmap.Config bitmapConfig = bm.getConfig();
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bm = bm.copy(bitmapConfig, true);

        // By ratio scale
        float aspectRatio = bm.getWidth() / (float) bm.getHeight();
        final int MAX_SIZE = 512;
        int newWidth = MAX_SIZE;
        int newHeight = MAX_SIZE;
        float resizeRatio = 1;
        newHeight = Math.round(newWidth / aspectRatio);
        if (bm.getWidth() > MAX_SIZE && bm.getHeight() > MAX_SIZE) {
            Timber.tag(WEKA_TEST).d("Resize Bitmap");
            bm = getResizedBitmap(bm, newWidth, newHeight);
            Timber.tag(WEKA_TEST).d("resizeRatio " + resizeRatio);
        }

        Bitmap inverted = Bitmap.createScaledBitmap(bm, EmotionPredictionTFMobile.DIM_IMG_SIZE_WIDTH, EmotionPredictionTFMobile.DIM_IMG_SIZE_WIDTH, true);
        Results result = mClassifier.classify(inverted);
        String class_predicted = cs.GetClassLabel(result.getNumber());

        class_name_new = GetClassName(class_predicted);


        return new BitmapDrawable(getResources(), bm);
    }

    private String GetClassName(String cls){
        return  cls;
    }

    public Classifier loadModel(){

        AssetManager assetManager = getAssets();
        Classifier cls = null;

        Log.d(TAG, "Debug message static image "+modelName);

        if(modelName == null){
            Toast.makeText(MainActivity.this, "Please load a model", Toast.LENGTH_SHORT).show();
        }
        else {
            try {
                cls = (Classifier) weka.core.SerializationHelper.read(assetManager.open(modelName));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {

                e.printStackTrace();
            }
        }
        return cls;
    }

    //Resized bitmap to a new adapted scale
    @DebugLog
    protected Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
        return resizedBitmap;
    }

    private void init() {
        try {
            mClassifier = new EmotionPredictionTFMobile(getApplicationContext(), tfModel);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to create classifier.", e);
        }
    }
}
