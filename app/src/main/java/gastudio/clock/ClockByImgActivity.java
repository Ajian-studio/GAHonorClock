package gastudio.clock;

import android.app.Activity;
import android.os.Bundle;

import gastudio.clock.ui.ClockViewByImg;

public class ClockByImgActivity extends Activity {

    private ClockViewByImg mClockViewByImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock_by_img);
        mClockViewByImg = (ClockViewByImg) findViewById(R.id.clock_view_by_img);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClockViewByImg.performAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClockViewByImg.cancelAnimation();
    }
}
