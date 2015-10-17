package poverless.com.poverless;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by andreibadoi on 17/10/15.
 */
public class PositionData {
    LatLng center;
    Float radius = Const.DEFAULT_RADIUS;

    Circle currCircle;
}
