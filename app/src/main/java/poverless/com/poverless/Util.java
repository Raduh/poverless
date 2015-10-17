package poverless.com.poverless;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by raduh on 10/17/15.
 */
public class Util {
    public static void showInterestArea(GoogleMap map, PositionData posData) {
        Circle radCircle = posData.currCircle;

        map.addMarker(new MarkerOptions()
                .position(posData.center)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.youhere)));

        if (radCircle == null) {
            posData.currCircle =
                    map.addCircle(new CircleOptions()
                            .center(posData.center)
                            .radius(posData.radius)
                            .fillColor(Const.BLUE_SHADE)
                            .strokeColor(Const.BLUE_STROKE)
                            .strokeWidth(Const.STROKE_WIDTH));
        } else {
            radCircle.setRadius(posData.radius);
        }
    }
}
