package esmond.home.mywheelmenu;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private WheelView wheelView;
    private TextView textView;
    private static String[] cars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initCarsList();
        wheelView = (WheelView)findViewById(R.id.wheelView);
        textView = (TextView)findViewById(R.id.textView);
        textView.setText("You selected: " + "\n"+WheelView.menuItems.get(wheelView.getSelectedPosition()));
        wheelView.setWheelChangeListener(new WheelView.WheelChangeListener() {
            @Override
            public void onSelectionChange(int selectedPosition) {
                textView.setText("You selected: " + "\n" +WheelView.menuItems.get(wheelView.getSelectedPosition()-1));
            }
        });
    }

    public void initCarsList(){
        cars = new String[]{
                "Acura", "Alfa Romeo", "Audi", "Bentley", "BMW", "Bugatti", "Buick", "Cadillac",
                "Chevrolet", "Chrysler", "Citroen", "Dodge", "Ferrari", "Fiat", "Ford", "Honda",
                "Hyundai", "Infiniti", "Jaguar", "Jeep", "KIA", "Lamborghini", "Land Rover", "Lexus",
                "Lincoln", "Maserati", "Mercedes-Benz", "Mini", "Nissan", "Peugeot", "Porsche",
                "Renault", "Rolls-Royce", "Subaru", "Tesla", "Toyota", "Volkswagen", "Volvo"
        };
        Log.d("initCarsList()", "cars in list: " + cars.length);
    }

    public static String[] getCarsList(){
        return cars;
    }
    public static String getCarFromList(int i){
        return cars[i];
    }
    public static int getCarIndex(int i){
        return Arrays.asList(cars).indexOf(i);
    }
}
