package com.zfchen.ecusoftwareupdatetool;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.zfchen.dbhelper.CANDatabaseHelper;
import com.zfchen.dbhelper.CANDatabaseHelper.UpdateStep;
import com.zfchen.uds.ISO14229;
import com.zfchen.uds.ISO15765;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class UpdateActivity extends Activity implements CallbackBundle{
	
	enum UpdateType{
		OnlyApplication,
		OnlyCalibrationData,
		ApplicationAndCalibrationData,
		None
	};
	
	UpdateType selectedUpdateFile;
	
	/*-----�����������-------*/
	static BluetoothSocket BTsocket;
	static OutputStream outStream;
	static InputStream inStream;
	
	static byte[] sendBuffer = new byte[12];
	static byte[] receiveBuffer = new byte[12];
	boolean receiverThreadFlag = false;
	int ProcessStep = 0;
	String manufacturer = null;
	/*--------handler�������ڸ���activity��ʾ--------*/
	HandlerMessage messageHandler = new HandlerMessage();
	int datafileDialogId = 0;
	int appfileDialogId = 1;
	int driverfileDialogId = 2;
	// UI--select file
	CheckBox dataSwitch;
	Button dataButton;
	TextView dataInput;
	CheckBox appSwitch;
	Button appButton;
	TextView appInput;
	CheckBox driverSwitch;
	Button driverButton;
	TextView driverInput;
	Button updateButton;
	ProgressBar progressBar;
	//CAN protocol
	ISO15765 iso15765 = null;
	ISO14229 iso14229 = null;
	UpdateSoftwareProcess updateProcess = null;
	
	public class UpdateSoftwareProcess{
		boolean UpdateSoftwareFunctionState;
		UpdateStep ProcessStep;
		
		public UpdateSoftwareProcess(){
			super();
			UpdateSoftwareFunctionState = false;
			ProcessStep = UpdateStep.ReadECUHardwareNumber;
		}

		public boolean isUpdateSoftwareFunctionState() {
			return UpdateSoftwareFunctionState;
		}

		public void setProcessStep(UpdateStep processStep) {
			ProcessStep = processStep;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.update_software);
		//��ȡUI�ؼ�
		dataSwitch = (CheckBox)findViewById(R.id.Data_switch);
		dataButton = (Button)findViewById(R.id.selectDataButton);
		dataInput = (TextView)findViewById(R.id.Data_Input);
		
		appSwitch = (CheckBox)findViewById(R.id.App_switch);
		appButton = (Button)findViewById(R.id.selectAppButton);
		appInput = (TextView)findViewById(R.id.App_Input);
		
		driverSwitch = (CheckBox)findViewById(R.id.Driver_switch);
		driverButton = (Button)findViewById(R.id.selectDriverButton);
		driverInput = (TextView)findViewById(R.id.Driver_Input);
		
		updateButton = (Button)findViewById(R.id.updateSWButton);
		progressBar = (ProgressBar)findViewById(R.id.progressBar);
		this.Update();
		
		Intent intent = getIntent();
		manufacturer = intent.getStringExtra("manufacturer");
		//System.out.println(manufacturer);
		
		CANDatabaseHelper dbHelper = new CANDatabaseHelper(this, "canId.db3",null, 1);
//		SQLiteDatabase db = dbHelper.getReadableDatabase();
//		dbHelper.generateCanDB(db);
//		dbHelper.generateUpdateDB(db);
		
		iso14229 = new ISO14229(dbHelper, BTsocket, manufacturer);
		updateProcess = new UpdateSoftwareProcess();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	/*-------�������-------*/
	protected void Update(){
		//System.out.println("software update...");
		
		// ���õ����ѡ��ʱ��ʾ/�����ļ�·��
		CompoundButton.OnCheckedChangeListener check_listener = new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				CheckBox box = (CheckBox) buttonView;
				String str = (String) box.getText();
				switch (str) {
				case "�궨����":
					if(true == isChecked){
						dataButton.setEnabled(true);
						dataInput.setVisibility(View.VISIBLE);
						if(selectedUpdateFile == UpdateType.OnlyApplication)
							selectedUpdateFile = UpdateType.ApplicationAndCalibrationData;
						else
							selectedUpdateFile = UpdateType.OnlyCalibrationData;
					} else{
						dataButton.setEnabled(false);
						dataInput.setVisibility(View.INVISIBLE);
						if(selectedUpdateFile == UpdateType.ApplicationAndCalibrationData)
							selectedUpdateFile = UpdateType.OnlyApplication;
						else if(selectedUpdateFile == UpdateType.OnlyCalibrationData)
							selectedUpdateFile = UpdateType.None;
					}
					break;
					
				case "Ӧ���ļ�":
					if(true == isChecked){
						appButton.setEnabled(true);
						appInput.setVisibility(View.VISIBLE);
						if(selectedUpdateFile == UpdateType.OnlyCalibrationData)
							selectedUpdateFile = UpdateType.ApplicationAndCalibrationData;
						else
							selectedUpdateFile = UpdateType.OnlyApplication;
					} else{
						appButton.setEnabled(false);
						appInput.setVisibility(View.INVISIBLE);
						if(selectedUpdateFile == UpdateType.ApplicationAndCalibrationData)
							selectedUpdateFile = UpdateType.OnlyCalibrationData;
						else if(selectedUpdateFile == UpdateType.OnlyApplication)
							selectedUpdateFile = UpdateType.None;
					}			
					break;
					
				case "�����ļ�":
					if(true == isChecked){
						driverButton.setEnabled(true);
						driverInput.setVisibility(View.VISIBLE);
					} else{
						driverButton.setEnabled(false);
						driverInput.setVisibility(View.INVISIBLE);
					}
					break;
					
				default:
					break;
				}
			}
		};
		
		dataSwitch.setOnCheckedChangeListener(check_listener);
		appSwitch.setOnCheckedChangeListener(check_listener);
		driverSwitch.setOnCheckedChangeListener(check_listener);
		
		// ���õ�����ťʱ���ļ��Ի���
        Button.OnClickListener button_listener = new Button.OnClickListener() {
			@SuppressWarnings("deprecation")
			@Override
			public void onClick(View arg0) {
				//showDialog(openfileDialogId);
				Button bt = (Button) arg0;
				String str = (String) bt.getText();
				switch (str) {
					case "ѡ������":
						//dataInput.setText(filepath);
						showDialog(datafileDialogId);
						break;
					
					case "ѡ��Ӧ��":
						//appInput.setText(filepath);
						showDialog(appfileDialogId);
						break;
						
					case "ѡ������":
						//driverInput.setText(filepath);
						showDialog(driverfileDialogId);
						break;
						
					default:
						break;
				}
			}
		};
		
		dataButton.setOnClickListener(button_listener);
		appButton.setOnClickListener(button_listener);
		driverButton.setOnClickListener(button_listener);
		
		updateButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(driverInput.getText() != null)
				{
					String[] filePath = new String[3];
					if(appInput.getText() != null || dataInput.getText() != null){
						filePath[0] = driverInput.getText().toString();	//�����ļ�·��
						filePath[1] = appInput.getText().toString();	//Ӧ���ļ�·��
						filePath[2] = dataInput.getText().toString();	//�����ļ�·��
						System.out.println("Update ECU software ...");
						if(updateProcess.UpdateSoftwareFunctionState == false){
							//����ѡ���������, ��ʼ����
							iso14229.setFilePath(filePath);
							boolean result = iso14229.update(manufacturer, filePath, updateProcess);
							if(result){
								Toast.makeText(getApplicationContext(), "�������", Toast.LENGTH_LONG).show();
							}
						}else{
							updateProcess.UpdateSoftwareFunctionState = true;
							Toast.makeText(getApplicationContext(), "�������������Ե�", Toast.LENGTH_LONG).show();
						}
					}
				}else {
					Toast.makeText(getApplicationContext(), "����ѡ�������ļ�", Toast.LENGTH_LONG).show();
				}
			}
			
		});
	}
	
	
	@Override
	public void callback(Bundle bundle) {
		// TODO Auto-generated method stub
		TextView dataInput = (TextView)findViewById(R.id.Data_Input);
		TextView appInput = (TextView)findViewById(R.id.App_Input);
		TextView driverInput = (TextView)findViewById(R.id.Driver_Input);
		
		if(bundle.getInt("id") == datafileDialogId)
			dataInput.setText(bundle.getString("path"));
		else if(bundle.getInt("id") == appfileDialogId)
			appInput.setText(bundle.getString("path"));
		else
			driverInput.setText(bundle.getString("path"));
	}

	
	protected Dialog onCreateDialog(int id) {
		if((id==datafileDialogId)||(id==appfileDialogId)||(id==driverfileDialogId)){
			Map<String, Integer> images = new HashMap<String, Integer>();
			// ���漸�����ø��ļ����͵�ͼ�꣬ ��Ҫ���Ȱ�ͼ����ӵ���Դ�ļ���
			images.put(OpenFileDialog.sRoot, R.drawable.filedialog_root);	// ��Ŀ¼ͼ��
			images.put(OpenFileDialog.sParent, R.drawable.filedialog_folder_up);	//������һ���ͼ��
			images.put(OpenFileDialog.sFolder, R.drawable.filedialog_folder);	//�ļ���ͼ��
			images.put("hex", R.drawable.hex);	//hex�ļ�ͼ��
			images.put(OpenFileDialog.sEmpty, R.drawable.filedialog_root);
			Dialog dialog = OpenFileDialog.createDialog(id, this, "ѡ���ļ�", this, ".hex;", images);
			return dialog;
		}
		return null;
	}
	
	
	
	void SendMessageToHandler(Object obj){
    	Message msg = messageHandler.obtainMessage();
    	msg.what = 123;
    	msg.obj = obj;
		msg.sendToTarget();
    }
	
	/*----------handler���ڴ�������������ʾ-------------*/
	class HandlerMessage extends Handler{
		public HandlerMessage(){
			
		}

		@Override
		public void handleMessage(Message msg) {
			if(msg.what == 123){
				int progress = 0;
				UpdateStep step = (UpdateStep)msg.obj;
				if(step == UpdateStep.ReadBootloaderID)
					progress = 10;
				else if(step == UpdateStep.WriteConfigureDate)
					progress = 30;
				else if(step == UpdateStep.TransferData)
					progress = 50;
				else if(step == UpdateStep.CheckProgrammDependency)
					progress = 70;
				else if(step == UpdateStep.RequestToDefaultSession)
					progress = 100;
				
				progressBar.setProgress(progress);
			}
		}	
	}
}

