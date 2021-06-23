import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Scanner;

public class eis
{
	final String CONFIG___EIS_FILE_EXTENSION = ".eis";
	final String CONFIG___EIS_FILE_VERSION = "4.1.0";
	String CONFIG___EIS_DIRECTORY_PATH;
	int CONFIG___HASH_RANGE;
	
	final String CONFIG___GENERATE_AUTOMATIC_KEY_EIS_COMMAND = "-generate_automatic_key";
	final String CONFIG___ENCRYPT_EIS_COMMAND = "-encrypt";
	final String CONFIG___DECRYPT_EIS_COMMAND = "-decrypt";
	
	final Charset CONFIG___CHARACTER_CODE = StandardCharsets.UTF_16LE;
	final String CONFIG___RANDOM_ALGORITHM = "Windows-PRNG";
	final String CONFIG___HASH_ALGORITHM = "SHA3-512";
	final String CONFIG___TIME_FORMAT = "yyyy年MM月dd日HH時mm分ss.SSS秒";
	
	byte[][] CONFIG___AUTOMATIC_KEY_DATA = new byte[512][512];
	final int[] CONFIG___AUTOMATIC_KEY_SEED_RANGE = {512, 262144};
	final String CONFIG___AUTOMATIC_KEY_FOLDER_NAME = "00___automatic_key\\";
	final String CONFIG___AUTOMATIC_KEY_FILE_EXTENSION = ".eak";
	
	byte[][] CONFIG___MANUAL_KEY_DATA = new byte[5][];
	final String CONFIG___MANUAL_KEY_FILE_NAME = "___manual_key";
	final String CONFIG___MANUAL_KEY_FILE_EXTENSION = ".txt";
	
	String CONFIG___INPUT_FILE_PATH;
	long CONFIG___INPUT_FILE_SIZE;
	String CONFIG___OUtPUt_DIRECTORY_FILE_NAME = "06___output_directory_path";
	String CONFIG___OUTPUT_DIRECTORY_FILE_EXTENSION = ".txt";
	String CONFIG___OUTPUT_DIRECTORY_PATH;
	final String CONFIG___OUTPUT_ENCRYPT_DIRECTORY_PATH = "encrypt\\";
	final String CONFIG___OUTPUT_DECRYPT_DIRECTORY_PATH = "decrypt\\";
	
	final int CONFIG___INPUT_FILE_LAP_BUFFER_SIZE = 8192;
	
	final int CONFIG___PROGRESS_DIVIDE = 100;
	long CONFIG___PROGRESS_SECTOR;
	int CONFIG___PROGRESS_PERCENT = 0;
	long CONFIG___PROGRESS_COUNTER = 0;
	
	final int CONFIG___CHECKSUM_SIZE = 25;
	
	//ファイルの復号化がちゃんと出来ているのかチェックサム値で照合。
	void verifyCheckSum(final byte[] PARAM___ORIGIN_CHECKSUM, final byte[] PARAM___CURRENT_CHECKSUM) throws Exception
	{
		System.out.println("暗号化前のチェックサム値: " + getThirtySixCharacter(PARAM___ORIGIN_CHECKSUM));
		System.out.println("復号化後のチェックサム値: " + getThirtySixCharacter(PARAM___CURRENT_CHECKSUM));
		
		if(Arrays.equals(PARAM___ORIGIN_CHECKSUM, PARAM___CURRENT_CHECKSUM))
		{
			System.out.println("二つのチェックサム値が一致。完全な復号化に成功しました。");
		}
		else
		{
			System.out.println("二つのチェックサム値が不一致。完全な復号化に失敗しました。");
		}
	}
	
	//eisファイルなのかチェック。
	void checkFileEis(final File PARAM___INPUT_FILE) throws Exception
	{
		if(!getFileTotalExtension(PARAM___INPUT_FILE.getName()).equals(CONFIG___EIS_FILE_EXTENSION))
		{
			System.out.println("eisファイルではないため、復号化をせずにスルーします。");
			System.exit(0);
		}
	}
	
	//ファイル、フォルダの存在有無。
	void fileFolderExistence(final String PARAM___F_TYPE, final File PARAM___OUTPUT_F) throws Exception
	{
		Scanner s = new Scanner(System.in);
		
		if(PARAM___OUTPUT_F.exists())
		{
			if(PARAM___F_TYPE.equals("file"))
			{
				System.out.print("出力先にファイルが既に存在しています。上書きしますか？（y=はい。y以外の文字=いいえ。）>");
			}
			else
			{
				System.out.print("出力先にフォルダが既に存在しています。上書きしますか？過去の暗号ファイルは復号化できなくなります。（y=はい。y以外の文字=いいえ。）>");
			}
			
			if(!s.next().equals("y"))
			{
				System.out.println("\r上書きをしないためスルーします。");
				System.exit(0);
			}
		}
	}
	
