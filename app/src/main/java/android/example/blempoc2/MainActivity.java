package android.example.blempoc2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.isNull;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView mText;
    private Button mAdvertiseButton;
    private Button mDiscoverButton;

    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mText = (TextView) findViewById(R.id.text);
        mDiscoverButton = (Button) findViewById(R.id.discover_btn);
        mAdvertiseButton = (Button) findViewById(R.id.advertise_btn);

        mDiscoverButton.setOnClickListener(this);
        mAdvertiseButton.setOnClickListener(this);

        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        if (!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "Multiple advertisement not supported", Toast.LENGTH_SHORT).show();
            mAdvertiseButton.setEnabled(false);
            mDiscoverButton.setEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.discover_btn) {
            discover();
        } else if (v.getId() == R.id.advertise_btn) {
            advertise();
        }
    }
    private void discover() {

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        mBluetoothLeScanner.startScan(null, settings, mScanCallback);
        Toast.makeText(getApplicationContext(),"scanning...",Toast.LENGTH_SHORT).show();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                Toast.makeText(getApplicationContext(),"stopped scanning",Toast.LENGTH_SHORT).show();
                mBluetoothLeScanner.stopScan(mScanCallback);
            }
        }, 10000);
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if( result == null||result.getScanRecord()==null||result.getScanRecord().getServiceUuids()==null)
                return;
            for(ParcelUuid uuid:result.getScanRecord().getServiceUuids()) {
                if(!isNull(result.getScanRecord().getServiceData(uuid) )) {
                    mText.append(uuid.toString()+"="+new String(result.getScanRecord().getServiceData(uuid), StandardCharsets.UTF_8) + "\n");
                }else{
                    mText.append(".");
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Toast.makeText(getApplicationContext(),"batch",Toast.LENGTH_SHORT).show();
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("BLE", "Discovery onScanFailed: " + errorCode);
            super.onScanFailed(errorCode);
        }
    };


    AdvertisingSetCallback callback = new AdvertisingSetCallback() {
        @Override
        public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
            Toast.makeText(getApplicationContext(),"advertisingsetstarted",Toast.LENGTH_SHORT).show();
            currentAdvertisingSet= advertisingSet;
        }

        @Override
        public void onAdvertisingDataSet(AdvertisingSet advertisingSet, int status) {

            Toast.makeText(getApplicationContext(),"advertisingdataset",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onScanResponseDataSet(AdvertisingSet advertisingSet, int status) {
            Toast.makeText(getApplicationContext(),"scanresponsedataset",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {

            Toast.makeText(getApplicationContext(),"advertisingsetstopped",Toast.LENGTH_SHORT).show();
        }
    };

    AdvertisingSet currentAdvertisingSet;
    BluetoothLeAdvertiser advertiser;
    private void advertise() {
        advertiser =
                BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        AdvertisingSetParameters parameters = (new AdvertisingSetParameters.Builder())
                .setLegacyMode(true) // True by default, but set here as a reminder.
                .setConnectable(false)
                .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
                .build();

        AdvertiseData data = (new AdvertiseData.Builder()).setIncludeDeviceName(false).addServiceData(new ParcelUuid(UUID.randomUUID()),"hello".getBytes()).build();


        advertiser.startAdvertisingSet(parameters, data, null, null, null, callback);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                Toast.makeText(getApplicationContext(),"stop advertising",Toast.LENGTH_SHORT).show();
                advertiser.stopAdvertisingSet(callback);
            }
        }, 10000);
    }

}
