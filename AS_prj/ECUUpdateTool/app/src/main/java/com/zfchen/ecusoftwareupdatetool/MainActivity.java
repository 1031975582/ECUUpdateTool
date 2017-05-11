package com.zfchen.ecusoftwareupdatetool;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	/*---�ؼ�����----*/
	private Button diagFunction = null;
	private Button ConnectDevice = null;
	private Button auxilaryFunction = null;
	private Button extendFunction = null;
	private TextView leftTitle = null;
	private TextView rightTitle = null;
	/*---������������---*/
	private static final int REQUEST_CONNECT_DEVICE = 0;
	private static final int REQUEST_ENABLE_BT = 1;
	public static boolean ConnectState = false;
	/*-----�����������-----*/
	private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //�������ڷ���
	public static String ServerBluetoothMAC = null;
	/* BluetoothAdapter: ��������������豸 */
	public static BluetoothAdapter adapter = null;
	public static BluetoothDevice device = null;
	
	/* �ͻ���ʹ��BluetoothSocket����������������  */
	public static BluetoothSocket socket = null;
	
	private boolean connectThreadFlag = false;
	BluetoothReceiver BluetoothStateDetect = null;
	/*----handlerMessage-----*/
	myHandler messageHandler = null;
	private static final int connectSuccess = 0;
	private static final int connectFail = 1;
	private static final int connectGiveUp = 2;
	/*-------���������߳�-------*/
	ConnectThread connectThread = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/* ���ô��ڵ���չ���ԣ� FEATURE_CUSTOM_TITLE���Զ������ */
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);	
		setContentView(R.layout.activity_main); /* ����view�����ļ� */
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.custom_title);  //�����Զ�����Ⲽ��
		/* �õ��ؼ�����(setContentView����Ҫ����findviewbyid֮ǰ����Ϊview�ڼ���֮ǰ���޷����õ�) */
		diagFunction = (Button) findViewById(R.id.diagFunction);	//��Ϲ��ܰ�ť
		diagFunction.getBackground().setAlpha(0);
		
		ConnectDevice = (Button) findViewById(R.id.connectDevice);	//�����豸��ť
		ConnectDevice.getBackground().setAlpha(0);
		
		auxilaryFunction = (Button) findViewById(R.id.auxilaryFunction);	//�������ܰ�ť
		auxilaryFunction.getBackground().setAlpha(0);
		
		extendFunction = (Button) findViewById(R.id.extendFunction);	//��չ���ܰ�ť
		extendFunction.getBackground().setAlpha(0);
		
		/* ���ñ���  */
		leftTitle = (TextView) findViewById(R.id.title_left_text);
		rightTitle = (TextView) findViewById(R.id.title_right_text);
		leftTitle.setText(R.string.title_activity_main); //���ñ���Ϊ������ECU��������ǡ�
		rightTitle.setText("�豸δ����");
		
		diagFunction.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					diagFunction.getBackground().setAlpha(255);
					break;
				case MotionEvent.ACTION_UP:
					v.performClick();	//�������¼�
					diagFunction.getBackground().setAlpha(0);
					Intent intent = new Intent();
					//intent.putExtra("filename", "��ϵ.txt");
					//intent.setClass(MainActivity.this, CarSeriesSelectActivity.class);
					intent.setClass(MainActivity.this, ManufacturerActivity.class);
					MainActivity.this.startActivity(intent);  //��ת��������ѡ��Activity
					break;
				}
				return false;
			}
		});
		
		ConnectDevice.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					ConnectDevice.getBackground().setAlpha(255);
					break;
				case MotionEvent.ACTION_UP:
					v.performClick();	//�������¼�
					ConnectDevice.getBackground().setAlpha(0);
					Intent serverIntent = new Intent(MainActivity.this,
							DeviceListActivity.class);
					
					/* startActivityForResult: ��ָ��������������Intentƥ���Activity�����ҳ��򽫻�ȵ�������Activity�Ľ�� */
					startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
					break;
				}
				return false;
			}
		});
		
		auxilaryFunction.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					auxilaryFunction.getBackground().setAlpha(255);
					break;
				case MotionEvent.ACTION_UP:
					v.performClick();	//�������¼�
					auxilaryFunction.getBackground().setAlpha(0);
					break;
				}
				return false;
			}
		});
		
		extendFunction.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					extendFunction.getBackground().setAlpha(255);
					break;
				case MotionEvent.ACTION_UP:
					v.performClick();	//�������¼�
					extendFunction.getBackground().setAlpha(0);
					break;
				}
				return false;
			}
		});
		
		ShowTips();
		/*----------*/
		IntentFilter filter = new IntentFilter(
				BluetoothDevice.ACTION_ACL_DISCONNECTED);
		BluetoothStateDetect = new BluetoothReceiver();
		registerReceiver(BluetoothStateDetect, filter);
		
	}

	public class BluetoothReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				Toast.makeText(getApplicationContext(), "�������ӶϿ��ˣ�",
						Toast.LENGTH_LONG).show();
				rightTitle.setText("���ӶϿ���");
				Intent myIntent = new Intent();
				myIntent.setClass(MainActivity.this, DisconnectWarningActivity.class);
				startActivity(myIntent);
			}
		}

	}

	public int getResourceId(String name) {
		try {
			Field field = R.drawable.class.getField(name);
			return Integer.parseInt(field.get(null).toString());
		} catch (Exception e) {

		}
		return 0;
	}

	void ShowTips() {
		String html = "<img src='car_tips'/>  ���ʹ��С��ʾ������ѡ�������豸�������������ӳɹ��󣬼��ɽ��롱������������ܣ�";
		CharSequence ch = Html.fromHtml(html, new ImageGetter() {

			@Override
			public Drawable getDrawable(String source) {
				// TODO Auto-generated method stub
				Drawable drawable = getResources().getDrawable(
						getResourceId(source));
				drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
						drawable.getIntrinsicHeight());
				return drawable;
			}
		}, null);	//fromHtml()���÷���ͼƬ�������ʾ�ģ���
		TextView tips = (TextView) findViewById(R.id.start_tips);
		tips.setText(ch);
		html = "<img src='adayo_logo'/>";
		ch = Html.fromHtml(html, new ImageGetter() {

			@Override
			public Drawable getDrawable(String source) {
				// TODO Auto-generated method stub
				Drawable drawable = getResources().getDrawable(
						getResourceId(source));
				drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
						drawable.getIntrinsicHeight());
				return drawable;
			}
		}, null);
		TextView logo = (TextView) findViewById(R.id.logo);
		logo.setText(ch);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (DeviceListActivity.mBtAdapter != null) {
			DeviceListActivity.mBtAdapter.disable();
		}
		if (connectThread != null) {
			connectThread = null;
			try {
				socket.close();

			} catch (IOException i) {
				i.printStackTrace();
			}
		}
		connectThreadFlag = false;
		unregisterReceiver(BluetoothStateDetect);
	}
	
	public class StateCheckThread extends Thread {

		StateCheckThread() {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();

		}

	}

	public class ConnectThread extends Thread {
		public ConnectThread() {

			try {
				adapter = DeviceListActivity.mBtAdapter;
				device = adapter.getRemoteDevice(ServerBluetoothMAC);// ��������������ַ
				socket = device.createRfcommSocketToServiceRecord(uuid);
				connectThreadFlag = true;
			} catch (IOException e) {
				e.printStackTrace();
				connectThreadFlag = false;
			}
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			int tryCount = 0;
			while (connectThreadFlag) {
				try {
					// System.out.println("���������������ͨѶ");
					socket.connect();
					connectThreadFlag = false;
					ConnectState = true;
					// System.out.println("�������ӳɹ�");
					/*
					 * ParcelUuid temp[]; int len = device.getUuids().length;
					 * temp = device.getUuids(); for(int i = 0;i < len;i++){
					 * System.out.println(temp[i].getUuid()); }
					 */
					SendMessage(connectSuccess);
				} catch (IOException i) {
					i.printStackTrace();
					// System.out.println("��������ʧ��");
					ConnectState = false;
					tryCount++;
					if (tryCount < 10) {
						SendMessage(connectFail);
					} else {
						// System.out.println("������������");
						connectThreadFlag = false;
						SendMessage(connectGiveUp);
					}
					try {
						Thread.sleep(2000);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	void SendMessage(int arg) {
		Message msg = messageHandler.obtainMessage();
		msg.what = arg;
		msg.sendToTarget();
	}

	class myHandler extends Handler {
		public myHandler() {

		}

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			String content = null;
			switch (msg.what) {
			case connectSuccess:
				content = "���ӳɹ�";
				rightTitle.setText("���ӳɹ�");
				break;
			case connectFail:
				content = "����ʧ�ܣ��ٴγ�������";
				rightTitle.setText("����ʧ�ܣ��ٴγ�������");
				break;
			case connectGiveUp:
				content = "�������ӳ�ʱ����������";
				rightTitle.setText("�������ӳ�ʱ����������");
				break;
			default:

				break;
			}
			Toast.makeText(MainActivity.this, content, Toast.LENGTH_SHORT).show();
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			if (resultCode == Activity.RESULT_OK) {
				if (connectThread != null) {
					connectThread = null;
					try {
						socket.close();
					} catch (IOException i) {
						i.printStackTrace();
					}
				}
				connectThreadFlag = false;

				ServerBluetoothMAC = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				connectThread = new ConnectThread();
				connectThread.start();
				/*---������Ϣ�������--*/
				messageHandler = new myHandler();

			} else {
				Toast.makeText(this, "�û�δѡ������豸,������ѡ��", Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