	//バイト配列を数字とアルファベットに変換して返します。
	String getThirtySixCharacter(final byte[] PARAM___BYTE_ARRAY) throws Exception
	{
		String result = "";
		String characterCode = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		int calc = 0;
		
		for(int byteLap = 0; byteLap < PARAM___BYTE_ARRAY.length; byteLap++)
		{
			calc = 512 + PARAM___BYTE_ARRAY[byteLap];
			calc = calc % characterCode.length();
			result += characterCode.substring(calc, calc + 1);
		}
		
		return result;
	}
	
	//拡張子無しのファイル名取得。
	String getFileName(final String PARAM___STRING) throws Exception
	{
		return PARAM___STRING.substring(0, PARAM___STRING.indexOf("."));
	}
	
	//全ての拡張子を取得。
	String getFileTotalExtension(final String PARAM___STRING) throws Exception
	{
		return PARAM___STRING.substring(PARAM___STRING.indexOf("."));
	}
	
	//文字列を指定した文字コードでバイト配列化。
	byte[] getCharacterByte(final String PARAM___STRING) throws Exception
	{
		return PARAM___STRING.getBytes(CONFIG___CHARACTER_CODE);
	}
	
	//バイト配列を指定した文字コードで文字列化。
	String getCharacterString(final byte[] PARAM___BUFFER) throws Exception
	{
		return new String(PARAM___BUFFER, CONFIG___CHARACTER_CODE);
	}
	
	//現在時刻を取得。
	byte[] getCurrentTime() throws Exception
	{
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(CONFIG___TIME_FORMAT);
		
		return getCharacterByte(sdf.format(c.getTime()));
	}
	
	//テキストファイルの一行目のみを取得。
	String getTroupe(final String PARAM___FILE_PATH) throws Exception
	{
		FileReader fr = new FileReader(PARAM___FILE_PATH);
		BufferedReader br = new BufferedReader(fr);
		
		return br.readLine();
	}
	
	//引数の範囲の数字を疑似乱数で取得。
	int getRandomNumber(final int PARAM___FROM_NUMBER, final int PARAM___TO_NUMBER) throws Exception
	{
		SecureRandom sr = SecureRandom.getInstance(CONFIG___RANDOM_ALGORITHM);
		return PARAM___FROM_NUMBER + sr.nextInt(PARAM___TO_NUMBER - PARAM___FROM_NUMBER);
	}
	
	//指定したバイト数のバイト配列を疑似乱数で取得。
	byte[] getRandomSeed(final int PARAM___BYTE_AMOUNT) throws Exception
	{
		SecureRandom sr = SecureRandom.getInstance(CONFIG___RANDOM_ALGORITHM);
		return sr.generateSeed(PARAM___BYTE_AMOUNT);
	}
	
	//二つの配列を結合。
	byte[] getConnectArray(final byte[] PARAM___FRONT_BUFFER, final byte[] PARAM___REAR_BUFFER) throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if(PARAM___FRONT_BUFFER != null)baos.write(PARAM___FRONT_BUFFER);
		if(PARAM___REAR_BUFFER != null)baos.write(PARAM___REAR_BUFFER);
		
