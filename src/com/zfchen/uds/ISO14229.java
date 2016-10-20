package com.zfchen.uds;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.bluetooth.BluetoothSocket;
import android.database.sqlite.SQLiteDatabase;

import com.zfchen.uds.ISO15765;
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
		OutputStream outStream = null;
		ArrayList<Byte> message = canDBHelper.getCANMessage(step);
		
		try{
			outStream = this.socket.getOutputStream();
		}catch(IOException e){
			e.printStackTrace();
		}
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
		//iso15765.new SendThread(socket, message).start();
		
		iso15765.PackCANFrameData(message, iso15765.frameBuffer, request_can_id);
		int num = iso15765.frameBuffer.getFrame().size();
		for(int i=0; i<num; i++){
			for(int j=0; j<12; j++){
				int a = (int)(iso15765.frameBuffer.getFrame().get(i).data[j]&0xFF);
				System.out.printf("%2h ", a);
			}
			System.out.println();
			
			try {
				outStream.write(iso15765.frameBuffer.getFrame().get(i).data);
				Thread.sleep(1);
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//iso15765.PackCANFrameData(message, iso15765.frameBuffer, request_can_id);
		//iso15765.new SendThread(socket, message).start();
		//���Ϳ�����
		return positiveCode;
	}
	
	public boolean update(String manufac, String[] filePathList, UpdateSoftwareProcess step){
		//byte positiveResponse;
		boolean result = false;
		UpdateProcess updateProcess;
		switch (manufac) {
		case "zotye":
		case "baic":
		case "dfsk":	//Ŀǰ zotye,baic��dfsk���ߵ���������һ��
			updateProcess = new ZotyeUpdateProcess(this, filePathList);
			updateProcess.update();
			break;
					
		case "geely":
			updateProcess = new GeelyUpdateProcess(this, filePathList);
			updateProcess.update();
			break;
			
		default:
			System.out.println("The manufacturer isn't supported!");
			break;
		}
		
		result = true;
		return result;
	}
	
	protected void addParameter(UpdateStep step, String manufac, String filePath, ArrayList<Byte> frame){

		byte[] check = null;
		if((path != filePath) && (filePath!=null)){
			h2b.getFileSize(filePath);
			System.out.println(filePath);
			fileSize = h2b.getSize();
			System.out.println(fileSize);
			startAddress = h2b.getStarting_address();	//��ʼ��ַ
			System.out.println(startAddress);
			this.data = h2b.getHexFileData(filePath);	//hex�ļ������ݲ���
			this.path = filePath;
		}
		
		if(step == UpdateStep.CheckSum){
			byte[] hex_data = new byte[this.data.size()];
			for (int i = 0; i < this.data.size(); i++) {
				hex_data[i] = this.data.get(i);
			}
			switch (manufac) {	//У����(��ͬ���̹涨��У�鷽ʽ��ͬ)
			case "zotye":
				crc = new Crc(0x4c11db7, 32, false, false);
				break;
			
			case "dfsk":
				crc = new Crc(0x4c11db7, 32, false, false);	//DFSK��У�鷽ʽ�д�ȷ��	
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
			
			check = crc.CRC32(hex_data, hex_data.length);  //ʹ��CRCУ�鷽������ת�������������bin�ļ�������У�� 
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
	
	
	/** ����������������⣬����Ҫ�������(��ʼ��Ŵ�1��ʼ��֮���0��Fѭ������,ÿ����һ�����ݿ���ȴ�Ŀ��ECU�Ļ�����Ӧ)
	 * @param maxBlockLength �������ݿ鳤��
	 * @param transferData ����������ݣ�����ǰ�����ֽ�Ϊ36 01����SID��sn��
	 * @param CANFrame ���ͻ�����������֡��
	 * @return
	 */
	protected void transferFile(int maxBlockLength, ArrayList<Byte> transferData, BluetoothSocket socket, int can_id){
		OutputStream outStream = null;
		//SendThread sendThread = iso15765.new SendThread(socket, null);
		ArrayList<Byte> blockFrame = new ArrayList<Byte>();
		blockFrame.ensureCapacity(maxBlockLength);
		int dataLength = transferData.size();	//ԭʼ���ݳ���
		
		byte SID = 0x36; //��������ֽڣ�SID(���ݴ����ID��)��sn(���)
		byte blockNumInitial = 1;
		int blockNum = dataLength/(maxBlockLength-2);  //���鳤��-2����Ϊÿ�����ǰ�����ֽڷֱ�Ϊ��SID��sn���������ID�Ϳ���ţ�������Ĳ���ԭʼ����
		
		for (int i = 0; i < blockNum; i++) {
			blockFrame.clear();
			blockFrame.add(SID);	//�����Ϸ���ID
			blockFrame.add(blockNumInitial++); //��ӿ�ţ����ѭ��:��һ���Ǵ�1��ʼ������������FF���ٴ�0��ʼѭ���������������byte���ͣ�������Զ���Ϊ0��
			
			for(int j=0; j<(maxBlockLength-2); j++){	//ȡ��һ�����ݿ������
				blockFrame.add(transferData.get(j+(i*(maxBlockLength-2))));
			}
			/*
			iso15765.setSendData(blockFrame);
			if(sendThread.getState() == Thread.State.NEW)
				sendThread.start();
			else if(sendThread.getState() == Thread.State.WAITING)
				sendThread.notifyAll();
			*/
			
			//���Ϳ�����
			iso15765.PackCANFrameData(blockFrame, iso15765.frameBuffer, request_can_id);
			try{
				outStream = this.socket.getOutputStream();
			}catch(IOException e){
				e.printStackTrace();
			}
			int num = iso15765.frameBuffer.getFrame().size();
			for(int k=0; k<num; k++){
				for(int m=0; m<12; m++){
					int a = (int)(iso15765.frameBuffer.getFrame().get(k).data[m]&0xFF);
					System.out.printf("%2h ", a);
				}
				System.out.println();
				
				//������д�����С�����������
				try {
					outStream.write(iso15765.frameBuffer.getFrame().get(k).data);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		int lastBlockLength = dataLength%(maxBlockLength-2); //�����һ�����ݿ��������ȡ����(���һ�����ݿ鳤�Ȳ���maxBlockLength)
		if(0 != lastBlockLength){
			blockFrame.clear();
			blockFrame.add(SID);
			blockFrame.add(blockNumInitial++);
			for(int i=0; i<lastBlockLength; i++){
				blockFrame.add(transferData.get(dataLength-lastBlockLength+i));
			}
			
			/*
			iso15765.setSendData(blockFrame);
			if(sendThread.getState() == Thread.State.NEW)
				sendThread.start();
			else if(sendThread.getState() == Thread.State.WAITING)
				sendThread.notifyAll();
			
			sendThread.setFlag(false);
			*/
			
			//iso15765.new SendThread(socket, blockFrame).start();
			iso15765.PackCANFrameData(blockFrame, iso15765.frameBuffer, request_can_id);
			int num = iso15765.frameBuffer.getFrame().size();
			for(int k=0; k<num; k++){
				for(int m=0; m<12; m++){
					int a = (int)(iso15765.frameBuffer.getFrame().get(k).data[m]&0xFF);
					System.out.printf("%2h ", a);
				}
				System.out.println();
				
				//������д�����С�����������
				try {
					outStream.write(iso15765.frameBuffer.getFrame().get(k).data);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
	}
	
	
	/*
	public class SendThread extends Thread{
		BluetoothSocket socket;
		OutputStream outStream;
		InputStream inStream;
		ArrayList<Byte> receiveData;
		ArrayList<Byte> sendData;
		CANFrameBuffer buf;
		boolean flag = true;
		
		String[] filePathList;
		UpdateSoftwareProcess step;
		
		public SendThread(BluetoothSocket socket, ArrayList<Byte> output, String[] filePath) {
			super();
			// TODO Auto-generated constructor stub
			this.socket = socket;
			this.sendData = output;
			this.filePathList = filePath;
			
			try{
				this.outStream = socket.getOutputStream();
				this.inStream  = socket.getInputStream();
			}catch(IOException e){
				e.printStackTrace();
			}
			
		}

		public void setFlag(boolean flag) {
			this.flag = flag;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			while(flag == true){
				buf = iso15765.new CANFrameBuffer();
				iso15765.PackCANFrameData(sendData, buf, request_can_id);
				
				byte[] tempBuffer = new byte[12];
				int num = buf.getFrame().size();
				
				try{
					outStream.write(buf.getFrame().get(0).data);	// ���͵�һ֡/��֡
					num += inStream.read(tempBuffer); //ÿ�ζ�ȡ���12���ֽ�, ����ʵ�ʶ�ȡ�����ֽ��� 
					if(num < 12)
						id = (int)((tempBuffer[1]<<8)|tempBuffer[2]);
					
					if(num > 1){	//���ڶ�֡����,�����ڵ�һ֮֡����Ҫ�ȴ�����������֡
						//wait();
						for(int i=1; i<=num-1; i++){
							outStream.write(buf.getFrame().get(i).data);
							sleep(1);	//sleep 1 ms
						}
					}
	
				}catch(IOException | InterruptedException e){
					e.printStackTrace();
				}

				for(int j=0; j<num; j++){
					for(int i=0; i<12; i++){
						int a = (int)(buf.getFrame().get(j).data[i]&0xFF);
						System.out.printf("%2h ", a);
					}
					System.out.println();
				}
				try {
					wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	*/
	
}
