package com.example.guardingapp;

import java.nio.ByteBuffer;


/**
 * Class for creating NDEF record
 * 
 * @author Sandeep Tamrakar
 */
public class NdefRecord {
	
	/**
	 *  Indicates no type or payload associated with this NDEF record.
	 *  please see section 3.2.6 of NFCForum-TS-NDEF_1.0 for details
 	 */
	public static final short TNF_EMPTY = 0x00;
	
	/**
	 *  Indicates the NDEF record contains an RTD type data.
	 *  please see NFCForum-TS-RTD_1.0 for details on RTD
	 */
	public static final short TNF_WELL_KNOWN = 0x01;
	
	/**
	 * First byte of NDEF record that defines the property of the NDEF record
	 *  
	 *  MSB                           LSB
	 *  +-------------------------------+
	 *  | MB | ME | CF | 1 | IL | T N F |
	 *  +-------------------------------+
	 *  |  1 |  1 |  0 | 1 |  0 |  000  |
	 *  +-------------------------------+
	 *  Message Begin - Message End - Chunk Flag - 
	 *  
	 *  For simplicity of this exercise, necessary Flags has been set.
	 *  Students are only required to set TNF bits accordingly
	 */	
	public static final byte PRESET_RECORD_HEADER = (byte) 0xD0;
	
	/**
	 *  RTD Text type
	 */
	public static final byte[] RTD_TEXT = {0x54}; // "T"
	
	/**
	 * RTD URI type
	 */
	public static final byte[] RTD_URI = {0x55};   // "U" 
	
	/** 
	 * URI identifier see section 3.2.2 of NFCForum-TS-RTD_URI_1.0
	 * Students need to replace the prefix in the url with the 
	 * index value. E.g. http://www. = 0x01
	 */
	private static final String[] URI_IDENTIFIER = new String[] {
		"",
		"http://www.",
        "https://www.",
        "http://",
        "https://",
        "tel:",
        "mailto:",        
	};
	
	private static final String SMS_HEADER = "sms:";
	
	private byte mNdefRecordHeader; 
	private short mTnf; 
	private byte[] mType = null;
	private byte[] mPayload = null;
	
	/**
	 * Constructor for the NdefRecord class.
	 * Creates an empty NDEF record
	 * 
	 */
	public NdefRecord () {		
		mTnf = TNF_EMPTY;
		mNdefRecordHeader = PRESET_RECORD_HEADER | TNF_EMPTY;
		mType = null;
		mPayload = null;
		
	}
	
	/**
	 * Constructor for the NdefRecord class.
	 * 
	 * @param tnf 	a 3-bit TNF value	  
	 * @param type  byte array defining type of the payload
	 * @param payload  byte array
	 * 
	 */
	public NdefRecord(short tnf, byte[] type, byte[] payload) {
		
		if ((tnf < 0 ) || (tnf > 0x07)) {
			throw new IllegalArgumentException("TNF out of range" + tnf);
		}
		if ((tnf != TNF_EMPTY) && ((type == null) || (payload == null))){
			throw new IllegalArgumentException("TNF supplied require type and payload data");
		}
		
		mTnf = tnf;
		mNdefRecordHeader = (byte) (PRESET_RECORD_HEADER | mTnf);
		if (type != null) 
			mType = type.clone();
		if (payload !=null)
			mPayload = payload.clone();
	}
	
	/**
	 * Returns TNF of the NDEF record
	 * @return TNF value
	 */
	public short getTnf() {
		return mTnf;
	}
	
	/**
	 * Returns NDEF payload type
	 * @return byte array
	 */
	public byte[] getType() {
		return mType;
	}
	
	/**
	 * Returns NDEF record payload
	 * @return byte array
	 */
	public byte[] getPayload() {
		return mPayload;
	}
	
	/**
	 * Returns URI string if an NDEF record contains URI
	 * @return String if NDEF records contains URI; null otherwise
	 */
	public String getUriString() {
		
		//check if NDFS record contains TNF is 0x01 and type is 0x55
		String uriString = "";
		byte type = getType()[0];
		
		if ((mNdefRecordHeader == (byte)0xD1) && (type == (byte)0x55)){
			byte[] pl = getPayload();
			
			uriString += URI_IDENTIFIER[(byte)(pl[0])];
			for (int i = 0; i < pl.length - 1; i++){
				byte dec = (byte)pl[i + 1];
				uriString += (char)dec;
			}
			uriString += "\0";
			return uriString;
		}
		return null;
	}
	
