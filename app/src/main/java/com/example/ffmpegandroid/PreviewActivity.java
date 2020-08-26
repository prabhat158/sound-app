package com.example.ffmpegandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ClippingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class PreviewActivity extends AppCompatActivity{

    private static PlayerView playerView;
    String video_url;
    SeekBar seekBar;
//    ImageView imageView = null;
    Handler handler;
    Runnable runnable;
    ImageView selectedImageView;
    int progress;
    TextView cut_video;
    TextView delete_btn;

    int totalWidth = 0;

    List<ImageView> imageViews;
    List<Bitmap> bitmapImages;

    FFmpegMediaMetadataRetriever med;
    MediaMetadataRetriever retriever;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        cut_video = findViewById(R.id.cut_btn);
        delete_btn = findViewById(R.id.delete_btn);
        final LinearLayout ll = (LinearLayout)findViewById(R.id.LinearLayout);

        seekBar=(SeekBar)findViewById(R.id.seekBar);
        playerView = findViewById(R.id.playerview);
//        imageView = findViewById(R.id.frameimage);

        video_url= Variables.outputfile2;

        retriever = new MediaMetadataRetriever();
        retriever.setDataSource(video_url);

        med = new FFmpegMediaMetadataRetriever();
        med.setDataSource(video_url);
        imageViews = new ArrayList<>();
        bitmapImages = new ArrayList<>();

        final long ffmduration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

        Log.d("screen_size", ""+Variables.screen_width);
        Log.d("screen_size", ""+Variables.screen_height);

//        final ArrayList<Bitmap> bitms =  new ArrayList<Bitmap>();
        Bitmap first_img = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST);
        first_img = Bitmap.createScaledBitmap(first_img,(int)(first_img.getWidth()*0.1), (int)(first_img.getHeight()*0.1), true);
        for (int i=0; i<20; i++){
            Bitmap temp = retriever.getFrameAtTime(i * ffmduration * 1000/19, MediaMetadataRetriever.OPTION_CLOSEST);
            first_img = combineImages(first_img, Bitmap.createScaledBitmap(temp,(int)(temp.getWidth()*0.1), (int)(temp.getHeight()*0.1), true));
//            bitms.add(retriever.getFrameAtTime(i * ffmduration * 1000/100, MediaMetadataRetriever.OPTION_CLOSEST));
        }

//        ############### Init First ImageView
        final ImageView ii= new ImageView(getApplicationContext());
        ii.setPadding(2,2,2,2);
        ii.setImageBitmap(first_img);
        ll.addView(ii);
        totalWidth = first_img.getWidth();
        ii.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage(ii);
            }
        });
        imageViews.add(ii);
        bitmapImages.add(first_img);
//        ############### End ImageView


        seekBar.setPadding(12, 0, 12, 0);

        seekBar.setOnTouchListener(new SeekBar.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                int action = event.getAction();
                switch (action)
                {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                // Handle Seekbar touch events.
                v.onTouchEvent(event);
                return true;
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progr,
                                          boolean fromUser) {
                progress = progr;
//                imageView.setImageBitmap(retriever.getFrameAtTime(progress * ffmduration * 1000/100, MediaMetadataRetriever.OPTION_CLOSEST));
//                Toast.makeText(getApplicationContext(),"seekbar progress: "+progress, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        final Bitmap finalFirst_img = first_img;
        cut_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int temp_size = 0;
                for (int i = 0; i < bitmapImages.size(); i++) {
                    temp_size += bitmapImages.get(i).getWidth();
                    if (temp_size > progress * totalWidth / 100) {
                        Bitmap[] bitmaps = splitBitmap(bitmapImages.get(i), progress * totalWidth / 100 +bitmapImages.get(i).getWidth() - temp_size);
                        bitmapImages.remove(i);
                        bitmapImages.add(i, bitmaps[0]);
                        bitmapImages.add(i + 1, bitmaps[1]);

                        imageViews.get(i).setImageBitmap(bitmaps[0]);

                        final ImageView ii = new ImageView(getApplicationContext());
                        ii.setPadding(2, 2, 2, 2);
                        ii.setImageBitmap(bitmaps[1]);
                        ll.addView(ii, i + 1);
                        imageViews.add(i + 1, ii);

                        ii.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                selectImage(ii);
                            }
                        });

                        break;
                    }
                }
            }
        });
        delete_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = imageViews.indexOf(selectedImageView);

                totalWidth = totalWidth  - bitmapImages.get(index).getWidth();

                imageViews.remove(index);
                bitmapImages.remove(index);
                ll.removeView(selectedImageView);
                selectedImageView = null;
            }
        });

        Set_Player(video_url);

    }


    SimpleExoPlayer player;
    public void Set_Player(String path){
        DefaultTrackSelector trackSelector = new DefaultTrackSelector();
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "TikTok"));

        MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(path));

        ClippingMediaSource clippingSource =
                new ClippingMediaSource(
                        videoSource,
                        /* startPositionUs= */ 18_000_000,
                        /* endPositionUs= */ 852_000_000);

        player.prepare(clippingSource);
        player.setPlayWhenReady(true);
        playerView.setPlayer(player);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                seekBar.setProgress((int) ((player.getCurrentPosition()*100)/player.getDuration()));
                handler.postDelayed(runnable, 10);
            }
        };
        handler.postDelayed(runnable, 0);
    }

    public Bitmap combineImages(Bitmap c, Bitmap s) { // can add a 3rd parameter 'String loc' if you want to save the new image - left some code to do that at the bottom
        Bitmap cs = null;
        int width, height = 0;

        if(c.getWidth() > s.getWidth()) {
            width = c.getWidth() + s.getWidth();
            height = c.getHeight();
        } else {
            width = s.getWidth() + s.getWidth();
            height = c.getHeight();
        }

        cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas comboImage = new Canvas(cs);

        comboImage.drawBitmap(c, 0f, 0f, null);
        comboImage.drawBitmap(s, c.getWidth(), 0f, null);

        return cs;
    }

    public Bitmap[] splitBitmap(Bitmap bitmap,int progress_width) {
        Bitmap[] bitmaps = new Bitmap[2];
        int height, real_width;

//        width = bitmap.getWidth() * progress / 100;
        real_width = bitmap.getWidth();
        height = bitmap.getHeight();

        bitmaps[0] = Bitmap.createBitmap(bitmap, 0,0, progress_width-2, height);
        bitmaps[1] = Bitmap.createBitmap(bitmap, progress_width + 2, 0, real_width - progress_width-2, height);

        return bitmaps;
    }

    public void selectImage(ImageView image){
        if(selectedImageView != null && selectedImageView != image)
            selectedImageView.setBackgroundDrawable(null);
        if(image.getBackground() == null) {
            selectedImageView = image;
            image.setBackgroundResource(R.drawable.border);
            Log.d("setBackgroundResource", "h 1");
        }else {
            selectedImageView = null;
            image.setBackgroundDrawable(null);
            Log.d("setBackgroundResource", "h 2");
        }
    }

}