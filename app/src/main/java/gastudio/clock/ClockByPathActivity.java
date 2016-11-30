package gastudio.clock;

import android.app.Activity;
import android.os.Bundle;

import gastudio.clock.ui.ClockViewByPath;

public class ClockByPathActivity extends Activity {

    private ClockViewByPath mClockViewByPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock_by_path);
        mClockViewByPath = (ClockViewByPath) findViewById(R.id.clock_view_by_path);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClockViewByPath.performAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClockViewByPath.cancelAnimation();
    }
}
