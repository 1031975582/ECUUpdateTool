package com.zfchen.dbhelper;

import java.util.ArrayList;
import java.util.EnumMap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class CANDatabaseHelper extends SQLiteOpenHelper {
	
	public enum UpdateStep{ /*java��ö�����Ͷ����﷨��C�е���Щ����, ���һ����Ա������һ��';'��*/
		ReadECUSparePartNumber,
		ReadECUHardwareNumber,
		ReadBootloaderID,
		RequestToExtendSession,
		DisableDTCStorage,
		DisableNonDiagComm,
		RequestToProgrammingSession,
		RequestSeed,
		SendKey,
		WriteTesterSerialNumber,
		WriteECUSparePartNumber,
		WriteConfigureData,
		WriteUpdateDate,
		RequestDownload,
		TransferData,
		TransferExit,
		CheckSum,
		EraseMemory,
		CheckProgrammDependency,
		CheckProgrammCondition,
		ResetECU,
		EnableNonDiagComm,
		EnableDTCStorage,
		RequestToDefaultSession
	};
	
	EnumMap<UpdateStep, String> stepMap = null;
	ArrayList<Integer> CANIdList = null;
	ArrayList<Byte> CANMessage = null;
	final String CREATE_CANID_TABLE_SQL = "CREATE TABLE IF NOT EXISTS candb (canid integer primary key autoincrement, " +
			"manufacturer varchar(10), phyCANid varchar(4),funcCANid varchar(4), respCANid varchar(4))";
	
	/*final String CREATE_UPDATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS updateMessage (messageid integer primary key autoincrement, " +
			"step varchar(10), CAN_Message varchar(12))";*/
	
	public CANDatabaseHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
		CANIdList = new ArrayList<>();
		CANMessage = new ArrayList<>();
		stepMap = new EnumMap<UpdateStep, String>(UpdateStep.class);
	}

	@Override
	public void onCreate(SQLiteDatabase arg0) {
		// TODO Auto-generated method stub
		arg0.execSQL(CREATE_CANID_TABLE_SQL);
	}
	
	@Override
	public synchronized void close() {
		// TODO Auto-generated method stub
		super.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		System.out.println("--------onUpdate Called--------"
				+ oldVersion + "--->" + newVersion);
	}
	
	public void generateCanDB(SQLiteDatabase db){
		insertData(db, "zotye", "76D", "7DF", "70D");
		insertData(db, "dfsk", "726", "7DF", "7A6");
		insertData(db, "geely", "7C6", "7DF", "7CE");	
		insertData(db, "baic", "76D", "7DF", "70D"); /* CAN ID �д����� */
	}
	
	public void generateUpdateGeelyDatabase(SQLiteDatabase db){	/* ����Geely��Bootloader���� */
		stepMap.clear();
		stepMap.put(UpdateStep.ReadECUHardwareNumber, "22F19362");	/*���һ��byte�ǻ�����Ӧ��*/
		stepMap.put(UpdateStep.ReadBootloaderID, "22F18062");
		stepMap.put(UpdateStep.RequestToExtendSession, "100350");
		stepMap.put(UpdateStep.DisableDTCStorage, "8502C5");
		stepMap.put(UpdateStep.DisableNonDiagComm, "28030168");
		stepMap.put(UpdateStep.RequestToProgrammingSession, "100250");
		stepMap.put(UpdateStep.RequestSeed, "271167");
		stepMap.put(UpdateStep.SendKey, "271267");
		stepMap.put(UpdateStep.WriteTesterSerialNumber, "2EF1986E");
		stepMap.put(UpdateStep.WriteConfigureData, "2EF1996E");
		stepMap.put(UpdateStep.RequestDownload, "34004474");
		stepMap.put(UpdateStep.TransferData, "360176");
		stepMap.put(UpdateStep.TransferExit, "3777");
		stepMap.put(UpdateStep.CheckSum, "3101020271");
		stepMap.put(UpdateStep.EraseMemory, "3101FF004471");
		stepMap.put(UpdateStep.CheckProgrammDependency, "3101FF0171");
		stepMap.put(UpdateStep.ResetECU, "110151");
		stepMap.put(UpdateStep.EnableNonDiagComm, "28000168");
		stepMap.put(UpdateStep.EnableDTCStorage, "8501C5");
		stepMap.put(UpdateStep.RequestToDefaultSession, "100150");
	}
	
	public void generateUpdateForyouDatabase(SQLiteDatabase db){	/* ���պ����Bootloader���� */
		stepMap.clear();
		stepMap.put(UpdateStep.ReadECUSparePartNumber, "22F18762");	/*���һ��byte�ǻ�����Ӧ��*/
		stepMap.put(UpdateStep.RequestToExtendSession, "100350");
		stepMap.put(UpdateStep.DisableDTCStorage, "8502C5");
		stepMap.put(UpdateStep.DisableNonDiagComm, "28030168");
		stepMap.put(UpdateStep.RequestToProgrammingSession, "100250");
		stepMap.put(UpdateStep.RequestSeed, "270567");
		stepMap.put(UpdateStep.SendKey, "270667");
		stepMap.put(UpdateStep.WriteTesterSerialNumber, "2EF1986E");
		stepMap.put(UpdateStep.WriteECUSparePartNumber, "2EF1876E");
		stepMap.put(UpdateStep.WriteUpdateDate, "2EF1996E");
		stepMap.put(UpdateStep.RequestDownload, "34004474");
		stepMap.put(UpdateStep.TransferData, "360176");
		stepMap.put(UpdateStep.TransferExit, "3777");
		stepMap.put(UpdateStep.CheckSum, "3101F00171");
		stepMap.put(UpdateStep.EraseMemory, "3101FF004471");
		stepMap.put(UpdateStep.CheckProgrammCondition, "3101F00071");
		stepMap.put(UpdateStep.CheckProgrammDependency, "3101FF0171");
		stepMap.put(UpdateStep.ResetECU, "110151");
		stepMap.put(UpdateStep.RequestToDefaultSession, "100150");
	}
	
	/*
	public ArrayList<Integer> getCANMessage(UpdateStep step){
		CANMessage.clear();
		CANMessage.add(Integer.parseInt(stepMap.get(step), 16));
		return CANMessage;
	}*/
	
	public ArrayList<Byte> getCANMessage(UpdateStep step){
		CANMessage.clear();
		String str = stepMap.get(step);
		//��ʮ�������ַ���ת��Ϊbyte����
		byte a[] = str.getBytes();
		for(int i=0; i< a.length; i++){ 
			if( a[i]>='0' && a[i]<='9'){ //����0~9
				a[i] = (byte)(a[i]-'0');
			} else if( a[i]>='A' && a[i]<='Z'){ //��д��ĸ A~Z
				a[i] =(byte) (a[i]-'0'-7);
			} else if( a[i]>='a' && a[i]<='z'){ //Сд��ĸ a~z
				a[i] =(byte) (a[i]-'A'-23);
			} else{
				System.out.println("The String isn't Hex!");
			}
		}
		for(int i=0; i<(a.length)/2; i++){
			CANMessage.add((byte)(a[2*i]<<4 | a[2*i+1]));
		}
		//CANMessage.add(Integer.parseInt(stepMap.get(step), 16));
		return CANMessage;
	}
	
	public ArrayList<Integer> getCANID(SQLiteDatabase db, String key){
		CANIdList.clear();
		Cursor cursor = db.rawQuery("select * from candb where manufacturer like ?", new String[]{key});
		if(cursor.moveToFirst()){
			/*
			String phyId = cursor.getString(cursor.getColumnIndex("phyCANid"));
			String funcId = cursor.getString(cursor.getColumnIndex("funcCANid"));
			String respId = cursor.getString(cursor.getColumnIndex("respCANid"));
			//System.out.printf("phyCANid=%s\n",fun);
			System.out.printf("phyCANid=%4h\n",Integer.parseInt(funcId, 16));
			
			/* ���ڰ��������֣�a,b,c�����ַ�����ֱ��ʹ��parseInt()ת��ʱ���׳��쳣  */
			/* ����ʮ�����Ƶ��ַ�������ת��ʱ��Ҫʹ�� parseInt("", 16), ��Ҫָ��16���ƣ�����ᱨ��  */
			CANIdList.add(Integer.parseInt(cursor.getString(cursor.getColumnIndex("phyCANid")), 16));  
			CANIdList.add(Integer.parseInt(cursor.getString(cursor.getColumnIndex("funcCANid")), 16));
			CANIdList.add(Integer.parseInt(cursor.getString(cursor.getColumnIndex("respCANid")), 16));
			/* ���⣬��ʹ��CANIdList֮ǰ��û�н���ʵ����, ��������ʱ����ָ���쳣  */
			cursor.close();
		}
		
		return CANIdList;
	}
	
	private void insertData(SQLiteDatabase db, String manufaturer, String phyid, String funcid,  String respid)
	{
		// ִ�в������
		db.execSQL("insert into candb values(null , ? , ?, ? , ?)"
			, new String[] {manufaturer, phyid, funcid, respid});
	}

}

