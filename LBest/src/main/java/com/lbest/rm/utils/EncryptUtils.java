package com.lbest.rm.utils;

import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by dell on 2017/10/20.
 */

public class EncryptUtils {
    public static String weiXinBase64Encrypt(byte[] data) {
        byte[] key = new byte[]{0x11, (byte) 0x9b, (byte) 0xf0, (byte) 0xce, 0x10,
                0x58, 0x72, 0x4b, 0x1f, 0x12, (byte) 0xac, (byte) 0xa9, 0x33, (byte) 0xef, 0x10, 0x45};

        byte[] newData = new byte[data.length];
        for (int i = 0; i < key.length; i++) {
            newData[i] = (byte) (key[i] ^ data[i]);
        }

        return Base64(newData);
    }


    public static String Base64(byte[] data) {
        return new String(Base64.encode(data, Base64.NO_WRAP));
    }

    /**
     * SHA1 加密
     *
     * @param value
     * @return
     */
    public static String SHA1(String value) {

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] sha1hash = new byte[40];

            md.update(value.getBytes());
            sha1hash = md.digest();
            return CommonUtils.bytesToHexString(sha1hash);
        } catch (Exception e) {
        }

        return "";
    }

    public static byte[] aesPKCS7PaddingDecryptByte(byte[] key, byte[] iv, byte[] data) {
        if(data == null){
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);

            byte[] original = cipher.doFinal(data);
            return original;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * md5
     *
     * @param data
     * @return md5 加密后的十六进制字符串
     */
    public static final String MD5String(String data) {
        try {
            return CommonUtils.bytesToHexString(MD5(data));
        } catch (Exception e) {
        }

        return null;
    }

    /**
     * md5
     *
     * @param data
     * @return
     */
    public static final byte[] MD5(String data) {
        return MD5(data.getBytes());
    }

    /**
     * md5
     *
     * @param data
     * @return
     */
    public static final byte[] MD5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(data);
        } catch (Exception e) {
        }

        return null;
    }

    /**
     * 文件SHA1 加密
     *
     * @param file
     * @return
     */
    public static String fileSHA1(File file) {
        String value = null;
        try {
            FileInputStream inputStream = new FileInputStream(file);
            MappedByteBuffer byteBuffer = inputStream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(byteBuffer);
            byte[] digest = sha1.digest();
            value = CommonUtils.bytesToHexString(digest);
            inputStream.close();
        } catch (Exception e) {

        }
        return value;
    }

    /**
     * 文件md5 加密
     *
     * @param file
     * @return
     */
    public static String fileMD5(File file) {
        String value = null;
        try {
            FileInputStream inputStream = new FileInputStream(file);
            MappedByteBuffer byteBuffer = inputStream.getChannel().map(
                    FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            byte[] digest = md5.digest();
            value = CommonUtils.bytesToHexString(digest);

            inputStream.close();
        } catch (Exception e) {

        }
        return value;
    }

    /**
     * AES/CBC/ZeroBytePadding加密
     * <br> iv = [(byte) 0xEA, (byte) 0xAA, (byte) 0xAA, 0x3A,
     * (byte) 0xBB, 0x58, 0x62, (byte) 0Xa2, 0x19, 0x18, (byte) 0xb5,
     * 0x77, 0x1D, 0x16, 0x15, (byte) 0xaa
     * ]
     *
     * @param hexKey 加密十六静止字符串key
     * @param data   加密数据
     * @return 加密之后返回的数据
     */
    public static byte[] aesNoPadding(String hexKey, String data) {
        byte[] iv = new byte[]{(byte) 0xEA, (byte) 0xAA, (byte) 0xAA, 0x3A,
                (byte) 0xBB, 0x58, 0x62, (byte) 0Xa2, 0x19, 0x18, (byte) 0xb5,
                0x77, 0x1D, 0x16, 0x15, (byte) 0xaa};

        return aesNoPadding(iv,
                CommonUtils.parseStringToByte(hexKey), data);
    }

    /**
     * AES/CBC/ZeroBytePadding加密
     * <br> iv = [(byte) 0xEA, (byte) 0xAA, (byte) 0xAA, 0x3A,
     * (byte) 0xBB, 0x58, 0x62, (byte) 0Xa2, 0x19, 0x18, (byte) 0xb5,
     * 0x77, 0x1D, 0x16, 0x15, (byte) 0xaa
     * ]
     *
     * @param key  加密key
     * @param data 加密数据
     * @return 加密之后返回的数据
     */
    public static byte[] aesNoPadding(byte[] key, String data) {
        byte[] iv = new byte[]{(byte) 0xEA, (byte) 0xAA, (byte) 0xAA, 0x3A,
                (byte) 0xBB, 0x58, 0x62, (byte) 0Xa2, 0x19, 0x18, (byte) 0xb5,
                0x77, 0x1D, 0x16, 0x15, (byte) 0xaa};

        return aesNoPadding(iv, key, data);
    }

    /***
     * AES/CBC/ZeroBytePadding加密
     *
     * @param iv   偏移量
     * @param key
     * @param data 加密数据
     * @return 加密之后返回的数据
     */
    public static byte[] aesNoPadding(byte[] iv, byte[] key, String data) {
        try {
            byte[] dataBytes = data.getBytes();
            Cipher cipher = Cipher.getInstance("AES/CBC/ZeroBytePadding");
            int blockSize = cipher.getBlockSize();

            int plaintextLength = dataBytes.length;
            if (plaintextLength % blockSize != 0) {
                plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
            }

            byte[] plaintext = new byte[plaintextLength];
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);

            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            byte[] encrypted = cipher.doFinal(plaintext);
            return encrypted;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * aesCBCEncryptByte(AES/CBC/PKCS5Padding)加密
     */
    public static byte[] aesPKCS5PaddingEncrypt(byte[] iv, byte[] key, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            byte[] encrypted = cipher.doFinal(data);

            return encrypted;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * aesCBCEncryptByte(AES/CBC/PKCS5Padding)解密
     */
    public static byte[] aesPKCS5PaddingDevrypt(byte[] iv, byte[] key, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);

            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String aesPKCS7PaddingDecrypt(byte[] key, byte[] iv, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);

            byte[] original = cipher.doFinal(data);
            return new String(original);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] aesPKCS7PaddingDecryptToByte(byte[] key, byte[] iv, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);

            byte[] original = cipher.doFinal(data);
            return original;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] aeskeyDecrypt(String srcKey) {
        byte[] newKeys = new byte[16];
        int[] swap = new int[]{7, 12, 3, 0, 11, 15, 2, 4, 5, 9, 14, 1, 13,
                10, 8, 6,};
        byte[] md5key = MD5(srcKey);
        for (int i = 0; i < 16; i++) {
            newKeys[i] = md5key[swap[i]];
        }

        return newKeys;
    }

    /**
     * SHA1 加密
     * <p>
     * spkchannel.broadlink.com.cn + timestamp + BroadLinkDNA@
     *
     * @param timestamp
     * @return
     */
    public static String broadlinkSPKSHA1(long timestamp) {
        return SHA1("spkchannel.broadlink.com.cn" + timestamp);
    }

    /**
     * 文件SHA1 加密
     *
     * @param file
     * @return
     */
    public static String getSHA1ByFile(File file) {
        String value = null;
        try {
            FileInputStream inputStream = new FileInputStream(file);
            MappedByteBuffer byteBuffer = inputStream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(byteBuffer);
            byte[] digest = sha1.digest();
            value = parseData(digest, digest.length);
            inputStream.close();
        } catch (Exception e) {

        }
        return value;
    }

    /**
     * 将解释到的数据转为String
     *
     * @param receiverDate
     * @param receiverLength
     * @return
     */
    public static String parseData(byte[] receiverDate, long receiverLength) {
        StringBuffer re = new StringBuffer();
        for (int i = 0; i < receiverLength; i++) {
            re.append(CommonUtils.to16(receiverDate[i]));
        }

        return re.toString();
    }

    /**
     * aesCBCDecrypt(AES/CBC/PKCS5Padding) 解密
     */
    public static String ms1NetRadioAesCBCDecrypt(byte[] data, long timestamp) {
        try {
            String keySha1 = SHA1(String.valueOf(timestamp));

            byte[] keySha1Bytes = CommonUtils.parseStringToByte(keySha1);

            byte[] key = new byte[16];
            System.arraycopy(keySha1Bytes, 0, key, 0, 16);

            byte[] ivBytes = String.valueOf(timestamp + "spkchannel v1 authentication").getBytes();
            byte[] iv = new byte[16];
            System.arraycopy(ivBytes, 0, iv, 0, 16);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);

            byte[] original = cipher.doFinal(data);
            return new String(original);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * "Broadlink:290" + timestamp shaBase 64 -> base 64 ->md5
     *
     * @param timestamp
     * @return
     */
    public static String BL_BASE64_SHA1_MD5(long timestamp) {

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] sha1hash = new byte[40];

            String oder = "Broadlink:290" + timestamp;
            md.update(oder.getBytes("iso-8859-1"), 0, oder.length());
            sha1hash = md.digest();
            String data = encode(sha1hash);
            return BLMD5(data);
        } catch (Exception e) {
        }

        return "";
    }

    /**
     * md5
     *
     * @param data
     * @return
     */
    public static final String BLMD5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] sha1hash = new byte[40];

            md.update(data.getBytes("iso-8859-1"), 0, data.length());
            sha1hash = md.digest();
            return parseData(sha1hash, sha1hash.length);
        } catch (Exception e) {
        }

        return "";
    }

    public static String encode(byte[] data) {
        char[] pz = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
        byte start = 0;
        int len = data.length;
        StringBuffer buf = new StringBuffer(data.length * 3 / 2);
        int end = len - 3;
        int i = start;
        int n = 0;

        int d;
        while (i <= end) {
            d = (data[i] & 255) << 16 | (data[i + 1] & 255) << 8 | data[i + 2] & 255;
            buf.append(pz[d >> 18 & 63]);
            buf.append(pz[d >> 12 & 63]);
            buf.append(pz[d >> 6 & 63]);
            buf.append(pz[d & 63]);
            i += 3;
            if (n++ >= 14) {
                n = 0;
                buf.append(" ");
            }
        }

        if (i == start + len - 2) {
            d = (data[i] & 255) << 16 | (data[i + 1] & 255) << 8;
            buf.append(pz[d >> 18 & 63]);
            buf.append(pz[d >> 12 & 63]);
            buf.append(pz[d >> 6 & 63]);
            buf.append("=");
        } else if (i == start + len - 1) {
            d = (data[i] & 255) << 16;
            buf.append(pz[d >> 18 & 63]);
            buf.append(pz[d >> 12 & 63]);
            buf.append("==");
        }

        return buf.toString();
    }
}
