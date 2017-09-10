package com.example.yuuma.connectarduino;

import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;

import static java.lang.String.valueOf;


public class MainActivity extends AppCompatActivity {
    private Button monButton;
    private Button moffButton;
    private TextView mTextView;
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume(){
        super.onResume();

        mTextView = (TextView)findViewById(R.id.text);
        monButton = (Button)findViewById(R.id.button);
        moffButton = (Button)findViewById(R.id.button2);
        mUsbManager = (UsbManager)getSystemService(USB_SERVICE);

        updateList();
        monButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (mUsbDevice == null){
                    return;
                }

                if (!mUsbManager.hasPermission(mUsbDevice)){
                    mUsbManager.requestPermission(mUsbDevice,
                            PendingIntent.getBroadcast(MainActivity.this,0,new Intent("なにか"), 0));
                    return;
                }

                connectDevice(1);
            }
        });

        moffButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (mUsbDevice == null){
                    return;
                }

                if (!mUsbManager.hasPermission(mUsbDevice)){
                    mUsbManager.requestPermission(mUsbDevice,
                            PendingIntent.getBroadcast(MainActivity.this,0,new Intent("なにか"), 0));
                    return;
                }

                connectDevice(2);
            }
        });
    }

    public void onPause(){
        super.onPause();
        mUsbDevice = null;
        monButton.setOnClickListener(null);
        moffButton.setOnClickListener(null);
    }

    private void updateList(){
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

        if (deviceList == null || deviceList.isEmpty()){
            mTextView.setText("no device found");
        } else {
            String string = "";

            for (String name : deviceList.keySet()){
                string += name;

                if (deviceList.get(name).getVendorId() == 10755){
                    string += "(Arduino)\n";
                    mUsbDevice = deviceList.get(name);
                } else{
                    string += "\n";
                }
            }
            mTextView.setText(string);
        }
    }

    private void connectDevice(int status){
        final String st = valueOf(status);
        new Thread(new Runnable(){
            @Override
            public void run(){
                UsbDeviceConnection connection = mUsbManager.openDevice(mUsbDevice);

                if (!connection.claimInterface(mUsbDevice.getInterface(1), true)){
                    connection.close();
                    return;
                }

                connection.controlTransfer(0x21, 34, 0, 0, null, 0, 0);
                connection.controlTransfer(0x21, 32, 0, 0, new byte[]{
                        (byte) 0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08
                }, 7, 0);

                UsbEndpoint epIN = null;
                UsbEndpoint epOUT = null;

                UsbInterface usbIf = mUsbDevice.getInterface(1);
                for (int i=0; i< usbIf.getEndpointCount(); i++){
                    if (usbIf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK){
                        if (usbIf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN)
                            epIN = usbIf.getEndpoint(i);
                        else
                            epOUT = usbIf.getEndpoint(i);
                    }
                }

                connection.bulkTransfer(epOUT, st.getBytes(), 1, 0);
                connection.close();
            }
        }).start();
    }
}