	/** 
	 * Creates an NDEF record from byte array if possible
	 * @param byte[] byte array
	 * @return NdefRecord if possible, null otherwise 
	 */
	public static NdefRecord fromByteArray(byte[] data){
		/*
		 * TODO: create NdefRecord from byte array
		 */
		byte tnf = data[0];
		byte[] type = {data[1]};
		byte[] payload = new byte[data.length - 2];
		
		for (int i = 0; i < data.length - 2; i++)
			payload[i] = data[i + 2];
		
		NdefRecord record = new NdefRecord(tnf, type, payload);				
		return record;
	}
	
	/**
	 * Returns an NDEF record with URI as payload
	 * 
	 * @param uri 	a uri string
	 * @return NdefRecord created from the uri string
	 */
	public static NdefRecord fromUriString(String uri) {
	
		byte[] type = {0x55};
		int id = 0;
		
		//find the identifier of the uri string
		
		for (int i = 1; i < URI_IDENTIFIER.length; i++){
			if (uri.startsWith(URI_IDENTIFIER[i])){
				id = i;
				break;
			}
		}
		//uri string after deducting the identifier
		String pl_wo_id = uri.substring(URI_IDENTIFIER[id].length());
		
		//payload comprises identifider and string after identifier
		
		byte[] payload = new byte[1 + pl_wo_id.length()];
		payload[0] = (byte)id; 
		byte[] temp = pl_wo_id.getBytes();
		//System.out.println("identifier = " + payload[0]);
		
		for (int i = 0; i < pl_wo_id.length(); i++){
			payload[i + 1] = temp[i]; 
			//System.out.println(payload[i + 1] + "	");
		}
		
		NdefRecord record = new NdefRecord(TNF_WELL_KNOWN, type, payload);
			
		return record;
	}
	
	//sms:00358468427953?body=anh anh
	//will fix the title later on
	public static NdefRecord fromSMSMessage(String phoneNumber, String textMessage){
		byte[] type = {0x55};
		int id = 0;
		
		String str_payload = SMS_HEADER + phoneNumber + "?body=" + textMessage;
		//this is for title
		
		//str_payload = str_payload + "Q" + "\u0001" + "\u0005" + "T" + "\u0002" + "en" + title;
		byte[] payload = new byte [1 + str_payload.length()];
		payload[0] = (byte) 0x00;
		byte[]temp = str_payload.getBytes();
		for (int i = 0; i < str_payload.length(); i++){
			payload[i + 1] = temp[i];
		}
		NdefRecord record = new NdefRecord(TNF_WELL_KNOWN, type, payload);
		return record;
	}
	
	/**
	 * Returns the byte array value of the NdefRecord
	 * 
	 * NOTE: this function currently produce only empty NDEF record
	 * byte arrary
	 *
	 *
	 *  MSB                           LSB
	 *  +-------------------------------+
	 *  | MB | ME | CF | 1 | IL | T N F |
	 *  +-------------------------------+
	 *  |         TYPE    LENGTH        |
	 *  +-------------------------------+
	 *  |      PAYLOAD  	 LENGTH     | 
	 *  +-------------------------------+
	 *  |             TYPE              |
	 *  +-------------------------------+
	 *  |           PAYLOAD             |
	 *  +-------------------------------+ 
	 * 
	 * Also note that ID field is removed for this exercise.
	 * 
	 * TODO: Students are required to construct the
	 * 		 NDEF record byte array according to the
	 *       format shown above.
	 * @return byte array
	 *  
	 */
	public byte[] toByteArray() {		
		/**
		 * TODO: return the NDEF record into byte array
		 * Currently it returns an empty ndef record
		 */
		/*int len = 3;
		
		ByteBuffer record = ByteBuffer.allocate(len);
        record.put((byte)(mNdefRecordHeader));
        	record.put((byte) 0x00);
        	record.put((byte) 0x00);
        	
        
        return record.array();
		*/
		byte[] payload = getPayload();
		byte[] type = getType();
		
		//because the SR flag is set --> payload_length field is 1 octet.
		int len = 1 + 1 + 1 + 1 + payload.length;
		ByteBuffer record = ByteBuffer.allocate(len);
		record.put((byte)0xD1);
		record.put((byte)0x01);
		record.put((byte)payload.length);
		record.put((byte)0x55);
		
		for (int i = 0; i < payload.length; i++){
			record.put((byte)payload[i]);
		}
		
		return record.array();
	}

}
