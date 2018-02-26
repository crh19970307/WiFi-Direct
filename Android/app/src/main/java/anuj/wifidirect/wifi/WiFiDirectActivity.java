
package anuj.wifidirect.wifi;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.kbeanie.multipicker.api.FilePicker;

import java.util.ArrayList;
import java.util.List;

import anuj.wifidirect.R;
import anuj.wifidirect.utils.PermissionsAndroid;
import anuj.wifidirect.utils.Utils;

import static anuj.wifidirect.utils.PermissionsAndroid.WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */

public class WiFiDirectActivity extends AppCompatActivity implements ChannelListener, DeviceListFragment.DeviceActionListener, DeviceListFragment.MyListener{
    private static ProgressDialog mProgressDialog;
    private View mContentView = null;
    private Toolbar mToolbar;
    public static final String TAG = "wifidirectdemo";
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private FilePicker filePicker;
    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;
    private FragmentManager fragmanager;
    private FragmentTransaction transaction;
    private ArrayList GroupArray;
    private ArrayList<WifiP2pDevice> device;
    private ArrayList deviceAddress;
    static long ActualFilelength = 0;
    private ArrayList<WifiP2pDevice> wholeDevice;
    private String[] array;
    private ArrayList groupList;
    WifiP2pDeviceList peerList;
    private String[] dialogName;
    private String groupName;
    private int groupListNum;
    private ArrayList<ArrayList> groupDevice;
    private ListView listView;
    private String[] groupList2;
    private String[] itemPick;
    private ArrayAdapter<String>  groupInformation;
    private String[] groupDeviceList;
    ProgressDialog progressDialog = null;
    private ArrayList<WifiP2pDevice> temp;
    private ArrayList GroupDevice;
    private ArrayList wholeDeviceName;
    private Boolean[] deviceState;
    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        groupList = new ArrayList();//初始化存放groupList的列表（用来放name？)
        groupListNum=0;
        groupDevice = new ArrayList();
        wholeDeviceName = new ArrayList();
        wholeDevice = new ArrayList();
        temp = new ArrayList<WifiP2pDevice> ();
        checkStoragePermission();
        initViews();
        Log.d(WiFiDirectActivity.TAG,"begin oncreate");

    }

    private void checkStoragePermission() {
        boolean isExternalStorage = PermissionsAndroid.getInstance().checkWriteExternalStoragePermission(this);
        if (!isExternalStorage) {
            PermissionsAndroid.getInstance().requestForWriteExternalStoragePermission(this);
        }
    }

    private void initViews() {
        // add necessary intent values to be matched.
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("WifiDirect");
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        //获取 WifiP2pManager对象并在下一步初始化
        channel = manager.initialize(this, getMainLooper(), this);
    }


    /**
     * register the BroadcastReceiver with the intent values to be matched
     */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
