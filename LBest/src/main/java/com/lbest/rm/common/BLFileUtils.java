package com.lbest.rm.common;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by dell on 2017/11/1.
 */

public class BLFileUtils {

    /**
     * 保存字节流至文件
     *
     * @param bytes 字节流
     * @param file  目标文件
     */
    public static final boolean saveBytesToFile(byte[] bytes, File file) {
        if (bytes == null) {
            return false;
        }

        ByteArrayInputStream bais = null;
        BufferedOutputStream bos = null;
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();

            bais = new ByteArrayInputStream(bytes);
            bos = new BufferedOutputStream(new FileOutputStream(file));

            int size;
            byte[] temp = new byte[1024];
            while ((size = bais.read(temp, 0, temp.length)) != -1) {
                bos.write(temp, 0, size);
            }

            bos.flush();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bos = null;
            }
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bais = null;
            }
        }
        return false;
    }


    /**
     * 将图片保存成文件
     *
     * @param bitmap   位图
     * @param savePath 保存的路径
     */
    public static final File saveBitmapToFile(Bitmap bitmap, String savePath, String fileName) {
        return saveBitmapToFile(bitmap, Bitmap.CompressFormat.PNG, 0, savePath, fileName);
    }

    public static final File saveBitmapToFile(Bitmap bitmap, Bitmap.CompressFormat format, int quality,
                                              String savePath, String fileName) {
        try {
            File f = new File(savePath + File.separator + fileName);

            //创建文件的目录
            File filePath = new File(savePath);
            if (!filePath.exists()) {
                filePath.mkdirs();
            }

            f.createNewFile();

            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            bitmap.compress(format, quality, fOut);
            try {
                fOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return f;
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    /**
     * 读取文本文件内容
     *
     * @param path 文件路径
     * @return
     */
    public static String readTextFileContent(String path) {
        BufferedReader br = null;
        try {

            File file = new File(path);
            if (!file.exists()) {
                return null;
            }

            br = new BufferedReader(new FileReader(file));
            StringBuffer result = new StringBuffer();
            String line = null;

            while ((line = br.readLine()) != null) {
                result.append(line).append("\r\n");
            }

            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                br = null;
            }
        }

        return null;
    }


    /**
     * 保存字符串到文件
     *
     * @param value
     * @param filePath
     */
    public static final void saveStringToFile(String value, String filePath) {
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file);
            fw.write(value);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 将assert 目录下的文件拷贝到sdka
     *
     * @param context    上下文
     * @param assestPath assest文件夹
     * @param trgPath    目标路径
     * @return 拷贝成功
     */
    public static boolean copyAssertFilesToSDCard(Context context, String assestPath, String trgPath) {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            File file = new File(trgPath);

            if (!file.exists()) {
                file.createNewFile();

                is = context.getResources().getAssets().open(assestPath);
                fos = new FileOutputStream(trgPath);

                byte[] buffer = new byte[1024];
                int count = 0;

                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }

                return true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭输出流
            // 关闭输入流
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fos = null;
            }

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                is = null;
            }
        }

        return false;
    }


    /**
     * 将assert 目录下的文件拷贝到sdka
     *
     * @param context    上下文
     * @param assestPath assest文件夹
     * @param trgPath    目标路径
     * @return 拷贝成功
     */
    public static void copyAssertDirToSDCard(Context context, String assetDir, String dir) {
        String[] files;
        try {
            // 获得Assets一共有几多文件
            files = context.getResources().getAssets().list(assetDir);
        } catch (IOException e1) {
            return;
        }
        File mWorkingPath = new File(dir);
        // 如果文件路径不存在
        if (!mWorkingPath.exists()) {
            // 创建文件夹
            if (!mWorkingPath.mkdirs()) {
                // 文件夹创建不成功时调用
            }
        }

        for (int i = 0; i < files.length; i++) {
            try {
                // 获得每个文件的名字
                String fileName = files[i];
                // 根据路径判断是文件夹还是文件
                if (!fileName.contains(".")) {
                    if (0 == assetDir.length()) {
                        copyAssertDirToSDCard(context,fileName, dir + fileName + "/");
                    } else {
                        copyAssertDirToSDCard(context,assetDir + "/" + fileName, dir + "/"
                                + fileName + "/");
                    }
                    continue;
                }
                File outFile = new File(mWorkingPath, fileName);
                if (outFile.exists()){
                    //outFile.delete();
                    continue;
                }
                InputStream in = null;
                if (0 != assetDir.length())
                    in = context.getAssets().open(assetDir + "/" + fileName);
                else
                    in = context.getAssets().open(fileName);
                OutputStream out = new FileOutputStream(outFile);

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                in.close();
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
