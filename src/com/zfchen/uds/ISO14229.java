package com.zfchen.uds;

import java.util.ArrayList;

import android.bluetooth.BluetoothSocket;
import android.database.sqlite.SQLiteDatabase;

import com.zfchen.uds.ISO15765;
import com.zfchen.uds.ISO15765.CANFrameBuffer;
import com.zfchen.uds.ISO15765.SendThread;

import com.zfchen.dbhelper.CANDatabaseHelper;
import com.zfchen.dbhelper.CANDatabaseHelper.*;
import com.zfchen.ecusoftwareupdatetool.Hex2Bin;
import com.zfchen.ecusoftwareupdatetool.Crc;
import com.zfchen.ecusoftwareupdatetool.UpdateActivity.UpdateSoftwareProcess;

public class ISO14229 {
	
	final int blockLength = 514;	/*ָ��ÿ�δ����������ݿ鳤��*/
	int response_can_id = 0;
	int request_can_id = 0;
	SQLiteDatabase db = null;
	byte[] CAN_ID = new byte[2];
	CANDatabaseHelper canDBHelper = null;
	ArrayList<Byte> frame = null;
	ISO15765 iso15765 = null;
	Hex2Bin h2b = null;
	Crc crc = null;
	BluetoothSocket socket;
	ArrayList<Byte> data;
	String manufacturer;
	String[] filePath;
	String path;
	byte[] fileSize;
	byte[] startAddress;
	public ISO14229(CANDatabaseHelper helper, BluetoothSocket bTsocket, String manufacturer){
		super();
		this.db = helper.getReadableDatabase();
		helper.generateCanDB(db);
		helper.generateUpdateDB(db);
		
		this.canDBHelper = helper;
		this.frame = new ArrayList<Byte>();
		this.h2b = new Hex2Bin();
		this.socket = bTsocket;
		this.manufacturer = manufacturer;
	}
	
	
	public void setFilePath(String[] filePath) {
		this.filePath = filePath;
	}


	/**
	 * @param step ��������
	 * @param manufac ������
	 * @param filePath �����ļ���·��
	 * @return
	 */
	protected byte requestDiagService(UpdateStep step, String manufac, String filePath){	//ReadECUHardwareNumber
		byte positiveCode = 0;	/* ���ظñ��Ķ�Ӧ�Ļ�����Ӧ��  */
		
		ArrayList<Byte> message = canDBHelper.getCANMessage(step);
		/*
		for (Byte b : al) {
			System.out.printf("%2h\n",b);
		}
		*/
		positiveCode = message.get(message.size()-1);	//ȡ��������Ӧ��
		message.remove(message.size()-1);

		ArrayList<Integer> canIDList = canDBHelper.getCANID(db, manufac);
		
		if(step == UpdateStep.RequestToExtendSession || step == UpdateStep.RequestToDefaultSession
				|| step == UpdateStep.DisableDTCStorage || step == UpdateStep.EnableDTCStorage
				|| step == UpdateStep.DisableNonDiagComm || step == UpdateStep.EnableNonDiagComm){ //�⼸����Ϸ����ǹ���Ѱַ
			request_can_id = canIDList.get(1); //funcCANid
		} else{
			request_can_id = canIDList.get(0); //phyCANid
		}
		response_can_id = canIDList.get(2); //respCANid, �������˴ӳ���CAN������յ��ı���(���ǽ��ò���������λ����Ȼ������λ���Ա��Ľ��й���)
		
		if(step == UpdateStep.RequestDownload || step == UpdateStep.CheckSum || step == UpdateStep.EraseMemory){
			//���ڣ��������ء��������ݡ�У�����ݡ������߼��飬 �⼸��������Ҫ������Ĳ���
			this.addParameter(step, manufac, filePath, message);
		} else if(step == UpdateStep.TransferData){
			//this.data.addAll(0, message);
			//message.addAll(this.data);
			System.out.println("the length of file transfered is " + this.data.size());
			this.transferFile(514, this.data, this.socket, request_can_id);
			return positiveCode;
		}
		
		this.iso15765 = new ISO15765(this.socket, request_can_id, response_can_id);
		//���Ϳ�����
		//sendThread = iso15765.new SendThread(socket, message);
		//sendThread.start();
		/*
		iso15765.PackCANFrameData(message, iso15765.frameBuffer, request_can_id);
		int num = iso15765.frameBuffer.getFrame().size();
		for(int i=0; i<num; i++){
			for(int j=0; j<12; j++){
				int a = (int)(iso15765.frameBuffer.getFrame().get(i).data[j]&0xFF);
				System.out.printf("%2h ", a);
			}
			System.out.println();
		}
		*/
		//iso15765.PackCANFrameData(message, iso15765.frameBuffer, request_can_id);
		SendThread sendThread = iso15765.new SendThread(socket, message);
		//���Ϳ�����
		sendThread.start();
		return positiveCode;
	}
	
