package com.zfchen.ecusoftwareupdatetool;

public class Seed2Key implements SecurityAccess {

	byte key[]  = {0,0,0,0}; /*��Կ����*/
	byte cal[]  = {0,0,0,0};
	boolean status_lock = true;
	String manufacturer;
	
	
	public Seed2Key(String manufacturer) {
		super();
		this.manufacturer = manufacturer;
	}

	public byte[] getKey() {
		return key;
	}

	@Override
	public byte[] generateKey(byte[] seed, byte level) {
		// TODO Auto-generated method stub

		if(checkSeed(seed)){
			switch (manufacturer) {
			//Ŀǰ zotye,baic��dfsk���ߵ��������̶��ǲ��ջ�����������������
			case "zotye":
				key_ZOTYE(seed, level);
				break;
				
			case "baic":
				//����
				break;
				
			case "dfsk":
				key_DFSK(seed, level);
				break;
						
			case "geely":	//�������������̲���"���������������"
				key_GEELY(seed, level);
				break;
				
			default:
				System.out.println("The manufacturer isn't supported!");
				break;
			}
		}
		return getKey();
	}
	
	private boolean checkSeed(byte[] seed){ /* seed always have a non-zero value and not consist of all [0xFF] value. */
		long temp;
		long key_temp[] = {0,0,0,0};;
		for(byte i=0; i<4; i++)
		{
			key_temp[i] = seed[i];
		}
		temp = (key_temp[0] << 24) | (key_temp[1] << 16) | (key_temp[2] << 8) | (key_temp[3]);
		if((0 == temp) && (0xFFFFFFFFL == temp))
			return false;
		else
			return true;
	}

	private void key_DFSK(byte[] seed, byte level){ /* seed always have a non-zero value and not consist of all [0xFF] value. */
		for (int i=0; i < 4; i++)
			cal[i] = (byte) (seed[i] ^ xor_DFSK[i]);

		key[0] = (byte) (((cal[0] & 0x0FL) << 4) | (cal[1] & 0xF0L));		//Cal[0]�ĵ�4λ ����4λ  |  Cal[1]�ĸ�4λ
		key[1] = (byte) (((cal[1] & 0x0FL) << 4) | ((cal[2] & 0xF0L) >>> 4)); //Cal[1]�ĵ�4λ ����4λ  |  Cal[2]�ĸ�4λ ����4λ
		key[2] = (byte) (((cal[2] & 0xF0L))      | ((cal[3] & 0xF0L) >>> 4));
		key[3] = (byte) (((cal[3] & 0x0FL) << 4) | (cal[0] & 0x0FL));
	}
	
	private void key_ZOTYE(byte[] seed, byte level){
		for (int i=0; i < 4; i++)
			cal[i] = (byte) (seed[i] ^ xor_ZOTYE[i]);

		key[0] = (byte) (((cal[0] & 0x0FL) << 4) | (cal[1] & 0xF0L));
		key[1] = (byte) (((cal[1] & 0x0FL) << 4) | ((cal[2] & 0xF0L) >>> 4));
		key[2] = (byte) (((cal[2] & 0xF0L))      | ((cal[3] & 0xF0L) >>> 4));
		key[3] = (byte) (((cal[3] & 0x0FL) << 4) | (cal[0] & 0x0FL));
	}

	private void key_GEELY(byte[] seed, byte level){
		switch(level){
			case 1:
				for (int i=0; i < 4; i++)
					cal[i] = (byte) (seed[i] ^ xor_GEELY[i]);

				key[0] = (byte) (((cal[3] & 0x0FL) << 4) | (cal[3] & 0xF0L));
				key[1] = (byte) (((cal[1] & 0x0FL) << 4) | ((cal[0] & 0xF0L) >>> 4));
				key[2] = (byte) (((cal[1] & 0xF0L))      | ((cal[2] & 0xF0L) >>> 4));
				key[3] = (byte) (((cal[0] & 0x0FL) << 4) | (cal[2] & 0x0FL));
				break;
			
			case 3:
				for (int i=0; i < 4; i++)
				{
					cal[i] = (byte) (((seed[i]&0xF8) >>> 3) ^ xor_GEELY[i]);
				}
				key[0] = (byte) (((cal[3] & 0x07L) << 5) | ((cal[0] & 0xF8L) >>> 3));
				key[1] = (byte) (((cal[0] & 0x07L) << 5) | (cal[2] & 0x1FL));
				key[2] = (byte) (((cal[1] & 0xF8L))      | ((cal[3] & 0xE0L) >>> 5));
				key[3] = (byte) (((cal[2] & 0xF8L) << 4) | (cal[1] & 0x07L));
				break;

			case 11:
				for (int i=0; i < 4; i++)
				{
					cal[i] = (byte) (seed[i] ^ xor_GEELY[i]);
				}
				key[0] = (byte) (((cal[2] & 0x03L) << 6) | ((cal[3] & 0xFCL) >>> 2));
				key[1] = (byte) (((cal[3] & 0x03L) << 6) | (cal[0] & 0x3FL));
				key[2] = (byte) (((cal[0] & 0xFCL))      | ((cal[1] & 0xC0L) >>> 6));
				key[3] = (byte) ((cal[1] & 0xFCL)        | (cal[2] & 0x03L));
				break;

			default:
				break;
		}
		
	}
	
}
