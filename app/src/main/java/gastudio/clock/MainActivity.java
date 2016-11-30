package gastudio.clock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock_main);
        initViews();
    }

    private void initViews() {
        int[] ids = new int[]{R.id.btn_show_clock_by_img,
                R.id.btn_show_clock_by_calculate,
                R.id.btn_show_clock_by_path
        };

        Intent[] intents = new Intent[]{
                new Intent(MainActivity.this, ClockByImgActivity.class),
                new Intent(MainActivity.this, ClockByCalculationActivity.class),
                new Intent(MainActivity.this, ClockByPathActivity.class)
        };

        int idsArrayLen = ids.length;
        for (int index = 0; index < idsArrayLen; index++) {
            final Intent intent = intents[index];
            findViewById(ids[index]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(intent);
                }
            });
        }
    }
}
