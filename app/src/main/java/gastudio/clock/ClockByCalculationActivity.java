package gastudio.clock;

import android.app.Activity;
import android.os.Bundle;

import gastudio.clock.ui.ClockViewByCalculation;

public class ClockByCalculationActivity extends Activity {

    private ClockViewByCalculation mClockViewByCalculation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock_by_caculation);
        mClockViewByCalculation = (ClockViewByCalculation) findViewById(R.id.clock_view_by_calculation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClockViewByCalculation.performAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClockViewByCalculation.cancelAnimation();
    }
}