/*
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }


    //listview的点击效果
    public AdapterView.OnItemClickListener listener= new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            AlertDialog.Builder itemSelect = new AlertDialog.Builder(WiFiDirectActivity.this);

            itemPick = new String[3];
            itemPick[0]="Connect";
            itemPick[1]="Remove";
            itemPick[2]="查看group member信息";
            final int place= position; //选的哪个group
            final int k=groupDevice.get(place).size();//这个group的device数目
            temp = groupDevice.get(place);
            deviceState = new Boolean[k];
            AlertDialog dialog =itemSelect.setTitle("选择操作")//标题栏
                    .setItems(
                            itemPick,
                            new DialogInterface.OnClickListener(){
                                @Override
                                public void onClick(
                                        DialogInterface dialog,int which){
                                  switch (which) {
                                      case 0://先连接再选文件发送
                                          //connect peeers
                                          DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getSupportFragmentManager()
                                                  .findFragmentById(R.id.frag_detail);
                                          fragmentDetails.send(k);
                                          for (int j=0;j<k;j++) {
                                              if (temp.get(j).status == WifiP2pDevice.CONNECTED)
                                              {
                                                  deviceState[j]=true;
                                              }
                                              else {
                                                  deviceState[j] = false;
                                                  //tempAddress = groupDeviceAddress.get(place);
                                                  WifiP2pConfig config = new WifiP2pConfig();
                                                  if (config != null && config.deviceAddress != null && device != null) {
                                                      config.deviceAddress = temp.get(j).deviceAddress.toString();
                                                      config.wps.setup = WpsInfo.PBC;

                                                      connect(config);
                                                      try {
                                                          Thread.sleep(6000);
                                                      } catch (InterruptedException e) {
                                                          e.printStackTrace();
                                                      }
                                                      }
                                              }
                                          }
                                          fragmentDetails.checkExternalStoragePermission();

                                          //加一个选择文件 and 发送文件
                                          //还要考虑要不要加一个disconnect
                                          break;

                                      case 1://退出group

                                          //添加一个广播各位朋友们，然后发送消息，让他们更改group member
                                          groupList.remove(place);
                                          groupDevice.remove(place);
                                          groupListNum -=1;
                                          if (groupListNum==0) {
                                              groupList2 = new String[1];
                                              groupList2[0] = "No device!";
                                              groupInformation=new ArrayAdapter<String>(WiFiDirectActivity.this, android.R.layout.simple_expandable_list_item_1,groupList2);
                                              listView.setAdapter(groupInformation);
                                          }
                                          else {
                                          int i=0;
                                          for (Object devicename:groupList){
                                              groupList2[i]=devicename.toString();
                                              i+=1;
                                          }}
                                          break;

                                      case 2://显示group Device的信息
                                          groupDeviceList = new String[k];
                                          for (int i=0;i<k;i++) {
                                              groupDeviceList[i]=temp.get(i).deviceName.toString();
                                          }
                                          AlertDialog.Builder showDevice = new AlertDialog.Builder(WiFiDirectActivity.this);
                                          AlertDialog dialog2 =showDevice.setTitle("Group member device list")//标题栏
                                                  .setItems(
                                                          groupDeviceList,
                                                          new DialogInterface.OnClickListener(){
                                                              @Override
                                                              public void onClick(
                                                                      DialogInterface dialog,int which){
                                                                  }
                                                              })
                                                  .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                                              @Override
                                                              public void onClick(DialogInterface dialog,
                                                                                  int which) {
                                                              }
                                                          }
                                                  ).create();
                                          dialog2.show();
                                  }
                                }
                            })
                    .create();
            dialog.show();

        }
    };

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_direct_enable:
                if (manager != null && channel != null) {

                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.

                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;

            case R.id.atn_direct_discover:
                if (!isWifiP2pEnabled) {
                    Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                        .findFragmentById(R.id.frag_list);
                fragment.onInitiateDiscovery();
                manager.discoverPeers(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            //搞个group  --目前的bug有点再取消还是会在列表中
            case R.id.formGroup:
                wholeDevice=GroupDevice;//wholedevice含当前所有的device的数组
                int len;                //所有扫描到设备的数量
                if (wholeDevice == null) len=0;
                else len=wholeDevice.size();

                dialogName = new String[len]; //选择device的名字的字符串
                device = new ArrayList();//存放被选择的device的名字
                boolean isChecked[] = new boolean[len]; //对数组初始化
                for (int i=0;i<len;i++) {
                    isChecked[i] = false;
                    dialogName[i] = wholeDevice.get(i).deviceName.toString();
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(WiFiDirectActivity.this);
                AlertDialog dialog =builder.setTitle("Group member挑选")//标题栏
                      .setMultiChoiceItems(
                            dialogName,
                    isChecked,
                    new DialogInterface.OnMultiChoiceClickListener(){
                        @Override
                        public void onClick(
                                DialogInterface dialog,int which,boolean isChecked){
                            if (isChecked){
                                device.add(wholeDevice.get(which));//device中加入挑选的设备
                                Log.d(WiFiDirectActivity.TAG,"device size"+device.size());
                             }
                        }
                    })
                        .setPositiveButton("确定",//positiveButton即确定按钮，negativeButton为取消按钮
                                           new DialogInterface.OnClickListener() {//对确定按钮的点击事件进行设置于处理
                            @Override
                            public void onClick(DialogInterface dialog,
                            int which){
                                groupListNum += 1; //group 列表的数目增加
                                groupName="Group"+String.valueOf(groupListNum);
                                groupList.add(groupName);//在列表中加入GroupList
                                groupDevice.add(device);
                               // groupDeviceAddress.add(deviceAddress);
                                groupList2 = new String[groupList.size()];
                                int i= 0;
                                for (Object devicename:groupList){
                                    groupList2[i]=devicename.toString();
                                    i+=1;
                                }

                                listView=(ListView)findViewById(R.id.listView1);//生成一个ListView来显示group名称
                                listView.setOnItemClickListener(listener);//给listView添加按键效果
                                groupInformation=new ArrayAdapter<String>(WiFiDirectActivity.this, android.R.layout.simple_expandable_list_item_1,groupList2);
                                listView.setAdapter(groupInformation);
                                dialog.dismiss();//关闭dialog
                                Toast.makeText(WiFiDirectActivity.this, "确定",
                                        Toast.LENGTH_SHORT).show();
                                //
                                Log.d(WiFiDirectActivity.TAG,"begin");
                                Log.d(WiFiDirectActivity.TAG,"size"+ device.size());
                            }
                            }
                        ).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();
                            }
                        }
                ).create();
                dialog.show();

                //


            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void sendDevice(ArrayList arrayDevice){
        GroupDevice = arrayDevice;
    }


    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);

    }

    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        manager.removeGroup(channel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
            }

        });
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void cancelDisconnect() {

        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        List<Fragment> listOfFragments = getSupportFragmentManager().getFragments();

        if(listOfFragments.size()>=1){
            for (Fragment fragment : listOfFragments) {
                if(fragment instanceof DeviceDetailFragment){
                    fragment.onActivityResult(requestCode, resultCode, data);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.

    }
    private void checkExStoragePermission() {
        boolean isExternalStorage = PermissionsAndroid.getInstance().checkWriteExternalStoragePermission(this);
        if (!isExternalStorage) {
            PermissionsAndroid.getInstance().requestForWriteExternalStoragePermission(this);
        } else {
            pickFilesSingle();
        }
    }
    private void pickFilesSingle() {
        filePicker = new FilePicker(this);
        //filePicker.setFilePickerCallback(this);
        filePicker.setFolderName(this.getString(R.string.app_name));
        filePicker.pickFile();
    }
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFilesSingle();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Utils.getInstance().showToast("Camera/Storage permission Denied");
                }
                return;
            }
        }
    }





}

