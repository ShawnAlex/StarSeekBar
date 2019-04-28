package com.shawnalex.thumbupseekbarlib;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by ShawnAlex on 2018/3/13.
 */

public class ThumbUpSeekBarActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thumb_up_seek_bar);

        ThumbUpStartSeekbar thumbUpStartSeekbar =null;
        thumbUpStartSeekbar = (ThumbUpStartSeekbar) findViewById(R.id.activity_choose_difficulty_rangeSeekbar);
//        thumbUpStartSeekbar.setCursorSelection(1);//设置默认刻度
        thumbUpStartSeekbar.setOnCursorChangeListener(new ThumbUpStartSeekbar.OnCursorChangeListener() {
            @Override
            public void onCursorChanged(int location) {
                Log.e("ThumbUpSeekBarActivity=","=difficult="+(location));
            }
        });

    }
}
