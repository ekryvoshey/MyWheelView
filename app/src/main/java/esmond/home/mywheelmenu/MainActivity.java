package esmond.home.mywheelmenu;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private WheelView wheelView;
    private TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wheelView = (WheelView)findViewById(R.id.wheelView);
        textView = (TextView)findViewById(R.id.textView);
        textView.setText("selected: " + (wheelView.getSelectedPosition()) );
        wheelView.setWheelChangeListener(new WheelView.WheelChangeListener() {
            @Override
            public void onSelectionChange(int selectedPosition) {
                textView.setText("selected: " + (wheelView.getSelectedPosition()) );
            }
        });
    }
}
