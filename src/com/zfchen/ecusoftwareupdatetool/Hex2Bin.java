package com.zfchen.ecusoftwareupdatetool;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Hex2Bin {
	
	final static byte length_index = 0;	/* ��Ҫ����Ϊstatic����, �ſ�����list����Ϊ����ֱ��ʹ��, final����Ϊ���� */
	final static byte address_MSB_index = 1;
	final static byte address_LSB_index = 2;
	final static byte type_index = 3;
	final static byte base_address_MSB_index = 4;
	final static byte base_address_LSB_index = 5;
	
	final static byte NO_ADDRESS_TYPE_SELECTED = 0;
	final static byte LINEAR_ADDRESS = 1;
	final static byte SEGMENTED_ADDRESS = 2;
	
	byte[] starting_address = {0,0,0,0};
	byte[] size = {0,0,0,0};
	int data_bytes, type, checksum;
	int first_word, address;
	
	byte seg_lin_select;
	long lowest_address, highest_address, phys_addr, segment, upper_address;
	int Records_Start;
	int min_block_size;
	int max_length;
	String buffer = null;
	
	byte hex[] = null;
	ArrayList<Integer> al = null;
	ArrayList<Record> result = null;
	Record record = null;
	
	public enum RecordType{
		Data_Rrecord,
		EOF_Record,
		Extended_Segment_Address_Record,
		Start_Segment_Address_Record,
		Extended_Linear_Address_Record,
		Start_Linear_Address_Record;	/*java��ö�����Ͷ����﷨��C�е���Щ����, ���һ����Ա������һ��';'��*/
	}
	
	public byte[] getStarting_address() {
		/*
		byte[] temp = Long.toHexString(this.lowest_address).getBytes();
		for(int i=0; i<temp.length; i++){
			starting_address[i] = temp[i];
		}*/
		for(int i=3; i>=0; i--){
			starting_address[3-i] = (byte)(this.lowest_address>>(i*8));
		}
		//2220
		//0  0 22 20   ����ַ�ĸ��ֽڴ��ڵ�һ�������У�
		return starting_address;
	}
	
	public byte[] getSize() {
		/*byte[] temp = Integer.toHexString(this.max_length).getBytes();
		for(int i=0; i<temp.length; i++){
			size[i] = temp[i];
		}*/
		for(int i=3; i>=0; i--){
			size[3-i] = (byte)(this.max_length>>(i*8));
		}
		return size;
	}


	public Hex2Bin() {
		super();
		// TODO Auto-generated constructor stub
		address = 0;
		first_word = 0;
		segment = 0;
		upper_address = 0;
		highest_address = 0;
		max_length = 0;
		lowest_address = Long.MAX_VALUE;
		seg_lin_select = NO_ADDRESS_TYPE_SELECTED;
		al = new ArrayList<Integer>();
		result = new ArrayList<Record>();
		record = new Record(0, null);
	}
	

	@SuppressWarnings("unchecked")
	private void parseHex(RecordType type){
		switch (type) {	//values()�� ��̬����������һ������ȫ��ö��ֵ������
			case Data_Rrecord:
				if(data_bytes == 0)
					break;
				
				address = first_word;
				if(seg_lin_select == SEGMENTED_ADDRESS){
					phys_addr = (segment << 4) + address;	////16-bit address
				}
				else
				{
					phys_addr = (upper_address << 16) + address;	//32-bit address
				}
				
				if((phys_addr) < lowest_address)	//�ҵ���͵ĵ�ֵַ
					lowest_address = phys_addr;
				
				long temp = phys_addr + data_bytes -1;  //�ҵ���ߵĵ�ֵַ
				if(temp > highest_address)
					highest_address = temp;
				
				if(data_bytes > 0){	
					for(int j =0; j<4 ; j++)
						al.remove(0);	/* �Ƴ�ĳ��Ԫ�غ�, �����Ԫ�ؾͻ��Զ���ǰ�ƶ� */
					
					al.remove(al.size()-1);	//remove the byte of checksum
					
					/* �˴��漰�������ǳ���������⣬����al�� recordÿ��ѭ������ı䣬������������������result��ȡֵʱ�����������󶼻�ָ�����һ��ѭ���ĵ�ַ */
					record.setAddressAndList(phys_addr, (ArrayList<Integer>)al.clone());
					try {
						result.add((Record) record.clone());
					} catch (CloneNotSupportedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				break;
				
			case EOF_Record:				
				break;
								
			case Extended_Segment_Address_Record:
				if(seg_lin_select == NO_ADDRESS_TYPE_SELECTED){
					seg_lin_select = SEGMENTED_ADDRESS;
				}
				
				if(seg_lin_select == SEGMENTED_ADDRESS){
					segment = (al.get(base_address_MSB_index)<<4)|al.get(base_address_LSB_index);
					phys_addr = (segment << 4);
				}
				else{
					System.out.printf("Ignored extended linear address record %d\n", data_bytes);
				}
				
				break;
				
			case Start_Segment_Address_Record:
				break;
				
			case Extended_Linear_Address_Record:
				if(seg_lin_select == NO_ADDRESS_TYPE_SELECTED)
					seg_lin_select = LINEAR_ADDRESS;
				
				if(seg_lin_select == LINEAR_ADDRESS){
					upper_address = (al.get(base_address_MSB_index)<<4)|al.get(base_address_LSB_index);
					phys_addr = (upper_address << 16);
				}
				break;
				
			case Start_Linear_Address_Record:
				break;
	
			default:
				System.out.printf("The type of record exits error!\n");
				break;
		}
	}
	
	
	public int getFileSize(String filename){
		try 
		{
			FileReader fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);
			
			highest_address = 0;
			lowest_address = Long.MAX_VALUE;
			seg_lin_select = NO_ADDRESS_TYPE_SELECTED;
			result.clear();
			
			while ((buffer = br.readLine()) != null)//����ѭ����ʽ��һ��һ�еĶ�ȡ
			{
				hex = buffer.substring(1).getBytes();//ȡ��ð��(:)���������

				al = this.ASC_II2Hexadecimal(hex);		
				data_bytes = al.get(length_index);
				
				type = al.get(type_index);
				checksum = al.get(al.size()-1);	//���һλ��У���
				first_word = ((al.get(address_MSB_index)<<8)|al.get(address_LSB_index))&0xFFFF;	//��1��2λ�ǵ�ַ
				
				if(type>4 || type<0){
					System.out.println("The hex file exist error!");
				}	
				
				/* calculate checksum and compare */
				int sum = 0;
				for(int i=0; i<(al.size()-1); i++){
					sum += al.get(i);
				}
				if(((sum+checksum) & 0xFF) != 0)
					System.out.println("The checksum of hex file exist error!");
				
				this.parseHex(RecordType.values()[type]);
				//al.clear();
			}
			
			max_length = (int)(highest_address - lowest_address+1);
			br.close();
			fr.close();
		} catch (Exception ioe) {
			// TODO: handle exception
			ioe.printStackTrace();
		}
		
		return max_length;
	}
	
	private ArrayList<Integer> ASC_II2Hexadecimal(byte data[]){
		ArrayList<Integer> list = new ArrayList<>();
		for(int i=0; i< data.length; i++){
			if( data[i]>='0' && data[i]<='9'){
				data[i] = (byte)(data[i]-'0');
			} else if( data[i]>='A' && data[i]<='Z'){
				data[i] =(byte) (data[i]-'0'-7);
			}
		}
		
		/*����java�������޷������ͣ�����byte���Ͷ��Դ˴�����Ľ������ָ�����byte����ռ8λ�����λ��Ϊ1ʱ�ᱻ������������*/
		/* 0xeb ��ӡʱ����� -21 , ʵ�������ߵĶ����Ʋ�����ͬ, ����һ�����޷�����, һ�����з�����, ���ͷ�ʽ��ͬ*/
		for(int i=0; i< data.length-1; i+=2){
			list.add((Integer) ((data[i]<<4)|data[i+1]));
		}
		return list;
	}
	
	public boolean Convert(String source_filename, String destination_filename)
	{
		File file = new File(destination_filename);
		max_length = (int)(highest_address - lowest_address+1);
		ArrayList<Byte> file_content = new ArrayList<Byte>();	//Integer --> Byte
		file_content.ensureCapacity(getFileSize(source_filename));
		for(int i=0; i<max_length; i++){	// ������0xFF��ʼ��buffer 
			file_content.add((byte) 0xFF);
		}
		
		for (Record r : result) {
			long addr = r.getAddress();
			addr -= lowest_address;
			ArrayList<Integer> al_temp = r.getList();
			for(int i=0; i<al_temp.size(); i++){
				file_content.set((int)(addr+i), al_temp.get(i).byteValue());
			}
		}
		
		try {
			FileOutputStream fops = new FileOutputStream(file);
			for (byte b : file_content) {
				fops.write(b);
			}
			
			fops.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}//д��ԭʼ�ֽ�����ʹ�� FileOutputStream
		return true;
	}
	
	public ArrayList<Byte> getHexFileData(String source_filename)
	{
		//max_length = (int)(highest_address - lowest_address+1);
		ArrayList<Byte> file_content = new ArrayList<Byte>();	//Integer --> Byte
		//file_content.ensureCapacity(getFileSize(source_filename));
		file_content.ensureCapacity(max_length);
		for(int i=0; i<max_length; i++){	// ������0xFF��ʼ��buffer 
			file_content.add((byte) 0xFF);
		}
		
		for (Record r : result) {
			long addr = r.getAddress();
			addr -= lowest_address;
			ArrayList<Integer> al_temp = r.getList();
			for(int i=0; i<al_temp.size(); i++){
				file_content.set((int)(addr+i), al_temp.get(i).byteValue());
			}
		}
		System.out.println("getHexFileData......");
		return file_content;
	}
}	

