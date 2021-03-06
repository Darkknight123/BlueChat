package com.example.bluechat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private chatUtility chatUtils;

    private ListView listMainChat;
    private EditText editText;
    private Button sendMsg;
    private ArrayAdapter<String>adapterMainChat;

    private final int LOCATION_PERMISSION_REQUEST=101;
    private final int SELECT_DEVICE=102;

    public static final int MESSAGE_STATE_CHANGED=0;
    public static final int MESSAGE_READ=1;
    public static final int MESSAGE_WRITE=2;
    public static final int MESSAGE_DEVICE_NAME=3;
    public static final int MESSAGE_TOAST=4;

    public static final String DEVICE_NAME="deviceName";
    private String connectedDevice;
    public static final String TOAST="toast";


    private Handler handler =new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case MESSAGE_STATE_CHANGED:
                    switch (msg.arg1){
                        case chatUtility.STATE_NONE:
                            setState("Not connected");
                            break;
                        case chatUtility.STATE_LISTEN:
                            setState("Not Connected");
                            break;
                        case chatUtility.STATE_CONNECTION:
                            setState("Connecting....");
                            break;
                        case chatUtility.STATE_CONNECTED:
                            setState("Connected: " + connectedDevice);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] buffer1=(byte[]) msg.obj;
                    String outputBuffer=new String(buffer1);
                    adapterMainChat.add("Me: " + outputBuffer);
                    break;
                case MESSAGE_READ:
                    byte[] buffer = (byte[]) msg.obj;
                    String inputBuffer =new String(buffer,0,msg.arg1);
                    adapterMainChat.add(connectedDevice + ":" + inputBuffer);
                    break;

                case MESSAGE_DEVICE_NAME:
                    connectedDevice = (String) msg.getData().get(DEVICE_NAME);
                    Toast.makeText(context, connectedDevice,Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(context,msg.getData().getString(TOAST),Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });
    private void setState(CharSequence subTitle){
        getSupportActionBar().setSubtitle(subTitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context =this;

        init();
        chatUtils =new chatUtility(context,handler);
        initBluetooth();
    }
    private void init(){
        listMainChat =findViewById(R.id.list_conv);
        editText=findViewById(R.id.ed_enter_message);
        sendMsg =findViewById(R.id.send_btn);

        adapterMainChat=new ArrayAdapter<String>(context,R.layout.message_layout);
        listMainChat.setAdapter(adapterMainChat);

        sendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message=editText.getText().toString();
                if (!message.isEmpty()){
                    editText.setText("");
                    try {
                        chatUtils.write(String.valueOf(message.getBytes()).getBytes(message));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
    }
    private void initBluetooth(){
        bluetoothAdapter =BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter ==null){
            Toast.makeText(context,"No Bluetooth found",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_search_devices:
               checkPermission();
                return true;
            case R.id.menu_enable_bluetooth:
          enableBluetooth();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
    private void checkPermission(){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_PERMISSION_REQUEST);
        }else {
            Intent intent=new Intent(context,DeviceListActivity.class);
            startActivityForResult(intent,SELECT_DEVICE);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable  Intent data) {
        if (requestCode==SELECT_DEVICE && resultCode==RESULT_OK){
            String address =data.getStringExtra("deviceAddress");
            chatUtils.connecting(bluetoothAdapter.getRemoteDevice(address));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull  String[] permissions, @NonNull  int[] grantResults) {

        if (requestCode==LOCATION_PERMISSION_REQUEST){
            if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){

                Intent intent =new Intent(context,DeviceListActivity.class);
                startActivityForResult(intent,SELECT_DEVICE);

            }else {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Location permission is required.\n Please grant")
                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                               checkPermission();
                            }
                        })
                        .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                               MainActivity.this.finish();
                            }
                        })
                        .show();
            }

        }else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    private  void enableBluetooth(){
        if (bluetoothAdapter.isEnabled()){Toast.makeText(context,"Bluetooth Already on",Toast.LENGTH_SHORT).show();

        }else {
            bluetoothAdapter.enable();
        }

        if (bluetoothAdapter.getScanMode() !=BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            startActivity(discoveryIntent);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatUtils!=null){
            chatUtils.stop();
        }
    }
}