	public boolean update(String manufac, String[] filePathList, UpdateSoftwareProcess step){
		byte positiveResponse;
		boolean result = false;
		
		step.setProcessStep(UpdateStep.ReadECUHardwareNumber);
		positiveResponse = requestDiagService(UpdateStep.ReadECUHardwareNumber, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
		
		step.setProcessStep(UpdateStep.ReadBootloaderID);
		positiveResponse = requestDiagService(UpdateStep.ReadBootloaderID, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
		
		step.setProcessStep(UpdateStep.RequestToExtendSession);
		positiveResponse = requestDiagService(UpdateStep.RequestToExtendSession, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
		
		step.setProcessStep(UpdateStep.DisableDTCStorage);
		positiveResponse = requestDiagService(UpdateStep.DisableDTCStorage, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.DisableNonDiagComm);
		positiveResponse = requestDiagService(UpdateStep.DisableNonDiagComm, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.RequestToProgrammingSession);
		positiveResponse = requestDiagService(UpdateStep.RequestToProgrammingSession, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.RequestSeed);
		positiveResponse = requestDiagService(UpdateStep.RequestSeed, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.SendKey);
		positiveResponse = requestDiagService(UpdateStep.SendKey, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.WriteTesterSerialNumber);
		positiveResponse = requestDiagService(UpdateStep.WriteTesterSerialNumber, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.WriteConfigureDate);
		positiveResponse = requestDiagService(UpdateStep.WriteConfigureDate, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
//		//����flash driver�ļ�
		step.setProcessStep(UpdateStep.RequestDownload);
		positiveResponse = requestDiagService(UpdateStep.RequestDownload, manufacturer, filePathList[0]);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.TransferData);
		positiveResponse = requestDiagService(UpdateStep.TransferData, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.TransferExit);
		positiveResponse = requestDiagService(UpdateStep.TransferExit, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.CheckSum);
		positiveResponse = requestDiagService(UpdateStep.CheckSum, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.EraseMemory);
		positiveResponse = requestDiagService(UpdateStep.EraseMemory, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
//		//����application��calibration data�ļ�(�ظ��������ع���)
		if(filePathList[1] != null){
			positiveResponse = requestDiagService(UpdateStep.RequestDownload, manufacturer, filePathList[1]);//application
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//			
			positiveResponse = requestDiagService(UpdateStep.TransferData, manufacturer, null);
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//			
			positiveResponse = requestDiagService(UpdateStep.TransferExit, manufacturer, null);
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//			
			positiveResponse = requestDiagService(UpdateStep.CheckSum, manufacturer, null);
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
		}
		
		if(filePathList[2] != null){
			positiveResponse = requestDiagService(UpdateStep.RequestDownload, manufacturer, filePathList[2]);//calibration data
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//			
			positiveResponse = requestDiagService(UpdateStep.TransferData, manufacturer, null);
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//			
			positiveResponse = requestDiagService(UpdateStep.TransferExit, manufacturer, null);
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//			
			positiveResponse = requestDiagService(UpdateStep.CheckSum, manufacturer, null);
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
		}
//		
		step.setProcessStep(UpdateStep.CheckProgrammDependency);
		positiveResponse = requestDiagService(UpdateStep.CheckProgrammDependency, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.ResetECU);
		positiveResponse = requestDiagService(UpdateStep.ResetECU, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.RequestToExtendSession);
		positiveResponse = requestDiagService(UpdateStep.RequestToExtendSession, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.EnableNonDiagComm);
		positiveResponse = requestDiagService(UpdateStep.EnableNonDiagComm, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.EnableDTCStorage);
		positiveResponse = requestDiagService(UpdateStep.EnableDTCStorage, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.RequestToDefaultSession);
		positiveResponse = requestDiagService(UpdateStep.RequestToDefaultSession, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
		
		result = true;
		return result;
	}
	
	protected void addParameter(UpdateStep step, String manufac, String filePath, ArrayList<Byte> frame){
		//int dataLength = h2b.getFileSize(filePath);	//���ݳ���
		
		byte[] check = null;
		
		if((path != filePath) && (filePath!=null)){
			h2b.getFileSize(filePath);
			System.out.println(filePath);
			fileSize = h2b.getSize();
			System.out.println(fileSize);
			startAddress = h2b.getStarting_address();	//��ʼ��ַ
			System.out.println(startAddress);
			this.data = h2b.getHexFileData(filePath); //hex�ļ������ݲ���
			this.path = filePath;
		}
		
		if(step == UpdateStep.CheckSum){
			byte[] hex_data = new byte[this.data.size()];
			for (int i = 0; i < this.data.size(); i++) {
				hex_data[i] = this.data.get(i);
			}
			switch (manufac) {//У����(��ͬ���̹涨��У�鷽ʽ��ͬ)
			case "zotye":
				crc = new Crc(0x4c11db7, 32, false, false);
				break;
			
			case "dfsk":
				crc = new Crc(0x4c11db7, 32, false, false); //DFSK��У�鷽ʽ�д�ȷ��	
				break;
						
			case "geely":
				crc = new Crc(0x4c11db7, 32, true, true);
				break;
				
			case "baic":
				crc = new Crc(0x4c11db7, 32, false, false); //BAIC��У�鷽ʽ�д�ȷ��	
				break;	
				
			default:
				System.out.println("The manufacturer isn't supported!");
				break;
			}
			
			check = crc.CRC32(hex_data, hex_data.length);  //ʹ��CrcУ�鷽������ת�������������bin�ļ�������У�� 
		}
		
		switch(step){//���ݲ�ͬ����Ϸ��񣬼����Ӧ�Ĳ���
		case RequestDownload:
			for (byte b : startAddress) {	//add starting address
				frame.add(b);
			}
			for (byte b : fileSize) {	//add data length
				frame.add(b);
			}
				break;
		/*		
		case TransferData:
			//����������������⣬����Ҫ�������(��ʼ��Ŵ�1��ʼ��֮���0��Fѭ������,ÿ����һ�����ݿ���ȴ�Ŀ��ECU�Ļ�����Ӧ)
			
				break;*/
				
		case CheckSum:
			for (byte b : check) {	//add CRC result
				frame.add(b);
			}
				break;
				
		case EraseMemory:
			for (byte b : startAddress) {	//add starting address
				frame.add(b);
			}
			for (byte b : fileSize) {	//add data length
				frame.add(b);
			}
				break;
				
		default:
				break;
		}
		
	}
	
	
	/**
	 * @param maxBlockLength �������ݿ鳤��
	 * @param transferData ����������ݣ�����ǰ�����ֽ�Ϊ36 01����SID��sn��
	 * @param CANFrame ���ͻ�����������֡��
	 * @return
	 */
	protected void transferFile(int maxBlockLength, ArrayList<Byte> transferData, BluetoothSocket socket, int can_id){
		
		//SendThread sendThread = iso15765.new SendThread(socket, transferData);
		SendThread sendThread = iso15765.new SendThread(socket, null);
		ArrayList<Byte> blockFrame = new ArrayList<Byte>();
		blockFrame.ensureCapacity(maxBlockLength);
		int dataLength = transferData.size();	//ԭʼ���ݳ���
		//byte SID = transferData.get(0);
		//byte blockNumInitial = transferData.get(1);	//��ŵĳ�ʼֵ
		byte SID = 0x36; //��������ֽڣ�SID��sn
		byte blockNumInitial = 1;
		int blockNum = dataLength/(maxBlockLength-2);  //���鳤��-2����Ϊÿ�����ǰ�����ֽڷֱ�Ϊ��SID��sn���������ID�Ϳ���ţ�������Ĳ���ԭʼ���ݡ�
		
		for (int i = 0; i < blockNum; i++) {
			blockFrame.clear();
			blockFrame.add(SID);	//�����Ϸ���ID
			blockFrame.add(blockNumInitial++); //��ӿ�ţ����ѭ��:��һ���Ǵ�1��ʼ������������FF���ٴ�0��ʼѭ���������������byte���ͣ�������Զ���Ϊ0��
			
			for(int j=0; j<(maxBlockLength-2); j++){	//ȡ��һ�����ݿ������
				blockFrame.add(transferData.get(j+(i*(maxBlockLength-2))));
				//iso15765.PackCANFrameData(blockFrame, iso15765.frameBuffer, can_id);
				//���Ϳ�����
				//sendThread = iso15765.new SendThread(socket, blockFrame);
			}
			/*
			iso15765.setSendData(blockFrame);
			sendThread.start();
			*/
			iso15765.PackCANFrameData(blockFrame, iso15765.frameBuffer, request_can_id);
			int num = iso15765.frameBuffer.getFrame().size();
			for(int k=0; k<num; k++){
				for(int m=0; m<12; m++){
					int a = (int)(iso15765.frameBuffer.getFrame().get(k).data[m]&0xFF);
					System.out.printf("%2h ", a);
				}
				System.out.println();
			}
		}
		
		int lastBlockLength = dataLength%(maxBlockLength-2); //�����һ�����ݿ��������ȡ����(���һ�����ݿ鳤�Ȳ���maxBlockLength)
		if(0 != lastBlockLength){
			blockFrame.clear();
			blockFrame.add(SID);
			blockFrame.add(blockNumInitial++);
			for(int i=0; i<lastBlockLength; i++){
				blockFrame.add(transferData.get(dataLength-lastBlockLength+i));
				//iso15765.PackCANFrameData(blockFrame, iso15765.frameBuffer, can_id);
				//���Ϳ�����
				//sendThread = iso15765.new SendThread(socket, blockFrame);
				
				/*
				iso15765.setSendData(blockFrame);
				sendThread.start();
				*/
			}
			
			iso15765.PackCANFrameData(blockFrame, iso15765.frameBuffer, request_can_id);
			int num = iso15765.frameBuffer.getFrame().size();
			for(int k=0; k<num; k++){
				for(int m=0; m<12; m++){
					int a = (int)(iso15765.frameBuffer.getFrame().get(k).data[m]&0xFF);
					System.out.printf("%2h ", a);
				}
				System.out.println();
			}
		}
	}
	
}