		return baos.toByteArray();
	}
	
	//ハッシュ値を取得。
	byte[] getHash(final byte[] PARAM___BUFFER) throws Exception
	{
		MessageDigest md = MessageDigest.getInstance(CONFIG___HASH_ALGORITHM);
		
		return md.digest(PARAM___BUFFER);
	}
	
	//メイン関数。
	public static void main(final String[] PARAM)
	{
		try
		{
			new eis().initialize(PARAM);
		}
		catch(Exception error)
		{
			error.printStackTrace();
		}
	}
	
	
	//初期化
	void initialize(final String[] PARAM___COMMAND) throws Exception
	{
		CONFIG___EIS_DIRECTORY_PATH = PARAM___COMMAND[1];
		CONFIG___HASH_RANGE = getHash(getRandomSeed(1)).length;
		
		switch(PARAM___COMMAND[0])
		{
			case CONFIG___GENERATE_AUTOMATIC_KEY_EIS_COMMAND:
				generateAutomaticKey();
			break;
			
			case CONFIG___ENCRYPT_EIS_COMMAND:
			case CONFIG___DECRYPT_EIS_COMMAND:
				CONFIG___INPUT_FILE_PATH = PARAM___COMMAND[2];
				CONFIG___INPUT_FILE_SIZE = new File(CONFIG___INPUT_FILE_PATH).length();
				CONFIG___PROGRESS_SECTOR = CONFIG___INPUT_FILE_SIZE / CONFIG___INPUT_FILE_LAP_BUFFER_SIZE / CONFIG___PROGRESS_DIVIDE;
				CONFIG___OUTPUT_DIRECTORY_PATH = getTroupe(CONFIG___EIS_DIRECTORY_PATH + CONFIG___OUtPUt_DIRECTORY_FILE_NAME + CONFIG___OUTPUT_DIRECTORY_FILE_EXTENSION);
				
				for(int readLap = 0; readLap < CONFIG___AUTOMATIC_KEY_DATA.length; readLap++)
				{
					CONFIG___AUTOMATIC_KEY_DATA[readLap] = Files.readAllBytes(Paths.get(CONFIG___EIS_DIRECTORY_PATH + CONFIG___AUTOMATIC_KEY_FOLDER_NAME + String.format("%03d", readLap) + CONFIG___AUTOMATIC_KEY_FILE_EXTENSION));
				}
				
				for(int readLap = 0; readLap < CONFIG___MANUAL_KEY_DATA.length; readLap++)
				{
					CONFIG___MANUAL_KEY_DATA[readLap] = getCharacterByte(new String(Files.readAllBytes(Paths.get(CONFIG___EIS_DIRECTORY_PATH + String.format("%02d", readLap + 1) + CONFIG___MANUAL_KEY_FILE_NAME + CONFIG___MANUAL_KEY_FILE_EXTENSION))));
				}
				
				if(PARAM___COMMAND[0].equals(CONFIG___ENCRYPT_EIS_COMMAND))
				{
					enCrypt();
				}
				else
				{
					deCrypt();
				}
			break;
		}
	}
	
	//オートマチックキーの生成。
	void generateAutomaticKey() throws Exception
	{
		byte[] hash = null;
		File fo = new File(CONFIG___EIS_DIRECTORY_PATH + CONFIG___AUTOMATIC_KEY_FOLDER_NAME);
		System.out.println(fo.getPath() + "にオートマチックキーを生成します。");
		fileFolderExistence("folder", fo);
		fo.mkdirs();
		
		for(int fileLap = 0; fileLap < CONFIG___AUTOMATIC_KEY_DATA.length; fileLap++)
		{
			fo = new File(CONFIG___EIS_DIRECTORY_PATH + CONFIG___AUTOMATIC_KEY_FOLDER_NAME + String.format("%03d", fileLap) + CONFIG___AUTOMATIC_KEY_FILE_EXTENSION);
			FileOutputStream fos = new FileOutputStream(fo);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			
			hash = null;
			for(int valueLap = 0; valueLap < CONFIG___AUTOMATIC_KEY_DATA[fileLap].length / CONFIG___HASH_RANGE; valueLap++)
			{
				hash = getConnectArray(hash, getHash(getRandomSeed(getRandomNumber(CONFIG___AUTOMATIC_KEY_SEED_RANGE[0],CONFIG___AUTOMATIC_KEY_SEED_RANGE[1]))));
			}
			
			bos.write(hash);
			bos.close();
			
			System.out.println(getThirtySixCharacter(getHash(hash)) + ": " + String.format("%03d", fileLap) + CONFIG___AUTOMATIC_KEY_FILE_EXTENSION);
		}
		
		System.out.println("\rオートマチックキーの生成が完了しました。" + CONFIG___AUTOMATIC_KEY_DATA.length + "File x " + CONFIG___AUTOMATIC_KEY_DATA[0].length + "Volume.");
	}
	
	//進捗状況の表示。
	void displayProgress()
	{
		CONFIG___PROGRESS_COUNTER++;
		
		if(CONFIG___PROGRESS_SECTOR != 0 && CONFIG___PROGRESS_COUNTER == CONFIG___PROGRESS_SECTOR && CONFIG___PROGRESS_PERCENT < CONFIG___PROGRESS_DIVIDE)
		{
			CONFIG___PROGRESS_PERCENT++;
			System.out.print("\r進捗状況, " + String.format("%03d", CONFIG___PROGRESS_PERCENT) + "%");
			CONFIG___PROGRESS_COUNTER = 0;
		}
	}
	
	//ファイルを暗号化
	void enCrypt() throws Exception
	{
		byte[][] manualKeyHash = CONFIG___MANUAL_KEY_DATA;
		byte[] readBuffer = new byte[CONFIG___INPUT_FILE_LAP_BUFFER_SIZE];
		int readBufferLimit = -1;
		int hashCounter = 0;
		
		int pickFrontNumber = 0;
		int pickRearNumber = 0;
		int checkSumCounter = 0;
		
		File fi = new File(CONFIG___INPUT_FILE_PATH);
		FileInputStream fis = new FileInputStream(fi);
		BufferedInputStream bis = new BufferedInputStream(fis);
		
		byte[] eisExtension = getCharacterByte(CONFIG___EIS_FILE_EXTENSION);
		byte[] eisVersion = getCharacterByte(CONFIG___EIS_FILE_VERSION);
		byte outputFileExtensionRange = (byte)getFileTotalExtension(fi.getName()).length();
		byte[] outputFileExtension = getCharacterByte(getFileTotalExtension(fi.getName()));
		byte[] timeStamp = getCurrentTime();
		byte[] checkSum = new byte[CONFIG___CHECKSUM_SIZE];
		
		String outputFilePath = CONFIG___OUTPUT_DIRECTORY_PATH + CONFIG___EIS_FILE_EXTENSION + "\\" + CONFIG___OUTPUT_ENCRYPT_DIRECTORY_PATH + getFileName(fi.getName()) + CONFIG___EIS_FILE_EXTENSION;
		File fo = new File(outputFilePath);
		new File(CONFIG___OUTPUT_DIRECTORY_PATH + CONFIG___EIS_FILE_EXTENSION + "\\" + CONFIG___OUTPUT_ENCRYPT_DIRECTORY_PATH).mkdirs();
		System.out.println(fi.getPath() + "を暗号化しています。");
		fileFolderExistence("file", fo);
		FileOutputStream fos = new FileOutputStream(fo);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		
		bos.write(eisExtension);
		bos.write(eisVersion);
		bos.write(outputFileExtensionRange);
		bos.write(outputFileExtension);
		bos.write(timeStamp);
		bos.write(checkSum);
		
		System.out.print("\r進捗状況, 000%");
		
		manualKeyHash[0] = getHash(getConnectArray(timeStamp, manualKeyHash[0]));
		for(int hashLap = 1; hashLap < manualKeyHash.length; hashLap++)
		{
			manualKeyHash[hashLap] = getHash(getConnectArray(manualKeyHash[0], manualKeyHash[hashLap]));
		}
		
		while((readBufferLimit = bis.read(readBuffer)) != -1)
		{
			for(int byteLap = 0; byteLap < readBuffer.length; byteLap++)
			{
				if(hashCounter == CONFIG___HASH_RANGE)
				{
					manualKeyHash[0] = getHash(manualKeyHash[0]);
					for(int hashLap = 1; hashLap < manualKeyHash.length; hashLap++)
					{
						manualKeyHash[hashLap] = getHash(getConnectArray(manualKeyHash[0], manualKeyHash[hashLap]));
					}
					
					hashCounter = 0;
				}
				
				if(checkSumCounter == CONFIG___CHECKSUM_SIZE)
				{
					checkSumCounter = 0;
				}
				
				if(byteLap < readBufferLimit)checkSum[checkSumCounter] += readBuffer[byteLap];
				
				pickFrontNumber = 0;
				pickRearNumber = 0;
				if(manualKeyHash[1][hashCounter] < 0)pickFrontNumber = 256;
				if(manualKeyHash[2][hashCounter] < 0)pickRearNumber = 256;
				readBuffer[byteLap] -= CONFIG___AUTOMATIC_KEY_DATA[manualKeyHash[3][hashCounter] + 128 + pickFrontNumber][manualKeyHash[4][hashCounter] + 128 + pickRearNumber];
				
				hashCounter++;
				checkSumCounter++;
			}
			
			bos.write(readBuffer, 0, readBufferLimit);
			bos.flush();
			displayProgress();
		}
		
		bis.close();
		bos.close();
		
		RandomAccessFile raf = new RandomAccessFile(outputFilePath, "rw");
		raf.seek(raf.getFilePointer() + eisExtension.length);
		raf.seek(raf.getFilePointer() + eisVersion.length);
		raf.seek(raf.getFilePointer() + 1);
		raf.seek(raf.getFilePointer() + outputFileExtension.length);
		raf.seek(raf.getFilePointer() + timeStamp.length);
		raf.write(checkSum);
		raf.close();
		
		System.out.print("\r進捗状況, 100%");
		System.out.println("\r暗号化が完了しました。" + fo.getPath());
	}
	
	//ファイルを復号化
	void deCrypt() throws Exception
	{
		byte[][] manualKeyHash = CONFIG___MANUAL_KEY_DATA;
		byte[] readBuffer = new byte[CONFIG___INPUT_FILE_LAP_BUFFER_SIZE];
		int readBufferLimit = -1;
		int hashCounter = 0;
		
		int pickFrontNumber = 0;
		int pickRearNumber = 0;
		int checkSumCounter = 0;
		
		byte[] eisExtension = getCharacterByte(CONFIG___EIS_FILE_EXTENSION);
		byte[] eisVersion = getCharacterByte(CONFIG___EIS_FILE_VERSION);
		byte[] outputFileExtensionRange = new byte[1];
		byte[] outputFileExtension;
		byte[] timeStamp = getCurrentTime();
		byte[] originCheckSum = new byte[CONFIG___CHECKSUM_SIZE];
		byte[] currentCheckSum = new byte[CONFIG___CHECKSUM_SIZE];
		
		File fi = new File(CONFIG___INPUT_FILE_PATH);
		FileInputStream fis = new FileInputStream(fi);
		BufferedInputStream bis = new BufferedInputStream(fis);
		
		bis.read(eisExtension);
		bis.read(eisVersion);
		bis.read(outputFileExtensionRange);
		outputFileExtension = new byte[getCharacterByte(".").length * outputFileExtensionRange[0]];
		bis.read(outputFileExtension);
		bis.read(timeStamp);
		bis.read(originCheckSum);
		
		String outputFilePath = CONFIG___OUTPUT_DIRECTORY_PATH + CONFIG___EIS_FILE_EXTENSION + "\\" + CONFIG___OUTPUT_DECRYPT_DIRECTORY_PATH + getFileName(fi.getName()) + getCharacterString(outputFileExtension);
		File fo = new File(outputFilePath);
		new File(CONFIG___OUTPUT_DIRECTORY_PATH + CONFIG___EIS_FILE_EXTENSION + "\\" + CONFIG___OUTPUT_DECRYPT_DIRECTORY_PATH).mkdirs();
		System.out.println(fi.getPath() + "を復号化しています。");
		checkFileEis(fi);
		fileFolderExistence("file", fo);
		FileOutputStream fos = new FileOutputStream(fo);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		
		System.out.print("\r進捗状況, 000%");
		
		manualKeyHash[0] = getHash(getConnectArray(timeStamp, manualKeyHash[0]));
		for(int hashLap = 1; hashLap < manualKeyHash.length; hashLap++)
		{
			manualKeyHash[hashLap] = getHash(getConnectArray(manualKeyHash[0], manualKeyHash[hashLap]));
		}
		
		while((readBufferLimit = bis.read(readBuffer)) != -1)
		{
			for(int byteLap = 0; byteLap < readBuffer.length; byteLap++)
			{
				if(hashCounter == CONFIG___HASH_RANGE)
				{
					manualKeyHash[0] = getHash(manualKeyHash[0]);
					for(int hashLap = 1; hashLap < manualKeyHash.length; hashLap++)
					{
						manualKeyHash[hashLap] = getHash(getConnectArray(manualKeyHash[0], manualKeyHash[hashLap]));
					}
					
					hashCounter = 0;
				}
				
				if(checkSumCounter == CONFIG___CHECKSUM_SIZE)
				{
					checkSumCounter = 0;
				}
				
				pickFrontNumber = 0;
				pickRearNumber = 0;
				if(manualKeyHash[1][hashCounter] < 0)pickFrontNumber = 256;
				if(manualKeyHash[2][hashCounter] < 0)pickRearNumber = 256;
				readBuffer[byteLap] += CONFIG___AUTOMATIC_KEY_DATA[manualKeyHash[3][hashCounter] + 128 + pickFrontNumber][manualKeyHash[4][hashCounter] + 128 + pickRearNumber];
				
				if(byteLap < readBufferLimit)currentCheckSum[checkSumCounter] += readBuffer[byteLap];
				
				hashCounter++;
				checkSumCounter++;
			}
			
			bos.write(readBuffer, 0, readBufferLimit);
			bos.flush();
			displayProgress();
		}
		
		bis.close();
		bos.close();
		
		System.out.print("\r進捗状況, 100%");
		System.out.println("\r復号化が完了しました。" + fo.getPath());
		verifyCheckSum(originCheckSum, currentCheckSum);
	}
}