package eis;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class eis extends Thread
{
	//手動で二つ、文字列を入力して鍵（パスワード）を生成。
	public static String configManualKeyFront = "jurtsfdeASAw3975449eiefUQasae";
	public static String configManualKeyRear = "swdFRR8745iuhNNHSDEpo0as123YsueUUi";
	
	public static final Charset CONFIG_CHARACTER = StandardCharsets.UTF_16LE;
	public static final String CONFIG_RANDOM = "Windows-PRNG";
	public static final String CONFIG_HASH = "SHA3-512";
	public static final String CONFIG_TIME = "SSSssmmHHddMMyyyy";
	
	public static final String CONFIG_EXTENSION = ".eis";
	public static final String CONFIG_VERSION = "3.3.0";
	public static final String CONFIG_AUTO_KEY_FOLDER_NAME = "0___holder\\";
	public static final String CONFIG_AUTO_KEY_FILE_EXTENSION = ".autokey";
	public static final int CONFIG_AUTO_KEY_SIZE = 256;
	public static final int CONFIG_RANDOM_WIDTH = 512;
	public static final int CONFIG_BUFFER_SIZE = 131072;
	
	public static String configAutoKeyFolderPath = "\\";
	public static String configImportFilePath = "c:\\sample.txt";
	public static String configExportDirectoryPathEncrypt = "C:\\workbench\\" + CONFIG_EXTENSION + "\\encrypt\\";
	public static String configExportDirectoryPathDecrypt = "C:\\workbench\\" + CONFIG_EXTENSION + "\\decrypt\\";
	
	public static Long configImportFileFullSize = -1L;
	public static boolean configInitializeProgress = false;
	public static int configProgressSector = -1;
	public static int configProgressSectorCounter = 0;
	public static int configProgressPercent = 0;
	
	public static int configHashLength = -1;
	
	//マルチスレッド用変数。
	private boolean eis;
	private byte[][] raf;
	private byte[] hashFront;
	private byte[] hashRear;
	private int hashLength;
	private byte[] buffer;
	
	//マルチスレッド初期設定。
	public eis(final boolean EIS, final byte[][] RAF, final byte[] HASH_FRONT, final byte[] HASH_REAR, final int HASH_LENGTH, final byte[] BUFFER) throws Exception
	{
		this.eis = EIS;
		this.raf = RAF;
		this.hashFront = HASH_FRONT;
		this.hashRear = HASH_REAR;
		this.hashLength = HASH_LENGTH;
		this.buffer = BUFFER;
	}
	
	@Override //マルチスレッド処理。
	public void run()
	{
		int hashCounter = 0;
		
		try
		{
			hashCounter = this.hashLength;
			for(int lap = 0; lap < this.buffer.length; lap++)
			{
				if(hashCounter == this.hashLength)
				{
					this.hashFront = getHash(this.hashFront);
					this.hashRear = getHash(this.hashRear);
					
					hashCounter = 0;
				}
				
				if(!this.eis)
				{
					this.buffer[lap] -= raf[(this.hashFront[hashCounter] + 128)][(this.hashRear[hashCounter] + 128)];
				}
				else
				{
					this.buffer[lap] += raf[(this.hashFront[hashCounter] + 128)][(this.hashRear[hashCounter] + 128)];
				}
				
				hashCounter++;
			}
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
	}
	
	public synchronized byte[] getBuffer()
	{
		return this.buffer;
	}
	
	public static void main(String[] args) throws Exception
	{
		commandLine(args);
	}
	
	public static void commandLine(final String[] COMMAND) throws Exception
	{
		switch(COMMAND[0])
		{
		case "-keygenerate":
				configAutoKeyFolderPath = COMMAND[1];
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
	
	//進捗状況を表示。
	public static void displayProgress()
	{
		if(!configInitializeProgress)
		{
			configProgressSector = (int)(configImportFileFullSize / CONFIG_BUFFER_SIZE / 100);
			configInitializeProgress = true;
			System.out.print("進捗状況, 0%");
		}
		else
		{
			configProgressSectorCounter++;
			if(configProgressSector != 0 && configProgressSector == configProgressSectorCounter && configProgressPercent != 100)
			{
				configProgressPercent++;
				System.out.print("\r");
				System.out.print("進捗状況, " + configProgressPercent + "%");
				configProgressSectorCounter = 0;
			}
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
		if(FRONT != null)baos.write(FRONT);
		if(REAR != null)baos.write(REAR);
		
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
		System.out.println("鍵を自動生成します。");
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
			System.out.println(configAutoKeyFolderPath + f.getPath());
		}
		
		System.out.println("鍵の自動生成が完了しました。");
	}
	
	//引数のファイルを取得して暗号化。
	public static void enCrypt(final String IMPORT_FILE_PATH, final String EXPORT_DIRECTORY_PATH) throws Exception
	{
		File fi = new File(IMPORT_FILE_PATH);
		FileInputStream fis = new FileInputStream(fi);
		BufferedInputStream bis = new BufferedInputStream(fis);
		if(fi.getPath().contains(EXPORT_DIRECTORY_PATH + fi.getName()))System.exit(0);
		configImportFileFullSize = fi.length();
		configHashLength = getHash(getSeed(1)).length;
		
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
		byte[] buffer = new byte[CONFIG_BUFFER_SIZE];
		/*raf*/byte[][] raf = new byte[CONFIG_AUTO_KEY_SIZE][];
		/*raf*/for(int lap = 0; lap < raf.length; lap++)raf[lap] = Files.readAllBytes(Paths.get(configAutoKeyFolderPath + CONFIG_AUTO_KEY_FOLDER_NAME + lap + CONFIG_AUTO_KEY_FILE_EXTENSION));
		System.out.println(fi.getPath() + "を暗号化中。しばらくお待ち下さい……。");
		while((bufferLimit = bis.read(buffer)) != -1)
		{
			int sectorFront = 0;
			int sectorRear = 0;
			eis[] eisThreadCore = new eis[16];
			for(int lap = 0; lap < eisThreadCore.length; lap++)
			{
				sectorRear += (CONFIG_BUFFER_SIZE / eisThreadCore.length);
				eisThreadCore[lap] = new eis(false, raf, configManualKeyFrontHash, configManualKeyRearHash, configHashLength, Arrays.copyOfRange(buffer, sectorFront, sectorRear));
				eisThreadCore[lap].start();
				sectorFront += (CONFIG_BUFFER_SIZE / eisThreadCore.length);
				
				for(int blockChain = 0; blockChain < (CONFIG_BUFFER_SIZE / eisThreadCore.length) / configHashLength; blockChain++)
				{
					configManualKeyFrontHash = getHash(configManualKeyFrontHash);
					configManualKeyRearHash = getHash(configManualKeyRearHash);
				}
			}
			
			buffer = null;
			for(int lap = 0; lap < eisThreadCore.length; lap++)
			{
				eisThreadCore[lap].join();
				buffer = getArray(buffer, eisThreadCore[lap].getBuffer());
				eisThreadCore[lap] = null;
			}
			
			bos.write(buffer, 0, bufferLimit);
			bos.flush();
			displayProgress();
		}
		
		bis.close();
		bos.close();
		System.out.print("\r");
		System.out.println("進捗状況, 100%");
		System.out.println("暗号化が完了しました。" + fo.getPath() + "に出力。");
	}
	
	//引数のファイルを取得して復号化。
	public static void deCrypt(final String IMPORT_FILE_PATH, final String EXPORT_DIRECTORY_PATH) throws Exception
	{
		File fi = new File(IMPORT_FILE_PATH);
		FileInputStream fis = new FileInputStream(fi);
		BufferedInputStream bis = new BufferedInputStream(fis);
		if(!getFileExtension(fi.getName()).contains(CONFIG_EXTENSION))System.exit(0);
		configHashLength = getHash(getSeed(1)).length;
		
		int throughLength = 0;
		final byte[] THROUGH = new byte[(getByte(CONFIG_EXTENSION).length + getByte(CONFIG_VERSION).length)];
		bis.read(THROUGH);throughLength += THROUGH.length;
		final byte[] TIME = new byte[getTime().length];
		bis.read(TIME);throughLength += TIME.length;
		final byte[] EXPORT_FILE_EXTENSION_LENGTH = new byte[1];
		bis.read(EXPORT_FILE_EXTENSION_LENGTH);throughLength += EXPORT_FILE_EXTENSION_LENGTH.length;
		final byte[] EXPORT_FILE_EXTENSION = new byte[(EXPORT_FILE_EXTENSION_LENGTH[0] * getByte("b").length)];
		bis.read(EXPORT_FILE_EXTENSION);throughLength += EXPORT_FILE_EXTENSION.length;
		configImportFileFullSize = (fi.length() - throughLength);
		
		byte[] configManualKeyFrontHash = getArray(TIME, getByte(configManualKeyFront));
		byte[] configManualKeyRearHash = getArray(TIME, getByte(configManualKeyRear));
		
		new File(configExportDirectoryPathDecrypt).mkdirs();
		File fo = new File(EXPORT_DIRECTORY_PATH + getFileName(fi.getName()) + getString(EXPORT_FILE_EXTENSION));
		FileOutputStream fos = new FileOutputStream(fo);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		
		int bufferLimit = -1;
		byte[] buffer = new byte[CONFIG_BUFFER_SIZE];
		/*raf*/byte[][] raf = new byte[CONFIG_AUTO_KEY_SIZE][];
		/*raf*/for(int lap = 0; lap < raf.length; lap++)raf[lap] = Files.readAllBytes(Paths.get(configAutoKeyFolderPath + CONFIG_AUTO_KEY_FOLDER_NAME + lap + CONFIG_AUTO_KEY_FILE_EXTENSION));
		System.out.println(fi.getPath() + "を復号中。しばらくお待ち下さい……。");
		while((bufferLimit = bis.read(buffer)) != -1)
		{
			int sectorFront = 0;
			int sectorRear = 0;
			eis[] eisThreadCore = new eis[16];
			for(int lap = 0; lap < eisThreadCore.length; lap++)
			{
				sectorRear += (CONFIG_BUFFER_SIZE / eisThreadCore.length);
				eisThreadCore[lap] = new eis(true, raf, configManualKeyFrontHash, configManualKeyRearHash, configHashLength, Arrays.copyOfRange(buffer, sectorFront, sectorRear));
				eisThreadCore[lap].start();
				sectorFront += (CONFIG_BUFFER_SIZE / eisThreadCore.length);
				
				for(int blockChain = 0; blockChain < (CONFIG_BUFFER_SIZE / eisThreadCore.length) / configHashLength; blockChain++)
				{
					configManualKeyFrontHash = getHash(configManualKeyFrontHash);
					configManualKeyRearHash = getHash(configManualKeyRearHash);
				}
			}
			
			buffer = null;
			for(int lap = 0; lap < eisThreadCore.length; lap++)
			{
				eisThreadCore[lap].join();
				buffer = getArray(buffer, eisThreadCore[lap].getBuffer());
				eisThreadCore[lap] = null;
			}
			
			bos.write(buffer, 0, bufferLimit);
			bos.flush();
			displayProgress();
		}
		
		bis.close();
		bos.close();
		System.out.print("\r");
		System.out.println("進捗状況, 100%");
		System.out.println("復号化が完了しました。" + fo.getPath() + "に出力。");
	}
}
