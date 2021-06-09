package hielo_v2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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

public class ernte
{
	public static final String MASTER_KEY = "HELLO WORLD !!: JAVA.";
	
	public static final Charset ALGORITHM_FONT = StandardCharsets.UTF_16LE;
	public static final String ALGORITHM_RANDOM = "Windows-PRNG";
	public static final String ALGORITHM_HASH = "SHA3-512";
	public static final String ALGORITHM_TIME = "SSSssmmHHddMMyyyy";
	
	public static final String EXTENSION = ".xxxxx";
	public static final String VERSION = "1.0.0";
	
	public static void main(String[] args) throws Exception
	{
		//鍵ファイルを生成。
		keyGenerate();
		
		//c:\workbench\trial\prototype.txtを手動で作成して適当に文章を書いて保存すれば下記のコードで暗号化&復号化。
		new File("C:\\workbench\\trial\\").mkdir();
		new File("C:\\workbench\\trial\\encrypt\\").mkdir();
		new File("C:\\workbench\\trial\\decrypt\\").mkdir();
		enCrypt("C:\\workbench\\trial\\prototype.txt", "C:\\workbench\\trial\\encrypt\\");
		deCrypt("C:\\workbench\\trial\\encrypt\\prototype.xxxxx", "C:\\workbench\\trial\\decrypt\\");
	}
	
	//現在時刻を取得。
	public static byte[] getTime() throws Exception
	{
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(ALGORITHM_TIME);
		
		return sdf.format(c.getTime()).getBytes(ALGORITHM_FONT);
	}
	
	//疑似乱数のバイト配列を取得。
	public static byte[] getRandom(final int SIZE) throws Exception
	{
		SecureRandom sr = SecureRandom.getInstance(ALGORITHM_RANDOM);
		
		return sr.generateSeed(SIZE);
	}
	
	//引数のバイト配列からハッシュ値を取得。
	public static byte[] getHash(final byte[] SEED) throws Exception
	{
		MessageDigest md = MessageDigest.getInstance(ALGORITHM_HASH);
		md.update(SEED);
		
		return md.digest();
	}
	
	//鍵ファイル群を生成。
	public static void keyGenerate() throws Exception
	{	
		new File("key").mkdir();
		for(int loop = 0; loop < 256; loop++)
		{
			File f = new File("key\\" + loop + ".key");
			FileOutputStream fos = new FileOutputStream(f);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			
			bos.write(getRandom(256));
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
		
		
		final String EXPORT_FILE_NAME = fi.getName().substring(0, fi.getName().lastIndexOf("."));
		final String EXPORT_FILE_PATH = EXPORT_DIRECTORY_PATH + EXPORT_FILE_NAME + EXTENSION;
		File fo = new File(EXPORT_FILE_PATH);
		FileOutputStream fos = new FileOutputStream(fo);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		
		int bufferSize = -1;
		int hashCounter = getHash(getRandom(1)).length;
		byte[] buffer = new byte[65536];
		byte[] key = new byte[1];
		
		byte[] time = getTime();
		byte[] timeHash = time;
		byte[] masterHash = MASTER_KEY.getBytes(ALGORITHM_FONT);
		
		
		//このプログラムの拡張子を書き込み。
		bos.write(EXTENSION.getBytes(ALGORITHM_FONT));
		//このプログラムのバージョンを書き込み。
		bos.write(VERSION.getBytes(ALGORITHM_FONT));
		//読み込みファイルの拡張子文字数を書き込み。
		final String IMPORT_FILE_EXTENSION = IMPORT_FILE_PATH.substring(IMPORT_FILE_PATH.lastIndexOf("."));
		bos.write((byte)IMPORT_FILE_EXTENSION.length());
		//読み込みファイルの拡張子を書き込み。
		bos.write(IMPORT_FILE_EXTENSION.getBytes(ALGORITHM_FONT));
		//生成した時刻を書き込み。
		bos.write(time);
		
		while((bufferSize = bis.read(buffer)) != -1)
		{
			for(int loop = 0; loop < bufferSize; loop++)
			{
				if(hashCounter == getHash(getRandom(1)).length)
				{
					timeHash = getHash(timeHash);
					masterHash = getHash(masterHash);
					
					hashCounter = 0;
				}
				
				RandomAccessFile raf = new RandomAccessFile("key\\" + (timeHash[hashCounter] + 128) + ".key", "r");
				raf.seek((timeHash[hashCounter] + 128));
				raf.read(key);
				raf.close();
				
				buffer[loop] -= key[0];
				
				hashCounter++;
			}
			bos.write(buffer, 0, bufferSize);
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
		
		//確認用なので読み込んで飛ばします。
		final byte[] THROUGH = new byte[(EXTENSION.getBytes(ALGORITHM_FONT).length + VERSION.getBytes(ALGORITHM_FONT).length)];
		bis.read(THROUGH);
		//出力ファイルの拡張子文字数を取得。
		final byte[] EXPORT_FILE_EXTENSION_LENGTH = new byte[1];
		bis.read(EXPORT_FILE_EXTENSION_LENGTH);
		//出力ファイルの拡張子を取得。
		final byte[] EXPORT_FILE_EXTENSION = new byte[("v".getBytes(ALGORITHM_FONT).length * EXPORT_FILE_EXTENSION_LENGTH[0])];
		bis.read(EXPORT_FILE_EXTENSION);
		//読み込みファイルを生成した時刻を取得。
		final byte[] IMPORT_TIME = new byte[getTime().length];
		bis.read(IMPORT_TIME);
		
		final String EXPORT_FILE_NAME = fi.getName().substring(0, fi.getName().lastIndexOf("."));
		final String EXPORT_FILE_PATH = EXPORT_DIRECTORY_PATH + EXPORT_FILE_NAME + new String(EXPORT_FILE_EXTENSION, ALGORITHM_FONT);
		File fo = new File(EXPORT_FILE_PATH);
		FileOutputStream fos = new FileOutputStream(fo);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		
		int bufferSize = -1;
		int hashCounter = getHash(getRandom(1)).length;
		byte[] buffer = new byte[65536];
		byte[] key = new byte[1];
		
		byte[] timeHash = IMPORT_TIME;
		byte[] masterHash = MASTER_KEY.getBytes(ALGORITHM_FONT);
		
		while((bufferSize = bis.read(buffer)) != -1)
		{
			for(int loop = 0; loop < bufferSize; loop++)
			{
				if(hashCounter == getHash(getRandom(1)).length)
				{
					timeHash = getHash(timeHash);
					masterHash = getHash(masterHash);
					
					hashCounter = 0;
				}
				
				RandomAccessFile raf = new RandomAccessFile("key\\" + (timeHash[hashCounter] + 128) + ".key", "r");
				raf.seek((timeHash[hashCounter] + 128));
				raf.read(key);
				raf.close();
				
				buffer[loop] += key[0];
				
				hashCounter++;
			}
			bos.write(buffer, 0, bufferSize);
		}
		bis.close();
		bos.flush();
		bos.close();
	}
}
