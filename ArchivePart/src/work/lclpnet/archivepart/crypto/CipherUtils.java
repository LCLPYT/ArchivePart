package work.lclpnet.archivepart.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CipherUtils {

	/*public static Cipher getAESEncryptCipher(final String key) throws GeneralSecurityException {
		byte[] keyBytes = getKeyBytes(key.getBytes());
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(keyBytes));
		return cipher;
	}

	public static Cipher getAESDecryptCipher(final String key) throws GeneralSecurityException {
		byte[] keyBytes = getKeyBytes(key.getBytes());
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(keyBytes));
		return cipher;
	}

	private static byte[] getKeyBytes(final byte[] key) {
		byte[] keyBytes = new byte[16];
		System.arraycopy(key, 0, keyBytes, 0, Math.min(key.length, keyBytes.length));
		return keyBytes;
	}*/

	public static byte[] translate(byte[] input, String secretCode) {
		if(secretCode == null) return input;
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XorOutputStream xOut = new XorOutputStream(out, secretCode);

		try {
			xOut.write(input);
			xOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return out.toByteArray();
	}

}
