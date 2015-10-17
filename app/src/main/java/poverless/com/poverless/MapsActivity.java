package poverless.com.poverless;

import android.app.Dialog;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.braintreepayments.api.models.CardBuilder;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import io.card.payment.CardIOActivity;
import io.card.payment.CreditCard;

public class MapsActivity extends FragmentActivity {
    public static final int MY_SCAN_REQUEST_CODE = 13;
    public static final String SERVER_BASE = "http://jupiter.eecs.jacobs-university.de";
    public static final int SERVER_PORT = 1414;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private PositionData positionData = new PositionData();
    private float mostRecentPaymentAmount = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressCircle);
        final SeekBar seekBar = (SeekBar) findViewById(R.id.radiusSlider);
        seekBar.setProgress((int)(100 * Const.DEFAULT_RADIUS / Const.MAX_RADIUS));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                final float newRadius = (i / 100.0f) * Const.MAX_RADIUS;
                positionData.radius = newRadius;

                Util.showInterestArea(mMap, positionData);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        new LocationTask(mMap, progressBar, getApplicationContext(), positionData).execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK  their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    public void askPayAmount(View view) {
        final Dialog dialog = new Dialog(MapsActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.amount_dialog);
        dialog.setCancelable(true);
        dialog.show();

        final EditText amount_txt = (EditText) dialog.findViewById(R.id.donationAmountTxt);
        final Button confirmBtn = (Button) dialog.findViewById(R.id.confirmAmountBtn);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String amountStr = amount_txt.getText().toString();
                dialog.cancel();

                float amount;
                try {
                    amount = Float.parseFloat(amountStr);
                } catch (Exception ex) {
                    return;  // TODO: announce user that input is invalid
                }
                startPayment(amount);
            }
        });

    }

    public void startPayment(float amount) {
        mostRecentPaymentAmount = amount;
        Intent scanIntent = new Intent(this, CardIOActivity.class);

        // customize these values to suit your needs.
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, false); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, false); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false); // default: false

        // MY_SCAN_REQUEST_CODE is arbitrary and is only used within this activity.
        startActivityForResult(scanIntent, MY_SCAN_REQUEST_CODE);
    }

    public void startChargeTransaction(String cardNumber, float amount) {
        CardBuilder cardBuilder = new CardBuilder().cardNumber(cardNumber);
        // TODO: finalize payment details
        AsyncHttpClient client = new AsyncHttpClient(SERVER_PORT, SERVER_PORT);
        RequestParams params = new RequestParams();
        params.add("amount", String.valueOf(amount));
        params.add("latitude", String.valueOf(positionData.center.latitude));
        params.add("longitude", String.valueOf(positionData.center.longitude));
        params.add("radius", String.valueOf(positionData.radius));
        params.add("type", "P");
        client.get(SERVER_BASE, params, new TextHttpResponseHandler());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MY_SCAN_REQUEST_CODE) {
            final float payAmount = mostRecentPaymentAmount;
            mostRecentPaymentAmount = -1;

            String resultDisplayStr;
            if (data != null && data.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
                CreditCard scanResult = data.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);
                startChargeTransaction(scanResult.cardNumber, payAmount);

                // Never log a raw card number. Avoid displaying it, but if necessary use getFormattedCardNumber()
                resultDisplayStr = "Card Number: " + scanResult.getRedactedCardNumber() + "\n";

                // Do something with the raw number, e.g.:
                // myService.setCardNumber( scanResult.cardNumber );

                if (scanResult.isExpiryValid()) {
                    resultDisplayStr += "Expiration Date: " + scanResult.expiryMonth + "/" + scanResult.expiryYear + "\n";
                }

                if (scanResult.cvv != null) {
                    // Never log or display a CVV
                    resultDisplayStr += "CVV has " + scanResult.cvv.length() + " digits.\n";
                }

                if (scanResult.postalCode != null) {
                    resultDisplayStr += "Postal Code: " + scanResult.postalCode + "\n";
                }
            }
            else {
                resultDisplayStr = "Scan was canceled.";
            }
            // do something with resultDisplayStr, maybe display it in a textView
            // resultTextView.setText(resultStr);
        }
        // else handle other activity results
    }
}
