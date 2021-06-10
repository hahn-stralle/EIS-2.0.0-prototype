package eis;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class eis
{
	//手動で二つ、文字列を入力して鍵を生成。（パスワード的な。）
	public static String configManualKeyFront = "jurtsfdeASAw3975449eiefUQasae";
	public static String configManualKeyRear = "swdFRR8745iuhNNHSDEpo0as123YsueUUi";
	
	public static final Charset CONFIG_CHARACTER = StandardCharsets.UTF_16LE;
	public static final String CONFIG_RANDOM = "Windows-PRNG";
	public static final String CONFIG_HASH = "SHA3-512";
	public static final String CONFIG_TIME = "SSSssmmHHddMMyyyy";
	
	public static final String CONFIG_EXTENSION = ".eis";
	public static final String CONFIG_VERSION = "3.1.0";
	public static final String CONFIG_AUTO_KEY_FOLDER_NAME = "0___holder\\";
	public static final String CONFIG_AUTO_KEY_FILE_EXTENSION = ".autokey";
	public static final int CONFIG_AUTO_KEY_SIZE = 256;
	public static final int CONFIG_RANDOM_WIDTH = 512;
	public static final int CONFIG_BUFFER_SIZE = 65536;
	
	public static String configAutoKeyFolderPath = "\\";
	public static String configImportFilePath = "c:\\sample.txt";
	public static String configExportDirectoryPathEncrypt = "C:\\workbench\\" + CONFIG_EXTENSION + "\\encrypt\\";
	public static String configExportDirectoryPathDecrypt = "C:\\workbench\\" + CONFIG_EXTENSION + "\\decrypt\\";
	
	public static void main(String[] args) throws Exception
	{
		commandLine(args);
	}
	
	public static void commandLine(final String[] COMMAND) throws Exception
	{
		switch(COMMAND[0])
		{
		case "-keygenerate":
				keyGenerate();
			break;
		case "-encrypt":
				configManualKeyFront = COMMAND[1];
				configManualKeyRear = COMMAND[2];
				configImportFilePath = COMMAND[3];
				configExportDirectoryPathEncrypt = COMMAND[4];
				configAutoKeyFolderPath = COMMAND[5] + configAutoKeyFolderPath;
				enCrypt(configImportFilePath, configExportDirectoryPathEncrypt);
			break;
		case "-decrypt":
				configManualKeyFront = COMMAND[1];
				configManualKeyRear = COMMAND[2];
				configImportFilePath = COMMAND[3];
				configExportDirectoryPathDecrypt = COMMAND[4];
				configAutoKeyFolderPath = COMMAND[5] + configAutoKeyFolderPath;
				deCrypt(configImportFilePath, configExportDirectoryPathDecrypt);
			break;
		}
	}
	
	//引数のバイト配列を文字列化して取得。
	public static String getString(final byte[] BUFFER) throws Exception
	{
		return new String(BUFFER, CONFIG_CHARACTER);
	}
	
	//引数の文字列からファイル名のみを取得。
	public static String getFileName(final String STRING) throws Exception
	{
		return STRING.substring(0, STRING.lastIndexOf("."));
	}
	
	//引数の文字列から拡張子のみを取得。
	public static String getFileExtension(final String STRING) throws Exception
	{
		return STRING.substring(STRING.lastIndexOf("."));
	}
	
	//引数の二つあるバイト配列を結合して取得。
	public static byte[] getArray(final byte[] FRONT, final byte[] REAR) throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(FRONT);
		baos.write(REAR);
		
		return baos.toByteArray();
	}
	
	//引数の文字列をバイト配列に変換し取得。
	public static byte[] getByte(final String STRING) throws Exception
	{
		return STRING.getBytes(CONFIG_CHARACTER);
	}
	
	//現在時刻を取得。
	public static byte[] getTime() throws Exception
	{
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(CONFIG_TIME);
		
		return getByte(sdf.format(c.getTime()));
	}
	
	//一つ目の引数から二つ目の引数までの数字から擬似乱数で選び取得。
	public static int getRandom(final int FRONT, final int REAR) throws Exception
	{
		SecureRandom sr = SecureRandom.getInstance(CONFIG_RANDOM);
		return (FRONT + sr.nextInt(REAR));
	}
	
	//疑似乱数のバイト配列を取得。
	public static byte[] getSeed(final int SIZE) throws Exception
	{
		SecureRandom sr = SecureRandom.getInstance(CONFIG_RANDOM);
		return sr.generateSeed(SIZE);
	}
	
	//引数のバイト配列からハッシュ値を取得。
	public static byte[] getHash(final byte[] SEED) throws Exception
	{
		MessageDigest md = MessageDigest.getInstance(CONFIG_HASH);
		md.update(SEED);
		
		return md.digest();
	}
	
	//鍵ファイル群を生成。
	public static void keyGenerate() throws Exception
	{
		new File(CONFIG_AUTO_KEY_FOLDER_NAME).mkdir();
		
		for(int outLap = 0; outLap < CONFIG_AUTO_KEY_SIZE; outLap++)
		{
			File f = new File(CONFIG_AUTO_KEY_FOLDER_NAME + outLap + CONFIG_AUTO_KEY_FILE_EXTENSION);
			FileOutputStream fos = new FileOutputStream(f);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			
			for(int inLap = 0; inLap < CONFIG_AUTO_KEY_SIZE / getHash(getSeed(1)).length; inLap++)
			{
				bos.write(getHash(getSeed(getRandom(CONFIG_RANDOM_WIDTH, CONFIG_RANDOM_WIDTH))));
			}
			bos.flush();
			bos.close();
		}
	}
	
	//引数のファイルを取得して暗号化。
	public static void enCrypt(final String IMPORT_FILE_PATH, final String EXPORT_DIRECTORY_PATH) throws Exception
	{
		File fi = new File(IMPORT_FILE_PATH);
		FileInputStream fis = new FileInputStream(fi);
		BufferedInputStream bis = new BufferedInputStream(fis);
		
		final byte[] TIME = getTime();
		byte[] configManualKeyFrontHash = getArray(TIME, getByte(configManualKeyFront));
		byte[] configManualKeyRearHash = getArray(TIME, getByte(configManualKeyRear));
		
		new File(configExportDirectoryPathEncrypt).mkdirs();
		File fo = new File(EXPORT_DIRECTORY_PATH + getFileName(fi.getName()) + CONFIG_EXTENSION);
		FileOutputStream fos = new FileOutputStream(fo);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		
		bos.write(getByte(CONFIG_EXTENSION));
		bos.write(getByte(CONFIG_VERSION));
		bos.write(TIME);
		bos.write((byte)getFileExtension(fi.getName()).length());
		bos.write(getByte(getFileExtension(fi.getName())));
		
		int bufferLimit = -1;
		int hashCounter = getHash(getSeed(1)).length;
		byte[] buffer = new byte[CONFIG_BUFFER_SIZE];
		byte[] cell = new byte[1];
		while((bufferLimit = bis.read(buffer)) != -1)
		{
			for(int lap = 0; lap < bufferLimit; lap++)
			{
				if(hashCounter == getHash(getSeed(1)).length)
				{
					configManualKeyFrontHash = getHash(configManualKeyFrontHash);
					configManualKeyRearHash = getHash(configManualKeyRearHash);
					hashCounter = 0;
				}
				
				RandomAccessFile raf = new RandomAccessFile(configAutoKeyFolderPath + CONFIG_AUTO_KEY_FOLDER_NAME + (configManualKeyFrontHash[hashCounter] + 128) + CONFIG_AUTO_KEY_FILE_EXTENSION, "r");
				raf.seek((configManualKeyRearHash[hashCounter] + 128));
				raf.read(cell);
				raf.close();
				
				buffer[lap] -= cell[0];
				hashCounter++;
			}
			
			bos.write(buffer, 0, bufferLimit);
		}
		
		bis.close();
		bos.flush();
		bos.close();
	}
	
	//引数のファイルを取得して復号化。
	public static void deCrypt(final String IMPORT_FILE_PATH, final String EXPORT_DIRECTORY_PATH) throws Exception
	{
		File fi = new File(IMPORT_FILE_PATH);
		FileInputStream fis = new FileInputStream(fi);
		BufferedInputStream bis = new BufferedInputStream(fis);
		
		final byte[] THROUGH = new byte[(getByte(CONFIG_EXTENSION).length + getByte(CONFIG_VERSION).length)];
		bis.read(THROUGH);
		final byte[] TIME = new byte[getTime().length];
		bis.read(TIME);
		final byte[] EXPORT_FILE_EXTENSION_LENGTH = new byte[1];
		bis.read(EXPORT_FILE_EXTENSION_LENGTH);
		final byte[] EXPORT_FILE_EXTENSION = new byte[(EXPORT_FILE_EXTENSION_LENGTH[0] * getByte("b").length)];
		bis.read(EXPORT_FILE_EXTENSION);
		
		byte[] configManualKeyFrontHash = getArray(TIME, getByte(configManualKeyFront));
		byte[] configManualKeyRearHash = getArray(TIME, getByte(configManualKeyRear));
		
		new File(configExportDirectoryPathDecrypt).mkdirs();
		System.out.println(EXPORT_DIRECTORY_PATH + getFileName(fi.getName()) + getString(EXPORT_FILE_EXTENSION));
		File fo = new File(EXPORT_DIRECTORY_PATH + getFileName(fi.getName()) + getString(EXPORT_FILE_EXTENSION));
		FileOutputStream fos = new FileOutputStream(fo);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		
		int bufferLimit = -1;
		int hashCounter = getHash(getSeed(1)).length;
		byte[] buffer = new byte[CONFIG_BUFFER_SIZE];
		byte[] cell = new byte[1];
		while((bufferLimit = bis.read(buffer)) != -1)
		{
			for(int lap = 0; lap < bufferLimit; lap++)
			{
				if(hashCounter == getHash(getSeed(1)).length)
				{
					configManualKeyFrontHash = getHash(configManualKeyFrontHash);
					configManualKeyRearHash = getHash(configManualKeyRearHash);
					hashCounter = 0;
				}
				
				RandomAccessFile raf = new RandomAccessFile(configAutoKeyFolderPath + CONFIG_AUTO_KEY_FOLDER_NAME + (configManualKeyFrontHash[hashCounter] + 128) + CONFIG_AUTO_KEY_FILE_EXTENSION, "r");
				raf.seek((configManualKeyRearHash[hashCounter] + 128));
				raf.read(cell);
				raf.close();
				
				buffer[lap] += cell[0];
				hashCounter++;
			}
			
			bos.write(buffer, 0, bufferLimit);
		}
		
		bis.close();
		bos.flush();
		bos.close();
	}
}
