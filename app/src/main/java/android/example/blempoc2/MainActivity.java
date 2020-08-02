package android.example.blempoc2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import android.content.Context;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.isNull;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView mText;
    private Button mAdvertiseButton;
    private Button mDiscoverButton;

    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler = new Handler();
    private UUID servUUID=UUID.fromString("b4250400-fb4b-4746-b2b0-93f0e61122c6");
    private UUID charUUID=UUID.fromString("b4250401-fb4b-4746-b2b0-93f0e61122c6");
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
        mBluetoothManager=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
    }
    private int mCounter=0;
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
        List<ScanFilter> filters=new ArrayList();
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(servUUID)).build());
        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
        Toast.makeText(getApplicationContext(),"scanning...",Toast.LENGTH_SHORT).show();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),"stopped scanning",Toast.LENGTH_SHORT).show();
                mBluetoothLeScanner.stopScan(mScanCallback);
            }
        }, 10000);
    }

    private BluetoothGattCallback bluetoothGattCallBack = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState==BluetoothGatt.STATE_CONNECTED) {
                mCurrentlyConnecting=false;
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "client connected", Toast.LENGTH_SHORT).show());
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(gatt.getService(servUUID)!=null){
                runOnUiThread(()->Toast.makeText(getApplicationContext(),"service discovered!",Toast.LENGTH_SHORT).show());
                if(gatt.getService(servUUID).getCharacteristic(charUUID)!=null){
                    gatt.readCharacteristic(gatt.getService(servUUID).getCharacteristic(charUUID));
                    runOnUiThread(()->Toast.makeText(getApplicationContext(),"characteristic not null",Toast.LENGTH_SHORT).show());
                }
            }else{
                runOnUiThread(()->Toast.makeText(getApplicationContext(),"service is null",Toast.LENGTH_SHORT).show());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            runOnUiThread(()->mText.append(gatt.getDevice().getName()+" says: "+gatt.getService(servUUID).getCharacteristic(charUUID).getStringValue(0)));
        }
    };
    boolean mCurrentlyConnecting=false;
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if( result == null||result.getScanRecord()==null||result.getScanRecord().getServiceUuids()==null)
                return;
            for(ParcelUuid uuid:result.getScanRecord().getServiceUuids()) {
                if(!mCurrentlyConnecting&&!mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT).contains(result.getDevice())) {
                    mCurrentlyConnecting=true;
                    result.getDevice().connectGatt(getApplicationContext(), false, bluetoothGattCallBack);
                    runOnUiThread(()->Toast.makeText(getApplicationContext(),"connecting to:"+result.getDevice(),Toast.LENGTH_SHORT).show());
                }else{

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
    BluetoothManager mBluetoothManager;
    BluetoothGattServer mBluetoothGattServer;
    private void advertise() {
        advertiser =
                BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        AdvertisingSetParameters parameters = (new AdvertisingSetParameters.Builder())
                .setLegacyMode(true) // True by default, but set here as a reminder.
                .setConnectable(true)
                .setScannable(true)
                .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
                .build();

        AdvertiseData data = (new AdvertiseData.Builder()).setIncludeDeviceName(true).addServiceData(new ParcelUuid(servUUID), "".getBytes()).build();


        advertiser.startAdvertisingSet(parameters, data, null, null, null, callback);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                Toast.makeText(getApplicationContext(),"stop advertising",Toast.LENGTH_SHORT).show();
                advertiser.stopAdvertisingSet(callback);
            }
        }, 10000);
        mBluetoothGattServer = mBluetoothManager.openGattServer(getApplicationContext(), gattServerCallback);
        BluetoothGattService service = new BluetoothGattService(servUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

//add a read characteristic.
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(charUUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        service.addCharacteristic(characteristic);
        mBluetoothGattServer.addService(service);
    }

    BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            runOnUiThread(()-> Toast.makeText(getApplicationContext(),"connectionStateChange:"+newState,Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);

            runOnUiThread(()-> Toast.makeText(getApplicationContext(),"service added",Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            if(characteristic.getUuid().equals(charUUID)){
                mBluetoothGattServer.sendResponse(device,requestId, BluetoothGatt.GATT_SUCCESS,offset,("Hello, World!"+mCounter).getBytes());
                mCounter++;
                mBluetoothGattServer.notifyCharacteristicChanged(device,characteristic,false);
            }
            runOnUiThread(()-> Toast.makeText(getApplicationContext(),"characteristic read request",Toast.LENGTH_SHORT).show());

            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

            runOnUiThread(()-> Toast.makeText(getApplicationContext(),"characteristic write request",Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            runOnUiThread(()-> Toast.makeText(getApplicationContext(),"descriptor write request",Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

            runOnUiThread(()-> Toast.makeText(getApplicationContext(),"descriptor read request",Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);

            runOnUiThread(()-> Toast.makeText(getApplicationContext(),"notification sent",Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);

            runOnUiThread(()-> Toast.makeText(getApplicationContext(),"mtu changed",Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);

            runOnUiThread(()-> Toast.makeText(getApplicationContext(),"execute write",Toast.LENGTH_SHORT).show());
        }
    };
}
